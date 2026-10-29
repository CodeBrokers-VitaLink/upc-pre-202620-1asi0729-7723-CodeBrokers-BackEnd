# VitaLink

VitaLink es una plataforma de monitoreo preventivo de adultos mayores que conecta pacientes, familiares, cuidadores y centros de salud. Es desarrollada por el equipo **CodeBrokers**.

## Objetivo del repositorio

`vitalink-backend` implementa IAM y seguridad compartida dentro del monolito `com.codebrokers.vitalink`. Los módulos clínicos conservan sus límites y siguen pendientes de implementación por sus responsables.

## Tecnologías utilizadas

| Tecnología | Propósito |
| --- | --- |
| Java 21 | Lenguaje de desarrollo |
| Spring Boot 4.1.2 / Spring Security | HTTP, validación, JWT y autorización |
| Gradle 9.8.2 (wrapper) | Gestión de dependencias y compilación |
| MySQL 8.4 / Flyway / JPA | Persistencia y migraciones |

Se conserva el stack Gradle/MySQL del esqueleto. La propuesta anterior de Maven/PostgreSQL no fue implementada. No se cambia el nombre del repositorio backend **7723** por el del frontend **7737**.

## Arquitectura y estructura

Se propone un **monolito modular basado en Domain-Driven Design (DDD)**: una aplicación organizada en módulos de negocio con límites y responsabilidades explícitos. Cada módulo separa comunicación externa, casos de uso, reglas de negocio e integraciones. El dominio debe mantenerse independiente de los detalles técnicos de infraestructura.

La estructura de trabajo prevista es:

```text
vitalink-backend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── codebrokers/
│   │   │           └── vitalink/
│   │   │               ├── iam/
│   │   │               │   ├── interfaces/
│   │   │               │   ├── application/
│   │   │               │   ├── domain/
│   │   │               │   └── infrastructure/
│   │   │               ├── profiles/
│   │   │               │   ├── interfaces/
│   │   │               │   ├── application/
│   │   │               │   ├── domain/
│   │   │               │   └── infrastructure/
│   │   │               ├── monitoring/
│   │   │               │   ├── interfaces/
│   │   │               │   ├── application/
│   │   │               │   ├── domain/
│   │   │               │   └── infrastructure/
│   │   │               ├── alerts/
│   │   │               │   ├── interfaces/
│   │   │               │   ├── application/
│   │   │               │   ├── domain/
│   │   │               │   └── infrastructure/
│   │   │               │       └── notifications/
│   │   │               └── shared/
│   │   │                   ├── configuration/
│   │   │                   ├── exception/
│   │   │                   └── security/
│   │   └── resources/
│   │       └── db/
│   │           └── migration/
│   └── test/
│       └── java/
├── README.md
└── .gitignore
```

Las carpetas vacías contienen un archivo `.gitkeep` para conservarlas en Git; se omite en el árbol para facilitar la lectura.

| Carpeta o capa | Finalidad |
| --- | --- |
| `interfaces` | Comunicación externa y API REST. |
| `application` | Casos de uso y coordinación de procesos. |
| `domain` | Entidades y reglas de negocio del módulo. |
| `infrastructure` | Persistencia e integraciones externas. |
| `alerts/infrastructure/notifications` | Integraciones para el envío de notificaciones asíncronas. |
| `shared/configuration` | Configuración técnica transversal futura. |
| `shared/exception` | Tratamiento común de excepciones. |
| `shared/security` | Soporte técnico de seguridad compartido, bajo responsabilidad de IAM & Security. |
| `src/main/resources/db/migration` | Futuros scripts de migración de la base de datos. |
| `src/test/java` | Futuras pruebas del backend. |

`shared` agrupa elementos técnicos comunes; las reglas de negocio pertenecen a sus respectivos módulos.

## Módulos y equipo

| Módulo | Responsable | Alcance previsto | Technical Stories |
| --- | --- | --- | --- |
| IAM & Security (`iam`) | Kirk Douglas Quiliano Motta (Kirkcito), Integrante 1 | Gestión de usuarios, autenticación JWT, roles, RBAC y Spring Security. | TS-11 |
| Patient Management (`profiles`) | Pablo | Pacientes, centros médicos, afiliaciones, cuidadores y validaciones. | TS-06, TS-07, TS-08 |
| Monitoring (`monitoring`) | Deiby | Telemetría, signos vitales, historial clínico, umbrales y origen de datos. | TS-01, TS-03, TS-09 |
| Alerts & Notifications (`alerts`) | Fern | Alertas, transiciones de estado, notas clínicas y notificaciones asíncronas. | TS-02, TS-04, TS-05, TS-10 |

## Estado actual

IAM implementa cuentas persistentes, contraseñas BCrypt, login, perfil propio, JWT HS256, errores localizados y CORS. No existe registro público ni un rol administrador. El esquema se valida con JPA y se migra con Flyway; nunca se recrean tablas al iniciar.

**TS-11 sigue pendiente de integración clínica.** La política y el puerto de afiliaciones están implementados, pero `profiles`, `monitoring` y `alerts` todavía no tienen relaciones ni endpoints reales. Sin adaptador de afiliaciones, una consulta de acceso clínico falla cerrada con `503 AFFILIATIONS_UNAVAILABLE`. El contexto de una cuenta no vinculada contiene IDs nulos.

## Configuración y ejecución

Requiere Java 21 y MySQL 8.4 con una base vacía de desarrollo. Configurar las variables de [.env.example](.env.example) en **Run Configuration → Environment variables** de IntelliJ o en el proceso de PowerShell. Spring Boot **no carga `.env` automáticamente**. La clave JWT debe ser aleatoria, externa y contener al menos 32 bytes decodificados; no hay secreto por defecto.

```powershell
# Genera una clave solo en la sesión actual; no la imprime ni persiste.
$jwtKeyBytes = New-Object byte[] 32
$jwtKeyGenerator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtKeyGenerator.GetBytes($jwtKeyBytes)
$env:JWT_SECRET_BASE64 = [Convert]::ToBase64String($jwtKeyBytes)
$jwtKeyGenerator.Dispose()
.\gradlew.bat --version
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat bootRun --args='--spring.profiles.active=local'
```

Las pruebas utilizan H2 para HTTP/seguridad y un contenedor temporal **MySQL 8.4 real** para migraciones, mapeo y concurrencia. Docker debe estar iniciado; la suite falla si MySQL no puede ejecutarse, no omite esas pruebas. No utiliza la BD configurada en variables para las pruebas.

Para cuentas ficticias, activar explícitamente `LOCAL_BOOTSTRAP_ENABLED=true`, con perfil `local` y contraseñas externas `LOCAL_DOCTOR_PASSWORD` y `LOCAL_FAMILY_PASSWORD` (mínimo 12 caracteres, máximo 72 bytes UTF-8). Se crean `doctor@example.test` y `family@example.test` sin afiliaciones. El bootstrap no modifica cuentas existentes y está excluido si se activa `prod`.

## Contrato y revisión

- [Contrato IAM y decisiones de seguridad](docs/iam-contract.md).
- [Integración de perfiles, monitoring y alerts](docs/module-integration.md).
- [Ejemplos HTTP](docs/iam.http).
- [Parche Angular y pasos de aplicación](docs/frontend-integration.md).
- [Evidencia de verificación y pendientes](docs/verification.md).

Solo `POST /api/v1/auth/login` es público. `GET /api/v1/auth/me` requiere Bearer; los demás recursos API requieren autenticación y sus casos de uso deben aplicar autorización por paciente. El frontend mantiene `doctor` y `family`; las autoridades internas son `ROLE_PHYSICIAN` y `ROLE_FAMILY_MEMBER`.
