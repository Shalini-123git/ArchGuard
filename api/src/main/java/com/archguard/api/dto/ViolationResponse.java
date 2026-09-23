package com.archguard.api.dto;

import java.util.List;
import java.util.UUID;

/** Deterministic violation facts returned by the API. */
public record ViolationResponse(UUID id, String ruleId, String severity, String fromModule, String toModule,
                                int blastRadiusCount, List<String> affectedModules, String explanation,
                                boolean explanationFallback) { }
