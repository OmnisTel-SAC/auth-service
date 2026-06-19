# Auth Service

Microservicio de autenticación y gestión de usuarios del sistema OmnisTel.
Proporciona registro, inicio de sesión, administración de roles y emisión de tokens JWT.

## Tecnologías

- Java 17
- Spring Boot 3.x
- Spring Security (OAuth2 Resource Server)
- JWT (RSA-256 con nimbus-jose)
- Redis (rate limiting)
- MySQL 8.0
- Kafka (event bus)
- Eureka Discovery Client
- Spring Cloud Config Client
- Resilience4j (retry)
- OpenAPI / Swagger

## Estructura

```
auth-service/
├── src/
│   ├── main/
│   │   ├── java/com/omnistel/authservice/
│   │   │   ├── config/
│   │   │   │   ├── OpenApiConfig.java
│   │   │   │   ├── RsaKeyProvider.java
│   │   │   │   ├── SecurityConfig.java
│   │   │   ├── controller/
│   │   │   │   └── AuthController.java
│   │   │   ├── dto/
│   │   │   │   ├── AdminRegisterRequest.java
│   │   │   │   ├── AuthResponse.java
│   │   │   │   ├── ErrorResponse.java
│   │   │   │   ├── LoginRequest.java
│   │   │   │   ├── RegisterRequest.java
│   │   │   │   └── UserResponse.java
│   │   │   ├── entity/
│   │   │   │   ├── Role.java
│   │   │   │   └── User.java
│   │   │   ├── exception/
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── RateLimitExceededException.java
│   │   │   ├── repository/
│   │   │   │   └── UserRepository.java
│   │   │   └── service/
│   │   │       ├── AuthService.java
│   │   │       ├── CustomUserDetailsService.java
│   │   │       └── RateLimitService.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── bootstrap.yml
│   └── test/
│       ├── java/com/omnistel/authservice/
│       │   ├── AuthServiceApplicationTests.java
│       │   ├── controller/AuthControllerTest.java
│       │   ├── repository/UserRepositoryTest.java
│       │   └── service/AuthServiceTest.java
│       └── resources/
│           └── application.yml
├── Dockerfile
├── pom.xml
└── .gitignore
```

## Patrones de Diseño

| Patrón | Descripción |
|--------|-------------|
| **DTO Pattern** | Separación de entidades JPA y objetos de respuesta/request |
| **Repository Pattern** | Abstracción de acceso a datos con Spring Data JPA |
| **Strategy Pattern** | Múltiples estrategias de registro (CLIENT vs ADMIN/AGENT) |
| **Token Bucket** | Rate limiting por IP para login y registro |
| **Global Exception Handler** | Manejo centralizado de errores con `@ControllerAdvice` |

## Infraestructura

| Componente | Uso |
|------------|-----|
| **MySQL** | Persistencia de usuarios y roles |
| **Redis** | Rate limiting y bloqueo temporal por intentos fallidos |
| **Kafka** | Publicación de eventos de usuario |
| **Eureka** | Registro y descubrimiento de servicios |
| **Config Server** | Configuración centralizada desde classpath (modo native) |

## Endpoints

| Método | Ruta | Descripción | Autenticación |
|--------|------|-------------|---------------|
| POST | `/api/auth/register` | Registro de cliente | Público |
| POST | `/api/auth/login` | Inicio de sesión | Público |
| GET | `/api/auth/me` | Obtener usuario actual | JWT |
| GET | `/api/auth/users/{id}` | Obtener usuario por ID | Interno / JWT |
| GET | `/api/auth/users?role=` | Listar usuarios por rol | Interno / JWT |
| POST | `/api/auth/admin/register` | Crear admin o agente | ADMIN |

## Puerto

- `8081` (interno, accedido vía API Gateway)

## Dependencias

- **Config Server** — configuración centralizada en `config-server`
- **Eureka Server** — registro y descubrimiento
- **Redis** — rate limiting de sesiones
- **MySQL** — persistencia de usuarios
