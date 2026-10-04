import { findCycles, getCycleMembers, getViolationEdges, isSimpleLoop, orderedLoop } from '../graphUtils.js';

function ruleTitle(ruleId) {
  if (ruleId === 'no-cycles') return 'Circular dependency';
  const match = ruleId.match(/^[^:]+:([^>]+)->(.+)$/);
  return match ? `${match[1].trim()} → ${match[2].trim()}` : ruleId;
}

function severityInfo(value) {
  const raw = String(value ?? '');
  const normalized = raw.toUpperCase();
  return {
    label: ['HIGH', 'MEDIUM', 'LOW'].includes(normalized) ? normalized : raw,
    className: ['HIGH', 'MEDIUM', 'LOW'].includes(normalized) ? normalized.toLowerCase() : 'neutral'
  };
}

function ViolationCard({ violation, dependencies, selected, onSelect }) {
  const severity = severityInfo(violation.severity);
  const isCycle = violation.ruleId === 'no-cycles';
  const violationEdges = isCycle ? getViolationEdges(violation, dependencies) : [];
  const cycleMembers = isCycle ? getCycleMembers(violation, dependencies) : [];
  const cyclePath = isCycle && cycleMembers.length <= 4 && isSimpleLoop(cycleMembers, violationEdges)
    ? orderedLoop(cycleMembers, violationEdges)
    : null;
  const cycleSummary = cyclePath
    ? cyclePath.join(' → ')
    : `${cycleMembers.slice(0, 4).join(', ')}${cycleMembers.length > 4 ? ` +${cycleMembers.length - 4} more` : ''}`;
  const blastRadius = `${violation.blastRadiusCount} ${violation.blastRadiusCount === 1 ? 'package' : 'packages'}`;
  const select = () => onSelect(violation);
  const handleKeyDown = (event) => {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      select();
    }
  };

  return <article className={`violation-card ${selected ? 'selected' : ''}`}>
    <button className="violation-card-content" aria-pressed={selected} onClick={select} onKeyDown={handleKeyDown}>
      <span className={`severity-badge ${severity.className}`}>{severity.label}</span>
      <strong className="violation-title">{ruleTitle(violation.ruleId)}</strong>
      <small>Rule: {violation.ruleId}</small>
      {isCycle ? <><small>Packages in cycle: {cycleMembers.length}</small><small className="cycle-members">{cycleSummary}</small></> : <small>Dependency: {violation.fromModule} → {violation.toModule}</small>}
      <small>Blast radius: {blastRadius}</small>
    </button>
    <button className="violation-graph-button" onClick={select}>View on graph</button>
  </article>;
}

export default function ViolationList({ violations, modules = [], dependencies, selectedId, onSelect }) {
  if (!violations.length) {
    const hasCycles = findCycles(modules, dependencies).length > 0;
    return <section className="panel violation-list"><h3>Violations <span>0</span></h3><div className="violations-empty">{hasCycles
      ? <strong>⚠ Cycles exist in the graph but no violation was reported.</strong>
      : <><strong>✓ No architecture violations detected</strong><p>Cycle detection ran on all analyzed packages. Layer rules are checked only if you supplied them.</p></>}</div></section>;
  }
  return <section className="panel violation-list"><h3>Violations <span>{violations.length}</span></h3><div className="violation-card-list">{violations.map((violation) => <ViolationCard dependencies={dependencies} key={violation.id} onSelect={onSelect} selected={selectedId === violation.id} violation={violation} />)}</div></section>;
}
