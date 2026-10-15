import React, { useState, useEffect, useRef } from 'react';
import { ArrowUp, Sparkles, Wrench, Database, GitBranch } from 'lucide-react';
import ProcessLogsAccordion from './components/ProcessLogsAccordion';
import AnswerCard from './components/AnswerCard';
import IngestionModal from './components/IngestionModal';
import { checkBackendStatus, executeV2Query, executeV1Query } from './services/api';

const QUICK_SUGGESTIONS = [
  'What are the root causes for DTC P0101 across C-Class and S-Class?',
  'What are the ECM connector pinout numbers and injector resistance for DTC P0301 on a C 300?',
  'My car throws random misfires (P0300) during acceleration. Could the 48V mild-hybrid setup be causing this?',
  'What is the MAF sensor normal idling voltage and pinout?'
];

export default function App() {
  const [backendStatus, setBackendStatus] = useState({ online: false });
  const [isDemoMode, setIsDemoMode] = useState(false);
  const [isIngestModalOpen, setIsIngestModalOpen] = useState(false);
  const [selectedPipeline, setSelectedPipeline] = useState('V2'); // 'V2' | 'V1'
  
  const [inputQuery, setInputQuery] = useState('');
  const [isGenerating, setIsGenerating] = useState(false);
  const [messages, setMessages] = useState([]);
  
  // Real-time live log process state for active query
  const [activeProcessState, setActiveProcessState] = useState(null);

  const messagesEndRef = useRef(null);
  const timerRef = useRef(null);
  const startTimeRef = useRef(0);

  // Probe backend status on mount
  useEffect(() => {
    async function initCheck() {
      const res = await checkBackendStatus();
      setBackendStatus(res);
      if (!res.online) {
        setIsDemoMode(true);
      }
    }
    initCheck();
  }, []);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, activeProcessState]);

  const handleSend = async (queryText = inputQuery) => {
    const text = queryText.trim();
    if (!text || isGenerating) return;

    setInputQuery('');
    setIsGenerating(true);

    const userMessageId = `user-${Date.now()}`;
    const assistantMessageId = `asst-${Date.now()}`;
    const activePipeline = selectedPipeline;

    // Add user message
    setMessages((prev) => [
      ...prev,
      { id: userMessageId, role: 'user', content: text }
    ]);

    // Initialize progressive process state
    startTimeRef.current = Date.now();
    setActiveProcessState({
      isRunning: true,
      elapsedMs: 0,
      steps: [],
      pipelineVersion: activePipeline,
      endpointCalled: activePipeline === 'V2' ? '/api/v2/rag/query' : '/api/v1/rag/query'
    });

    // Start timer ticker
    clearInterval(timerRef.current);
    timerRef.current = setInterval(() => {
      const elapsed = Date.now() - startTimeRef.current;
      setActiveProcessState((prev) => prev ? { ...prev, elapsedMs: elapsed } : null);
    }, 50);

    try {
      const progressCallback = (progress) => {
        setActiveProcessState((prev) => {
          if (!prev) return null;
          return {
            ...prev,
            steps: progress.completedSteps || prev.steps
          };
        });
      };

      // Call Real Backend V2 or V1 API based on selection
      const result = activePipeline === 'V2' 
        ? await executeV2Query(
            text,
            5,
            20,
            isDemoMode || !backendStatus.online,
            progressCallback
          )
        : await executeV1Query(
            text,
            5,
            isDemoMode || !backendStatus.online,
            progressCallback
          );

      clearInterval(timerRef.current);
      const totalElapsed = Date.now() - startTimeRef.current;

      const finalProcess = {
        isRunning: false,
        elapsedMs: totalElapsed,
        steps: result.executionLogSteps || activeProcessState?.steps || [],
        pipelineVersion: result.pipelineVersion || activePipeline,
        endpointCalled: result.endpointCalled || (activePipeline === 'V2' ? '/api/v2/rag/query' : '/api/v1/rag/query')
      };

      setMessages((prev) => [
        ...prev,
        {
          id: assistantMessageId,
          role: 'assistant',
          answer: result.answer,
          sources: result.sources || [],
          pipelineVersion: result.pipelineVersion || activePipeline,
          processState: finalProcess
        }
      ]);

      setActiveProcessState(null);
    } catch (err) {
      console.error(err);
      clearInterval(timerRef.current);
      setActiveProcessState(null);
    } finally {
      setIsGenerating(false);
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <>
      {/* Stagnant Sticky Top Header Dock */}
      <header className="top-dock">
        <div className="top-dock-content">
          <div className="brand-minimal">
            <div className="brand-icon">
              <Sparkles size={18} />
            </div>
            <span>Karcliq AI</span>
          </div>

          {/* Central Engine Switcher: V2 vs V1 */}
          <div style={{
            display: 'flex',
            alignItems: 'center',
            background: 'var(--bg-surface)',
            padding: '2px',
            borderRadius: 'var(--radius-full)',
            border: '1px solid var(--border-subtle)',
            fontSize: '0.74rem'
          }}>
            <button
              type="button"
              onClick={() => setSelectedPipeline('V2')}
              style={{
                padding: '0.25rem 0.65rem',
                borderRadius: 'var(--radius-full)',
                border: 'none',
                cursor: 'pointer',
                fontWeight: 500,
                background: selectedPipeline === 'V2' ? 'rgba(56, 189, 248, 0.15)' : 'transparent',
                color: selectedPipeline === 'V2' ? '#38bdf8' : 'var(--text-muted)',
                transition: 'all 0.15s ease'
              }}
              title="Two-Stage Hybrid Reranking (PGVector + Lexical + Source Diversity)"
            >
              V2 Reranked
            </button>
            <button
              type="button"
              onClick={() => setSelectedPipeline('V1')}
              style={{
                padding: '0.25rem 0.65rem',
                borderRadius: 'var(--radius-full)',
                border: 'none',
                cursor: 'pointer',
                fontWeight: 500,
                background: selectedPipeline === 'V1' ? 'rgba(168, 85, 247, 0.15)' : 'transparent',
                color: selectedPipeline === 'V1' ? '#c084fc' : 'var(--text-muted)',
                transition: 'all 0.15s ease'
              }}
              title="Single-Stage Direct Cosine Search (No Reranker)"
            >
              V1 Direct
            </button>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            {/* Data Ingestion API Trigger Button */}
            <button 
              type="button" 
              className="header-status-pill"
              onClick={() => setIsIngestModalOpen(true)}
              title="Ingest & vectorize PDFs into PGVector"
              style={{ display: 'flex', alignItems: 'center', gap: 5 }}
            >
              <Database size={12} color="var(--accent)" />
              <span>Ingest PDFs</span>
            </button>

            {/* Backend Connection Indicator */}
            <button 
              type="button" 
              className="header-status-pill"
              onClick={() => setIsDemoMode(!isDemoMode)}
              title={backendStatus.online ? 'Connected to live Spring Boot backend (port 8080). Click to toggle demo.' : 'Click to test backend connection'}
            >
              <span className={`status-indicator ${backendStatus.online && !isDemoMode ? '' : 'demo'}`} />
              <span>{backendStatus.online && !isDemoMode ? 'Spring AI :8080' : 'Demo Engine'}</span>
            </button>
          </div>
        </div>
      </header>

      <div className="app-layout">
        {/* Main Chat Flow */}
        <main className="chat-container">
        {messages.length === 0 && !activeProcessState ? (
          <div className="welcome-screen">
            <h1 className="welcome-title">Automotive Diagnostic Intelligence</h1>
            <p className="welcome-subtitle">
              Ask diagnostic questions across Mercedes-Benz service manuals (C-Class & S-Class). Active pipeline: <strong style={{ color: selectedPipeline === 'V2' ? '#38bdf8' : '#c084fc' }}>{selectedPipeline === 'V2' ? 'V2 Two-Stage Reranked' : 'V1 Direct Cosine Search'}</strong>.
            </p>

            <div className="prompt-suggestions">
              {QUICK_SUGGESTIONS.map((suggestion, idx) => (
                <button
                  key={idx}
                  type="button"
                  className="suggestion-chip"
                  onClick={() => handleSend(suggestion)}
                >
                  <Wrench size={13} style={{ opacity: 0.7 }} />
                  <span>{suggestion}</span>
                </button>
              ))}
            </div>
          </div>
        ) : (
          <div className="messages-flow">
            {messages.map((msg) => (
              <div key={msg.id} className={msg.role === 'user' ? 'user-msg-row' : 'assistant-msg-row'}>
                {msg.role === 'user' ? (
                  <div className="user-msg-text">
                    {msg.content}
                  </div>
                ) : (
                  <>
                    {/* Expandable Live Process Accordion (with V1/V2 Badge) */}
                    {msg.processState && (
                      <ProcessLogsAccordion processState={msg.processState} />
                    )}

                    {/* Answer Body with V1/V2 Engine Tag */}
                    <AnswerCard 
                      answer={msg.answer} 
                      sources={msg.sources}
                      pipelineVersion={msg.pipelineVersion}
                    />
                  </>
                )}
              </div>
            ))}

            {/* Active Query Live Process Indicator */}
            {activeProcessState && (
              <div className="assistant-msg-row">
                <ProcessLogsAccordion processState={activeProcessState} />
              </div>
            )}

            <div ref={messagesEndRef} />
          </div>
        )}
      </main>
    </div>

    {/* Minimal Floating Bottom Input Bar */}
    <div className="bottom-dock">
      <div className="dock-content">
        <div className="simple-input-bar">
          <textarea
            className="simple-textarea"
            placeholder={`Ask automotive diagnostic question (${selectedPipeline === 'V2' ? 'V2 Two-Stage Reranked' : 'V1 Direct Retrieval'})...`}
            rows={1}
            value={inputQuery}
            onChange={(e) => setInputQuery(e.target.value)}
            onKeyDown={handleKeyDown}
          />
          <button
            type="button"
            className="send-icon-btn"
            onClick={() => handleSend()}
            disabled={!inputQuery.trim() || isGenerating}
            title="Send"
          >
            <ArrowUp size={16} strokeWidth={2.5} />
          </button>
        </div>
      </div>
    </div>

    {/* Data Ingestion Modal */}
    <IngestionModal 
      isOpen={isIngestModalOpen}
      onClose={() => setIsIngestModalOpen(false)}
    />
  </>
);
}
