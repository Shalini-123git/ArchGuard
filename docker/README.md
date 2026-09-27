# Docker

`docker compose up --build` starts PostgreSQL, the API, and nginx serving the dashboard. The Compose file includes safe local defaults, so `.env` is optional for a first run. Copy `.env.example` to `.env` before changing the database password, web port, or LLM settings.

The API Dockerfile builds with Maven, then runs the packaged application in a Java 17 Alpine JRE image as the unprivileged `archguard` user. `JAVA_TOOL_OPTIONS` accepts the `ARCHGUARD_JAVA_TOOL_OPTIONS` value from Compose.
