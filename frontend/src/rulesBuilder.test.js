import { describe, expect, it } from 'vitest';
import { buildRulesYaml, emptyModel, presets, validateModel } from './rulesBuilder.js';

describe('rules builder', () => {
  it('returns an empty YAML string for the basic model', () => {
    expect(buildRulesYaml(emptyModel())).toBe('');
  });

  it.each(Object.entries(presets))('%s preset matches the supported YAML format', (_, model) => {
    expect(buildRulesYaml(model)).toBe(buildRulesYaml({
      ...model,
      layers: model.layers.map((layer) => ({ ...layer, packagePatterns: [...layer.packagePatterns] })),
      forbidden: model.forbidden.map((rule) => ({ ...rule }))
    }));
    expect(buildRulesYaml(model)).toContain('layers:\n');
    expect(buildRulesYaml(model)).toContain('noCycles: true\n');
    expect(buildRulesYaml(model)).toMatch(/name: ".*"\n\s+packagePatterns: \[".*"\]/);
  });

  it('catches duplicate layers, unknown references, and bad package names', () => {
    const problems = validateModel({
      layers: [
        { name: 'web', packagePatterns: ['com.example.web'] },
        { name: 'web', packagePatterns: ['com.example web'] }
      ],
      forbidden: [{ from: 'web', to: 'missing', severity: 'URGENT' }],
      noCycles: true
    });
    expect(problems.map(({ message }) => message)).toEqual(expect.arrayContaining([
      'Layer names must be unique.',
      'Use a Java package prefix such as com.example.web.',
      'Select an existing layer.',
      'Severity must be HIGH, MEDIUM or LOW.'
    ]));
  });
});
