package com.karcliq.rag.controller;

import com.karcliq.rag.model.ChunkingStrategy;
import com.karcliq.rag.model.PdfProcessingResponse;
import com.karcliq.rag.service.PdfDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "Document Processing", description = "Endpoints for reading, parsing, and chunking diagnostic PDFs")
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class PdfDocumentController {

    private final PdfDocumentService pdfDocumentService;

    @Operation(summary = "Read, parse and chunk automotive diagnostic PDFs")
    @PostMapping("/process-pdfs")
    public ResponseEntity<PdfProcessingResponse> processPdfs(
            @Parameter(description = "Directory path containing PDF files")
            @RequestParam(defaultValue = "C:\\AI\\automotive_diagnostic", required = false) String folderPath,

            @Parameter(description = "Chunking strategy to apply")
            @RequestParam(defaultValue = "RECURSIVE", required = false) ChunkingStrategy strategy,

            @Parameter(description = "Target chunk size in characters (course default: 1000)")
            @RequestParam(defaultValue = "1000", required = false) int chunkSize,

            @Parameter(description = "Chunk overlap in characters to preserve semantic context (course default: 200)")
            @RequestParam(defaultValue = "200", required = false) int chunkOverlap,

            @Parameter(description = "Clear previous chunks in PGVector table before storing new ones")
            @RequestParam(defaultValue = "false", required = false) boolean clearExisting
    ) {
        log.info("Request to process PDFs. Folder: '{}', Strategy: {}, ChunkSize: {}, Overlap: {}, ClearExisting: {}",
                folderPath, strategy, chunkSize, chunkOverlap, clearExisting);
        PdfProcessingResponse response = pdfDocumentService.processPdfFolder(folderPath, strategy, chunkSize, chunkOverlap, clearExisting);
        return ResponseEntity.ok(response);
    }
}
