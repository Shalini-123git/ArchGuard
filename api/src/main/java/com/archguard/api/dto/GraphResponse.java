package com.archguard.api.dto;

import java.util.List;

/** Persisted graph nodes and directed edges for one completed scan. */
public record GraphResponse(List<GraphNodeResponse> modules, List<GraphEdgeResponse> dependencies) {
    public record GraphNodeResponse(String id, String layer, boolean cycleMember) { }
    public record GraphEdgeResponse(String id, String from, String to, List<String> violationIds) { }
}
