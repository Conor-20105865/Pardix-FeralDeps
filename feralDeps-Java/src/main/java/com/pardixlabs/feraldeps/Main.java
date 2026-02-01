package com.pardixlabs.feraldeps;

import java.io.File;
import java.util.*;

public class Main {

    public static void main(String[] args) throws Exception {
        // If no arguments, launch GUI
        if (args.length == 0) {
            GuiMain.main(args);
            return;
        }

        // Otherwise run CLI mode
        File pom = new File(args[0]);
        List<Dependency> deps = PomParser.parse(pom);

        System.out.println("FeralDeps scan results:\n");

        for (Dependency dep : deps) {
            System.out.println("• " + dep.coordinate() + ":" + dep.version);
            System.out.println("  Scope: " + dep.scope);
            System.out.println("  Version Constraint: " + dep.getVersionConstraintType());

            VulnerabilityDatabase.latestVersion(dep.coordinate())
                .ifPresent(latest -> {
                    if (!latest.equals(dep.version)) {
                        System.out.println("  Outdated → latest: " + latest);
                        if (dep.isVersionLocked()) {
                            System.out.println("  ⚠️  WARNING: Version is locked - upgrading may require code changes");
                        }
                    }
                });

            if (VulnerabilityDatabase.isVulnerable(dep)) {
                System.out.println("  ⚠️  Known vulnerable version");
                if (dep.isVersionLocked()) {
                    System.out.println("  ⚠️  CRITICAL: Vulnerable version is LOCKED - upgrade blocked by version constraint");
                }
            }

            System.out.println();
        }
    }
}
