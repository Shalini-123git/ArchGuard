package com.archguard.api.dto;

import java.util.List;

/** Persisted graph nodes and directed edges for one completed scan. */
public record GraphResponse(List<String> modules, List<GraphEdgeResponse> dependencies) {
    public record GraphEdgeResponse(String from, String to) { }
}
