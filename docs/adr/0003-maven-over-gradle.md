# 0003. Maven over Gradle

Status: accepted

## Context
Both frameworks support Maven and Gradle equally. The build is not what is being compared.

## Decision
Both apps use Maven, one standalone `pom.xml` per app, no parent project.

## Consequences
- A declarative build that most enterprise Java teams already read.
- No shared build logic between the apps, which is intended (see ADR 0002).
