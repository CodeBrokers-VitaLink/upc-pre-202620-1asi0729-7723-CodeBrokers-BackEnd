# VitaLink

VitaLink es una plataforma de monitoreo preventivo de adultos mayores que conecta pacientes, familiares, cuidadores y centros de salud. Es desarrollada por el equipo **CodeBrokers**.

## Objetivo del repositorio

`vitalink-backend` organiza la base de directorios y las responsabilidades del equipo para el desarrollo posterior del backend. El paquete previsto es `com.codebrokers.vitalink`.

## Tecnologías previstas

| Tecnología | Propósito previsto |
| --- | --- |
| Java 21 | Lenguaje de desarrollo |
| Spring Boot | Framework del backend |
| Maven | Gestión de dependencias y compilación |
| PostgreSQL | Base de datos relacional |

Estas tecnologías son referencias para el desarrollo futuro; esta entrega no las instala ni configura.

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
| IAM & Security (`iam`) | Integrante 1 | Gestión de usuarios, autenticación JWT, roles, RBAC y Spring Security. | TS-11 |
| Patient Management (`profiles`) | Pablo | Pacientes, centros médicos, afiliaciones, cuidadores y validaciones. | TS-06, TS-07, TS-08 |
| Monitoring (`monitoring`) | Deiby | Telemetría, signos vitales, historial clínico, umbrales y origen de datos. | TS-01, TS-03, TS-09 |
| Alerts & Notifications (`alerts`) | Fern | Alertas, transiciones de estado, notas clínicas y notificaciones asíncronas. | TS-02, TS-04, TS-05, TS-10 |

## Estado actual

Esta entrega prepara exclusivamente la estructura inicial y su documentación, sin implementar componentes funcionales del backend. Los módulos quedan disponibles para su desarrollo posterior por el equipo.

El repositorio ya contenía archivos de un esqueleto Spring Boot con Gradle, una clase principal, una prueba y `application.properties`. Se conservan sin modificaciones y no forman parte de la estructura añadida que muestra el árbol. Maven sigue siendo la herramienta prevista; esta entrega no realiza la migración ni agrega configuración, dependencias o código Java. Tampoco ejecuta compilaciones ni pruebas.
