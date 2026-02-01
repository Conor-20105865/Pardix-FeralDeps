package com.pardixlabs.feraldeps;

public class Dependency {
    public String groupId;
    public String artifactId;
    public String version;
    public String scope; // compile, test, provided, runtime, etc.

    public Dependency(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.scope = "compile"; // default scope
    }

    public String coordinate() {
        return groupId + ":" + artifactId;
    }

    /**
     * Checks if the version is locked to a specific version (hard-coded)
     * vs using a version range that allows upgrades
     */
    public boolean isVersionLocked() {
        if (version == null) return false;
        
        // Version ranges use brackets/parentheses: [1.0,2.0), (,1.0], etc.
        // Hard-locked versions are simple: 1.2.3, 1.2.3-SNAPSHOT, etc.
        return !version.contains("[") && 
               !version.contains("]") && 
               !version.contains("(") && 
               !version.contains(")") &&
               !version.contains(",");
    }

    /**
     * Returns a human-readable description of the version constraint
     */
    public String getVersionConstraintType() {
        if (!isVersionLocked()) {
            return "FLEXIBLE (version range allows upgrades)";
        }
        return "LOCKED (specific version pinned)";
    }
}
