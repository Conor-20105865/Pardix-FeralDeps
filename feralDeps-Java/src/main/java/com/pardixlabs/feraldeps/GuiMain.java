package com.pardixlabs.feraldeps;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

public class GuiMain extends JFrame {
    private JPanel allDepsPanel;
    private JPanel outdatedPanel;
    private JPanel upToDatePanel;
    private JPanel vulnerablePanel;
    private JPanel noInfoPanel;
    private JPanel ignoredPanel;
    private JButton selectButton;
    private JLabel statusLabel;
    private File currentProjectFolder;
    private IgnoredDependencies ignoredDependencies;
    private Map<String, List<DependencyCheckbox>> allCheckboxes = new HashMap<>();

    public GuiMain() {
        setTitle("FeralDeps - Dependency Scanner");
        setSize(900, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Create UI components
        JPanel topPanel = new JPanel(new BorderLayout());
        
        // Left side: logo and button
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        // Add logo if available
        URL logoURL = getClass().getClassLoader().getResource("Logo.png");
        if (logoURL != null) {
            ImageIcon logoIcon = new ImageIcon(logoURL);
            // Scale logo to reasonable size
            Image scaledImage = logoIcon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
            JLabel logoLabel = new JLabel(new ImageIcon(scaledImage));
            logoLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            leftPanel.add(logoLabel);
        }
        
        selectButton = new JButton("Select Project Folder");
        leftPanel.add(selectButton);
        
        // Right side: status
        statusLabel = new JLabel("No project selected");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        topPanel.add(leftPanel, BorderLayout.WEST);
        topPanel.add(statusLabel, BorderLayout.EAST);

        // Create tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();

        // All Dependencies tab
        allDepsPanel = new JPanel();
        allDepsPanel.setLayout(new BoxLayout(allDepsPanel, BoxLayout.Y_AXIS));
        JScrollPane allDepsScroll = new JScrollPane(allDepsPanel);
        allDepsScroll.getVerticalScrollBar().setUnitIncrement(16);
        tabbedPane.addTab("All Dependencies", allDepsScroll);

        // Outdated Dependencies tab
        outdatedPanel = new JPanel();
        outdatedPanel.setLayout(new BoxLayout(outdatedPanel, BoxLayout.Y_AXIS));
        JScrollPane outdatedScroll = new JScrollPane(outdatedPanel);
        outdatedScroll.getVerticalScrollBar().setUnitIncrement(16);
        tabbedPane.addTab("Outdated", outdatedScroll);

        // Up-to-Date Dependencies tab
        upToDatePanel = new JPanel();
        upToDatePanel.setLayout(new BoxLayout(upToDatePanel, BoxLayout.Y_AXIS));
        JScrollPane upToDateScroll = new JScrollPane(upToDatePanel);
        upToDateScroll.getVerticalScrollBar().setUnitIncrement(16);
        tabbedPane.addTab("Up-to-Date", upToDateScroll);

        // Known Vulnerable Dependencies tab
        vulnerablePanel = new JPanel();
        vulnerablePanel.setLayout(new BoxLayout(vulnerablePanel, BoxLayout.Y_AXIS));
        JScrollPane vulnerableScroll = new JScrollPane(vulnerablePanel);
        vulnerableScroll.getVerticalScrollBar().setUnitIncrement(16);
        tabbedPane.addTab("Known Vulnerable", vulnerableScroll);

        // No Info Available tab
        noInfoPanel = new JPanel();
        noInfoPanel.setLayout(new BoxLayout(noInfoPanel, BoxLayout.Y_AXIS));
        JScrollPane noInfoScroll = new JScrollPane(noInfoPanel);
        noInfoScroll.getVerticalScrollBar().setUnitIncrement(16);
        tabbedPane.addTab("No Info Available", noInfoScroll);

        // Ignored Dependencies tab
        ignoredPanel = new JPanel();
        ignoredPanel.setLayout(new BoxLayout(ignoredPanel, BoxLayout.Y_AXIS));
        JScrollPane ignoredScroll = new JScrollPane(ignoredPanel);
        ignoredScroll.getVerticalScrollBar().setUnitIncrement(16);
        tabbedPane.addTab("Ignored", ignoredScroll);

        // Layout
        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);

        // Button action
        selectButton.addActionListener(e -> selectAndScanProject());
    }

    private void selectAndScanProject() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("Select Project Folder");

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            currentProjectFolder = fileChooser.getSelectedFile();
            ignoredDependencies = new IgnoredDependencies(currentProjectFolder);
            statusLabel.setText("Scanning: " + currentProjectFolder.getName());
            
            // Clear all panels
            allDepsPanel.removeAll();
            outdatedPanel.removeAll();
            upToDatePanel.removeAll();
            vulnerablePanel.removeAll();
            noInfoPanel.removeAll();
            ignoredPanel.removeAll();
            allCheckboxes.clear();
            
            String header = "Searching for pom.xml files in: " + currentProjectFolder.getAbsolutePath();
            addHeaderLabel(allDepsPanel, header);
            addHeaderLabel(outdatedPanel, header);
            addHeaderLabel(upToDatePanel, header);
            addHeaderLabel(vulnerablePanel, header);
            addHeaderLabel(noInfoPanel, header);
            addHeaderLabel(ignoredPanel, header);

            // Run scan in background thread
            new Thread(() -> scanFolder(currentProjectFolder)).start();
        }
    }
    
    private void addHeaderLabel(JPanel panel, String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 12));
        label.setBorder(new EmptyBorder(10, 10, 10, 10));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);
    }

    private void scanFolder(File folder) {
        List<File> pomFiles = new ArrayList<>();
        findPomFiles(folder, pomFiles);

        if (pomFiles.isEmpty()) {
            SwingUtilities.invokeLater(() -> {
                addInfoLabel(allDepsPanel, "No pom.xml files found.");
                addInfoLabel(outdatedPanel, "No pom.xml files found.");
                addInfoLabel(upToDatePanel, "No pom.xml files found.");
                addInfoLabel(vulnerablePanel, "No pom.xml files found.");
                addInfoLabel(noInfoPanel, "No pom.xml files found.");
                addInfoLabel(ignoredPanel, "No pom.xml files found.");
                statusLabel.setText("No pom.xml files found");
            });
            return;
        }

        SwingUtilities.invokeLater(() -> {
            StringBuilder found = new StringBuilder("Found " + pomFiles.size() + " pom.xml file(s):");
            for (File pom : pomFiles) {
                found.append("\n  • ").append(pom.getAbsolutePath());
            }
            
            addInfoLabel(allDepsPanel, found.toString());
            addInfoLabel(outdatedPanel, found.toString());
            addInfoLabel(upToDatePanel, found.toString());
            addInfoLabel(vulnerablePanel, found.toString());
            addInfoLabel(noInfoPanel, found.toString());
            addInfoLabel(ignoredPanel, found.toString());
        });

        // Scan each pom.xml file
        int outdatedCount = 0;
        for (File pomFile : pomFiles) {
            outdatedCount += scanPomFile(pomFile);
        }

        final int finalOutdatedCount = outdatedCount;
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Scan complete - " + pomFiles.size() + " file(s), " + finalOutdatedCount + " outdated");
        });
    }

    private void findPomFiles(File directory, List<File> pomFiles) {
        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                // Skip common directories that won't have project pom.xml files
                String name = file.getName();
                if (!name.equals("target") && !name.equals("node_modules") && 
                    !name.startsWith(".") && !name.equals("build")) {
                    findPomFiles(file, pomFiles);
                }
            } else if (file.getName().equals("pom.xml")) {
                pomFiles.add(file);
            }
        }
    }

    private int scanPomFile(File pomFile) {
        SwingUtilities.invokeLater(() -> {
            addSectionLabel(allDepsPanel, "Scanning: " + pomFile.getAbsolutePath());
            addSectionLabel(outdatedPanel, "Scanning: " + pomFile.getAbsolutePath());
            addSectionLabel(upToDatePanel, "Scanning: " + pomFile.getAbsolutePath());
            addSectionLabel(vulnerablePanel, "Scanning: " + pomFile.getAbsolutePath());
            addSectionLabel(noInfoPanel, "Scanning: " + pomFile.getAbsolutePath());
            addSectionLabel(ignoredPanel, "Scanning: " + pomFile.getAbsolutePath());
        });

        int outdatedCount = 0;

        try {
            List<Dependency> dependencies = PomParser.parse(pomFile);

            if (dependencies.isEmpty()) {
                SwingUtilities.invokeLater(() -> {
                    addInfoLabel(allDepsPanel, "   No dependencies found.");
                    addInfoLabel(outdatedPanel, "   No dependencies found.");
                    addInfoLabel(upToDatePanel, "   No dependencies found.");
                    addInfoLabel(vulnerablePanel, "   No dependencies found.");
                    addInfoLabel(noInfoPanel, "   No dependencies found.");
                    addInfoLabel(ignoredPanel, "   No dependencies found.");
                });
                return 0;
            }

            List<DependencyCheckbox> allCheckboxList = new ArrayList<>();

            for (Dependency dep : dependencies) {
                // Handle ignored dependencies separately
                if (ignoredDependencies.isIgnored(dep)) {
                    // Add to Ignored tab with checked checkbox
                    boolean isVulnerable = VulnerabilityDatabase.isVulnerable(dep);
                    String latestVersion = VulnerabilityDatabase.latestVersion(dep.coordinate()).orElse(null);
                    
                    IgnoredDependencyCheckbox ignoredCheckbox = new IgnoredDependencyCheckbox(dep, pomFile, latestVersion, isVulnerable);
                    SwingUtilities.invokeLater(() -> {
                        ignoredPanel.add(ignoredCheckbox);
                    });
                    continue;
                }

                boolean isVulnerable = VulnerabilityDatabase.isVulnerable(dep);
                String latestVersion = VulnerabilityDatabase.latestVersion(dep.coordinate()).orElse(null);
                boolean isOutdated = latestVersion != null && !latestVersion.equals(dep.version);

                // Create checkbox for All Dependencies
                DependencyCheckbox allCheckbox = new DependencyCheckbox(dep, pomFile, latestVersion, isVulnerable);
                allCheckboxList.add(allCheckbox);
                
                SwingUtilities.invokeLater(() -> {
                    allDepsPanel.add(allCheckbox);
                });

                if (isVulnerable) {
                    DependencyCheckbox vulnCheckbox = new DependencyCheckbox(dep, pomFile, latestVersion, true);
                    allCheckboxList.add(vulnCheckbox);
                    SwingUtilities.invokeLater(() -> {
                        vulnerablePanel.add(vulnCheckbox);
                    });
                }

                if (latestVersion != null) {
                    if (isOutdated) {
                        outdatedCount++;
                        DependencyCheckbox outdatedCheckbox = new DependencyCheckbox(dep, pomFile, latestVersion, isVulnerable);
                        allCheckboxList.add(outdatedCheckbox);
                        SwingUtilities.invokeLater(() -> {
                            outdatedPanel.add(outdatedCheckbox);
                        });
                    } else {
                        DependencyCheckbox upToDateCheckbox = new DependencyCheckbox(dep, pomFile, latestVersion, isVulnerable);
                        allCheckboxList.add(upToDateCheckbox);
                        SwingUtilities.invokeLater(() -> {
                            upToDatePanel.add(upToDateCheckbox);
                        });
                    }
                } else {
                    DependencyCheckbox noInfoCheckbox = new DependencyCheckbox(dep, pomFile, null, isVulnerable);
                    allCheckboxList.add(noInfoCheckbox);
                    SwingUtilities.invokeLater(() -> {
                        noInfoPanel.add(noInfoCheckbox);
                    });
                }
            }

            // Store checkboxes for this pom file
            allCheckboxes.put(pomFile.getAbsolutePath(), allCheckboxList);

            SwingUtilities.invokeLater(() -> {
                allDepsPanel.revalidate();
                allDepsPanel.repaint();
                outdatedPanel.revalidate();
                outdatedPanel.repaint();
                upToDatePanel.revalidate();
                upToDatePanel.repaint();
                vulnerablePanel.revalidate();
                vulnerablePanel.repaint();
                noInfoPanel.revalidate();
                noInfoPanel.repaint();
                ignoredPanel.revalidate();
                ignoredPanel.repaint();
            });

        } catch (Exception e) {
            String errorMsg = "   Error parsing file: " + e.getMessage();
            SwingUtilities.invokeLater(() -> {
                addInfoLabel(allDepsPanel, errorMsg);
                addInfoLabel(outdatedPanel, errorMsg);
                addInfoLabel(upToDatePanel, errorMsg);
                addInfoLabel(vulnerablePanel, errorMsg);
                addInfoLabel(noInfoPanel, errorMsg);
                addInfoLabel(ignoredPanel, errorMsg);
            });
        }

        return outdatedCount;
    }
    
    private void addInfoLabel(JPanel panel, String text) {
        JLabel label = new JLabel("<html>" + text.replace("\n", "<br>") + "</html>");
        label.setFont(new Font("SansSerif", Font.PLAIN, 11));
        label.setBorder(new EmptyBorder(5, 10, 5, 10));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);
    }
    
    private void addSectionLabel(JPanel panel, String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 11));
        label.setBorder(new EmptyBorder(10, 10, 5, 10));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);
    }
    
    /**
     * Custom panel that represents a dependency with a checkbox to hide it
     */
    private class DependencyCheckbox extends JPanel {
        private Dependency dependency;
        private JCheckBox hideCheckbox;
        
        public DependencyCheckbox(Dependency dep, File pomFile, String latestVersion, boolean isVulnerable) {
            this.dependency = dep;
            
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(5, 10, 5, 10));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
            
            // Checkbox on the left
            hideCheckbox = new JCheckBox();
            hideCheckbox.setToolTipText("Hide this dependency from future scans");
            hideCheckbox.addActionListener(e -> {
                if (hideCheckbox.isSelected()) {
                    ignoredDependencies.ignore(dep);
                    // Remove all instances of this checkbox from all tabs
                    removeAllInstancesOfDependency(dep);
                }
            });
            
            JPanel checkboxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            checkboxPanel.add(hideCheckbox);
            add(checkboxPanel, BorderLayout.WEST);
            
            // Dependency info in the center
            StringBuilder info = new StringBuilder();
            info.append("<html>");
            info.append("<b>").append(dep.coordinate()).append(":").append(dep.version).append("</b><br>");
            info.append("<span style='font-size:10px;color:#666;'>Scope: ").append(dep.scope).append(" | ");
            info.append(dep.getVersionConstraintType()).append("</span><br>");
            
            if (latestVersion != null) {
                if (!latestVersion.equals(dep.version)) {
                    info.append("<span style='color:#FF6600;'>WARNING: Outdated → latest: ").append(latestVersion).append("</span>");
                    if (dep.isVersionLocked()) {
                        info.append("<span style='color:#FF0000;'> (Version LOCKED - may break if upgraded)</span>");
                    }
                    info.append("<br><span style='color:#0066CC;'>REMEDIATION: Update to &lt;version&gt;").append(latestVersion).append("&lt;/version&gt;</span>");
                } else {
                    info.append("<span style='color:#00AA00;'>Up-to-date (latest: ").append(latestVersion).append(")</span>");
                }
            } else {
                info.append("<span style='color:#888;'>? No version info available</span>");
            }
            
            if (isVulnerable) {
                info.append("<br><span style='color:#FF0000;'><b>WARNING: VULNERABLE - Security issues detected</b></span>");
                if (dep.isVersionLocked()) {
                    info.append("<span style='color:#FF0000;'> (CRITICAL: Vulnerable version is LOCKED)</span>");
                }
                
                // Get specific remediation from OSV
                VulnerabilityDatabase.getRemediationInfo(dep).ifPresent(remediation -> {
                    if (remediation.hasRemediation && !remediation.fixedVersions.isEmpty()) {
                        info.append("<br><span style='color:#CC0000;'><b>URGENT Remediation:</b> Upgrade to secure version:</span>");
                        for (String fixedVer : remediation.fixedVersions) {
                            info.append("<br><span style='color:#CC0000;'>  • &lt;version&gt;").append(fixedVer).append("&lt;/version&gt; (fixes vulnerabilities)</span>");
                        }
                        if (!remediation.summary.isEmpty()) {
                            info.append("<br><span style='color:#666;font-size:10px;'>Issue: ").append(remediation.summary).append("</span>");
                        }
                    } else if (latestVersion != null) {
                        info.append("<br><span style='color:#CC0000;'><b>URGENT Remediation:</b> Update to secure version &lt;version&gt;").append(latestVersion).append("&lt;/version&gt;</span>");
                    } else {
                        info.append("<br><span style='color:#CC0000;'><b>Remediation:</b> Find secure alternative at mvnrepository.com</span>");
                    }
                });
            }
            
            info.append("</html>");
            
            JLabel infoLabel = new JLabel(info.toString());
            infoLabel.setBorder(new EmptyBorder(0, 5, 0, 5));
            add(infoLabel, BorderLayout.CENTER);
            
            // Add update button on the right for outdated or vulnerable dependencies
            if (latestVersion != null && !latestVersion.equals(dep.version)) {
                JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
                JButton updateButton = new JButton("Update");
                updateButton.setToolTipText("Update to version " + latestVersion + " in pom.xml");
                updateButton.addActionListener(e -> updateDependency(dep, latestVersion, pomFile));
                buttonPanel.add(updateButton);
                add(buttonPanel, BorderLayout.EAST);
            } else if (isVulnerable) {
                // For vulnerable dependencies, show update button with first fixed version
                VulnerabilityDatabase.getRemediationInfo(dep).ifPresent(remediation -> {
                    if (remediation.hasRemediation && !remediation.fixedVersions.isEmpty()) {
                        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
                        String fixVersion = remediation.fixedVersions.get(0);
                        JButton updateButton = new JButton("Update");
                        updateButton.setToolTipText("Update to secure version " + fixVersion + " in pom.xml");
                        updateButton.addActionListener(e -> updateDependency(dep, fixVersion, pomFile));
                        buttonPanel.add(updateButton);
                        add(buttonPanel, BorderLayout.EAST);
                    }
                });
            }
        }
    }
    
    /**
     * Update a dependency in the pom.xml file
     */
    private void updateDependency(Dependency dep, String newVersion, File pomFile) {
        // Show confirmation dialog with warning
        int choice = JOptionPane.showOptionDialog(this,
            "Update " + dep.coordinate() + " from " + dep.version + " to " + newVersion + "?\n\n" +
            "Options:\n" +
            "• Update & Test: Update and compile to verify it works\n" +
            "• Update Only: Update without testing (faster)\n" +
            "• Cancel: Don't update",
            "Confirm Update",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            new Object[]{"Update & Test", "Update Only", "Cancel"},
            "Update & Test");
        
        if (choice == JOptionPane.CANCEL_OPTION || choice == JOptionPane.CLOSED_OPTION) {
            return; // User cancelled
        }
        
        boolean runTest = (choice == JOptionPane.YES_OPTION);
        
        try {
            // Read the pom.xml file
            String content = new String(java.nio.file.Files.readAllBytes(pomFile.toPath()));
            String originalContent = content;
            
            // Try to find and update the specific dependency
            // Pattern: <groupId>group</groupId><artifactId>artifact</artifactId><version>oldVersion</version>
            // We need to be careful to match the exact dependency
            
            String[] lines = content.split("\n");
            StringBuilder updatedContent = new StringBuilder();
            boolean inTargetDependency = false;
            boolean foundGroupId = false;
            boolean foundArtifactId = false;
            
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                
                // Check if we're entering a dependency tag
                if (line.trim().equals("<dependency>")) {
                    inTargetDependency = true;
                    foundGroupId = false;
                    foundArtifactId = false;
                }
                
                // Check if this is our target dependency
                if (inTargetDependency) {
                    if (line.contains("<groupId>" + dep.groupId + "</groupId>")) {
                        foundGroupId = true;
                    }
                    if (line.contains("<artifactId>" + dep.artifactId + "</artifactId>")) {
                        foundArtifactId = true;
                    }
                    
                    // If we found both groupId and artifactId, update the version
                    if (foundGroupId && foundArtifactId && line.contains("<version>")) {
                        String indent = line.substring(0, line.indexOf("<version>"));
                        line = indent + "<version>" + newVersion + "</version>";
                        inTargetDependency = false; // We found and updated it
                    }
                    
                    // Reset if we exit the dependency tag
                    if (line.trim().equals("</dependency>")) {
                        inTargetDependency = false;
                    }
                }
                
                updatedContent.append(line).append("\n");
            }
            
            // Write back to file if content changed
            String newContent = updatedContent.toString();
            if (!newContent.equals(originalContent)) {
                java.nio.file.Files.write(pomFile.toPath(), newContent.getBytes());
                
                if (runTest) {
                    // Test the update by trying to compile
                    testDependencyUpdate(dep, newVersion, pomFile, originalContent);
                } else {
                    // Show success message without testing
                    JOptionPane.showMessageDialog(this,
                        "Successfully updated " + dep.coordinate() + " from " + dep.version + " to " + newVersion + "\n" +
                        "File: " + pomFile.getName() + "\n\n" +
                        "Please rescan the project to see the changes.",
                        "Dependency Updated",
                        JOptionPane.INFORMATION_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this,
                    "Could not find the dependency in " + pomFile.getName() + "\n" +
                    "The dependency may be inherited from a parent POM.",
                    "Update Failed",
                    JOptionPane.WARNING_MESSAGE);
            }
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Error updating dependency: " + ex.getMessage(),
                "Update Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Test if the dependency update breaks compilation
     */
    private void testDependencyUpdate(Dependency dep, String newVersion, File pomFile, String originalContent) {
        // Show progress dialog
        JDialog progressDialog = new JDialog(this, "Testing Update", true);
        JLabel progressLabel = new JLabel("Compiling with new dependency version...");
        progressLabel.setBorder(new EmptyBorder(20, 20, 20, 20));
        progressDialog.add(progressLabel);
        progressDialog.pack();
        progressDialog.setLocationRelativeTo(this);
        
        // Run compilation in background thread
        new Thread(() -> {
            try {
                File projectDir = pomFile.getParentFile();
                
                // Run mvn compile
                ProcessBuilder pb = new ProcessBuilder("mvn", "clean", "compile");
                pb.directory(projectDir);
                pb.redirectErrorStream(true);
                
                Process process = pb.start();
                
                // Capture output
                StringBuilder output = new StringBuilder();
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                }
                
                int exitCode = process.waitFor();
                
                SwingUtilities.invokeLater(() -> {
                    progressDialog.dispose();
                    
                    if (exitCode == 0) {
                        // Compilation successful
                        JOptionPane.showMessageDialog(this,
                            "Update successful!\n\n" +
                            dep.coordinate() + ": " + dep.version + " → " + newVersion + "\n" +
                            "File: " + pomFile.getName() + "\n\n" +
                            "Project compiled successfully with the new version.\n" +
                            "Please rescan the project to see the changes.",
                            "Update Verified",
                            JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        // Compilation failed - offer to revert
                        int revert = JOptionPane.showConfirmDialog(this,
                            "WARNING: Compilation failed with new version!\n\n" +
                            dep.coordinate() + ": " + dep.version + " → " + newVersion + "\n\n" +
                            "The update may have introduced breaking changes.\n" +
                            "Do you want to revert to the original version?\n\n" +
                            "Build output:\n" + output.substring(Math.max(0, output.length() - 500)),
                            "Compilation Failed",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE);
                        
                        if (revert == JOptionPane.YES_OPTION) {
                            try {
                                // Revert the change
                                java.nio.file.Files.write(pomFile.toPath(), originalContent.getBytes());
                                JOptionPane.showMessageDialog(this,
                                    "Reverted to original version: " + dep.version,
                                    "Reverted",
                                    JOptionPane.INFORMATION_MESSAGE);
                            } catch (Exception e) {
                                JOptionPane.showMessageDialog(this,
                                    "Error reverting: " + e.getMessage(),
                                    "Revert Error",
                                    JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                });
                
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    progressDialog.dispose();
                    JOptionPane.showMessageDialog(this,
                        "Error testing update: " + e.getMessage() + "\n\n" +
                        "The dependency has been updated but not tested.",
                        "Test Error",
                        JOptionPane.WARNING_MESSAGE);
                });
            }
        }).start();
        
        // Show the progress dialog (blocks until compilation completes)
        progressDialog.setVisible(true);
    }
    
    /**
     * Remove all instances of a dependency from all tabs
     */
    private void removeAllInstancesOfDependency(Dependency dep) {
        String depKey = dep.coordinate() + ":" + dep.version;
        
        for (List<DependencyCheckbox> checkboxes : allCheckboxes.values()) {
            for (DependencyCheckbox checkbox : new ArrayList<>(checkboxes)) {
                if ((checkbox.dependency.coordinate() + ":" + checkbox.dependency.version).equals(depKey)) {
                    Container parent = checkbox.getParent();
                    if (parent != null) {
                        parent.remove(checkbox);
                        parent.revalidate();
                        parent.repaint();
                    }
                }
            }
        }
    }
    
    /**
     * Custom panel for ignored dependencies with checked checkbox that can be unchecked to restore
     */
    private class IgnoredDependencyCheckbox extends JPanel {
        private Dependency dependency;
        private JCheckBox restoreCheckbox;
        
        public IgnoredDependencyCheckbox(Dependency dep, File pomFile, String latestVersion, boolean isVulnerable) {
            this.dependency = dep;
            
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(5, 10, 5, 10));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
            
            // Checkbox on the left - checked by default
            restoreCheckbox = new JCheckBox();
            restoreCheckbox.setSelected(true);
            restoreCheckbox.setToolTipText("Uncheck to restore this dependency to the scan results");
            restoreCheckbox.addActionListener(e -> {
                if (!restoreCheckbox.isSelected()) {
                    // Remove from ignored list
                    ignoredDependencies.unignore(dep);
                    // Remove from ignored tab
                    Container parent = IgnoredDependencyCheckbox.this.getParent();
                    if (parent != null) {
                        parent.remove(IgnoredDependencyCheckbox.this);
                        parent.revalidate();
                        parent.repaint();
                    }
                    // Show message that they need to rescan
                    JOptionPane.showMessageDialog(GuiMain.this,
                        "Dependency restored. Please rescan the project to see it in other tabs.",
                        "Dependency Restored",
                        JOptionPane.INFORMATION_MESSAGE);
                }
            });
            
            JPanel checkboxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            checkboxPanel.add(restoreCheckbox);
            add(checkboxPanel, BorderLayout.WEST);
            
            // Dependency info in the center
            StringBuilder info = new StringBuilder();
            info.append("<html>");
            info.append("<b>").append(dep.coordinate()).append(":").append(dep.version).append("</b><br>");
            info.append("<span style='font-size:10px;color:#666;'>Scope: ").append(dep.scope).append(" | ");
            info.append(dep.getVersionConstraintType()).append("</span><br>");
            info.append("<span style='color:#888;'>Hidden from scan results</span>");
            
            if (latestVersion != null) {
                if (!latestVersion.equals(dep.version)) {
                    info.append("<br><span style='color:#FF6600;'>WARNING: Outdated → latest: ").append(latestVersion).append("</span>");
                    if (dep.isVersionLocked()) {
                        info.append("<span style='color:#FF0000;'> (Version LOCKED)</span>");
                    }
                    info.append("<br><span style='color:#0066CC;'>REMEDIATION: Update to &lt;version&gt;").append(latestVersion).append("&lt;/version&gt;</span>");
                }
            }
            
            if (isVulnerable) {
                info.append("<br><span style='color:#FF0000;'><b>WARNING: VULNERABLE - Security issues detected</b></span>");
                
                // Get specific remediation from OSV
                VulnerabilityDatabase.getRemediationInfo(dep).ifPresent(remediation -> {
                    if (remediation.hasRemediation && !remediation.fixedVersions.isEmpty()) {
                        info.append("<br><span style='color:#CC0000;'><b>URGENT Remediation:</b> Upgrade to secure version:</span>");
                        for (String fixedVer : remediation.fixedVersions) {
                            info.append("<br><span style='color:#CC0000;'>  • &lt;version&gt;").append(fixedVer).append("&lt;/version&gt; (fixes vulnerabilities)</span>");
                        }
                        if (!remediation.summary.isEmpty()) {
                            info.append("<br><span style='color:#666;font-size:10px;'>Issue: ").append(remediation.summary).append("</span>");
                        }
                    } else if (latestVersion != null) {
                        info.append("<br><span style='color:#CC0000;'><b>URGENT Remediation:</b> Update to secure version &lt;version&gt;").append(latestVersion).append("&lt;/version&gt;</span>");
                    } else {
                        info.append("<br><span style='color:#CC0000;'><b>Remediation:</b> Find secure alternative at mvnrepository.com</span>");
                    }
                });
            }
            
            info.append("</html>");
            
            JLabel infoLabel = new JLabel(info.toString());
            infoLabel.setBorder(new EmptyBorder(0, 5, 0, 5));
            add(infoLabel, BorderLayout.CENTER);
            
            // Add update button on the right for outdated or vulnerable dependencies  
            if (latestVersion != null && !latestVersion.equals(dep.version)) {
                JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
                JButton updateButton = new JButton("Update");
                updateButton.setToolTipText("Update to version " + latestVersion + " in pom.xml");
                updateButton.addActionListener(e -> updateDependency(dep, latestVersion, pomFile));
                buttonPanel.add(updateButton);
                add(buttonPanel, BorderLayout.EAST);
            } else if (isVulnerable) {
                // For vulnerable dependencies, show update button with first fixed version
                VulnerabilityDatabase.getRemediationInfo(dep).ifPresent(remediation -> {
                    if (remediation.hasRemediation && !remediation.fixedVersions.isEmpty()) {
                        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
                        String fixVersion = remediation.fixedVersions.get(0);
                        JButton updateButton = new JButton("Update");
                        updateButton.setToolTipText("Update to secure version " + fixVersion + " in pom.xml");
                        updateButton.addActionListener(e -> updateDependency(dep, fixVersion, pomFile));
                        buttonPanel.add(updateButton);
                        add(buttonPanel, BorderLayout.EAST);
                    }
                });
            }
        }
    }

    public static void main(String[] args) {
        // Set system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Use default if system L&F fails
        }

        // Show splash screen first, then main GUI
        SplashScreen.show(() -> {
            SwingUtilities.invokeLater(() -> {
                GuiMain frame = new GuiMain();
                frame.setVisible(true);
            });
        });
    }
}
