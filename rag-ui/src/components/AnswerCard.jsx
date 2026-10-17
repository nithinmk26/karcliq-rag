import React from 'react';
import { BookOpen, GitBranch, Cpu } from 'lucide-react';

export default function AnswerCard({ 
  answer, 
  sources = [], 
  pipelineVersion = 'V2', 
  pipelineLabel = null,
  rerankStrategy = null,
  model = 'gpt-4o-mini' 
}) {
  const isCrossEncoder = rerankStrategy === 'CROSS_ENCODER' || pipelineVersion === 'V2_CROSS' || pipelineLabel?.includes('Cross-Encoder');
  const isHybrid = rerankStrategy === 'HYBRID' || pipelineVersion === 'V2_HYBRID' || pipelineLabel?.includes('Hybrid');
  const isV2 = pipelineVersion.startsWith('V2');

  const badgeLabel = pipelineLabel || (
    isCrossEncoder ? 'V2 Cross-Encoder' : (isHybrid ? 'V2 Hybrid Reranked' : (isV2 ? 'V2 Reranked' : 'V1 Direct Cosine'))
  );

  const badgeColor = isCrossEncoder ? '#38bdf8' : (isHybrid ? '#34d399' : '#c084fc');
  const badgeBg = isCrossEncoder ? 'rgba(56, 189, 248, 0.12)' : (isHybrid ? 'rgba(52, 211, 153, 0.12)' : 'rgba(168, 85, 247, 0.12)');
  const badgeBorder = isCrossEncoder ? '1px solid rgba(56, 189, 248, 0.3)' : (isHybrid ? '1px solid rgba(52, 211, 153, 0.3)' : '1px solid rgba(168, 85, 247, 0.3)');

  // Format simple markdown
  const renderContent = (text) => {
    if (!text) return null;

    const lines = text.split('\n');
    const elements = [];
    let listItems = [];

    const flushList = () => {
      if (listItems.length > 0) {
        elements.push(
          <ul key={`ul-${elements.length}`}>
            {listItems.map((item, idx) => (
              <li key={idx} dangerouslySetInnerHTML={{ __html: item }} />
            ))}
          </ul>
        );
        listItems = [];
      }
    };

    lines.forEach((line, index) => {
      const trimmed = line.trim();

      if (trimmed.startsWith('### ')) {
        flushList();
        elements.push(<h3 key={index}>{trimmed.replace('### ', '')}</h3>);
      } else if (trimmed.startsWith('#### ')) {
        flushList();
        elements.push(<h4 key={index}>{trimmed.replace('#### ', '')}</h4>);
      } else if (trimmed.startsWith('* ') || trimmed.startsWith('- ')) {
        const itemContent = trimmed.substring(2)
          .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
          .replace(/`(.*?)`/g, '<code>$1</code>');
        listItems.push(itemContent);
      } else if (/^\d+\.\s/.test(trimmed)) {
        flushList();
        const content = trimmed.replace(/^\d+\.\s/, '')
          .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
          .replace(/`(.*?)`/g, '<code>$1</code>');
        elements.push(
          <p key={index} dangerouslySetInnerHTML={{ __html: `<strong>${trimmed.match(/^\d+\./)[0]}</strong> ${content}` }} />
        );
      } else if (trimmed.length > 0) {
        flushList();
        const formatted = trimmed
          .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
          .replace(/`(.*?)`/g, '<code>$1</code>');
        elements.push(<p key={index} dangerouslySetInnerHTML={{ __html: formatted }} />);
      }
    });

    flushList();
    return elements;
  };

  return (
    <div className="answer-body">
      {renderContent(answer)}

      {/* Citations & Engine Tag */}
      <div className="citations-row">
        {/* Pipeline Badge */}
        <span style={{
          fontSize: '0.72rem',
          fontFamily: 'var(--font-mono)',
          padding: '0.15rem 0.5rem',
          borderRadius: 'var(--radius-full)',
          background: badgeBg,
          color: badgeColor,
          border: badgeBorder,
          display: 'inline-flex',
          alignItems: 'center',
          gap: 4
        }}>
          <GitBranch size={11} />
          {badgeLabel}
        </span>

        {sources && sources.length > 0 && (
          <>
            <span style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginLeft: 4 }}>Sources:</span>
            {sources.map((src, idx) => (
              <span key={idx} className="citation-pill">
                <BookOpen size={11} />
                {src}
              </span>
            ))}
          </>
        )}
      </div>
    </div>
  );
}
