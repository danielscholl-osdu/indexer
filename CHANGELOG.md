# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 1.0.0 (2025-07-09)


### 🐛 Bug Fixes

* Aws build command ([48de2b9](https://github.com/danielscholl-osdu/indexer/commit/48de2b9ead9bd0f80a0bdd1d76a98400ec7672c5))
* Disable bagOfWords to complement mapBooleanToString for Azure in M25 ([03a02d4](https://github.com/danielscholl-osdu/indexer/commit/03a02d48b91eb95706b65b598759ae030bced790))
* Disable bagOfWords to complement mapBooleanToString for Azure in M25 ([aed6f55](https://github.com/danielscholl-osdu/indexer/commit/aed6f55ce54abf3ca5d4890ba89573cfa0417b94))
* Enabling mapping boolean to string fix for AWS ([0826dce](https://github.com/danielscholl-osdu/indexer/commit/0826dce9f5cd7f487becbc2c12ef8dd1c045145a))
* Enabling mapping boolean to string fix for AWS ([0c37fc9](https://github.com/danielscholl-osdu/indexer/commit/0c37fc9b5fb01fb808d2fc5cc17b0683c161f06e))
* Removed unused comments per sonarqube ([c101014](https://github.com/danielscholl-osdu/indexer/commit/c10101407863772db56a90f58aece0f4d0553cc9))
* Removed unused comments per sonarqube ([f3af12f](https://github.com/danielscholl-osdu/indexer/commit/f3af12fc2643b99ab2a3542a21148623c88da7cc))
* Removing core profile from aws build command ([c9cd398](https://github.com/danielscholl-osdu/indexer/commit/c9cd39897ec46bbe4a07057844458d1f4894b24c))
* Spring cves ([e711e2d](https://github.com/danielscholl-osdu/indexer/commit/e711e2dd340df34a0c78de51e03a2c0c088a11e8))
* Spring cves ([62e5ef4](https://github.com/danielscholl-osdu/indexer/commit/62e5ef46bb225b613ad2850772f19e7cfff583e0))
* Thread Exhaustion and ES Connection Errors by using request-scoped ElasticSearch client ([2864971](https://github.com/danielscholl-osdu/indexer/commit/28649712497257875a3683e00449fbd823e71da3))
* Tomcat-core security-crypto json-smart netty-common ([220cb5f](https://github.com/danielscholl-osdu/indexer/commit/220cb5f5787c751e7e5283376a2f109af4add0c1))
* Tomcat-core security-crypto json-smart netty-common ([9698e2b](https://github.com/danielscholl-osdu/indexer/commit/9698e2bdd2a49eecb55abb11764c10c66bb020ca))


### 🔧 Miscellaneous

* Complete repository initialization ([096753d](https://github.com/danielscholl-osdu/indexer/commit/096753df654de5a704ab3769046ed103400afbfa))
* Copy configuration and workflows from main branch ([8a6b3dd](https://github.com/danielscholl-osdu/indexer/commit/8a6b3dd8f29ad838ba9d7debbc3b6522aeacaf36))
* Deleting aws helm chart ([359b741](https://github.com/danielscholl-osdu/indexer/commit/359b741c71e88c541d8db53198ba1290056f2a65))
* Deleting aws helm chart ([f96a0e1](https://github.com/danielscholl-osdu/indexer/commit/f96a0e1b62df629dc1ad4949c9780c79d84a863b))
* Removing helm copy from aws buildspec ([c012554](https://github.com/danielscholl-osdu/indexer/commit/c012554dcfb522ec2640d4f0cb89c8e27dbafb45))

## [2.0.0] - Major Workflow Enhancement & Documentation Release

### ✨ Features
- **Comprehensive MkDocs Documentation Site**: Complete documentation overhaul with GitHub Pages deployment
- **Automated Cascade Failure Recovery**: System automatically recovers from cascade workflow failures
- **Human-Centric Cascade Pattern**: Issue lifecycle tracking with human notifications for critical decisions
- **Integration Validation**: Comprehensive validation system for cascade workflows
- **Claude Workflow Integration**: Full Claude Code CLI support with Maven MCP server integration
- **GitHub Copilot Enhancement**: Java development environment setup and firewall configuration
- **Fork Resources Staging Pattern**: Template-based staging for fork-specific configurations
- **Conventional Commits Validation**: Complete validation system with all supported commit types
- **Enhanced PR Label Management**: Simplified production PR labels with automated issue closure
- **Meta Commit Strategy**: Advanced release-please integration for better version management
- **Push Protection Handling**: Sophisticated upstream secrets detection and resolution workflows

### 🔨 Build System
- **Workflow Separation Pattern**: Template development vs. fork instance workflow isolation
- **Template Workflow Management**: 9 comprehensive template workflows for fork management
- **Enhanced Action Reliability**: Improved cascade workflow trigger reliability with PR event filtering
- **Base64 Support**: Enhanced create-enhanced-pr action with encoding capabilities

### 📚 Documentation
- **Structured MkDocs Site**: Complete documentation architecture with GitHub Pages
- **AI-First Development Docs**: Comprehensive guides for AI-enhanced development
- **ADR Documentation**: 20+ Architectural Decision Records covering all major decisions
- **Workflow Specifications**: Detailed documentation for all 9 template workflows
- **Streamlined README**: Focused quick-start guide directing to comprehensive documentation

### 🛡️ Security & Reliability
- **Advanced Push Protection**: Intelligent handling of upstream repositories with secrets
- **Branch Protection Integration**: Automated branch protection rule management
- **Security Pattern Recognition**: Enhanced security scanning and pattern detection
- **MCP Configuration**: Secure Model Context Protocol integration for AI development

### 🔧 Workflow Enhancements
- **Cascade Monitoring**: Advanced cascade workflow monitoring and SLA management
- **Dependabot Integration**: Enhanced dependabot validation and automation
- **Template Synchronization**: Sophisticated template update propagation system
- **Issue State Tracking**: Advanced issue lifecycle management and tracking
- **GITHUB_TOKEN Standardization**: Improved token handling across all workflows

### ♻️ Code Refactoring
- **Removed AI_EVOLUTION.md**: Migrated to structured ADR approach for better maintainability
- **Simplified README Structure**: Eliminated redundancy between README and documentation site
- **Enhanced Initialization Cleanup**: Improved fork repository cleanup and setup process
- **Standardized Error Handling**: Consistent error handling patterns across all workflows

### 🐛 Bug Fixes
- **YAML Syntax Issues**: Resolved multiline string handling in workflow configurations
- **Release Workflow Compatibility**: Updated to googleapis/release-please-action@v4
- **MCP Server Configuration**: Fixed Maven MCP server connection and configuration issues
- **Cascade Trigger Reliability**: Implemented pull_request_target pattern for better triggering
- **Git Diff Syntax**: Corrected git command syntax in sync-template workflow
- **Label Management**: Standardized label usage across all workflows and templates

## [1.0.0] - Initial Release

### ✨ Features
- Initial release of OSDU Fork Management Template
- Automated fork initialization workflow
- Daily upstream synchronization with AI-enhanced PR descriptions
- Three-branch management strategy (main, fork_upstream, fork_integration)
- Automated conflict detection and resolution guidance
- Semantic versioning and release management
- Template development workflows separation

### 📚 Documentation
- Complete architectural decision records (ADRs)
- Product requirements documentation
- Development and usage guides
- GitHub Actions workflow documentation
