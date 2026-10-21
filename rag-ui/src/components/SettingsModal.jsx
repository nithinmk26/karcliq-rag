import React, { useState, useEffect } from 'react';
import { X, Sliders, Check } from 'lucide-react';

export default function SettingsModal({ 
  isOpen, 
  onClose, 
  settings, 
  onSave 
}) {
  const [localSettings, setLocalSettings] = useState(settings || {
    topK: 5,
    initialPoolSize: 20,
    rerankStrategy: 'CROSS_ENCODER'
  });

  useEffect(() => {
    if (settings) {
      setLocalSettings(settings);
    }
  }, [settings, isOpen]);

  if (!isOpen) return null;

  const handleApply = () => {
    onSave(localSettings);
    onClose();
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
          animation: 'fade-in 0.2s ease',
          maxHeight: '90vh',
          overflowY: 'auto'
        }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
            <Sliders size={18} color="var(--accent-cyan)" />
            <h3 style={{ fontSize: '1.15rem', fontWeight: 600, color: 'var(--text-primary)' }}>
              Pipeline Parameters
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
          Tune two-stage retrieval depth, candidate pool size, and select between neural Cross-Encoder and algorithmic hybrid scoring for Mercedes-Benz diagnostic manuals.
        </p>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          {/* Top K */}
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.4rem' }}>
              <label style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', fontWeight: 500 }}>
                Top-K Context Chunks (Injected to Prompt)
              </label>
              <span style={{ fontFamily: 'var(--font-mono)', color: 'var(--accent-cyan)', fontWeight: 600, fontSize: '0.9rem' }}>
                {localSettings.topK}
              </span>
            </div>
            <input 
              type="range" 
              min={1} 
              max={8} 
              value={localSettings.topK}
              onChange={(e) => setLocalSettings({ ...localSettings, topK: Number(e.target.value) })}
              style={{ width: '100%', accentColor: 'var(--accent-cyan)' }}
            />
            <p style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginTop: 4 }}>
              Number of top reranked chunks injected into OpenAI gpt-4o-mini context window.
            </p>
          </div>

          {/* Initial Pool Size */}
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.4rem' }}>
              <label style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', fontWeight: 500 }}>
                Stage 1 Candidate Pool Size (PGVector)
              </label>
              <span style={{ fontFamily: 'var(--font-mono)', color: 'var(--accent-cyan)', fontWeight: 600, fontSize: '0.9rem' }}>
                {localSettings.initialPoolSize}
              </span>
            </div>
            <input 
              type="range" 
              min={4} 
              max={25} 
              value={localSettings.initialPoolSize}
              onChange={(e) => setLocalSettings({ ...localSettings, initialPoolSize: Number(e.target.value) })}
              style={{ width: '100%', accentColor: 'var(--accent-cyan)' }}
            />
            <p style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginTop: 4 }}>
              Initial broad candidate pool size retrieved from PGVector prior to reranking.
            </p>
          </div>

          {/* V2 Reranker Strategy */}
          <div>
            <label style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', fontWeight: 500, marginBottom: '0.5rem', display: 'block' }}>
              V2 Reranker Engine
            </label>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.6rem', marginBottom: '0.75rem' }}>
              <button
                type="button"
                onClick={() => setLocalSettings({ ...localSettings, rerankStrategy: 'CROSS_ENCODER' })}
                style={{
                  padding: '0.65rem 0.75rem',
                  borderRadius: 'var(--radius-md)',
                  border: (localSettings.rerankStrategy || 'CROSS_ENCODER') === 'CROSS_ENCODER' ? '1px solid #38bdf8' : '1px solid var(--border-subtle)',
                  background: (localSettings.rerankStrategy || 'CROSS_ENCODER') === 'CROSS_ENCODER' ? 'rgba(56, 189, 248, 0.14)' : 'rgba(255, 255, 255, 0.02)',
                  color: (localSettings.rerankStrategy || 'CROSS_ENCODER') === 'CROSS_ENCODER' ? '#38bdf8' : 'var(--text-secondary)',
                  cursor: 'pointer',
                  textAlign: 'left',
                  transition: 'all 0.15s ease'
                }}
              >
                <div style={{ fontWeight: 600, fontSize: '0.82rem', display: 'flex', alignItems: 'center', gap: 4 }}>
                  <span>⚡ Cross-Encoder</span>
                </div>
                <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)', marginTop: 3, lineHeight: 1.3 }}>
                  Deep neural cross-attention (Joint Query ⨁ Passage scoring)
                </div>
              </button>

              <button
                type="button"
                onClick={() => setLocalSettings({ ...localSettings, rerankStrategy: 'HYBRID' })}
                style={{
                  padding: '0.65rem 0.75rem',
                  borderRadius: 'var(--radius-md)',
                  border: localSettings.rerankStrategy === 'HYBRID' ? '1px solid #34d399' : '1px solid var(--border-subtle)',
                  background: localSettings.rerankStrategy === 'HYBRID' ? 'rgba(52, 211, 153, 0.14)' : 'rgba(255, 255, 255, 0.02)',
                  color: localSettings.rerankStrategy === 'HYBRID' ? '#34d399' : 'var(--text-secondary)',
                  cursor: 'pointer',
                  textAlign: 'left',
                  transition: 'all 0.15s ease'
                }}
              >
                <div style={{ fontWeight: 600, fontSize: '0.82rem', display: 'flex', alignItems: 'center', gap: 4 }}>
                  <span>🔀 Hybrid Search</span>
                </div>
                <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)', marginTop: 3, lineHeight: 1.3 }}>
                  Vector Cosine + Lexical Overlap + MMR Source Diversity
                </div>
              </button>
            </div>

            <div style={{ 
              background: 'rgba(11, 17, 32, 0.7)', 
              borderRadius: 'var(--radius-md)', 
              padding: '0.75rem', 
              fontSize: '0.73rem',
              color: 'var(--text-secondary)',
              border: '1px solid var(--border-subtle)',
              lineHeight: 1.5
            }}>
              {(localSettings.rerankStrategy || 'CROSS_ENCODER') === 'CROSS_ENCODER' ? (
                <>
                  <div style={{ color: '#38bdf8', fontWeight: 600, marginBottom: 2 }}>Neural Cross-Encoder Mode Active:</div>
                  <div>• Jointly attends to query and document tokens in deep neural layers.</div>
                  <div>• Heavily penalizes wrong-platform false positives (C-Class vs S-Class).</div>
                  <div>• Prioritizes exact wiring pinouts and component tolerances.</div>
                </>
              ) : (
                <>
                  <div style={{ color: '#34d399', fontWeight: 600, marginBottom: 2 }}>Hybrid Scoring Mode Active:</div>
                  <div>• <strong>50%</strong> Vector Cosine Similarity (PGVector)</div>
                  <div>• <strong>40%</strong> Lexical Keyword Overlap (with stemming)</div>
                  <div>• <strong>10%</strong> Source Diversity Balancing (MMR-style)</div>
                </>
              )}
            </div>
          </div>
        </div>

        {/* Action Buttons */}
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1.75rem' }}>
          <button 
            type="button" 
            onClick={onClose}
            style={{
              padding: '0.55rem 1rem',
              borderRadius: 'var(--radius-sm)',
              border: '1px solid var(--border-subtle)',
              background: 'transparent',
              color: 'var(--text-secondary)',
              cursor: 'pointer',
              fontSize: '0.85rem'
            }}
          >
            Cancel
          </button>
          <button 
            type="button" 
            onClick={handleApply}
            style={{
              padding: '0.55rem 1.25rem',
              borderRadius: 'var(--radius-sm)',
              border: 'none',
              background: 'var(--accent)',
              color: '#000',
              fontWeight: 600,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: 6,
              fontSize: '0.85rem'
            }}
          >
            <Check size={15} />
            Apply Settings
          </button>
        </div>
      </div>
    </div>
  );
}
