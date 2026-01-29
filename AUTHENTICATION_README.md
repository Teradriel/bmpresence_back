# Authentication Services - BMPresence

## Description

Two main services were added for authentication and session management:

### 1. **TokenService**

Service that handles generation, validation, and renewal of JWT (JSON Web Tokens).

**Features:**

- JWT generation with 30-day expiration
- Token validation
- User ID extraction from token
- Token renewal
- Secure implementation with HMAC-SHA256 signature

### 2. **AuthenticationService**

Main authentication service that manages the user session lifecycle.

**Features:**

- Login with username/password
- Register new users
- Change password
- Logout
- Session restore (keep session open)
- Secure password hashing with salt (SHA-256)
- Active user validation
- Automatic `lastActiveAt` updates

## API Endpoints

### POST `/api/auth/login`

Login with username and password.

**Request:**

```json
{
  "username": "usuario123",
  "password": "contraseña123"
}
```

**Response (success):**

```json
{
  "success": true,
  "message": "Login effettuato con successo",
  "user": {
    "id": 1,
    "name": "Mario",
    "lastName": "Rossi",
    "email": "mario@example.com",
    "username": "usuario123",
    "isAdmin": false,
    "active": true
  },
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### POST `/api/auth/register`

Register a new user.

**Request:**

```json
{
  "name": "Mario",
  "lastName": "Rossi",
  "email": "mario@example.com",
  "username": "usuario123",
  "password": "contraseña123",
  "isAdmin": false
}
```

**Response (success):**

```json
{
  "success": true,
  "message": "Utente registrato con successo"
}
```

### POST `/api/auth/logout`

Logs out the current user.

**Response:**

```json
{
  "success": true,
  "message": "Logout effettuato con successo"
}
```

### POST `/api/auth/change-password`

Change the authenticated user's password.

**Request:**

```json
{
  "currentPassword": "contraseñaActual",
  "newPassword": "nuevaContraseña"
}
```

**Response (success):**

```json
{
  "success": true,
  "message": "Password cambiata con successo"
}
```

### POST `/api/auth/restore-session`

Restore a session using a stored token (to keep the session open).

**Request:**

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response (success):**

```json
{
  "success": true,
  "message": "Sessione restaurata con successo",
  "user": {
    "id": 1,
    "name": "Mario",
    "lastName": "Rossi",
    "email": "mario@example.com",
    ...
  }
}
```

### POST `/api/auth/renew-token`

Renew an existing token (extends its validity by 30 more days).

**Request:**

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response (success):**

```json
{
  "success": true,
  "message": "Token rinnovato con successo",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### GET `/api/auth/validate-token`

Validate whether a token is valid.

**Headers:**

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response:**

```json
{
  "valid": true
}
```

### GET `/api/auth/current-user`

Get the currently authenticated user.

**Response (success):**

```json
{
  "id": 1,
  "name": "Mario",
  "lastName": "Rossi",
  "email": "mario@example.com",
  "username": "usuario123",
  ...
}
```

## Usage Flow to Keep Session Open

### 1. **Login**

```javascript
// Client performs login
const response = await fetch("/api/auth/login", {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ username: "usuario", password: "contraseña" }),
});

const data = await response.json();
// Store the token in localStorage or sessionStorage
localStorage.setItem("authToken", data.token);
localStorage.setItem("userId", data.user.id);
```

### 2. **Restore Session on App Load**

```javascript
// On app start, check if a stored token exists
const token = localStorage.getItem("authToken");

if (token) {
  const response = await fetch("/api/auth/restore-session", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ token }),
  });

  if (response.ok) {
    const data = await response.json();
    // Session restored, user stays logged in
    console.log("User:", data.user);
  } else {
    // Token expired or invalid, ask to login again
    localStorage.removeItem("authToken");
  }
}
```

### 3. **Renew Token Periodically (Optional)**

```javascript
// Renew token every 25 days to prevent expiration
setInterval(
  async () => {
    const token = localStorage.getItem("authToken");
    if (token) {
      const response = await fetch("/api/auth/renew-token", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ token }),
      });

      if (response.ok) {
        const data = await response.json();
        localStorage.setItem("authToken", data.token);
      }
    }
  },
  25 * 24 * 60 * 60 * 1000,
); // 25 days in milliseconds
```

## Security

### Passwords

- Passwords are hashed using **SHA-256**
- Each password has a unique 32-byte **salt**
- The salt is generated using `SecureRandom`
- Passwords are never stored in plain text

### JWT Tokens

- Tokens are signed with **HMAC-SHA256**
- The secret key should be configured via environment variables for production
- Tokens expire after 30 days
- The user ID is included in the payload

## Configuration

### Environment Variables (Recommended for Production)

Create a `.env` file or configure environment variables:

```properties
JWT_SECRET=tu-clave-secreta-super-segura-de-al-menos-256-bits
```

### application.properties

Default configuration is in `application.properties`:

```properties
jwt.secret=${JWT_SECRET:bmpresence-secret-key-change-this-in-production-must-be-at-least-256-bits-long-for-security}
jwt.expiration.days=30
```

**⚠️ IMPORTANT:** Change the JWT secret key in production. Use a random key of at least 256 bits.

## Dependencies Added

The following dependencies were added to `pom.xml`:

```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

## Full Usage Example

```javascript
// LOGIN
async function login(username, password) {
  const response = await fetch("http://localhost:8080/api/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password }),
  });

  const data = await response.json();

  if (data.success) {
    localStorage.setItem("authToken", data.token);
    localStorage.setItem("user", JSON.stringify(data.user));
    return data.user;
  } else {
    throw new Error(data.message);
  }
}

// RESTORE SESSION
async function restoreSession() {
  const token = localStorage.getItem("authToken");

  if (!token) return null;

  const response = await fetch(
    "http://localhost:8080/api/auth/restore-session",
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ token }),
    },
  );

  const data = await response.json();

  if (data.success) {
    localStorage.setItem("user", JSON.stringify(data.user));
    return data.user;
  } else {
    localStorage.removeItem("authToken");
    localStorage.removeItem("user");
    return null;
  }
}

// LOGOUT
async function logout() {
  await fetch("http://localhost:8080/api/auth/logout", {
    method: "POST",
  });

  localStorage.removeItem("authToken");
  localStorage.removeItem("user");
}

// APP USAGE
async function initApp() {
  // Try to restore session on load
  const user = await restoreSession();

  if (user) {
    console.log("Session restored:", user);
    // Show authenticated UI
  } else {
    console.log("No active session");
    // Show login
  }
}
```

## Testing

You can test the endpoints with curl or Postman:

```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"testpass"}'

# Restore Session
curl -X POST http://localhost:8080/api/auth/restore-session \
  -H "Content-Type: application/json" \
  -d '{"token":"your-token-here"}'
```

## Notes

- The service keeps a `currentUser` in memory. For multi-instance apps, consider using a distributed cache (Redis).
- Tokens are validated only by signature and expiration. There is no active token invalidation.
- For higher security, consider implementing separate refresh tokens.
