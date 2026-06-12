# SensorHub Platform Backend

SensorHub Platform is a sensor API publishing and data access platform built with Spring Boot, Dubbo, Gateway, MySQL, Redis, and WebSocket.

## Modules

- `sensorhub-backend`: management backend, authentication, API publishing, device registry, realtime query
- `sensorhub-common`: shared models, enums, and Dubbo contracts
- `sensorhub-client-sdk`: Java client SDK for northbound API invocation
- `sensorhub-gateway`: gateway, signature verification, quota accounting, and routing
- `sensorhub-interface`: HTTP interface proxy service

## Core Capabilities

- Unified user login, role control, and access key authentication
- API registration, publish/offline management, and invocation statistics
- Device registry, endpoint binding, and sensor workspace management
- Realtime monitoring, history query, and WebSocket push
- Java SDK invocation for GET and POST interfaces

## Local Startup

1. Start MySQL, Redis, and Nacos.
2. Import database scripts from `sql/`.
3. Start `sensorhub-interface`.
4. Start `sensorhub-gateway`.
5. Start `sensorhub-backend`.
6. Start the frontend project under `../sensorhub-frontend`.

## Default Ports

- Backend: `7529`
- Gateway: `8090`
- Interface service: `8123`
- Frontend: `18000`

## Publish Notes

- This repository has been cleaned for public sharing and interview review.
- Replace placeholder credentials with your local environment variables before running in production.
