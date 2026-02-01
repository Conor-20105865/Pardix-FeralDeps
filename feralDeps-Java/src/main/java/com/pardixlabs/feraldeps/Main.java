package com.pardixlabs.feraldeps;

import java.io.File;
import java.util.List;

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
                            System.out.println("  WARNING: Version is locked - upgrading may require code changes");
                        }
                        System.out.println("  REMEDIATION: Update pom.xml version to: <version>" + latest + "</version>");
                    }
                });

            if (VulnerabilityDatabase.isVulnerable(dep)) {
                System.out.println("  WARNING: Known vulnerable version");
                if (dep.isVersionLocked()) {
                    System.out.println("  CRITICAL: Vulnerable version is LOCKED - upgrade blocked by version constraint");
                }
                
                // Get specific remediation information from OSV
                VulnerabilityDatabase.getRemediationInfo(dep).ifPresent(remediation -> {
                    if (remediation.hasRemediation && !remediation.fixedVersions.isEmpty()) {
                        System.out.println("  REMEDIATION: URGENT - Upgrade to a secure version:");
                        for (String fixedVer : remediation.fixedVersions) {
                            System.out.println("     • <version>" + fixedVer + "</version> (fixes known vulnerabilities)");
                        }
                        if (!remediation.summary.isEmpty()) {
                            System.out.println("     Issue: " + remediation.summary);
                        }
                        System.out.println("     Steps:");
                        System.out.println("       1. Update version in pom.xml");
                        System.out.println("       2. Run: mvn clean install");
                        System.out.println("       3. Test your application thoroughly");
                    } else {
                        // Fallback to latest version if no specific fix versions found
                        VulnerabilityDatabase.latestVersion(dep.coordinate())
                            .ifPresent(latest -> {
                                System.out.println("  REMEDIATION: URGENT - Update to latest version: <version>" + latest + "</version>");
                                System.out.println("     1. Update version in pom.xml");
                                System.out.println("     2. Run: mvn clean install");
                                System.out.println("     3. Test your application thoroughly");
                            });
                    }
                });
                
                // If no remediation info available at all
                if (VulnerabilityDatabase.getRemediationInfo(dep).isEmpty()) {
                    System.out.println("  REMEDIATION: Check https://mvnrepository.com/artifact/" + dep.groupId + "/" + dep.artifactId + " for secure alternatives");
                }
            }

            System.out.println();
        }
    }
}
