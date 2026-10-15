import React, { useState } from 'react';
import { X, FileText, Copy, Check, Hash, Award, BarChart2 } from 'lucide-react';

export default function ChunkInspectorDrawer({ chunk, onClose }) {
  const [copied, setCopied] = useState(false);

  if (!chunk) return null;

  const handleCopy = () => {
    navigator.clipboard.writeText(chunk.contentSnippet || chunk.content || '');
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="drawer-backdrop" onClick={onClose}>
      <div className="drawer-content" onClick={(e) => e.stopPropagation()}>
        {/* Drawer Header */}
        <div className="drawer-header">
          <div className="drawer-title">
            <FileText size={18} color="var(--accent-cyan)" />
            <span>Document Chunk Details</span>
          </div>
          <button 
            type="button" 
            className="icon-btn" 
            onClick={onClose}
            style={{ width: 30, height: 30 }}
          >
            <X size={16} />
          </button>
        </div>

        {/* Source metadata card */}
        <div style={{
          background: 'rgba(15, 23, 42, 0.8)',
          border: '1px solid var(--border-subtle)',
          borderRadius: 'var(--radius-md)',
          padding: '0.85rem 1rem',
          display: 'flex',
          flexDirection: 'column',
          gap: '0.5rem'
        }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>SOURCE FILE</span>
            <span style={{ 
              fontSize: '0.7rem', 
              fontFamily: 'var(--font-mono)', 
              color: 'var(--accent-cyan)', 
              background: 'rgba(56, 189, 248, 0.1)', 
              padding: '0.15rem 0.5rem', 
              borderRadius: 4 
            }}>
              {chunk.id || 'Chunk ID N/A'}
            </span>
          </div>
          <div style={{ fontWeight: 600, fontSize: '0.92rem', color: '#f8fafc' }}>
            {chunk.source || 'Automotive Manual'}
          </div>
        </div>

        {/* Metric Badges */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '0.75rem' }}>
          <div style={{
            background: 'rgba(11, 17, 32, 0.6)',
            border: '1px solid var(--border-subtle)',
            borderRadius: 'var(--radius-md)',
            padding: '0.75rem',
          }}>
            <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginBottom: 4, display: 'flex', alignItems: 'center', gap: 4 }}>
              <Hash size={12} /> INITIAL RANK
            </div>
            <div style={{ fontSize: '1.1rem', fontWeight: 700, fontFamily: 'var(--font-mono)' }}>
              #{chunk.initialRank || 1}
            </div>
            <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>
              Score: {(chunk.initialScore || 0).toFixed(3)}
            </div>
          </div>

          <div style={{
            background: 'rgba(11, 17, 32, 0.6)',
            border: '1px solid rgba(56, 189, 248, 0.3)',
            borderRadius: 'var(--radius-md)',
            padding: '0.75rem',
          }}>
            <div style={{ fontSize: '0.72rem', color: 'var(--accent-cyan)', marginBottom: 4, display: 'flex', alignItems: 'center', gap: 4 }}>
              <Award size={12} /> RERANKED RANK
            </div>
            <div style={{ fontSize: '1.1rem', fontWeight: 700, fontFamily: 'var(--font-mono)', color: 'var(--accent-cyan)' }}>
              #{chunk.rerankedRank || 1}
            </div>
            <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>
              Score: {(chunk.rerankScore || 0).toFixed(3)}
            </div>
          </div>
        </div>

        {/* Content Snippet */}
        <div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.45rem' }}>
            <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)' }}>CHUNK CONTENT TEXT</span>
            <button 
              type="button" 
              className="copy-logs-btn" 
              onClick={handleCopy}
            >
              {copied ? <Check size={11} color="#10b981" /> : <Copy size={11} />}
              {copied ? 'Copied' : 'Copy Text'}
            </button>
          </div>
          <div className="drawer-chunk-body">
            {chunk.contentSnippet || chunk.content || 'No text snippet available'}
          </div>
        </div>
      </div>
    </div>
  );
}
