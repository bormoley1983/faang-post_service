# Post Service

Service responsible for creating, moderating, and publishing posts and comments. It integrates with Kafka for event delivery, Redis for caching, PostgreSQL for persistence, and S3-compatible storage for files.

## Quick start

Prerequisites:
- Java 25+ (JDK)
- Docker (for container runs)
- PostgreSQL (for persistence)
- Redis (for caching)
- Kafka (for event publishing)
- [faang-infra services](https://github.com/bormoley1983/faang-infra) running locally or accessible

Run locally:
```sh
./gradlew bootRun
```

Run tests:
```sh
./gradlew test --info
```

Build and run in Docker:
```sh
./gradlew build
docker build -t post-service .
docker run -p 8081:8081 post-service
```

## Configuration

Main config: [src/main/resources/application.yaml](src/main/resources/application.yaml)

Key default configuration properties:
- **Server Port**: 8081
- **PostgreSQL**: localhost:5432
- **Redis**: localhost:6379
- **Kafka Bootstrap Servers**: localhost:9092
- **S3 Endpoint**: http://127.0.0.1:9000
- **Moderation API**: https://commentanalyzer.googleapis.com/v1alpha1/comments:analyze
- **Spell Checker API**: https://speller.yandex.net/services/spellservice.json

## External Integrations

Kafka topics:
- analytics_comment_topic
- notification_comment_topic
- analytics-like-events
- notification_like_topic
- user_ban_topic
- publish_post_topic
- public_post_view_topic

Storage:
- S3-compatible storage configuration: [src/main/resources/application.yaml](src/main/resources/application.yaml)

Services:
- User Service: host/port in [src/main/resources/application.yaml](src/main/resources/application.yaml)
- Project Service: host/port in [src/main/resources/application.yaml](src/main/resources/application.yaml)
- Payment Service: host/port in [src/main/resources/application.yaml](src/main/resources/application.yaml)

## Architecture

Controllers:
- [src/main/java/faang/school/postservice/controller](src/main/java/faang/school/postservice/controller)

Services:
- [src/main/java/faang/school/postservice/service](src/main/java/faang/school/postservice/service)

Repositories:
- [src/main/java/faang/school/postservice/repository](src/main/java/faang/school/postservice/repository)

Messaging:
- Kafka publishers in [src/main/java/faang/school/postservice/publisher](src/main/java/faang/school/postservice/publisher)

Scheduling:
- Jobs under [src/main/java/faang/school/postservice/scheduler](src/main/java/faang/school/postservice/scheduler)

**Note:** Base code structure and architecture patterns are based on [FAANG School](https://github.com/faang-school) educational project.
