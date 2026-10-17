# Known Limitations and Future Improvements

## 1. Overview

AI Watch Spaces implements real-time watch-space communication, AI-assisted content, narrative variation voting, personalized recommendations, dashboard aggregation, and session analytics.

This document identifies areas that require additional hardening or verification. It distinguishes implemented functionality from improvements that have not yet been completed or fully verified.

## 2. Real-Time Playback Synchronization

**Current state:** The backend maintains playback state and broadcasts updates to connected participants.

**Limitation:** Maintaining playback state and broadcasting updates does not, by itself, guarantee frame-accurate synchronization across multiple clients. Playback drift correction and synchronization under network delays require further verification.

**Future improvements:**
- Add periodic playback-state synchronization.
- Define client behavior for seeking, reconnecting, and stale updates.
- Test synchronization with multiple simultaneous WebSocket clients.
- Measure state propagation latency under realistic network conditions.

## 3. WebSocket Connection Lifecycle

**Current state:** The WebSocket handler manages room connections, presence, chat, and supported real-time events.

**Limitation:** Reconnection behavior, duplicate connections for the same user, and abrupt network disconnections need additional concurrency testing. Incorrect handling of multiple connections can cause presence counts to become inaccurate.

**Future improvements:**
- Track connection identity separately from user identity.
- Make join and departure handling idempotent.
- Add reconnect and stale-connection cleanup tests.
- Test concurrent joins, disconnects, and reconnects.

## 4. AI Response Grounding and Source Metadata

**Current state:** The AI subsystem retrieves timeline context and uses it to construct grounded prompts for generated answers and trivia.

**Limitation:** The current AI answer response exposes the generated answer without explicit source metadata or citations. This limits a client's ability to show which timeline events support an answer.

**Future improvements:**
- Return relevant timeline-event identifiers with answers.
- Distinguish retrieved context from generated content.
- Add tests for questions with insufficient supporting context.
- Verify that answers do not claim unsupported story details.

## 5. External AI Provider Dependencies

**Current state:** AI generation uses a dedicated provider integration and supports a test provider for automated application testing.

**Limitation:** Live generation depends on the external provider's availability, API configuration, quotas, and rate limits. Requests can fail even when the application itself is functioning correctly.

**Future improvements:**
- Add bounded retries with backoff for retryable errors.
- Apply timeouts and controlled failure responses.
- Consider caching suitable repeated requests.
- Test provider failures and rate-limit responses without depending on live API calls.

## 6. Recommendation Quality

**Current state:** Recommendations combine content-based genre preferences with collaborative filtering. The current hybrid ranking weights these approaches at 60% and 40%, respectively.

**Limitation:** Recommendation quality depends on available interaction data. Sparse histories can reduce collaborative similarity, while newly introduced users and titles may have insufficient data for strong personalization.

**Future improvements:**
- Evaluate ranking quality with larger, representative datasets.
- Add explicit cold-start strategies.
- Compare precision and recall at multiple ranking cutoffs.
- Monitor recommendation quality as interaction data changes.

## 7. Session Analytics Accuracy

**Current state:** Analytics are calculated from persistent watch-space event logs and include duration, peak concurrent participants, AI questions, trivia cards, and chat messages.

**Limitation:** Duration is derived from the first and last recorded events. Peak concurrency depends on accurate participant join and departure events. Missing or duplicated events can therefore affect the reported metrics.

**Future improvements:**
- Define explicit session-start and session-end semantics.
- Make event processing robust to duplicate and out-of-order events.
- Test abrupt disconnects and concurrent connections.
- Reconcile analytics against a manually verified event sequence.

## 8. Database Schema Management

**Current state:** The application uses JPA/Hibernate for persistence and automatic schema updates in the current configuration.

**Limitation:** Automatic schema updates are convenient during development but do not provide a complete, versioned production migration process.

**Future improvements:**
- Introduce Flyway or Liquibase migrations.
- Review foreign-key and uniqueness constraints.
- Validate indexes against representative query workloads.
- Test schema upgrades against existing data.

## 9. Reactive Request Handling and Blocking Persistence

**Current state:** The application uses Spring WebFlux for HTTP and WebSocket handling, with blocking JPA operations moved to bounded-elastic scheduling in selected request paths.

**Limitation:** JPA is blocking. Any blocking database operation executed on a reactive event-loop thread can reduce throughput and responsiveness. Every reactive execution path must be reviewed, not only the controllers already updated.

**Future improvements:**
- Audit all WebFlux handlers and service invocation paths.
- Ensure blocking JPA and file-processing operations execute on appropriate schedulers.
- Consider reactive database access if the project requires a fully reactive persistence stack.
- Measure behavior under concurrent requests.

## 10. Security and Operational Hardening

**Current state:** JWT authentication, role-based administrative access, and resource-level authorization checks have been implemented and tested through API verification.

**Further work:**
- Review authorization rules for every resource and WebSocket action.
- Add explicit request-size and upload-size limits.
- Review token expiration and signing-secret management.
- Add rate limiting for sensitive endpoints.
- Improve structured logging, correlation identifiers, and health monitoring.
- Avoid logging credentials, bearer tokens, or sensitive user data.

## 11. Testing and Performance Evidence

**Current state:** The project has automated tests for selected AI, application-context, recommendation, and WebSocket behavior. Authentication and major API flows have also been manually verified.

**Limitation:** Passing the existing test suite does not establish production-scale throughput, latency, or reliability under load.

**Future improvements:**
- Expand integration tests for the complete watch-space lifecycle.
- Add multi-client WebSocket synchronization and reconnect tests.
- Test AI provider failures and rate limits.
- Run controlled load tests and publish measured results.
- Record reproducible demonstration steps and test evidence.

Performance claims should be based on measured results rather than assumptions.

## 12. Summary

AI Watch Spaces provides a working backend foundation with REST APIs, JWT-based access control, WebSocket communication, contextual AI functionality, hybrid recommendations, dashboard aggregation, and event-based analytics.

The next engineering priorities are stronger multi-client synchronization, more robust connection lifecycle handling, explicit AI source metadata, controlled database migrations, broader integration tests, and measurable performance validation.
