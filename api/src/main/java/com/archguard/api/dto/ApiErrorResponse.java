package com.archguard.api.dto;

import java.time.Instant;
import java.util.Map;

/** Consistent JSON error payload for all API failures. */
public record ApiErrorResponse(Instant timestamp, int status, String error, String message, Map<String, String> fields) { }
