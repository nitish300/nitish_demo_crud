# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Spring Boot 4.1.1 / Java 17 REST CRUD API for `User` records, backed by MySQL via Spring Data JPA, secured with stateless JWT auth (Spring Security) and Redis for token state. Lombok is used (annotation processor configured in `pom.xml`). Base package: `com.example.crud`.

## Commands

Use the Maven wrapper (`mvnw` / `mvnw.cmd` on Windows):

- Run app: `./mvnw spring-boot:run` (port 8080)
- Build: `./mvnw clean package`
- All tests: `./mvnw test`
- Single test class: `./mvnw test -Dtest=UserServiceTest`
- Single test method: `./mvnw test -Dtest=UserServiceTest#methodName`

No lint/format plugin is configured.

## Architecture

Layered: `controller` → `service` → `repository` → `entity`, with `dto` (`UserDTO` for responses, `UserRequestDTO` for validated requests) between the API and entities.

- `UserService` does all DTO↔entity mapping by hand (`mapDtoToEntity`, `convertToDTO`); there is no mapper library. Adding a `User` field means updating both methods plus both DTOs.
- All endpoints live in `UserController` under `/api/users` (CRUD plus `/email/{email}`, `/city/{city}`, `/search?query=`, and a `/test` health check). Create is mapped to both `/api/users` and `/api/users/create`.
- Error handling is centralised in `exception/GlobalExceptionHandler`: validation errors → 400 with a field→message map, `ResourceNotFoundException` → 404, and any other `RuntimeException` → 400 with its message. The service signals duplicate-email conflicts by throwing a plain `RuntimeException`, so they surface as 400 through that catch-all handler.

## Authentication

- `SecurityConfig` is stateless with CSRF off. Only `/api/auth/**`, `/api/users/test` and `/error` are public; everything else (including all of `/api/users/**`) requires a `Bearer` token. Method security is enabled.
- `AuthController` (`/api/auth`: `register`, `login`, `refresh`, `logout`, `me`, `test`) delegates to `AuthService`. Users are identified by **email** (`CustomUserDetails.getUsername()` returns the email); the role is normalised to a `ROLE_` prefix, defaulting to `ROLE_USER`.
- `JwtService` (jjwt) signs HS256 tokens from the base64 `jwt.secret`; access tokens last 15 min, refresh tokens 7 days. `JwtAuthenticationFilter` runs before `UsernamePasswordAuthenticationFilter`: it rejects blacklisted tokens, then loads the user and sets the security context. Invalid tokens are logged and the request continues unauthenticated, so the 401 comes from `JwtAuthenticationEntryPoint`.
- `RedisTokenService` holds token state: blacklisted access tokens (`jwt:blacklist:<token>`, TTL = remaining lifetime) and one refresh token per user (`jwt:refresh:<email>`). Refresh checks the presented token against the stored one, and logout blacklists the access token and deletes the refresh token. Every Redis call catches exceptions and logs them, so a Redis outage fails open: blacklist checks return false and refresh lookups return null, which makes refresh fail.
- As with duplicate emails, `AuthService` signals failures with plain `RuntimeException`s, which the global handler returns as 400.

## Configuration and tests

- Redis is expected on `localhost:6379` (override with `REDIS_HOST` / `REDIS_PORT`). `jwt.secret` and the token lifetimes are in `application.properties`; `JwtService` also has a hard-coded fallback secret if the property is missing.
- `application.properties` points at a local MySQL database `nitish` on `localhost:3306` with `ddl-auto=update` (the schema is auto-created and there are no migrations). A running MySQL is needed to start the app and for `NitishDemoCrudApplicationTests` (a context-load test).
- All unit tests (`UserService`, `UserController`, `AuthService`, `AuthController`, `RedisTokenService`, `JwtService`, `JwtAuthenticationFilter`) are pure Mockito tests. Controller tests use standalone `MockMvcBuilders` with `GlobalExceptionHandler` rather than `@WebMvcTest`, so they need no database, Redis or Spring context.
