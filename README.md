# BMPresence Backend

Spring Boot backend for managing users, presence resources, and presence appointments with JWT-based authentication.

## Main Features

### Authentication & Sessions

- **Login** with username/password and issue JWT.
- **Register** new users (with optional admin flag).
- **Logout** (server-side session reset for the current user).
- **Change password** for the authenticated user.
- **Restore session** using an existing token.
- **Renew token** to extend session without re-login.
- **Validate token** endpoint for frontend checks.
- **Current user** endpoint to fetch authenticated user details.

### Users

- **CRUD** for users.
- **Active users** filter.
- **Online users** tracking and retrieval.
- **Update online status** with automatic last-active timestamp.
- **Soft delete** (mark inactive) and **hard delete** (remove).

### Presence Resources

- **CRUD** for resources (e.g., desks, rooms).
- **Active resources** filter.
- **Soft delete** and **hard delete** support.

### Presence Appointments

- **CRUD** for appointments.
- **Active appointments** filter.
- **Date range** queries.
- **Resource-specific** queries.
- **Conflict prevention** per resource/day for the same subject.
- **Soft delete** and **hard delete** support.

### Security

- **JWT authentication filter** for protected routes.
- **Stateless sessions** (no server session storage).
- **CORS** configured for local frontend dev.

## API Base Path

- `/api`

## Auth Endpoints

- `POST /api/auth/login`
- `POST /api/auth/register`
- `POST /api/auth/logout`
- `POST /api/auth/change-password`
- `POST /api/auth/restore-session`
- `POST /api/auth/renew-token`
- `GET  /api/auth/validate-token`
- `GET  /api/auth/current-user`

## User Endpoints

- `GET  /api/users`
- `GET  /api/users/active`
- `GET  /api/users/online`
- `GET  /api/users/{id}`
- `GET  /api/users/email/{email}`
- `POST /api/users`
- `PUT  /api/users/{id}`
- `PUT  /api/users/{id}/online-status?isOnline={true|false}`
- `DELETE /api/users/{id}`
- `DELETE /api/users/{id}/hard`

## Resource Endpoints

- `GET  /api/resources`
- `GET  /api/resources/active`
- `GET  /api/resources/{id}`
- `POST /api/resources`
- `PUT  /api/resources/{id}`
- `DELETE /api/resources/{id}`
- `DELETE /api/resources/{id}/hard`

## Appointment Endpoints

- `GET  /api/appointments`
- `GET  /api/appointments/active`
- `GET  /api/appointments/{id}`
- `GET  /api/appointments/range?start={ISO_DATETIME}&end={ISO_DATETIME}`
- `GET  /api/appointments/resource/{resourceId}`
- `POST /api/appointments`
- `PUT  /api/appointments/{id}`
- `DELETE /api/appointments/{id}`
- `DELETE /api/appointments/{id}/hard`

## Configuration Notes

- Environment variables can be loaded from a `.env` file at project root.
- JWT settings are configured via `jwt.secret` and `jwt.expiration.days`.

For additional authentication details, see [AUTHENTICATION_README.md](AUTHENTICATION_README.md).
