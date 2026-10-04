import { useMemo, useState } from 'react';
import { buildRulesYaml, cloneModel, emptyModel, presets, validateModel } from '../rulesBuilder.js';

const tabs = [
  { id: 'basic', label: 'Basic' },
  { id: 'guided', label: 'Guided' },
  { id: 'yaml', label: 'YAML (advanced)' }
];

export default function ScanForm({ onSubmit, submitting }) {
  const [repoUrl, setRepoUrl] = useState('');
  const [tab, setTab] = useState('guided');
  const [model, setModel] = useState(emptyModel);
  const [rawYaml, setRawYaml] = useState('');
  const [preset, setPreset] = useState('');
  const [copied, setCopied] = useState(false);
  const problems = useMemo(() => tab === 'guided' ? validateModel(model) : [], [model, tab]);
  const generatedYaml = useMemo(() => buildRulesYaml(model), [model]);

  function updateModel(update) {
    setModel((current) => ({ ...current, ...update }));
  }

  function choosePreset(value) {
    setPreset(value);
    updateModel(value ? cloneModel(presets[value]) : emptyModel());
  }

  function updateLayer(index, update) {
    updateModel({ layers: model.layers.map((layer, layerIndex) => layerIndex === index ? { ...layer, ...update } : layer) });
  }

  function addLayer() {
    updateModel({ layers: [...model.layers, { name: '', packagePatterns: [] }] });
  }

  function removeLayer(index) {
    updateModel({ layers: model.layers.filter((_, layerIndex) => layerIndex !== index) });
  }

  function moveLayer(index, direction) {
    const target = index + direction;
    if (target < 0 || target >= model.layers.length) return;
    const layers = [...model.layers];
    [layers[index], layers[target]] = [layers[target], layers[index]];
    updateModel({ layers });
  }

  function addPrefix(index, value) {
    const prefixes = value.split(/[,\n]/).map((prefix) => prefix.trim()).filter(Boolean);
    if (!prefixes.length) return '';
    updateLayer(index, { packagePatterns: [...model.layers[index].packagePatterns, ...prefixes] });
    return '';
  }

  function removePrefix(layerIndex, prefixIndex) {
    updateLayer(layerIndex, { packagePatterns: model.layers[layerIndex].packagePatterns.filter((_, index) => index !== prefixIndex) });
  }

  function submit(event) {
    event.preventDefault();
    if (tab === 'guided' && problems.length) return;
    onSubmit({ repoUrl, rulesYaml: tab === 'basic' ? '' : tab === 'guided' ? generatedYaml : rawYaml });
  }

  async function copyYaml() {
    await navigator.clipboard.writeText(generatedYaml);
    setCopied(true);
    setTimeout(() => setCopied(false), 1500);
  }

  return <section className="panel scan-form"><h2>New scan</h2><p>Submit a public HTTPS Git repository. Rules are optional.</p>
    <form onSubmit={submit}>
      <label>Repository URL<input type="url" value={repoUrl} onChange={(event) => setRepoUrl(event.target.value)} placeholder="https://github.com/org/project.git" required /></label>
      <div className="rule-tabs" role="tablist" aria-label="Architecture rule setup">
        {tabs.map(({ id, label }) => <button type="button" role="tab" aria-selected={tab === id} className={tab === id ? 'rule-tab active' : 'rule-tab'} onClick={() => setTab(id)} key={id}>{label}</button>)}
      </div>
      {tab === 'basic' && <p className="form-help rule-tab-panel">Only circular dependencies are checked. No setup needed.</p>}
      {tab === 'guided' && <GuidedEditor model={model} problems={problems} preset={preset} onPreset={choosePreset} onUpdateLayer={updateLayer} onAddLayer={addLayer} onRemoveLayer={removeLayer} onMoveLayer={moveLayer} onAddPrefix={addPrefix} onRemovePrefix={removePrefix} onUpdate={updateModel} generatedYaml={generatedYaml} onCopy={copyYaml} copied={copied} />}
      {tab === 'yaml' && <label>Architecture rules <span>(optional)</span><textarea value={rawYaml} onChange={(event) => setRawYaml(event.target.value)} placeholder={'layers:\n  - name: web\n    packagePatterns: [com.example.web]'} rows="12" /><small className="form-help">Edits here are not shown in the Guided tab.</small></label>}
      <button disabled={submitting || (tab === 'guided' && problems.length > 0)}>{submitting ? 'Queueing scan…' : 'Start scan'}</button>
    </form>
  </section>;
}

function GuidedEditor({ model, problems, preset, onPreset, onUpdateLayer, onAddLayer, onRemoveLayer, onMoveLayer, onAddPrefix, onRemovePrefix, onUpdate, generatedYaml, onCopy, copied }) {
  const problemFor = (field) => problems.filter((problem) => problem.field === field || problem.field.startsWith(`${field}.`));
  return <div className="rule-tab-panel">
    <label>Preset<select value={preset} onChange={(event) => onPreset(event.target.value)}><option value="">Start from a preset</option><option value="Layered web app">Layered web app</option><option value="Clean architecture">Clean architecture</option><option value="">Empty</option></select></label>
    <p className="form-help">Not sure which packages to use? Run a Basic scan first, then use 'Edit rules & rescan' on the result page to pick from your project's real packages.</p>
    <div className="builder-heading"><h3>Layers</h3><button type="button" className="secondary small-button" onClick={onAddLayer}>Add layer</button></div>
    <p className="form-help">Order matters: the first matching layer wins, so list specific packages before general ones.</p>
    {model.layers.map((layer, index) => <div className="layer-editor" key={index}>
      <div className="layer-fields"><label>Layer name<input value={layer.name} onChange={(event) => onUpdateLayer(index, { name: event.target.value })} /></label><label>Package prefixes<div className="chip-input"><div className="chips">{layer.packagePatterns.map((prefix, prefixIndex) => <span className="chip" key={`${prefix}-${prefixIndex}`}>{prefix}<button type="button" aria-label={`Remove ${prefix}`} onClick={() => onRemovePrefix(index, prefixIndex)}>×</button></span>)}</div><input aria-label={`Add package prefix for ${layer.name || `layer ${index + 1}`}`} onKeyDown={(event) => { if (event.key === 'Enter' || event.key === ',') { event.preventDefault(); event.currentTarget.value = onAddPrefix(index, event.currentTarget.value); } }} onBlur={(event) => { event.currentTarget.value = onAddPrefix(index, event.currentTarget.value); }} placeholder="com.example.web" /></div></label></div>
      <div className="layer-actions"><button type="button" className="secondary small-button" disabled={index === 0} onClick={() => onMoveLayer(index, -1)} aria-label={`Move ${layer.name || `layer ${index + 1}`} up`}>↑</button><button type="button" className="secondary small-button" disabled={index === model.layers.length - 1} onClick={() => onMoveLayer(index, 1)} aria-label={`Move ${layer.name || `layer ${index + 1}`} down`}>↓</button><button type="button" className="secondary small-button" onClick={() => onRemoveLayer(index)}>Remove</button></div>
      {problemFor(`layers.${index}`).map((problem) => <p className="validation-message" key={problem.field}>{problem.message}</p>)}
    </div>)}
    <div className="builder-heading"><h3>Forbidden dependencies</h3><button type="button" className="secondary small-button" disabled={model.layers.length < 2} onClick={() => onUpdate({ forbidden: [...model.forbidden, { from: model.layers[0]?.name || '', to: model.layers[1]?.name || '', severity: 'HIGH' }] })}>Add rule</button></div>
    {model.forbidden.map((rule, index) => <div className="forbidden-editor" key={index}><span>Code in</span><select aria-label={`From layer ${index + 1}`} value={rule.from} onChange={(event) => onUpdate({ forbidden: model.forbidden.map((item, ruleIndex) => ruleIndex === index ? { ...item, from: event.target.value } : item) })}>{model.layers.map((layer) => <option key={layer.name} value={layer.name}>{layer.name || '(unnamed layer)'}</option>)}</select><span>must not depend on</span><select aria-label={`To layer ${index + 1}`} value={rule.to} onChange={(event) => onUpdate({ forbidden: model.forbidden.map((item, ruleIndex) => ruleIndex === index ? { ...item, to: event.target.value } : item) })}>{model.layers.map((layer) => <option key={layer.name} value={layer.name}>{layer.name || '(unnamed layer)'}</option>)}</select><select aria-label={`Severity ${index + 1}`} value={rule.severity} onChange={(event) => onUpdate({ forbidden: model.forbidden.map((item, ruleIndex) => ruleIndex === index ? { ...item, severity: event.target.value } : item) })}>{['HIGH', 'MEDIUM', 'LOW'].map((severity) => <option key={severity}>{severity}</option>)}</select><button type="button" className="secondary small-button" onClick={() => onUpdate({ forbidden: model.forbidden.filter((_, ruleIndex) => ruleIndex !== index) })}>Remove</button><p className="form-help rule-description">Code in {rule.from || 'this layer'} must never import code from {rule.to || 'that layer'}.</p>{problemFor(`forbidden.${index}`).map((problem) => <p className="validation-message" key={problem.field}>{problem.message}</p>)}</div>)}
    <label className="toggle-row"><input type="checkbox" checked={model.noCycles} onChange={(event) => onUpdate({ noCycles: event.target.checked })} /> Detect circular dependencies</label>
    {problems.length > 0 && <div className="validation-summary" role="alert">{problems.length} rule problem{problems.length === 1 ? '' : 's'} must be fixed before scanning.</div>}
    <div className="yaml-preview"><div className="builder-heading"><h3>Generated YAML</h3><button type="button" className="secondary small-button" onClick={onCopy}>{copied ? 'Copied' : 'Copy'}</button></div><pre>{generatedYaml || '(no rules; basic scan)'}</pre></div>
  </div>;
}
