# Locket Clone Backend

Backend service for a social photo-sharing application inspired by Locket.

The system provides REST APIs for user management, friendships, photo sharing, feeds, reactions, comments, and 1-on-1 messaging. It also supports real-time events through WebSocket and push notifications through Firebase Cloud Messaging.

Built with Java, Spring Boot, PostgreSQL, Firebase, Cloudinary, and Docker.

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Architecture & Design](#architecture--design)
- [Testing](#testing)
- [Known Limitations](#known-limitations)

---

## Features

- Authenticates users via Firebase (Phone OTP and Google Sign-In).
- Handles photo uploads and manages custom audience settings.
- Serves real-time photo feeds.
- Provides data to update home-screen widgets.
- Stores emoji reactions and comments on photos.
- Manages 1-on-1 private messaging.
- Handles friend requests (send, accept, decline, unfriend).
- Pushes notifications and in-app alerts for interactions.
- Deletes accounts and cascades deletion of photos and friendships, while anonymizing chat messages.

## Tech Stack

| Layer                     | Technology                                                            |
| ------------------------- | --------------------------------------------------------------------- |
| **Language**              | Java 17                                                               |
| **Framework**             | Spring Boot 4.1.1, Spring Data JPA, Spring Security, Spring WebSocket |
| **Database**              | PostgreSQL 16                                                         |
| **Authentication & Push** | Firebase Admin SDK 9.2.0                                              |
| **Storage**               | Cloudinary 1.38.0                                                     |
| **API Docs**              | springdoc-openapi (Swagger) 3.0.0                                     |
| **Build Tool**            | Maven                                                                 |
| **Containerization**      | Docker, Docker Compose                                                |
| **Testing**               | JUnit, Mockito, AssertJ                                               |

## Project Structure

```text
com.minh.locket_clone_backend
├── auth/         # Verifies Firebase tokens and handles authentication
├── chat/         # Manages 1-on-1 private conversations, messaging, and photo comments
├── common/       # Provides shared utilities, exceptions, and global configurations
├── feed/         # Aggregates and serves the photo feed
├── friend/       # Manages user relationships and friend requests
├── notification/ # Handles in-app notifications and FCM push delivery
├── photo/        # Handles photo uploads, metadata, and Cloudinary integration
├── reaction/     # Stores emoji reactions on photos
├── user/         # Manages user profiles and device tokens
└── websocket/    # Manages STOMP over WebSocket connections and real-time events
```

Modules follow package-by-domain architecture. Cross-module communication occurs exclusively through Service interfaces (e.g., `UserService`). Modules do not directly access other modules' Repositories.

## Getting Started

### Prerequisites

- Docker and Docker Compose (recommended)
- Java 17 (if running locally without Docker)
- Firebase Project with an Admin SDK `firebase-service-account.json` file
- Cloudinary Account

### 1. Clone the repository

```bash
git clone https://github.com/TrinhDacNhatMinh/locket-clone-backend.git
cd locket-clone-backend
```

### 2. Configure Environment

Create the `.env` file from the provided template:

```bash
cp .env.example .env
```

**Environment Variables**

| Variable                | Required | Description           |
| ----------------------- | -------- | --------------------- |
| `CLOUDINARY_CLOUD_NAME` | Yes      | Cloudinary cloud name |
| `CLOUDINARY_API_KEY`    | Yes      | Cloudinary API key    |
| `CLOUDINARY_API_SECRET` | Yes      | Cloudinary API secret |

Create a `config/` directory at the project root. Place the Firebase Admin SDK JSON file inside it:

```bash
mkdir config
# Place firebase-service-account.json into the config/ folder
```

### 3. Run the application

Run the application using Docker Compose (recommended) or locally via Maven.

**Option A: Run with Docker Compose (Recommended)**

Starts both the PostgreSQL database and the Spring Boot application container.

```bash
docker-compose -f docker-compose.yaml up --build -d
```

**Option B: Run with Maven (Local Development)**

Starts the database via Docker, then runs the Spring Boot app directly on the host machine.

```bash
# 1. Start the local Postgres database (uses compose.yaml)
docker-compose up -d

# 2. Run the Spring Boot app
mvn spring-boot:run
```

The API runs at `http://localhost:8080` in both scenarios.

## Architecture & Design

### System Architecture

```mermaid
flowchart TD
    Client[Client App] -->|HTTPS REST| API[Spring Boot Backend]
    Client <-->|WSS STOMP| API
    API <--> DB[(PostgreSQL)]
    API -->|Verify Token| FB_Auth[Firebase Auth]
    API -->|Upload Image| Cloudinary[Cloudinary Storage]
    API -->|Push Notification| FCM[Firebase Cloud Messaging]
    FCM -->|Silent/Visible Push| Client
```

### API Documentation

- **Swagger UI:** Available at `http://localhost:8080/swagger-ui/index.html`.
- **Authentication:** Most endpoints require a Firebase ID Token in the request header: `Authorization: Bearer <Firebase_ID_Token>`.

### Database Schema

Core domain entities:

- `User`: Stores user profiles and FCM tokens.
- `Photo` & `PhotoAudience`: Stores image URLs, metadata, and custom audience selection for non-public photos.
- `Friend` & `FriendRequest`: Manages relationships.
- `Block`: Manages blocked user relationships and privacy controls.
- `Conversation` & `Message`: Handles private chats and photo comments.
- `Reaction`: Handles emoji photo interactions.
- `Notification`: Stores in-app notification history.

```mermaid
erDiagram
    User {
        UUID id PK
        String firebaseUid UK
        String username UK
        String fcmToken
    }
    Photo {
        UUID id PK
        UUID ownerId FK
        String imageUrl
        String audienceType
    }
    PhotoAudience {
        UUID id PK
        UUID photoId FK
        UUID userId FK
    }
    Friend {
        UUID id PK
        UUID userAId FK
        UUID userBId FK
    }
    FriendRequest {
        UUID id PK
        UUID requesterId FK
        UUID addresseeId FK
        String status
    }
    Block {
        UUID id PK
        UUID blockerId FK
        UUID blockedId FK
    }
    Conversation {
        UUID id PK
    }
    ConversationParticipant {
        UUID id PK
        UUID conversationId FK
        UUID userId FK
    }
    Message {
        UUID id PK
        UUID conversationId FK
        UUID senderId FK
        UUID referencePhotoId FK "nullable"
        String content
        String type
    }
    Reaction {
        UUID id PK
        UUID photoId FK
        UUID userId FK
        String emoji
    }
    Notification {
        UUID id PK
        UUID userId FK
        String type
        JSONB payload
        Boolean isRead
    }

    User ||--o{ Photo : "creates"
    User ||--o{ Friend : "belongs to"
    User ||--o{ FriendRequest : "sends/receives"
    User ||--o{ Block : "initiates"
    User ||--o{ ConversationParticipant : "acts as"
    User ||--o{ Message : "sends"
    User ||--o{ Reaction : "creates"
    User ||--o{ Notification : "receives"
    User ||--o{ PhotoAudience : "is invited to view"
    Photo ||--o{ PhotoAudience : "custom audience"
    Photo ||--o{ Reaction : "has"
    Photo ||--o{ Message : "commented via"
    Conversation ||--|{ ConversationParticipant : "has"
    Conversation ||--o{ Message : "contains"
```

### API Flow

```mermaid
sequenceDiagram
    participant C as Client
    participant SF as Spring Security Filter Chain
    participant Ctrl as Controller
    participant Svc as Service
    participant Repo as Repository
    participant DB as PostgreSQL
    participant GEH as GlobalExceptionHandler

    C->>SF: HTTP Request
    Note over SF: Runs FirebaseAuthenticationFilter (OncePerRequestFilter)
    SF->>Ctrl: Request passes (SecurityContext populated)
    Note over Ctrl: Injects @AuthenticationPrincipal CustomUserDetails
    Ctrl->>Svc: call service method
    Svc->>Repo: call repository method
    Repo->>DB: SQL query
    DB-->>Repo: ResultSet
    Repo-->>Svc: Entity (e.g. Photo)
    alt Success
        Svc-->>Ctrl: Response DTO
        Ctrl-->>C: 200 ResponseEntity
    else BusinessException thrown
        Svc-->>GEH: throws BusinessException
        GEH-->>C: ResponseEntity (error status + BaseResponse)
    end
```

### Authentication Flow

```mermaid
sequenceDiagram
    participant C as Client
    participant F as FirebaseAuthenticationFilter
    participant AR as AuthenticationResolver
    participant FB as Firebase Admin SDK
    participant DB as PostgreSQL
    participant SC as SecurityContextHolder
    participant H as Controller/Handler

    C->>F: HTTP Request (Authorization: Bearer <token>)
    Note over F: Extract token from header via getJwtFromRequest()
    alt Token present
        F->>AR: resolveUser(idToken)
        AR->>FB: firebaseAuth.verifyIdToken(idToken)
        FB-->>AR: FirebaseToken (uid)
        AR->>DB: userService.findUserIdByFirebaseUid(uid)
        alt User found
            DB-->>AR: Optional<UUID> (userId)
            AR-->>F: CustomUserDetails(userId, firebaseUid)
            Note over F, SC: SecurityContextHolder.setAuthentication(...)
            F->>H: filterChain.doFilter() — pass through
            H-->>C: 200 Response
        else User not found (e.g. deleted account)
            DB-->>AR: Optional.empty()
            AR-->>F: throws BusinessException(USER_NOT_FOUND)
            Note over F: handlerExceptionResolver.resolveException()
            F-->>C: 404 Not Found
        end
    else No token (public endpoint)
        F->>H: filterChain.doFilter() — pass through
        H-->>C: 200 Response
    else Invalid or expired token
        Note over F: handlerExceptionResolver.resolveException()
        F-->>C: 401 Unauthorized
    end
```

### Upload Image Flow

```mermaid
sequenceDiagram
    participant C as Client
    participant PC as PhotoController
    participant PS as PhotoServiceImpl
    participant FS as FriendService
    participant SS as StorageService (Cloudinary)
    participant DB as PostgreSQL
    participant NS as NotificationService

    C->>PC: POST /api/v1/photos (multipart: image, caption, audienceType, audienceUserIds)
    PC->>PS: createPhoto(ownerId, image, caption, audienceType, audienceUserIds, metadataJson)
    Note over PS: parseAndValidateMetadata()
    alt Audience is custom
        PS->>FS: isFriend(ownerId, userId)
        Note over PS, FS: Silent filter non-friends
    end
    PS->>SS: storageService.uploadImage(image)
    SS-->>PS: imageUrl (Cloudinary secure URL)
    PS->>DB: photoRepository.save(Photo)
    alt Audience is custom
        PS->>DB: photoAudienceRepository.saveAll(audiences)
        Note over PS: Assign eligibleViewerIds = audienceUserIds
    else Audience is all friends
        PS->>FS: getFriendIds(ownerId)
        FS-->>PS: List<UUID> (eligibleViewerIds)
    end
    loop For each viewerId in eligibleViewerIds
        PS->>NS: notificationService.notify(viewerId, NEW_PHOTO, WIDGET_UPDATE, payload)
    end
    PS-->>PC: PhotoResponse
    PC-->>C: 201 Created
```

### WebSocket Flow

```mermaid
sequenceDiagram
    participant C as Client
    participant SHI as StompHandshakeInterceptor
    participant AR as AuthenticationResolver
    participant FB as Firebase Admin SDK
    participant SR as SessionRegistry
    participant MT as SimpMessagingTemplate

    Note over C, SR: --- Connection Phase ---
    C->>SHI: HTTP Upgrade Request to /ws?token=<firebase_token>
    Note over SHI: Extract token from query param or header
    alt Token present
        SHI->>AR: resolveUser(token)
        AR->>FB: firebaseAuth.verifyIdToken(token)
        FB-->>AR: FirebaseToken (uid)
        AR-->>SHI: CustomUserDetails(userId)
        Note over SHI: attributes.put(userId)
        SHI-->>C: 101 Switching Protocols (WebSocket established)
        Note over C, SR: SessionConnectedEvent — markOnline(userId, sessionId)
    else Invalid or expired token
        AR-->>SHI: throws FirebaseAuthException
        SHI-->>C: Reject handshake (401 / connection refused)
    end

    Note over C, MT: --- Server Push Phase ---
    MT->>C: convertAndSendToUser(userId, /queue/events, RealtimeEvent)

    Note over C, SR: --- Disconnection Phase ---
    Note over C, SR: SessionDisconnectEvent — markOffline(userId, sessionId)
```

### Notification Flow

```mermaid
sequenceDiagram
    participant S as Service (e.g. PhotoService)
    participant NS as NotificationServiceImpl
    participant DB as PostgreSQL
    participant SR as SessionRegistry
    participant REP as RealtimeEventPublisher
    participant MT as SimpMessagingTemplate
    participant CMS as CloudMessagingService (FCM)
    participant C as Client

    S->>NS: notify(recipientId, notificationType, realtimeEventType, payload, realtimePayload)

    alt Notification is for friend request or acceptance
        NS->>DB: notificationRepository.save(Notification)
    end

    NS->>SR: sessionRegistry.isOnline(recipientId)

    alt User is online
        SR-->>NS: boolean (true)
        NS->>REP: realtimeEventPublisher.publish(recipientId, eventType, payload)
        REP->>MT: convertAndSendToUser(recipientId, /queue/events, RealtimeEvent)
        MT-->>C: WebSocket push
    else User is offline
        SR-->>NS: boolean (false)
        NS->>DB: userService.getUserByIdIncludingDeleted(recipientId)
        DB-->>NS: User (fcmToken, deletedAt)
        alt User is active and has FCM token
            alt Notification is visible (Message, Comment, Request)
                NS->>CMS: sendVisibleNotification(fcmToken, title, body)
                CMS-->>C: Visible FCM push
            else Notification is silent (New Photo)
                NS->>CMS: sendSilentDataPush(fcmToken, data{type, photoId})
                CMS-->>C: Silent FCM data push (widget update)
            end
        end
    end
```

## Testing

Run tests via Maven:

```bash
mvn test
```

- **Scope:** Tests focus on the Service layer.
- **Dependencies:** Mocks external dependencies (Cloudinary, Firebase, Repositories) using Mockito for fast, isolated tests.
- **Naming Convention:** Methods follow the `methodName_condition_expectedResult` convention (e.g., `uploadPhoto_validFile_returnsPhotoResponse`).

## Known Limitations

- **Single FCM Token per User:** Stores one FCM token per user. Logging into a new device overwrites the previous token. Push notifications only reach the most recently active device. This simplifies token management.
- **No Group Features:** Supports 1-on-1 chats and individual friendships only. Group chats or shared albums are out of scope.
- **Account Deletion is Soft-Delete:** Nullifies the database link but does not call Firebase to delete the original identity. This avoids distributed transaction failures.
- **No Server-Side Photo Compression:** Assumes the client compresses photos before uploading. This saves server bandwidth and CPU usage.
