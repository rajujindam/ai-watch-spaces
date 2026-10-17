# API Reference — AI Watch Spaces

## 1. Overview

AI Watch Spaces exposes REST APIs for authentication, watch-space management, playback, timeline metadata, AI assistance, recommendations, user dashboards, and session analytics.

The backend also provides a WebSocket endpoint for real-time room communication.

**Base URL — Local:** `http://localhost:8080`

**Base URL — Deployed:** `https://ai-watch-spaces.onrender.com`

**API prefix:** `/api/v1`

## 2. Authentication

Protected endpoints require a JWT bearer token.

Include the token in the HTTP request header:

```http
Authorization: Bearer <JWT_TOKEN>
```

Registration and login are publicly accessible. Administrative endpoints under `/api/v1/admin/**` require the `ADMIN` role.

### Authentication endpoints

| Method | Endpoint | Purpose | Access |
|---|---|---|---|
| POST | `/api/v1/auth/register` | Register a user | Public |
| POST | `/api/v1/auth/login` | Authenticate and obtain a token | Public |

Use the response from the login endpoint to obtain the token. Refer to the actual authentication DTOs for the precise request and response fields.

## 3. Watch Space Management

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/v1/watch-spaces` | Create a watch space |
| GET | `/api/v1/watch-spaces/{watchSpaceId}` | Retrieve a watch space |
| POST | `/api/v1/watch-spaces/{watchSpaceId}/end` | End a live watch space |

### Create a watch space

**Request**

```http
POST /api/v1/watch-spaces
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>
```

```json
{
  "name": "Friday Movie Night",
  "titleId": 1,
  "maxParticipants": 5,
  "aiVerbosity": "NORMAL",
  "votingEnabled": true
}
```

The authenticated user becomes the host. The room is initially created with `SCHEDULED` status.

The maximum participant count must be between 1 and 50. The selected AI verbosity must be a supported value.

### Watch-space lifecycle

| Status | Meaning |
|---|---|
| `SCHEDULED` | Created but not started |
| `LIVE` | Session is active |
| `ENDED` | Host has ended the session |

The host can end a live session using the end endpoint. The application rejects invalid lifecycle transitions.

## 4. Participant Management

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/v1/watch-spaces/{watchSpaceId}/participants` | Join a watch space |
| GET | `/api/v1/watch-spaces/{watchSpaceId}/participants` | List room participants |

Joining requires authentication. The backend checks the room and prevents duplicate membership.

## 5. Title Management

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/v1/titles` | Create a title |
| GET | `/api/v1/titles/{id}` | Retrieve a title |

Titles contain metadata used by watch spaces, timeline retrieval, and recommendations. The genre association is also used by the recommendation engine.

## 6. Playback

Playback operations manage the playback state associated with a watch space.

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/v1/watch-spaces/{watchSpaceId}/playback` | Retrieve playback state |
| POST | `/api/v1/watch-spaces/{watchSpaceId}/playback` | Update playback state |
| POST | `/api/v1/watch-spaces/{watchSpaceId}/play` | Start or resume playback |
| POST | `/api/v1/watch-spaces/{watchSpaceId}/pause` | Pause playback |
| POST | `/api/v1/watch-spaces/{watchSpaceId}/seek?positionSeconds={seconds}` | Seek to a playback position |

The playback state includes the status, playback position, update time, and user responsible for the update.

Supported playback statuses include `PLAYING` and `PAUSED`.

Playback actions also produce WebSocket updates for connected room participants.

## 7. Timeline Events

Timeline events represent contextual metadata associated with a title and a playback timestamp.

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/v1/titles/{titleId}/timeline-events` | Create a timeline event |
| GET | `/api/v1/titles/{titleId}/timeline-events?startSeconds={start}&endSeconds={end}` | Retrieve events in a playback-time range |

Timeline events can carry structured JSON payloads. Timestamps are validated against the title's duration.

Supported metadata event types include:

- `trivia`
- `character`
- `variation_point`
- `glossary`

Use the actual timeline request DTO to confirm the complete JSON structure before submitting requests.

## 8. AI Assistance

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/v1/watch-spaces/{watchSpaceId}/ai/ask` | Ask a contextual question about the title |

**Request**

```http
POST /api/v1/watch-spaces/1/ai/ask
Content-Type: application/json
Authorization: Bearer <JWT_TOKEN>
```

```json
{
  "question": "What is happening in this part of the story?"
}
```

The AI subsystem retrieves relevant timeline context and uses the configured provider to generate an answer.

The response currently exposes the generated answer. Source metadata and citations should not be assumed to be included unless they are explicitly added to the response DTO.

AI generation requires a valid provider configuration and may be subject to external provider rate limits.

## 9. User Interactions and Recommendations

### Record an interaction

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/v1/interactions` | Record a user's interaction with a title |

An interaction records information such as watched duration, completion status, optional rating, and creation time.

The title must exist, watched duration must not be negative or exceed the title duration, and a completed interaction must satisfy the application's duration validation.

### Retrieve recommendations

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/v1/recommendations?limit=5` | Retrieve personalized recommendations |

The recommendation engine combines content-based and collaborative scores. The current hybrid ranking uses a 60% content-based and 40% collaborative weighting.

Titles already present in the user's interaction history are excluded from recommendation candidates.

## 10. Dashboard

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/v1/dashboard` | Retrieve the user's dashboard |

The dashboard aggregates:

- Recently watched titles.
- Upcoming scheduled watch spaces hosted by the user.
- Active watch spaces available for quick rejoining.
- Personalized recommendations.

The response is assembled from existing application services rather than maintained as a separate copy of the user's data.

## 11. Session Analytics

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/v1/watch-spaces/{watchSpaceId}/analytics` | Retrieve session analytics |

The analytics response includes:

| Field | Meaning |
|---|---|
| `durationSeconds` | Duration derived from the recorded event interval |
| `peakConcurrentParticipants` | Highest concurrent participant count calculated from recorded events |
| `aiQuestions` | Number of recorded AI question events |
| `triviaCards` | Number of trivia events recorded |
| `chatMessages` | Number of recorded chat messages |

The figures are derived from persistent watch-space event logs. Their accuracy depends on the completeness and correctness of event recording.

## 12. Administrative Timeline Metadata Upload

| Method | Endpoint | Purpose | Access |
|---|---|---|---|
| POST | `/api/v1/admin/timeline-metadata` | Upload title timeline metadata as a file | ADMIN |

The endpoint accepts a multipart file upload using the `file` field. JSON and YAML metadata are supported.

The upload process parses and validates the file, checks supported event types and title timestamps, and persists valid timeline events.

Malformed files and invalid metadata should return a client error with validation details.

## 13. WebSocket API

**Local endpoint**

```text
ws://localhost:8080/ws/watch-spaces/{watchSpaceId}
```

**Deployed endpoint**

```text
wss://ai-watch-spaces.onrender.com/ws/watch-spaces/{watchSpaceId}
```

A valid authenticated connection is required.

### Event types

| Event type | Purpose |
|---|---|
| `playback.update` | Communicate playback state changes |
| `presence.update` | Notify participants about room presence |
| `chat.message` | Send and broadcast chat messages |
| `ai.trivia` | Request or deliver AI-generated trivia |
| `room.variation.voteOpen` | Communicate that a variation voting round is open |
| `room.variation.applied` | Communicate the selected variation |

Presence updates distinguish `JOINED` and `LEFT` actions.

The handler processes supported event types and ignores malformed or unknown messages. Exact payload fields should be taken from the corresponding event factory and handler implementation.

### Example playback update

```json
{
  "type": "playback.update",
  "status": "PLAYING",
  "positionSeconds": 120
}
```

This is an illustrative event shape. Verify the actual serialized message produced by the event factory before using it as a strict client contract.

## 14. Common HTTP Status Codes

| Status | Meaning |
|---|---|
| `200 OK` | Request succeeded |
| `201 Created` | Resource created, where returned by the endpoint |
| `400 Bad Request` | Invalid input or failed validation |
| `401 Unauthorized` | Missing, invalid, or expired authentication |
| `403 Forbidden` | Authenticated user lacks the required permission |
| `404 Not Found` | Requested resource does not exist |
| `409 Conflict` | Operation conflicts with existing state, such as duplicate participation |
| `5xx` | Server-side failure |

The exact status code depends on the endpoint and its exception handling.

## 15. Testing the API

Recommended verification workflow:

1. Register a user and log in.
2. Save the JWT and use it for protected requests.
3. Create a title and a watch space.
4. Join the watch space and retrieve its participants.
5. Exercise playback operations.
6. Create timeline metadata and ask an AI question.
7. Record title interactions and retrieve recommendations.
8. Retrieve dashboard and session analytics.
9. Test administrative upload with both valid and invalid metadata.
10. Connect a WebSocket client and verify supported room events.

Use the deployed service for remote demonstrations and local Docker Compose for development.

**Important:** This document is a reference for the implemented features. Confirm route mappings and event payloads against the controllers, DTOs, and event factories before treating every example as a guaranteed API contract.
