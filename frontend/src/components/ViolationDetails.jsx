import { getCycleMembers, getViolationEdges } from '../graphUtils.js';

function severityInfo(value) {
  const raw = String(value ?? '');
  const normalized = raw.toUpperCase();
  const known = ['HIGH', 'MEDIUM', 'LOW'].includes(normalized);
  return { label: known ? normalized : raw, className: known ? normalized.toLowerCase() : 'neutral' };
}

export default function ViolationDetails({ violation, dependencies }) {
  if (!violation) return <section className="panel details"><h3>DETAILS</h3><p>Select a violation to focus its dependency and view its analysis.</p></section>;

  const severity = severityInfo(violation.severity);
  const isCycle = violation.ruleId === 'no-cycles';
  const cycleMembers = isCycle ? getCycleMembers(violation, dependencies) : [];
  const cycleEdges = isCycle ? getViolationEdges(violation, dependencies) : [];
  const hasExplanation = Boolean(violation.explanation);
  const blastRadius = `${violation.blastRadiusCount} ${violation.blastRadiusCount === 1 ? 'package' : 'packages'}`;

  return <section className="panel details">
    <h3>VIOLATION DETAILS</h3>
    <dl className="detail-fields">
      <div><dt>Severity:</dt><dd><span className={`severity-badge ${severity.className}`}>{severity.label}</span></dd></div>
      <div><dt>Rule:</dt><dd>{violation.ruleId}</dd></div>
      {isCycle ? <>
        <div><dt>Packages in cycle ({cycleMembers.length})</dt><dd><ul className="cycle-package-list">{cycleMembers.map((member) => <li key={member}>{member}</li>)}</ul></dd></div>
        <div><dt>Dependencies inside the cycle: {cycleEdges.length}</dt></div>
      </> : <div><dt>Dependency:</dt><dd>{`${violation.fromModule} → ${violation.toModule}`}</dd></div>}
      <div><dt>Blast radius:</dt><dd>{blastRadius}</dd></div>
    </dl>
    <h4>Affected packages</h4>
    <ul className="affected-packages">{violation.affectedModules?.length ? violation.affectedModules.map((module) => <li key={module}>{module}</li>) : <li>None</li>}</ul>
    <h4>Explanation</h4>
    <p className="explanation">{hasExplanation ? violation.explanation : 'No explanation available for this violation.'}</p>
    {hasExplanation && (violation.explanationFallback ? <span className="explanation-badge fallback-badge">Fallback explanation (AI service was unavailable)</span> : <small>Groq explanation</small>)}
  </section>;
}
