package com.archguard.api.llm;

/** Provider boundary for turning deterministic violation facts into plain-language explanations. */
public interface LlmClient {
    String explain(ViolationContext context);
}
