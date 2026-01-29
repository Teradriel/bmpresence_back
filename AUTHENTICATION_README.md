# Servicios de Autenticación - BMPresence

## Descripción

Se han añadido dos servicios principales para la autenticación y gestión de sesiones:

### 1. **TokenService**

Servicio que gestiona la generación, validación y renovación de tokens JWT (JSON Web Tokens).

**Características:**

- Generación de tokens JWT con expiración de 30 días
- Validación de tokens
- Extracción del ID de usuario desde el token
- Renovación de tokens
- Implementación segura con firma HMAC-SHA256

### 2. **AuthenticationService**

Servicio principal de autenticación que gestiona el ciclo de vida de las sesiones de usuario.

**Características:**

- Login con username/password
- Registro de nuevos usuarios
- Cambio de contraseña
- Logout
- Restauración de sesión (mantener sesión abierta)
- Hashing seguro de contraseñas con salt (SHA-256)
- Validación de usuarios activos
- Actualización automática de `lastActiveAt`

## API Endpoints

### POST `/api/auth/login`

Inicia sesión con username y contraseña.

**Request:**

```json
{
  "username": "usuario123",
  "password": "contraseña123"
}
```

**Response (éxito):**

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

Registra un nuevo usuario.

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

**Response (éxito):**

```json
{
  "success": true,
  "message": "Utente registrato con successo"
}
```

### POST `/api/auth/logout`

Cierra la sesión del usuario actual.

**Response:**

```json
{
  "success": true,
  "message": "Logout effettuato con successo"
}
```

### POST `/api/auth/change-password`

Cambia la contraseña del usuario autenticado.

**Request:**

```json
{
  "currentPassword": "contraseñaActual",
  "newPassword": "nuevaContraseña"
}
```

**Response (éxito):**

```json
{
  "success": true,
  "message": "Password cambiata con successo"
}
```

### POST `/api/auth/restore-session`

Restaura una sesión usando un token almacenado (para mantener la sesión abierta).

**Request:**

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response (éxito):**

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

Renueva un token existente (extiende su validez por otros 30 días).

**Request:**

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response (éxito):**

```json
{
  "success": true,
  "message": "Token rinnovato con successo",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### GET `/api/auth/validate-token`

Valida si un token es válido.

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

Obtiene el usuario actualmente autenticado.

**Response (éxito):**

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

## Flujo de Uso para Mantener Sesión Abierta

### 1. **Inicio de Sesión**

```javascript
// Cliente realiza login
const response = await fetch("/api/auth/login", {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ username: "usuario", password: "contraseña" }),
});

const data = await response.json();
// Guardar el token en localStorage o sessionStorage
localStorage.setItem("authToken", data.token);
localStorage.setItem("userId", data.user.id);
```

### 2. **Restaurar Sesión al Cargar la App**

```javascript
// Al iniciar la aplicación, verificar si hay un token guardado
const token = localStorage.getItem("authToken");

if (token) {
  const response = await fetch("/api/auth/restore-session", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ token }),
  });

  if (response.ok) {
    const data = await response.json();
    // Sesión restaurada, el usuario sigue logueado
    console.log("Usuario:", data.user);
  } else {
    // Token expirado o inválido, pedir login nuevamente
    localStorage.removeItem("authToken");
  }
}
```

### 3. **Renovar Token Periódicamente (Opcional)**

```javascript
// Renovar el token cada 25 días para evitar que expire
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
); // 25 días en milisegundos
```

## Seguridad

### Contraseñas

- Las contraseñas se hashean usando **SHA-256**
- Cada contraseña tiene un **salt único** de 32 bytes
- El salt se genera usando `SecureRandom`
- Nunca se almacenan contraseñas en texto plano

### Tokens JWT

- Los tokens están firmados con **HMAC-SHA256**
- La clave secreta debe configurarse en variables de entorno para producción
- Los tokens expiran después de 30 días
- Incluyen el ID del usuario en el payload

## Configuración

### Variables de Entorno (Recomendado para Producción)

Crea un archivo `.env` o configura las variables de entorno:

```properties
JWT_SECRET=tu-clave-secreta-super-segura-de-al-menos-256-bits
```

### application.properties

La configuración por defecto está en `application.properties`:

```properties
jwt.secret=${JWT_SECRET:bmpresence-secret-key-change-this-in-production-must-be-at-least-256-bits-long-for-security}
jwt.expiration.days=30
```

**⚠️ IMPORTANTE:** Cambia la clave secreta JWT en producción. Usa una clave aleatoria de al menos 256 bits.

## Dependencias Añadidas

Se han añadido las siguientes dependencias al `pom.xml`:

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

## Ejemplo de Uso Completo

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

// USO EN LA APP
async function initApp() {
  // Intentar restaurar sesión al cargar
  const user = await restoreSession();

  if (user) {
    console.log("Sesión restaurada:", user);
    // Mostrar interfaz autenticada
  } else {
    console.log("No hay sesión activa");
    // Mostrar login
  }
}
```

## Testing

Puedes probar los endpoints con curl o Postman:

```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"testpass"}'

# Restore Session
curl -X POST http://localhost:8080/api/auth/restore-session \
  -H "Content-Type: application/json" \
  -d '{"token":"tu-token-aqui"}'
```

## Notas

- El servicio mantiene un `currentUser` en memoria. Para aplicaciones multi-instancia, considera usar una cache distribuida (Redis).
- Los tokens se validan solo por su firma y expiración. No hay invalidación activa de tokens.
- Para mayor seguridad, considera implementar refresh tokens separados.
