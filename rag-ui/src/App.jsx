import React, { useState, useEffect, useRef } from 'react';
import { ArrowUp, Sparkles, Wrench, Database, GitBranch, Sliders } from 'lucide-react';
import ProcessLogsAccordion from './components/ProcessLogsAccordion';
import AnswerCard from './components/AnswerCard';
import IngestionModal from './components/IngestionModal';
import SettingsModal from './components/SettingsModal';
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
  const [isSettingsModalOpen, setIsSettingsModalOpen] = useState(false);
  const [selectedPipeline, setSelectedPipeline] = useState('V2_CROSS'); // 'V2_CROSS' | 'V2_HYBRID' | 'V1'
  const [settings, setSettings] = useState({
    topK: 5,
    initialPoolSize: 20,
    rerankStrategy: 'CROSS_ENCODER'
  });
  
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

    const isCross = activePipeline === 'V2_CROSS';
    const isHybrid = activePipeline === 'V2_HYBRID';
    const currentLabel = isCross ? 'V2 Cross-Encoder' : (isHybrid ? 'V2 Hybrid Search' : 'V1 Direct Cosine');

    // Initialize progressive process state
    startTimeRef.current = Date.now();
    setActiveProcessState({
      isRunning: true,
      elapsedMs: 0,
      steps: [],
      pipelineVersion: activePipeline,
      pipelineLabel: currentLabel,
      rerankStrategy: isCross ? 'CROSS_ENCODER' : (isHybrid ? 'HYBRID' : null),
      endpointCalled: activePipeline.startsWith('V2') ? '/api/v2/rag/query' : '/api/v1/rag/query'
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

      // Call Real Backend V2 (Cross-Encoder / Hybrid) or V1 API based on selection
      let result;
      if (activePipeline === 'V2_CROSS') {
        result = await executeV2Query(
          text,
          settings.topK,
          settings.initialPoolSize,
          isDemoMode || !backendStatus.online,
          progressCallback,
          'CROSS_ENCODER'
        );
      } else if (activePipeline === 'V2_HYBRID') {
        result = await executeV2Query(
          text,
          settings.topK,
          settings.initialPoolSize,
          isDemoMode || !backendStatus.online,
          progressCallback,
          'HYBRID'
        );
      } else {
        result = await executeV1Query(
          text,
          settings.topK,
          isDemoMode || !backendStatus.online,
          progressCallback
        );
      }

      clearInterval(timerRef.current);
      const totalElapsed = Date.now() - startTimeRef.current;

      const finalProcess = {
        isRunning: false,
        elapsedMs: totalElapsed,
        steps: result.executionLogSteps || activeProcessState?.steps || [],
        pipelineVersion: result.pipelineVersion || activePipeline,
        rerankStrategy: result.rerankStrategy || (isCross ? 'CROSS_ENCODER' : (isHybrid ? 'HYBRID' : null)),
        pipelineLabel: result.pipelineLabel || currentLabel,
        endpointCalled: result.endpointCalled || (activePipeline.startsWith('V2') ? '/api/v2/rag/query' : '/api/v1/rag/query')
      };

      setMessages((prev) => [
        ...prev,
        {
          id: assistantMessageId,
          role: 'assistant',
          answer: result.answer,
          sources: result.sources || [],
          pipelineVersion: activePipeline,
          pipelineLabel: finalProcess.pipelineLabel,
          rerankStrategy: finalProcess.rerankStrategy,
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

          {/* Central Engine Switcher: V2 Cross-Encoder vs V2 Hybrid vs V1 */}
          <div style={{
            display: 'flex',
            alignItems: 'center',
            background: 'var(--bg-surface)',
            padding: '2px',
            borderRadius: 'var(--radius-full)',
            border: '1px solid var(--border-subtle)',
            fontSize: '0.74rem',
            gap: 2
          }}>
            <button
              type="button"
              onClick={() => setSelectedPipeline('V2_CROSS')}
              style={{
                padding: '0.25rem 0.65rem',
                borderRadius: 'var(--radius-full)',
                border: 'none',
                cursor: 'pointer',
                fontWeight: 500,
                background: selectedPipeline === 'V2_CROSS' ? 'rgba(56, 189, 248, 0.18)' : 'transparent',
                color: selectedPipeline === 'V2_CROSS' ? '#38bdf8' : 'var(--text-muted)',
                transition: 'all 0.15s ease'
              }}
              title="Two-Stage Neural Cross-Encoder (Deep query ⨁ passage cross-attention)"
            >
              ⚡ V2 Cross-Encoder
            </button>
            <button
              type="button"
              onClick={() => setSelectedPipeline('V2_HYBRID')}
              style={{
                padding: '0.25rem 0.65rem',
                borderRadius: 'var(--radius-full)',
                border: 'none',
                cursor: 'pointer',
                fontWeight: 500,
                background: selectedPipeline === 'V2_HYBRID' ? 'rgba(52, 211, 153, 0.18)' : 'transparent',
                color: selectedPipeline === 'V2_HYBRID' ? '#34d399' : 'var(--text-muted)',
                transition: 'all 0.15s ease'
              }}
              title="Two-Stage Hybrid Search (Vector Cosine + Lexical Overlap + MMR Source Diversity)"
            >
              🔀 V2 Hybrid
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
                background: selectedPipeline === 'V1' ? 'rgba(168, 85, 247, 0.18)' : 'transparent',
                color: selectedPipeline === 'V1' ? '#c084fc' : 'var(--text-muted)',
                transition: 'all 0.15s ease'
              }}
              title="Single-Stage Direct Cosine Search (No Reranker)"
            >
              📄 V1 Direct
            </button>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            {/* Pipeline Configuration Button */}
            <button 
              type="button" 
              className="header-status-pill"
              onClick={() => setIsSettingsModalOpen(true)}
              title="Configure RAG parameters & reranker weights"
              style={{ display: 'flex', alignItems: 'center', gap: 5 }}
            >
              <Sliders size={12} color="var(--accent-cyan)" />
              <span>Settings</span>
            </button>

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
              Ask diagnostic questions across Mercedes-Benz service manuals (C-Class & S-Class). Active pipeline:{' '}
              <strong style={{
                color: selectedPipeline === 'V2_CROSS' ? '#38bdf8' : (selectedPipeline === 'V2_HYBRID' ? '#34d399' : '#c084fc')
              }}>
                {selectedPipeline === 'V2_CROSS' ? '⚡ V2 Neural Cross-Encoder' : (selectedPipeline === 'V2_HYBRID' ? '🔀 V2 Hybrid Search' : '📄 V1 Direct Cosine Search')}
              </strong>.
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
                      pipelineLabel={msg.pipelineLabel}
                      rerankStrategy={msg.rerankStrategy}
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
            placeholder={`Ask automotive diagnostic question (${selectedPipeline === 'V2_CROSS' ? '⚡ V2 Cross-Encoder' : (selectedPipeline === 'V2_HYBRID' ? '🔀 V2 Hybrid' : '📄 V1 Direct')})...`}
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

    {/* Pipeline Settings Configuration Modal */}
    <SettingsModal 
      isOpen={isSettingsModalOpen}
      onClose={() => setIsSettingsModalOpen(false)}
      settings={settings}
      onSave={(newSettings) => {
        setSettings(newSettings);
        if (newSettings.rerankStrategy === 'CROSS_ENCODER') {
          setSelectedPipeline('V2_CROSS');
        } else if (newSettings.rerankStrategy === 'HYBRID') {
          setSelectedPipeline('V2_HYBRID');
        }
      }}
    />

    {/* Data Ingestion Modal */}
    <IngestionModal 
      isOpen={isIngestModalOpen}
      onClose={() => setIsIngestModalOpen(false)}
    />
  </>
);
}
