import React from 'react';
import { 
  Zap, 
  GitCompare, 
  Search, 
  Database, 
  Sliders, 
  Activity,
  Cpu
} from 'lucide-react';

export default function Header({ 
  currentTab, 
  onSelectTab, 
  backendStatus, 
  onToggleSimulation, 
  onOpenSettings 
}) {
  const isOnline = backendStatus?.online;

  return (
    <header className="app-header">
      {/* Brand Logo & Name */}
      <div className="brand-section">
        <div className="brand-logo-icon">
          <Zap size={20} />
        </div>
        <div>
          <div className="brand-title">
            <span>Karcliq</span>
            <span style={{ fontWeight: 400, color: 'var(--accent-cyan)' }}>RAG</span>
            <span className="brand-badge">v2.0 Hybrid</span>
          </div>
        </div>
      </div>

      {/* Navigation Modes */}
      <nav className="nav-modes">
        <button 
          type="button"
          className={`nav-tab-btn ${currentTab === 'v2' ? 'active' : ''}`}
          onClick={() => onSelectTab('v2')}
          title="Two-Stage Hybrid Reranking (PGVector + Lexical + Source Diversity)"
        >
          <GitCompare size={14} />
          <span>V2 Reranked RAG</span>
        </button>

        <button 
          type="button"
          className={`nav-tab-btn ${currentTab === 'v1' ? 'active' : ''}`}
          onClick={() => onSelectTab('v1')}
          title="Direct Top-K Vector Cosine Retrieval"
        >
          <Search size={14} />
          <span>V1 Direct RAG</span>
        </button>

        <button 
          type="button"
          className={`nav-tab-btn ${currentTab === 'ingestion' ? 'active' : ''}`}
          onClick={() => onSelectTab('ingestion')}
          title="Ingest & Chunk Automotive Diagnostic PDFs"
        >
          <Database size={14} />
          <span>Ingest PDFs</span>
        </button>
      </nav>

      {/* Header Actions */}
      <div className="header-actions">
        {/* Backend Connection Indicator with toggle */}
        <div 
          className="status-indicator-badge"
          onClick={onToggleSimulation}
          title={isOnline ? 'Connected to Spring AI (port 8080). Click to toggle simulation.' : 'Operating in Live Demo Simulation mode. Click to test connection.'}
        >
          <span className={`status-dot ${isOnline ? 'online' : 'simulation'}`} />
          <span>{isOnline ? 'Spring AI :8080' : 'Demo Engine'}</span>
        </div>

        {/* Settings button */}
        <button 
          type="button" 
          className="icon-btn"
          onClick={onOpenSettings}
          title="Pipeline Settings & Parameters"
        >
          <Sliders size={16} />
        </button>
      </div>
    </header>
  );
}
