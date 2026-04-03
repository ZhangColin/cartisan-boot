# Gradle Configuration Backup

**Date:** 2026-04-03
**Reason:** Maven Migration

## Key Gradle Features to Preserve

1. **Java Platform**: cartisan-dependencies uses `java-platform` plugin
2. **Java 21**: All modules use Java 21 toolchain
3. **Compiler Args**: `-parameters` flag (preserve parameter names)
4. **Annotation Processors**: Lombok → MapStruct → lombok-mapstruct-binding
5. **Testing**: JUnit 5 platform, JaCoCo coverage
6. **PIT**: cartisan-core has mutation testing (70% threshold)
7. **JavaDoc**: cartisan-core has strict validation (Xdoclint:all,-missing)
8. **Publishing**: Maven local repository at build/local-maven-repo

## Module Dependencies

cartisan-dependencies (BOM)
  → cartisan-core
    → cartisan-web
      → cartisan-security / cartisan-data-jpa / cartisan-event
        → cartisan-data-query / cartisan-ai

  → cartisan-test (depends on cartisan-core)
