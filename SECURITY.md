# Security Policy

## Reporting a Vulnerability

**Do not open a public GitHub issue for security vulnerabilities.**

Please report them privately via [GitHub's private vulnerability reporting](https://github.com/RodrigoUma04/fennixs/security/advisories/new).

Include as much detail as possible:

- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Any suggested fix

We aim to respond within 72 hours. If the issue is confirmed, a fix will be prioritised and a public advisory will be published once resolved. Reporters are credited in the advisory unless they ask not to be.

We ask that you give us 90 days to release a fix before disclosing publicly. If we confirm an issue and then go quiet, you are free to disclose after that window.


## Supported Versions

Fennixs is pre-1.0. Only the latest release receives security fixes. Self-hosters should stay on the most recent tag.


## Scope

- The core API (`backend/core-api`)
- The importer (`backend/importer`)
- The frontend (`frontend/`)
- Keycloak's configuration
- The self-hosted Docker Compose setup
- Any data exposure or injection vulnerabilities


## Out of scope

- A self-hoster's own infrastructure, such as their reverse proxy, OS, or network
- Issues in third-party dependencies (report the vulnerability upstream, but tell us too if it's reachable through Fennixs so we can prioritise the upgrade)
- Reports from automated scanners with no demonstrated impact
- Missing security headers with no demonstrated exploit path
- Volumetric denial of service (resource exhaustion caused by application logic, such as an unbounded import, is in scope)


## Research Guidelines

When investigating a vulnerability, please:

- Only use accounts you own, and only test against your own instance
- Do not access, modify, or store other people's data
- Do not run destructive or volumetric tests against the hosted instance
- Stop as soon as you have confirmed the issue

Testing outside these boundaries falls outside this policy, and the safe harbor below does not apply to it.


## Safe Harbor

We will not pursue legal action against anyone who reports a vulnerability in good faith and follows this policy.