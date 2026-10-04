const compareIds = (left, right) => (left < right ? -1 : left > right ? 1 : 0);

export function uniqueLabels(moduleIds) {
  const suffixes = new Map(moduleIds.map((id) => [id, id.split('.') ]));
  const labels = new Map();

  for (const id of moduleIds) {
    const segments = suffixes.get(id);
    const maxSegments = Math.min(4, segments.length);
    let label = segments.slice(-maxSegments).join('.');
    for (let length = 1; length <= maxSegments; length++) {
      const suffix = segments.slice(-length).join('.');
      const matches = moduleIds.filter((moduleId) => suffixes.get(moduleId).slice(-length).join('.') === suffix);
      if (matches.length === 1) {
        label = suffix;
        break;
      }
    }
    labels.set(id, label);
  }

  return labels;
}

export function findCycles(modules, dependencies) {
  const moduleIds = modules.map((module) => module.id).sort(compareIds);
  const adjacency = new Map(moduleIds.map((id) => [id, new Set()]));
  const reverse = new Map(moduleIds.map((id) => [id, new Set()]));

  for (const dependency of dependencies) {
    if (adjacency.has(dependency.from) && adjacency.has(dependency.to)) {
      adjacency.get(dependency.from).add(dependency.to);
      reverse.get(dependency.to).add(dependency.from);
    }
  }

  const visited = new Set();
  const finishingOrder = [];
  for (const start of moduleIds) {
    if (visited.has(start)) continue;
    visited.add(start);
    const stack = [{ id: start, neighbors: [...adjacency.get(start)].sort(compareIds), index: 0 }];
    while (stack.length) {
      const current = stack[stack.length - 1];
      if (current.index < current.neighbors.length) {
        const neighbor = current.neighbors[current.index++];
        if (!visited.has(neighbor)) {
          visited.add(neighbor);
          stack.push({ id: neighbor, neighbors: [...adjacency.get(neighbor)].sort(compareIds), index: 0 });
        }
      } else {
        finishingOrder.push(current.id);
        stack.pop();
      }
    }
  }

  const components = [];
  const assigned = new Set();
  for (const start of finishingOrder.slice().reverse()) {
    if (assigned.has(start)) continue;
    const component = [];
    const stack = [start];
    assigned.add(start);
    while (stack.length) {
      const id = stack.pop();
      component.push(id);
      for (const neighbor of [...reverse.get(id)].sort(compareIds).reverse()) {
        if (!assigned.has(neighbor)) {
          assigned.add(neighbor);
          stack.push(neighbor);
        }
      }
    }
    if (component.length > 1) components.push(component.sort(compareIds));
  }

  return components.sort((left, right) => compareIds(left[0], right[0]));
}

export function getViolationEdges(violation, dependencies) {
  return dependencies.filter((dependency) => dependency.violationIds.includes(violation.id));
}

export function getCycleMembers(violation, dependencies) {
  return [...new Set(getViolationEdges(violation, dependencies).flatMap(({ from, to }) => [from, to]))].sort(compareIds);
}

export function isSimpleLoop(members, edges) {
  return orderedLoop(members, edges) !== null;
}

export function orderedLoop(members, edges) {
  if (members.length < 2) return null;
  const memberSet = new Set(members);
  const cycleEdges = edges.filter(({ from, to }) => memberSet.has(from) && memberSet.has(to));
  if (cycleEdges.length !== members.length) return null;

  const outgoing = new Map();
  const incoming = new Map();
  for (const edge of cycleEdges) {
    outgoing.set(edge.from, (outgoing.get(edge.from) || 0) + 1);
    incoming.set(edge.to, (incoming.get(edge.to) || 0) + 1);
  }
  if (members.some((member) => outgoing.get(member) !== 1 || incoming.get(member) !== 1)) return null;

  const next = new Map(cycleEdges.map(({ from, to }) => [from, to]));
  const path = [members[0]];
  const visited = new Set(path);
  while (path.length <= members.length) {
    const target = next.get(path[path.length - 1]);
    if (!target) return null;
    path.push(target);
    if (target === path[0]) return path.length === members.length + 1 ? path : null;
    if (visited.has(target)) return null;
    visited.add(target);
  }
  return null;
}