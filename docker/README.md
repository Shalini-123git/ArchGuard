# Docker

`docker compose up --build` starts PostgreSQL, the API, and nginx serving the dashboard. Copy `.env.example` to `.env` first and set a local database password.

The API Dockerfile builds with Maven, then runs the packaged application in a Java 17 Alpine JRE image as the unprivileged `archguard` user. `JAVA_TOOL_OPTIONS` accepts the `ARCHGUARD_JAVA_TOOL_OPTIONS` value from Compose.
