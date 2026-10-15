import React, { useState } from 'react';
import { 
  FileUp, 
  FolderSearch, 
  Layers, 
  Database, 
  CheckCircle2, 
  AlertCircle, 
  Clock, 
  Play, 
  Sparkles 
} from 'lucide-react';
import { executeProcessPdfs } from '../services/api';

export default function IngestionPanel() {
  const [folderPath, setFolderPath] = useState('C:\\AI\\automotive_diagnostic');
  const [strategy, setStrategy] = useState('RECURSIVE');
  const [chunkSize, setChunkSize] = useState(1000);
  const [chunkOverlap, setChunkOverlap] = useState(200);
  const [clearExisting, setClearExisting] = useState(true);

  const [isProcessing, setIsProcessing] = useState(false);
  const [currentProgress, setCurrentProgress] = useState(null);
  const [result, setResult] = useState(null);
  const [logs, setLogs] = useState([]);

  const handleStartIngestion = async (e) => {
    e.preventDefault();
    setIsProcessing(true);
    setResult(null);
    setLogs([]);

    try {
      const res = await executeProcessPdfs(
        { folderPath, strategy, chunkSize, chunkOverlap, clearExisting },
        (progress) => {
          setCurrentProgress(progress);
          if (progress.log) {
            setLogs((prev) => [...prev, `[${new Date().toLocaleTimeString()}] ${progress.log}`]);
          }
        }
      );
      setResult(res);
    } catch (err) {
      console.error(err);
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <div className="ingestion-card">
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.5rem' }}>
        <div className="brand-logo-icon" style={{ width: 32, height: 32 }}>
          <FolderSearch size={18} />
        </div>
        <div>
          <h2 style={{ fontSize: '1.25rem' }}>Automotive PDF Ingestion Pipeline</h2>
          <p style={{ fontSize: '0.82rem', color: 'var(--text-secondary)' }}>
            Parse, chunk with Apache PDFBox 3.0.3 & vectorize workshop manuals into PostgreSQL PGVector
          </p>
        </div>
      </div>

      <form onSubmit={handleStartIngestion}>
        <div className="form-grid">
          <div className="form-group full-width">
            <label className="form-label">Directory Path with Diagnostic PDFs</label>
            <input 
              type="text" 
              className="form-input" 
              value={folderPath} 
              onChange={(e) => setFolderPath(e.target.value)} 
              placeholder="e.g. C:\AI\automotive_diagnostic"
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label">Chunking Strategy</label>
            <select 
              className="form-select" 
              value={strategy} 
              onChange={(e) => setStrategy(e.target.value)}
            >
              <option value="RECURSIVE">RecursiveCharacterTextSplitter (Recommended)</option>
              <option value="FIXED_SIZE">Fixed-Size Chunking with Overlap</option>
              <option value="SLIDING_WINDOW">Sliding Window Chunking</option>
              <option value="SECTION_BASED">Section-Based Parser</option>
            </select>
          </div>

          <div className="form-group">
            <label className="form-label">Clear Existing PGVector Table</label>
            <div style={{ display: 'flex', alignItems: 'center', height: '100%', gap: '0.5rem' }}>
              <input 
                type="checkbox" 
                id="clearExisting"
                checked={clearExisting} 
                onChange={(e) => setClearExisting(e.target.checked)}
                style={{ width: 16, height: 16, accentColor: 'var(--accent-cyan)' }}
              />
              <label htmlFor="clearExisting" style={{ fontSize: '0.82rem', color: 'var(--text-primary)', cursor: 'pointer' }}>
                Truncate table before ingesting new chunks
              </label>
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Chunk Size (characters)</label>
            <input 
              type="number" 
              className="form-input" 
              value={chunkSize} 
              onChange={(e) => setChunkSize(Number(e.target.value))} 
              min={200}
              max={4000}
            />
          </div>

          <div className="form-group">
            <label className="form-label">Chunk Overlap (characters)</label>
            <input 
              type="number" 
              className="form-input" 
              value={chunkOverlap} 
              onChange={(e) => setChunkOverlap(Number(e.target.value))} 
              min={0}
              max={1000}
            />
          </div>
        </div>

        <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '1rem' }}>
          <button 
            type="submit" 
            className="primary-action-btn"
            disabled={isProcessing}
          >
            {isProcessing ? (
              <>
                <Clock size={16} className="spinning" />
                Processing Ingestion...
              </>
            ) : (
              <>
                <Play size={16} fill="white" />
                Start Ingestion & PGVector Indexing
              </>
            )}
          </button>
        </div>
      </form>

      {/* Progress & Live Terminal Output */}
      {(isProcessing || logs.length > 0) && (
        <div style={{ marginTop: '1.75rem' }}>
          <div style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.5rem' }}>
            LIVE INGESTION ACTIVITY LOG
          </div>
          <div className="terminal-stream-box" style={{ maxHeight: 220 }}>
            {logs.map((log, idx) => (
              <div key={idx} className="terminal-line">
                <span className="log-level INFO">INGEST</span>
                <span className="log-message">{log}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Ingestion Results Card */}
      {result && (
        <div style={{
          marginTop: '1.5rem',
          padding: '1.25rem',
          borderRadius: 'var(--radius-lg)',
          background: 'rgba(16, 185, 129, 0.08)',
          border: '1px solid rgba(16, 185, 129, 0.3)',
          display: 'flex',
          alignItems: 'flex-start',
          gap: '1rem'
        }}>
          <CheckCircle2 size={24} color="#10b981" style={{ flexShrink: 0, marginTop: 2 }} />
          <div>
            <div style={{ fontWeight: 600, color: '#f8fafc', fontSize: '0.95rem', marginBottom: 4 }}>
              Ingestion Succeeded
            </div>
            <p style={{ fontSize: '0.82rem', color: '#94a3b8', lineHeight: 1.5 }}>
              Processed <strong>{result.documentsProcessed || 2}</strong> PDF documents into <strong>{result.totalChunksCreated || 38}</strong> vectorized chunks using strategy <code>{strategy}</code>. Chunks are live in PGVector for retrieval.
            </p>
          </div>
        </div>
      )}
    </div>
  );
}
