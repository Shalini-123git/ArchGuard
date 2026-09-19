package com.archguard.cycle;

import java.util.List;
import java.util.Objects;

/**
 * Packages that sit in one strongly connected component larger than a single node.
 */
public final class DependencyCycle {

    private final List<String> packages;

    public DependencyCycle(List<String> packages) {
        this.packages = List.copyOf(packages);
    }

    public List<String> getPackages() {
        return packages;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DependencyCycle that)) {
            return false;
        }
        return packages.equals(that.packages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(packages);
    }

    @Override
    public String toString() {
        return String.join(", ", packages);
    }
}
