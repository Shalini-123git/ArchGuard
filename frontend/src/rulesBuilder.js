const SEVERITIES = ['HIGH', 'MEDIUM', 'LOW'];

export const presets = {
  'Layered web app': {
    layers: [
      { name: 'controller', packagePatterns: ['com.example.web'] },
      { name: 'service', packagePatterns: ['com.example.service'] },
      { name: 'repository', packagePatterns: ['com.example.data'] }
    ],
    forbidden: [
      { from: 'controller', to: 'repository', severity: 'HIGH' },
      { from: 'repository', to: 'controller', severity: 'HIGH' },
      { from: 'repository', to: 'service', severity: 'MEDIUM' }
    ],
    noCycles: true
  },
  'Clean architecture': {
    layers: [
      { name: 'domain', packagePatterns: ['com.example.domain'] },
      { name: 'application', packagePatterns: ['com.example.application'] },
      { name: 'infrastructure', packagePatterns: ['com.example.infrastructure'] },
      { name: 'presentation', packagePatterns: ['com.example.presentation'] }
    ],
    forbidden: [
      { from: 'domain', to: 'application', severity: 'HIGH' },
      { from: 'domain', to: 'infrastructure', severity: 'HIGH' },
      { from: 'application', to: 'infrastructure', severity: 'HIGH' },
      { from: 'application', to: 'presentation', severity: 'HIGH' }
    ],
    noCycles: true
  }
};

export function emptyModel() {
  return { layers: [], forbidden: [], noCycles: true };
}

function quote(value) {
  return JSON.stringify(String(value ?? ''));
}

export function buildRulesYaml(model) {
  if (!model.layers.length && !model.forbidden.length && model.noCycles) return '';
  const lines = ['layers:'];
  for (const layer of model.layers) {
    lines.push(`  - name: ${quote(layer.name)}`);
    lines.push(`    packagePatterns: [${layer.packagePatterns.map(quote).join(', ')}]`);
  }
  if (model.forbidden.length) {
    lines.push('forbidden:');
    for (const rule of model.forbidden) {
      lines.push(`  - from: ${quote(rule.from)}`);
      lines.push(`    to: ${quote(rule.to)}`);
      lines.push(`    severity: ${quote(rule.severity)}`);
    }
  }
  lines.push(`noCycles: ${model.noCycles}`);
  return `${lines.join('\n')}\n`;
}

export function validateModel(model) {
  const problems = [];
  const names = new Set();
  model.layers.forEach((layer, index) => {
    const field = `layers.${index}`;
    const name = String(layer.name ?? '').trim();
    if (!name) problems.push({ field: `${field}.name`, message: 'Layer name is required.' });
    else if (names.has(name)) problems.push({ field: `${field}.name`, message: 'Layer names must be unique.' });
    names.add(name);
    if (!layer.packagePatterns?.length) {
      problems.push({ field: `${field}.packagePatterns`, message: 'Add at least one package prefix.' });
    }
    (layer.packagePatterns || []).forEach((prefix, prefixIndex) => {
      if (!/^[A-Za-z0-9_]+(?:\.[A-Za-z0-9_]+)*$/.test(prefix)) {
        problems.push({ field: `${field}.packagePatterns.${prefixIndex}`, message: 'Use a Java package prefix such as com.example.web.' });
      }
    });
  });
  const validNames = new Set(model.layers.map((layer) => String(layer.name ?? '').trim()).filter(Boolean));
  model.forbidden.forEach((rule, index) => {
    const field = `forbidden.${index}`;
    if (!validNames.has(rule.from)) problems.push({ field: `${field}.from`, message: 'Select an existing layer.' });
    if (!validNames.has(rule.to)) problems.push({ field: `${field}.to`, message: 'Select an existing layer.' });
    if (rule.from === rule.to && validNames.has(rule.from)) problems.push({ field: `${field}.to`, message: 'Choose a different target layer.' });
    if (!SEVERITIES.includes(rule.severity)) problems.push({ field: `${field}.severity`, message: 'Severity must be HIGH, MEDIUM or LOW.' });
  });
  return problems;
}

export function cloneModel(model) {
  return {
    layers: model.layers.map((layer) => ({ ...layer, packagePatterns: [...layer.packagePatterns] })),
    forbidden: model.forbidden.map((rule) => ({ ...rule })),
    noCycles: model.noCycles
  };
}
