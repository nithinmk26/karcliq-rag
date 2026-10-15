import React from 'react';
import { X, Sliders, Check } from 'lucide-react';

export default function SettingsModal({ 
  isOpen, 
  onClose, 
  settings, 
  onSave 
}) {
  const [localSettings, setLocalSettings] = React.useState(settings);

  if (!isOpen) return null;

  const handleApply = () => {
    onSave(localSettings);
    onClose();
  };

  return (
    <div className="drawer-backdrop" onClick={onClose}>
      <div 
        style={{
          background: 'var(--bg-surface)',
          border: '1px solid var(--border-medium)',
          borderRadius: 'var(--radius-xl)',
          padding: '1.75rem',
          maxWidth: '480px',
          width: '92%',
          margin: 'auto',
          boxShadow: 'var(--shadow-lg)',
          animation: 'fade-in 0.2s ease'
        }}
        onClick={(e) => e.stopPropagation()}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
            <Sliders size={18} color="var(--accent-cyan)" />
            <h3 style={{ fontSize: '1.15rem' }}>Pipeline Parameters</h3>
          </div>
          <button type="button" className="icon-btn" onClick={onClose} style={{ width: 30, height: 30 }}>
            <X size={16} />
          </button>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.4rem' }}>
              <label className="form-label">Top-K Context Chunks</label>
              <span style={{ fontFamily: 'var(--font-mono)', color: 'var(--accent-cyan)', fontWeight: 600 }}>
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

          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.4rem' }}>
              <label className="form-label">Stage 1 Candidate Pool Size</label>
              <span style={{ fontFamily: 'var(--font-mono)', color: 'var(--accent-cyan)', fontWeight: 600 }}>
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
              Initial broad candidate pool size retrieved from PGVector prior to hybrid reranking.
            </p>
          </div>

          <div>
            <label className="form-label" style={{ marginBottom: '0.4rem', display: 'block' }}>Reranking Weights</label>
            <div style={{ 
              background: 'rgba(11, 17, 32, 0.7)', 
              borderRadius: 'var(--radius-md)', 
              padding: '0.75rem', 
              fontSize: '0.75rem',
              color: 'var(--text-secondary)',
              border: '1px solid var(--border-subtle)',
              lineHeight: 1.6
            }}>
              <div>• <strong>55%</strong> Vector Cosine Similarity</div>
              <div>• <strong>35%</strong> Lexical Keyword Overlap</div>
              <div>• <strong>10%</strong> Source Diversity Balancing (Mercedes C-Class vs S-Class)</div>
            </div>
          </div>
        </div>

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1.75rem' }}>
          <button 
            type="button" 
            className="icon-btn" 
            onClick={onClose}
            style={{ width: 'auto', padding: '0 1rem' }}
          >
            Cancel
          </button>
          <button 
            type="button" 
            className="primary-action-btn"
            onClick={handleApply}
          >
            <Check size={14} />
            Apply Settings
          </button>
        </div>
      </div>
    </div>
  );
}
