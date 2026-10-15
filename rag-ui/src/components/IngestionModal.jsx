import React, { useState } from 'react';
import { X, Database, Play, Clock, CheckCircle2 } from 'lucide-react';
import { executeProcessPdfs } from '../services/api';

export default function IngestionModal({ isOpen, onClose }) {
  const [folderPath, setFolderPath] = useState('C:\\AI\\automotive_diagnostic');
  const [strategy, setStrategy] = useState('RECURSIVE');
  const [chunkSize, setChunkSize] = useState(1000);
  const [chunkOverlap, setChunkOverlap] = useState(200);
  const [clearExisting, setClearExisting] = useState(false);

  const [isProcessing, setIsProcessing] = useState(false);
  const [currentStep, setCurrentStep] = useState(null);
  const [result, setResult] = useState(null);

  if (!isOpen) return null;

  const handleStartIngest = async (e) => {
    e.preventDefault();
    setIsProcessing(true);
    setResult(null);
    setCurrentStep({ label: 'Connecting to backend ingestion service...' });

    try {
      const res = await executeProcessPdfs(
        { folderPath, strategy, chunkSize, chunkOverlap, clearExisting },
        (stepInfo) => {
          setCurrentStep(stepInfo);
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
    <div 
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.75)',
        backdropFilter: 'blur(4px)',
        zIndex: 100,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '1rem'
      }}
      onClick={onClose}
    >
      <div 
        style={{
          background: 'var(--bg-surface)',
          border: '1px solid var(--border-subtle)',
          borderRadius: 'var(--radius-lg)',
          maxWidth: '520px',
          width: '100%',
          padding: '1.75rem',
          boxShadow: '0 20px 40px rgba(0,0,0,0.5)',
          animation: 'fade-in 0.2s ease'
        }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
            <Database size={18} color="var(--accent)" />
            <h3 style={{ fontSize: '1.15rem', fontWeight: 600, color: 'var(--text-primary)' }}>
              Data Ingestion API
            </h3>
          </div>
          <button 
            type="button" 
            onClick={onClose}
            style={{
              background: 'transparent',
              border: 'none',
              color: 'var(--text-muted)',
              cursor: 'pointer',
              display: 'flex',
              padding: 4
            }}
          >
            <X size={18} />
          </button>
        </div>

        <p style={{ fontSize: '0.82rem', color: 'var(--text-secondary)', marginBottom: '1.25rem', lineHeight: 1.5 }}>
          Parses Mercedes-Benz automotive diagnostic PDFs (C-Class & S-Class) via Apache PDFBox, splits technical manuals using Spring AI text splitters, and commits 1536-dim OpenAI embeddings to PostgreSQL PGVector.
        </p>

        <form onSubmit={handleStartIngest}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            {/* Folder path */}
            <div>
              <label style={{ fontSize: '0.78rem', color: 'var(--text-muted)', display: 'block', marginBottom: 4 }}>
                Diagnostic PDFs Directory
              </label>
              <input 
                type="text" 
                value={folderPath} 
                onChange={(e) => setFolderPath(e.target.value)}
                style={{
                  width: '100%',
                  background: 'var(--bg-main)',
                  border: '1px solid var(--border-subtle)',
                  borderRadius: 'var(--radius-sm)',
                  padding: '0.55rem 0.75rem',
                  color: 'var(--text-primary)',
                  fontSize: '0.86rem',
                  outline: 'none'
                }}
                required
              />
              <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.45rem' }}>
                <button
                  type="button"
                  onClick={() => setFolderPath('C:\\AI\\automotive_diagnostic')}
                  style={{
                    fontSize: '0.72rem',
                    padding: '0.22rem 0.55rem',
                    borderRadius: '4px',
                    border: '1px solid var(--border-subtle)',
                    background: 'rgba(56, 189, 248, 0.15)',
                    color: '#38bdf8',
                    cursor: 'pointer'
                  }}
                >
                  🚗 Mercedes-Benz Diagnostic PDFs (C-Class & S-Class)
                </button>
              </div>
            </div>

            {/* Strategy */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
              <div>
                <label style={{ fontSize: '0.78rem', color: 'var(--text-muted)', display: 'block', marginBottom: 4 }}>
                  Chunking Strategy
                </label>
                <select 
                  value={strategy} 
                  onChange={(e) => setStrategy(e.target.value)}
                  style={{
                    width: '100%',
                    background: 'var(--bg-main)',
                    border: '1px solid var(--border-subtle)',
                    borderRadius: 'var(--radius-sm)',
                    padding: '0.55rem 0.75rem',
                    color: 'var(--text-primary)',
                    fontSize: '0.84rem',
                    outline: 'none'
                  }}
                >
                  <option value="RECURSIVE">RecursiveCharacter</option>
                  <option value="FIXED_SIZE">Fixed-Size</option>
                  <option value="SLIDING_WINDOW">Sliding Window</option>
                  <option value="SECTION_BASED">Section-Based</option>
                </select>
              </div>

              <div>
                <label style={{ fontSize: '0.78rem', color: 'var(--text-muted)', display: 'block', marginBottom: 4 }}>
                  Chunk Size / Overlap
                </label>
                <div style={{ display: 'flex', gap: 6 }}>
                  <input 
                    type="number" 
                    value={chunkSize} 
                    onChange={(e) => setChunkSize(Number(e.target.value))}
                    style={{
                      width: '50%',
                      background: 'var(--bg-main)',
                      border: '1px solid var(--border-subtle)',
                      borderRadius: 'var(--radius-sm)',
                      padding: '0.55rem 0.5rem',
                      color: 'var(--text-primary)',
                      fontSize: '0.84rem'
                    }}
                    title="Chunk Size"
                  />
                  <input 
                    type="number" 
                    value={chunkOverlap} 
                    onChange={(e) => setChunkOverlap(Number(e.target.value))}
                    style={{
                      width: '50%',
                      background: 'var(--bg-main)',
                      border: '1px solid var(--border-subtle)',
                      borderRadius: 'var(--radius-sm)',
                      padding: '0.55rem 0.5rem',
                      color: 'var(--text-primary)',
                      fontSize: '0.84rem'
                    }}
                    title="Chunk Overlap"
                  />
                </div>
              </div>
            </div>

            {/* Clear existing */}
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 4 }}>
              <input 
                type="checkbox" 
                id="clearPdfs"
                checked={clearExisting} 
                onChange={(e) => setClearExisting(e.target.checked)}
                style={{ accentColor: 'var(--accent)' }}
              />
              <label htmlFor="clearPdfs" style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', cursor: 'pointer' }}>
                Clear existing PGVector table before storing
              </label>
            </div>
          </div>

          {/* Action buttons */}
          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.6rem', marginTop: '1.5rem' }}>
            <button 
              type="button" 
              onClick={onClose}
              style={{
                background: 'transparent',
                border: '1px solid var(--border-subtle)',
                color: 'var(--text-secondary)',
                borderRadius: 'var(--radius-full)',
                padding: '0.45rem 1rem',
                fontSize: '0.82rem',
                cursor: 'pointer'
              }}
            >
              Cancel
            </button>
            <button 
              type="submit" 
              disabled={isProcessing}
              style={{
                background: 'var(--text-primary)',
                border: 'none',
                color: 'var(--bg-main)',
                borderRadius: 'var(--radius-full)',
                padding: '0.45rem 1.15rem',
                fontSize: '0.82rem',
                fontWeight: 600,
                cursor: isProcessing ? 'not-allowed' : 'pointer',
                display: 'inline-flex',
                alignItems: 'center',
                gap: 6,
                opacity: isProcessing ? 0.7 : 1
              }}
            >
              {isProcessing ? (
                <>
                  <Clock size={13} className="spinning" />
                  <span>Ingesting...</span>
                </>
              ) : (
                <>
                  <Play size={13} fill="currentColor" />
                  <span>Run Ingestion</span>
                </>
              )}
            </button>
          </div>
        </form>

        {/* Live step progress */}
        {isProcessing && currentStep && (
          <div style={{
            marginTop: '1.25rem',
            padding: '0.75rem 0.95rem',
            borderRadius: 'var(--radius-sm)',
            background: 'var(--bg-main)',
            border: '1px solid var(--border-subtle)',
            fontSize: '0.78rem',
            color: 'var(--accent)',
            display: 'flex',
            alignItems: 'center',
            gap: 8
          }}>
            <Clock size={14} className="spinning" />
            <span>{currentStep.label || currentStep.log}</span>
          </div>
        )}

        {/* Success result */}
        {result && (
          <div style={{
            marginTop: '1.25rem',
            padding: '0.85rem 1rem',
            borderRadius: 'var(--radius-sm)',
            background: 'rgba(109, 213, 140, 0.1)',
            border: '1px solid rgba(109, 213, 140, 0.3)',
            display: 'flex',
            alignItems: 'flex-start',
            gap: 8
          }}>
            <CheckCircle2 size={16} color="var(--color-success)" style={{ marginTop: 2, flexShrink: 0 }} />
            <div style={{ fontSize: '0.82rem', color: '#e3e3e3', lineHeight: 1.45 }}>
              <strong>Ingestion Succeeded:</strong> {result.message}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
