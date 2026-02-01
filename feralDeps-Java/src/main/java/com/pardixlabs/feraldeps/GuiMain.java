package com.pardixlabs.feraldeps;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                    info.append("<span style='color:#FF6600;'>⚠ Outdated → latest: ").append(latestVersion).append("</span>");
                    if (dep.isVersionLocked()) {
                        info.append("<span style='color:#FF0000;'> (Version LOCKED - may break if upgraded)</span>");
                    }
                } else {
                    info.append("<span style='color:#00AA00;'>✓ Up-to-date (latest: ").append(latestVersion).append(")</span>");
                }
            } else {
                info.append("<span style='color:#888;'>? No version info available</span>");
            }
            
            if (isVulnerable) {
                info.append("<br><span style='color:#FF0000;'><b>⚠ VULNERABLE - Security issues detected</b></span>");
                if (dep.isVersionLocked()) {
                    info.append("<span style='color:#FF0000;'> (CRITICAL: Vulnerable version is LOCKED)</span>");
                }
            }
            
            info.append("</html>");
            
            JLabel infoLabel = new JLabel(info.toString());
            infoLabel.setBorder(new EmptyBorder(0, 5, 0, 5));
            add(infoLabel, BorderLayout.CENTER);
        }
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
                    info.append("<br><span style='color:#FF6600;'>⚠ Outdated → latest: ").append(latestVersion).append("</span>");
                    if (dep.isVersionLocked()) {
                        info.append("<span style='color:#FF0000;'> (Version LOCKED)</span>");
                    }
                }
            }
            
            if (isVulnerable) {
                info.append("<br><span style='color:#FF0000;'><b>⚠ VULNERABLE - Security issues detected</b></span>");
            }
            
            info.append("</html>");
            
            JLabel infoLabel = new JLabel(info.toString());
            infoLabel.setBorder(new EmptyBorder(0, 5, 0, 5));
            add(infoLabel, BorderLayout.CENTER);
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
