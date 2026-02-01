# FeralDeps - Maven Dependency Security Scanner

A powerful Java application that scans Maven projects for outdated and vulnerable dependencies, providing automated remediation with safety verification.

## Features

### Comprehensive Dependency Scanning
- **Automated POM Analysis**: Recursively scans project folders for all `pom.xml` files
- **Multi-Tab Organization**: View dependencies categorized by status (All, Outdated, Up-to-Date, Vulnerable, No Info)
- **Version Constraint Detection**: Identifies locked vs. flexible version constraints
- **Scope Tracking**: Displays dependency scopes (compile, test, runtime, etc.)

### Security & Vulnerability Detection
- **OSV Integration**: Queries the Open Source Vulnerabilities (OSV) database for known security issues
- **Detailed Remediation Info**: Displays specific fixed versions that address vulnerabilities
- **Maven Central Integration**: Checks for latest available versions from Maven Central
- **Vulnerability Summaries**: Shows descriptions of security issues to help prioritize fixes

### One-Click Updates with Safety Verification
- **Automated POM Updates**: Update dependency versions directly from the GUI with a single click
- **Compilation Testing**: Optionally test updates by running `mvn clean compile` before committing
- **Smart Revert**: Automatically rollback failed updates to prevent breaking changes
- **Build Output Display**: See compilation errors to understand compatibility issues

### Smart Management
- **Dependency Ignore List**: Hide dependencies you don't want to update (stored per-project)
- **Caching System**: Reduces API calls for faster subsequent scans
- **Background Processing**: Non-blocking scans with progress indicators
- **Multi-Project Support**: Scan entire folders containing multiple Maven projects

### User Interface
- **GUI Mode**: Clean, intuitive Swing interface with tabbed organization
- **CLI Mode**: Command-line support for automation and CI/CD integration
- **Splash Screen**: Branded startup experience
- **Tooltips & Help**: Contextual information throughout the interface

## Installation

### Prerequisites
- Java 17 or higher
- Maven 3.6+

### Build from Source
```bash
cd feralDeps-Java
mvn clean package
```

The compiled JAR will be in `target/feralDeps-Java-1.0-SNAPSHOT.jar`

## Usage

### GUI Mode (Default)
```bash
java -jar target/feralDeps-Java-1.0-SNAPSHOT.jar
```

Then use the "Select Project Folder" button to choose a directory containing Maven projects.

### CLI Mode
```bash
java -jar target/feralDeps-Java-1.0-SNAPSHOT.jar /path/to/pom.xml
```

Outputs a text-based report of all dependencies with their status.

## How It Works

1. **Scan**: FeralDeps parses all `pom.xml` files in the selected directory
2. **Check Versions**: Queries Maven Central for the latest available versions
3. **Check Security**: Queries the OSV API for known vulnerabilities and fixes
4. **Display Results**: Organizes findings by category with remediation advice
5. **Update (Optional)**: Click "Update" to modify the POM and optionally verify compilation

## Remediation Workflow

### For Outdated Dependencies
1. Review the "Outdated" tab to see which dependencies have newer versions
2. Check if version is locked (may require code changes if upgraded)
3. Click "Update" button
4. Choose "Update & Test" to verify compilation or "Update Only" for quick update
5. Rescan to see updated status

### For Vulnerable Dependencies
1. Review the "Known Vulnerable" tab for security issues
2. Read the vulnerability summary and recommended fix versions
3. Click "Update" to upgrade to the first secure version
4. Choose "Update & Test" to ensure compatibility
5. If compilation fails, choose to revert or manually resolve breaking changes

## Configuration

### Ignored Dependencies
Ignored dependencies are stored in `.feraldeps-ignore` in each project's root directory. This file contains one dependency coordinate per line:
```
groupId:artifactId:version
```

You can manually edit this file or use the checkboxes in the GUI to ignore/restore dependencies.

## Architecture

### Core Components
- **Main.java**: Entry point and CLI implementation
- **GuiMain.java**: Swing-based GUI with tabbed interface
- **PomParser.java**: XML parsing for Maven POM files
- **VulnerabilityDatabase.java**: API integration for version and security checks
- **Dependency.java**: Data model for dependency information
- **IgnoredDependencies.java**: Manages per-project ignore lists

### External APIs
- **Maven Central Search API**: `https://search.maven.org/solrsearch/select`
- **OSV API**: `https://api.osv.dev/v1/query`

## Upcoming Features

### Enhanced Security & Analysis
- **CVSS Score Display**: Show severity ratings for vulnerabilities (Critical, High, Medium, Low)
- **CVE Links**: Direct links to vulnerability databases for detailed information
- **Dependency Tree Analysis**: Identify transitive dependencies and their impact
- **License Compliance Checking**: Flag dependencies with incompatible licenses
- **Dependency Conflict Detection**: Identify version conflicts in the dependency tree

### Automation & Integration
- **Batch Update Mode**: Update multiple dependencies at once with a single test run
- **CI/CD Integration**: GitHub Actions / Jenkins plugin for automated scanning
- **Scheduled Scans**: Periodic background checks for new vulnerabilities
- **Email/Slack Notifications**: Alert teams when new vulnerabilities are discovered
- **SBOM Export**: Generate Software Bill of Materials in CycloneDX/SPDX formats

### Reporting & Analytics
- **PDF/HTML Reports**: Exportable scan results for documentation and compliance
- **Trend Analysis**: Track dependency health over time with charts
- **Risk Dashboard**: Visual overview of project security posture
- **Comparison Reports**: Before/after analysis of updates
- **Custom Report Templates**: Configurable report formats for different stakeholders

### Developer Experience
- **IDE Plugins**: IntelliJ IDEA and VS Code extensions
- **Git Integration**: Automatic commit messages for dependency updates
- **Rollback History**: Track and revert to previous dependency configurations
- **Smart Suggestions**: ML-based recommendations for safe upgrade paths
- **Bulk Ignore Patterns**: Ignore multiple dependencies by pattern (e.g., all test scopes)

### Multi-Ecosystem Support
- **Gradle Support**: Extend scanning to build.gradle and build.gradle.kts files
- **npm/yarn**: JavaScript package vulnerability scanning
- **pip/requirements.txt**: Python dependency analysis
- **NuGet**: .NET package scanning
- **Multi-Language Projects**: Unified dashboard for polyglot codebases

### UI/UX Improvements
- **Dark Mode**: Eye-friendly theme for extended use
- **Keyboard Shortcuts**: Quick navigation and actions
- **Customizable Columns**: Show/hide information based on preferences
- **Search & Filter**: Quick find for specific dependencies
- **Drag & Drop**: Drop POM files directly into the GUI

### Enterprise Features
- **Private Repository Support**: Scan dependencies from internal artifact repositories
- **Access Control**: Role-based permissions for teams
- **Audit Logging**: Track who updated what and when
- **Policy Enforcement**: Automated rules (e.g., block high-severity vulnerabilities)
- **Proxy Configuration**: Support for corporate network environments

### Testing & Validation
- **Unit Test Impact Analysis**: Run only tests affected by dependency updates
- **Performance Benchmarking**: Compare application performance before/after updates
- **Breaking Change Detection**: Advanced static analysis to predict API incompatibilities
- **Staged Rollouts**: Test updates in dev/staging before production
- **Compatibility Matrix**: Track which versions work together

## License

MIT

## Credits

Developed by Pardix Labs

### Third-Party Services
- Maven Central API for version information
- OSV (Open Source Vulnerabilities) for security data