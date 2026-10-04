function prefixAtDepth(packageName, depth) {
  const parts = packageName.split('.');
  return parts.length >= depth ? parts.slice(0, depth).join('.') : null;
}

export function packageSuggestions(modules) {
  const counts = new Map();
  for (const module of modules) {
    const packageName = typeof module === 'string' ? module : module.id;
    for (const depth of [3, 4]) {
      const prefix = prefixAtDepth(packageName, depth);
      if (prefix) counts.set(prefix, (counts.get(prefix) || 0) + 1);
    }
  }
  return [...counts.entries()]
    .map(([prefix, count]) => ({ prefix, count }))
    .sort((left, right) => right.count - left.count || left.prefix.localeCompare(right.prefix));
}
