# Architecture

Placeholder. A diagram of parser → graph → rules → persistence → dashboard will be added when those pieces exist.

Current shape:

```
sample / git clone
        │
        ▼
     core (parse, graph, rules, cycles, blast radius)
        │
   ┌────┴────┐
   ▼         ▼
  cli       api (later) → postgres
                 │
                 ▼
             frontend (later)
```
