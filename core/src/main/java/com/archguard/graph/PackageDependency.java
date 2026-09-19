package com.archguard.graph;

import java.util.Objects;

/**
 * A directed package dependency: the from-package imports the to-package.
 */
public final class PackageDependency {

    private final String fromPackage;
    private final String toPackage;

    public PackageDependency(String fromPackage, String toPackage) {
        this.fromPackage = Objects.requireNonNull(fromPackage, "fromPackage");
        this.toPackage = Objects.requireNonNull(toPackage, "toPackage");
    }

    public String getFromPackage() {
        return fromPackage;
    }

    public String getToPackage() {
        return toPackage;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PackageDependency that)) {
            return false;
        }
        return fromPackage.equals(that.fromPackage) && toPackage.equals(that.toPackage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromPackage, toPackage);
    }

    @Override
    public String toString() {
        return fromPackage + " -> " + toPackage;
    }
}
