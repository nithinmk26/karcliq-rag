package com.karcliq.rag.service;

import com.karcliq.rag.model.ChunkingStrategy;
import com.karcliq.rag.model.PdfProcessingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Service to load, parse, chunk PDF documents and save embeddings into PGVector.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfDocumentService {

    private final VectorStore vectorStore;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    public PdfProcessingResponse processPdfFolder(String folderPath, ChunkingStrategy strategy, int chunkSize, int chunkOverlap, boolean clearExisting) {
        log.info("Processing PDFs from folder: '{}' | Strategy: {} | ChunkSize: {} | Overlap: {} | ClearExisting: {}",
                folderPath, strategy, chunkSize, chunkOverlap, clearExisting);

        if (clearExisting && jdbcTemplate != null) {
            try {
                log.info("Clearing existing vector_store table before new ingestion...");
                jdbcTemplate.execute("TRUNCATE TABLE vector_store;");
            } catch (Exception e) {
                log.warn("Could not truncate vector_store: {}", e.getMessage());
            }
        }

        File dir = new File(folderPath);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new IllegalArgumentException("Invalid directory path: " + folderPath);
        }

        File[] pdfFiles = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".pdf"));
        if (pdfFiles == null || pdfFiles.length == 0) {
            log.warn("No PDF files found in: {}", folderPath);
            return new PdfProcessingResponse(0, 0, strategy, "No PDF files found in directory");
        }

        List<Document> allChunks = new ArrayList<>();

        for (File file : pdfFiles) {
            try {
                List<Document> fileChunks = processSinglePdf(file, strategy, chunkSize, chunkOverlap);
                allChunks.addAll(fileChunks);
                log.info("Parsed '{}': generated {} chunks", file.getName(), fileChunks.size());
            } catch (Exception e) {
                log.error("Failed to parse '{}': {}", file.getName(), e.getMessage());
            }
        }

        // Print details of each chunked Document model to console
        printChunksToConsole(allChunks, strategy);

        // Generate embeddings and save to PGVector database
        boolean savedToDb = false;
        String dbMessage = "";
        try {
            log.info("Generating embeddings and saving {} chunks to PGVector...", allChunks.size());
            vectorStore.add(allChunks);
            savedToDb = true;
            dbMessage = " Embeddings generated and saved to PGVector database.";
            log.info("Successfully saved {} chunks to PGVector database!", allChunks.size());
        } catch (Exception e) {
            log.warn("Could not save to PGVector (OpenAI token might be missing or invalid): {}", e.getMessage());
            dbMessage = " (Note: Could not save to PGVector yet: " + e.getMessage() + ")";
        }

        return new PdfProcessingResponse(
                pdfFiles.length,
                allChunks.size(),
                strategy,
                String.format("Processed %d PDFs into %d chunks using %s.%s",
                        pdfFiles.length, allChunks.size(), strategy, dbMessage)
        );
    }

    private List<Document> processSinglePdf(File file, ChunkingStrategy strategy, int chunkSize, int chunkOverlap) throws IOException {
        try (PDDocument pdDocument = Loader.loadPDF(file)) {
            PDFTextStripper textStripper = new PDFTextStripper();
            String fullText = textStripper.getText(pdDocument);

            Map<String, Object> baseMetadata = new HashMap<>();
            baseMetadata.put("source", file.getName());
            baseMetadata.put("filePath", file.getAbsolutePath());
            baseMetadata.put("totalPages", pdDocument.getNumberOfPages());
            baseMetadata.put("strategy", strategy.name());

            String docHeader = String.format("[Manual: %s]\n", file.getName().replace(".pdf", "").replace("_", " "));

            return switch (strategy) {
                case FIXED_SIZE_WITH_OVERLAP -> chunkFixedSizeWithOverlap(fullText, baseMetadata, chunkSize, chunkOverlap, docHeader);
                case SLIDING_WINDOW -> chunkSlidingWindow(fullText, baseMetadata, 150, 75, docHeader);
                case RECURSIVE -> chunkRecursive(fullText, baseMetadata, chunkSize, chunkOverlap, docHeader);
                case SECTION_BASED -> chunkSectionBased(fullText, baseMetadata, docHeader);
            };
        }
    }

    /**
     * 1. Fixed-size chunks with overlap (characters).
     */
    private List<Document> chunkFixedSizeWithOverlap(String text, Map<String, Object> baseMetadata, int chunkSize, int overlap, String docHeader) {
        List<Document> chunks = new ArrayList<>();
        int length = text.length();
        int start = 0;
        int chunkIdx = 0;

        while (start < length) {
            int end = Math.min(start + chunkSize, length);
            String chunkContent = text.substring(start, end).trim();
            if (!chunkContent.isEmpty()) {
                Map<String, Object> meta = new HashMap<>(baseMetadata);
                meta.put("chunkIndex", chunkIdx++);
                meta.put("chunkSize", chunkSize);
                meta.put("chunkOverlap", overlap);
                chunks.add(new Document(docHeader + chunkContent, meta));
            }
            if (end >= length) break;
            start += (chunkSize - overlap);
        }
        return chunks;
    }

    /**
     * 2. Sliding-window chunks (word-based window advancing by step size).
     */
    private List<Document> chunkSlidingWindow(String text, Map<String, Object> baseMetadata, int windowWords, int stepWords, String docHeader) {
        List<Document> chunks = new ArrayList<>();
        String[] words = text.split("\\s+");
        int chunkIdx = 0;

        for (int start = 0; start < words.length; start += stepWords) {
            int end = Math.min(start + windowWords, words.length);
            String chunkContent = String.join(" ", Arrays.copyOfRange(words, start, end)).trim();
            if (!chunkContent.isEmpty()) {
                Map<String, Object> meta = new HashMap<>(baseMetadata);
                meta.put("chunkIndex", chunkIdx++);
                meta.put("strategy", "SLIDING_WINDOW");
                chunks.add(new Document(docHeader + chunkContent, meta));
            }
            if (end >= words.length) break;
        }
        return chunks;
    }

    /**
     * 3. Recursive Character Text Splitter (LangChain/RAG course implementation)
     */
    private List<Document> chunkRecursive(String text, Map<String, Object> baseMetadata, int chunkSize, int chunkOverlap, String docHeader) {
        List<String> splits = splitText(text, List.of("\n\n", "\n", " ", ""), chunkSize);
        List<String> mergedChunks = mergeSplits(splits, chunkSize, chunkOverlap);
        List<Document> documents = new ArrayList<>();
        int chunkIdx = 0;

        for (String chunkText : mergedChunks) {
            String trimmed = chunkText.trim();
            if (!trimmed.isEmpty()) {
                Map<String, Object> meta = new HashMap<>(baseMetadata);
                meta.put("chunkIndex", chunkIdx++);
                meta.put("chunkSize", chunkSize);
                meta.put("chunkOverlap", chunkOverlap);
                documents.add(new Document(docHeader + trimmed, meta));
            }
        }
        return documents;
    }

    private List<String> splitText(String text, List<String> separators, int chunkSize) {
        List<String> finalChunks = new ArrayList<>();
        String separator = separators.isEmpty() ? "" : separators.get(0);
        List<String> remainingSeparators = separators.size() > 1 ? separators.subList(1, separators.size()) : Collections.emptyList();

        String[] splits = separator.isEmpty() ? text.split("") : text.split(Pattern.quote(separator));

        for (String s : splits) {
            if (s.length() > chunkSize && !remainingSeparators.isEmpty()) {
                finalChunks.addAll(splitText(s, remainingSeparators, chunkSize));
            } else if (!s.trim().isEmpty()) {
                finalChunks.add(s.trim());
            }
        }
        return finalChunks;
    }

    private List<String> mergeSplits(List<String> splits, int chunkSize, int chunkOverlap) {
        List<String> docs = new ArrayList<>();
        List<String> currentDoc = new ArrayList<>();
        int totalLen = 0;

        for (String split : splits) {
            int len = split.length();
            if (totalLen + len > chunkSize && !currentDoc.isEmpty()) {
                docs.add(String.join("\n", currentDoc));

                while (totalLen > chunkOverlap && !currentDoc.isEmpty()) {
                    totalLen -= currentDoc.remove(0).length();
                }
            }
            currentDoc.add(split);
            totalLen += len;
        }
        if (!currentDoc.isEmpty()) {
            docs.add(String.join("\n", currentDoc));
        }
        return docs;
    }

    /**
     * 4. Section-based chunks (splits by diagnostic/technical section headings).
     */
    private List<Document> chunkSectionBased(String text, Map<String, Object> baseMetadata, String docHeader) {
        Pattern sectionPattern = Pattern.compile("(?m)^(?=[A-Z][A-Za-z0-9\\s]{2,30}:|^[0-9]+\\.\\s+[A-Z])");
        String[] sections = sectionPattern.split(text);
        List<Document> chunks = new ArrayList<>();
        int chunkIdx = 0;

        for (String section : sections) {
            String trimmed = section.trim();
            if (!trimmed.isEmpty()) {
                Map<String, Object> meta = new HashMap<>(baseMetadata);
                meta.put("chunkIndex", chunkIdx++);
                meta.put("strategy", "SECTION_BASED");
                chunks.add(new Document(docHeader + trimmed, meta));
            }
        }
        if (chunks.isEmpty() && !text.isBlank()) {
            chunks.add(new Document(docHeader + text.trim(), baseMetadata));
        }
        return chunks;
    }

    private void printChunksToConsole(List<Document> documents, ChunkingStrategy strategy) {
        System.out.println("\n" + "=".repeat(100));
        System.out.printf(">>> CHUNKED DOCUMENT DETAILS [Strategy: %s | Total Chunks: %d] <<<%n",
                strategy, documents.size());
        System.out.println("=".repeat(100));

        int i = 1;
        for (Document doc : documents) {
            System.out.printf("%n[CHUNK #%d of %d]%n", i++, documents.size());
            System.out.printf("ID         : %s%n", doc.getId());
            System.out.printf("Char Length: %d%n", doc.getText() != null ? doc.getText().length() : 0);
            System.out.println("METADATA   :");
            doc.getMetadata().forEach((k, v) -> System.out.printf("   - %-14s: %s%n", k, v));
            System.out.println("PAGE CONTENT PREVIEW:");
            String text = doc.getText() != null ? doc.getText() : "";
            System.out.println(text.length() > 250 ? text.substring(0, 250) + "... [TRUNCATED]" : text);
            System.out.println("-".repeat(80));
        }
        System.out.println("=".repeat(100) + "\n");
    }
}
