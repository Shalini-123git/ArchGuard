import { describe, expect, it } from 'vitest';
import { packageSuggestions } from './packageSuggestions.js';

describe('packageSuggestions', () => {
  it('computes depth three and four prefixes with package counts', () => {
    expect(packageSuggestions([
      { id: 'com.termux.shared.util' },
      { id: 'com.termux.shared.io' },
      { id: 'com.termux.app' },
      { id: 'org.example.api' }
    ])).toEqual([
      { prefix: 'com.termux.shared', count: 2 },
      { prefix: 'com.termux.app', count: 1 },
      { prefix: 'com.termux.shared.io', count: 1 },
      { prefix: 'com.termux.shared.util', count: 1 },
      { prefix: 'org.example.api', count: 1 }
    ]);
  });
});
