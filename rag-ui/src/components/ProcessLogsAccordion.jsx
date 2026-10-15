import React, { useState } from 'react';
import { Sparkles, ChevronDown, ChevronUp, Check, Clock, Terminal, GitBranch, Database } from 'lucide-react';

export default function ProcessLogsAccordion({ processState }) {
  const [isExpanded, setIsExpanded] = useState(false);
  const [showRawLogs, setShowRawLogs] = useState(false);

  const { 
    isRunning = false, 
    elapsedMs = 0, 
    steps = [],
    pipelineVersion = 'V2',
    endpointCalled = '/api/v2/rag/query'
  } = processState || {};

  const seconds = (elapsedMs / 1000).toFixed(1);
  const triggerLabel = isRunning 
    ? `Thinking (${seconds}s)...` 
    : `Thought for ${seconds}s`;

  const isV2 = pipelineVersion === 'V2';

  return (
    <div className="thought-accordion">
      {/* Clickable Header Trigger */}
      <div 
        className="thought-accordion-trigger"
        onClick={() => setIsExpanded(!isExpanded)}
        role="button"
        tabIndex={0}
      >
        <div className="thought-trigger-left">
          <span className={`thought-sparkle ${isRunning ? 'pulsing' : ''}`}>
            <Sparkles size={14} />
          </span>
          <span style={{ fontWeight: 500, color: isRunning ? 'var(--accent)' : 'var(--text-secondary)' }}>
            {triggerLabel}
          </span>

          {/* Prominent Pipeline Badge */}
          <span style={{
            fontSize: '0.68rem',
            fontFamily: 'var(--font-mono)',
            padding: '0.1rem 0.45rem',
            borderRadius: 'var(--radius-full)',
            background: isV2 ? 'rgba(56, 189, 248, 0.12)' : 'rgba(168, 85, 247, 0.12)',
            color: isV2 ? '#38bdf8' : '#c084fc',
            border: isV2 ? '1px solid rgba(56, 189, 248, 0.25)' : '1px solid rgba(168, 85, 247, 0.25)',
            display: 'inline-flex',
            alignItems: 'center',
            gap: 3
          }}>
            <GitBranch size={10} />
            {isV2 ? 'V2 Reranked' : 'V1 Direct'}
          </span>

          {isRunning && steps.length > 0 && (
            <span style={{ color: 'var(--text-muted)', fontSize: '0.72rem' }}>
              • {steps[steps.length - 1]?.title}
            </span>
          )}
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <span style={{ fontSize: '0.72rem' }}>
            {isExpanded ? 'Hide' : 'Show steps'}
          </span>
          {isExpanded ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
        </div>
      </div>

      {/* Expandable Content */}
      {isExpanded && (
        <div className="thought-accordion-content">
          {/* Metadata bar showing exact endpoint and retrieval mode */}
          <div style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            paddingBottom: '0.45rem',
            borderBottom: '1px solid rgba(255, 255, 255, 0.06)',
            marginBottom: '0.35rem',
            fontSize: '0.72rem',
            color: 'var(--text-muted)'
          }}>
            <span style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
              <Database size={11} />
              Endpoint: <code style={{ color: isV2 ? '#38bdf8' : '#c084fc', background: 'transparent' }}>{endpointCalled}</code>
            </span>
            <span>
              {isV2 ? 'Two-Stage Hybrid Reranking (Vector + Lexical + Diversity)' : 'Single-Stage Direct Cosine Search'}
            </span>
          </div>

          {showRawLogs ? (
            <div style={{ 
              fontFamily: 'var(--font-mono)', 
              fontSize: '0.72rem', 
              color: 'var(--text-secondary)',
              display: 'flex',
              flexDirection: 'column',
              gap: 4
            }}>
              {steps.map((step, idx) => (
                <div key={idx} style={{ opacity: 0.9 }}>
                  <span style={{ color: 'var(--accent)' }}>[{step.level || 'INFO'}]</span> {step.log || step.desc}
                </div>
              ))}
            </div>
          ) : (
            steps.map((step, idx) => {
              const isLast = idx === steps.length - 1;
              const isRunningStep = isRunning && isLast;

              return (
                <div key={idx} className="thought-step-line">
                  <div className={`thought-step-icon ${isRunningStep ? 'running' : ''}`}>
                    {isRunningStep ? <Clock size={13} /> : <Check size={13} />}
                  </div>
                  <div className="thought-step-text">
                    <span style={{ color: '#ffffff', fontWeight: 500 }}>{step.title}</span>
                    <span style={{ color: 'var(--text-muted)', marginLeft: 6 }}>— {step.desc}</span>
                  </div>
                  <div className="thought-step-duration">
                    {step.duration || '+0ms'}
                  </div>
                </div>
              );
            })
          )}

          {/* Toggle raw logs link */}
          <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 4 }}>
            <button 
              type="button"
              onClick={() => setShowRawLogs(!showRawLogs)}
              style={{
                background: 'transparent',
                border: 'none',
                color: 'var(--text-muted)',
                fontSize: '0.7rem',
                cursor: 'pointer',
                display: 'inline-flex',
                alignItems: 'center',
                gap: 4
              }}
            >
              <Terminal size={11} />
              {showRawLogs ? 'Show formatted pipeline' : 'Show raw logs'}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
