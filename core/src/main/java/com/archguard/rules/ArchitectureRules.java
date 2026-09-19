package com.archguard.rules;

import java.util.ArrayList;
import java.util.List;

/**
 * Architecture rules loaded from YAML. Unknown packages do not match forbidden layer pairs.
 */
public final class ArchitectureRules {

    private List<LayerRule> layers = new ArrayList<>();
    private List<ForbiddenEdgeRule> forbidden = new ArrayList<>();
    private boolean noCycles = true;

    public List<LayerRule> getLayers() {
        return layers;
    }

    public void setLayers(List<LayerRule> layers) {
        this.layers = layers == null ? new ArrayList<>() : layers;
    }

    public List<ForbiddenEdgeRule> getForbidden() {
        return forbidden;
    }

    public void setForbidden(List<ForbiddenEdgeRule> forbidden) {
        this.forbidden = forbidden == null ? new ArrayList<>() : forbidden;
    }

    public boolean isNoCycles() {
        return noCycles;
    }

    public void setNoCycles(boolean noCycles) {
        this.noCycles = noCycles;
    }
}
