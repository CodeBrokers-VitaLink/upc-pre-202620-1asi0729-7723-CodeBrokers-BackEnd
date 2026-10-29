# AGENTS.md — VitaLink: IAM & Security / TS-11

## 1. Objetivo y reglas de ejecución

Completa la parte de **Kirk Douglas Quiliano Motta (Kirkcito)**: **IAM & Security, TS-11**, en el backend de VitaLink, equipo CodeBrokers. Implementa usuarios, autenticación JWT, roles, autorización por paciente y seguridad transversal; conecta el frontend existente con cambios acotados. Explica decisiones relevantes en español y verifica el resultado. Implementar localmente; no limitarse a carpetas.

Análisis estático del **28 de octubre de 2026**: no acredita ejecución ni pruebas. Los contratos propuestos aún deben implementarse.

Colocar `AGENTS.md` junto a `build.gradle`. Integrar instrucciones existentes sin borrar acuerdos. Revisar instrucciones superiores/específicas y cambios locales posteriores.

Antes de modificar:

1. Inspeccionar `git status`, rama, remotos, instrucciones, build, recursos, migraciones y pruebas; preservar cambios ajenos.
2. Verificar proyecto: paquete `com.codebrokers.vitalink` y módulos `iam`, `profiles`, `monitoring`, `alerts`, `shared`. El backend encontrado tiene **7723** en su nombre; frontend e informe, **7737**. No renombrarlo ni asumir equivalencia únicamente por el nombre.
3. Reutilizar implementación; rama local IAM según convención, propuesta `feature/iam-security`. Sin push, merge o despliegue.
4. Localizar el frontend. Si no está disponible para editar, consultar el repositorio y entregar un parche aplicable; no declarar integración ejecutada.
5. Plan breve e implementación incremental. Consultar ambigüedades de permisos/datos/acuerdos y continuar tareas independientes.

## 2. Evidencia y alcance

### Fotografía revisada

| Fuente | Referencia | Estado observado |
| --- | --- | --- |
| Frontend solicitado | `Kirkcito/upc-pre-202620-1asi0729-7737-CodeBrokers-Frontend`, `main`, `a51b67c1d15cbf34c73d83d85e6c1c73675c4b20` | Angular con autenticación de demostración y API simulada. |
| Backend encontrado | `CodeBrokers-VitaLink/upc-pre-202620-1asi0729-7723-CodeBrokers-BackEnd`, `main`, `31a26d1dd9a0f3b599009fba596cb36cae146152` | Esqueleto Spring Boot y carpetas DDD con `.gitkeep`; sin implementación funcional de módulos. |

El reparto procede del acuerdo previo del equipo y del README backend, donde IAM figura como “Integrante 1”. Las versiones locales pueden haber avanzado.

**Fuente funcional:** `upc-pre-202620-1asi0729-7737-CodeBrokers-report-TB1.pdf`: historias, páginas físicas **37–40**; arquitectura **85–90**; IAM/trazabilidad **92–93**. TS-11: física 40, impresa 39. El backend figura como planificado; esta guía permite iniciar sin los PDF.

Adaptar ejemplos académicos antiguos a Boot 4. Social Responsibility no es la rúbrica de este backend.

### Reparto

| Responsable | Módulo | Historias | Responsabilidad |
| --- | --- | --- | --- |
| Kirk | `iam`, seguridad compartida | TS-11 | Usuarios, JWT, roles y autorización. |
| Pablo | `profiles` | TS-06/07/08 | Pacientes, clínicas, afiliaciones, cuidadores y validación. |
| Deiby | `monitoring` | TS-01/03/09 | Telemetría, registros, historial, umbrales y origen. |
| Fern | `alerts`, notificaciones | TS-02/04/05/10 | Alertas, estados, observaciones y notificaciones. |

No duplicar módulos ajenos; IAM aporta identidad y políticas compartidas.

### Historias y aceptación

**TS-11: Control de acceso a datos de salud (RBAC).** Validar JWT, rol y afiliación. Criterio central: un médico que solicita datos de un paciente no afiliado recibe **403 Forbidden**, sin acceso a esos datos. La mención de “API Gateway” del informe se satisface en este monolito mediante Spring Security y autorización de aplicación; no exige agregar un microservicio.

| Historia | Requisito | Aporte de IAM |
| --- | --- | --- |
| US-02 | Informar medidas de privacidad/seguridad antes de registrar datos. | Controles reales; JWT no demuestra certificación HIPAA/GDPR. |
| US-07 | Explicar quién ve datos clínicos y familiares. | Política verificable de roles y acceso por paciente. |
| US-13 | Acceder al detalle biométrico desde una alerta. | Autorizar al paciente real asociado a la alerta. |
| TS-06 | `POST /api/v1/patients/{id}/affiliations`, relación paciente–clínica. | Consumir afiliaciones activas de Pablo para aislamiento. |
| TS-08 / US-24 | `PUT /api/v1/patients/{id}/caregivers`, red familiar. | Autorizar por relaciones persistidas, no IDs del navegador. |
| TS-02 / TS-03 | Listar alertas e historial. | Filtrar datos y conteos en servidor. |
| TS-04 / TS-05 | Cambiar estado y agregar observaciones. | Comprobar rol/afiliación y derivar autor desde el principal. |

Login/perfil habilitan TS-11; no inventar US. Fuera de alcance: registro público, recuperación, OAuth social, MFA, refresh, SMS, IoT, agenda y CRUD clínico. Registro figura como visión futura, sin pantalla actual.

## 3. Stack y discrepancias

| Evidencia real | Instrucción |
| --- | --- |
| Java toolchain 21; Spring Boot declarado `4.1.2`. | Mantener Java 21 y verificar compatibilidad/resolución de dependencias. |
| `build.gradle`, wrapper declarado `9.8.2`, `gradlew.bat`. | Usar Gradle existente; no generar Maven en paralelo. |
| Spring Data JPA, Security, Validation, WebMVC; driver `com.mysql:mysql-connector-j`. | Base inicial MySQL; configurar datasource y migraciones. |
| README propone Maven/PostgreSQL y reconoce que conservó el esqueleto Gradle. | Propuesta no implementada; actualizar documentación al resultado efectivo. |
| `application.properties` solo contiene nombre de aplicación. | BD, JWT y entornos no están configurados todavía. |
| Angular 22.2.x, signals, Material, HttpClient, ngx-translate; npm declarado 11.6.4. | Respetar versiones y lockfile; evitar actualización general del stack. |

**Propuesta:** conservar Gradle/MySQL, también citado en el informe. Si localmente ya migraron o hay un acuerdo posterior, seguirlo. No mezclar herramientas, drivers o SQL incompatibles.

Si una versión no resuelve, registrar el error y verificar documentación oficial antes del cambio mínimo. Usar APIs/imports compatibles, incluidos `jakarta.*`.

## 4. Mapa del frontend que debes revisar

Rutas relativas al frontend:

| Archivo | Hallazgo y cambio requerido |
| --- | --- |
| `src/app/iam/infrastructure/authentication.service.ts` | Hace `GET /users?email=...`, compara `user.password` en cliente y guarda ID/rol en `vitalink-login-session`. Sustituir por POST login y validación servidor. |
| `src/app/iam/infrastructure/user.response.ts`, `user.service.ts`, `user.assembler.ts` | DTO incluye contraseña; perfil se obtiene por `/users/{id}`. Eliminar password; usar `/auth/me`. |
| `src/app/iam/application/authentication.store.ts`, `user.store.ts` | Sesión restaurada síncronamente desde storage; perfil por ID. Agregar inicialización validada y limpieza/cancelación por sesión. |
| `src/app/iam/domain/model/session.entity.ts`, `infrastructure/session.response.ts`, `session.assembler.ts` | Solo `userId` y rol. Adaptar contrato, expiración y sesión pendiente. |
| `src/app/iam/domain/model/role.entity.ts`, `infrastructure/role.assembler.ts` | Roles `doctor`/`family`, destinos `/doctor/dashboard` y `/family/dashboard`. Preservar rutas y validar roles externos. |
| `src/app/iam/presentation/guards/role.guard.ts`, `src/app/app.routes.ts` | Guard confía en sesión local. Debe esperar validación; solo controla navegación. |
| `src/app/app.config.ts` | `provideHttpClient()` sin interceptor; `CARE_CONTEXT` desde environment. Registrar interceptor y contexto de identidad reactivo. |
| `src/environments/environment.ts`, `environment.production.ts` | Fake API `https://vitallink-fakeapi.onrender.com`; `providerId: DR-01`, `familyId: FC-01`. Reemplazar URL/IDs fijos en integración real. |
| `src/app/care/application/care.store.ts` | Filtra colecciones en cliente; compara `patient.providerId` con `doctor.id`. Consumir datos autorizados y vínculos explícitos. |
| `src/app/care/infrastructure/care/care-context.ts`, `care.service.ts` | Contexto fijo y adaptador HTTP; coordinar con backend clínico. |
| `src/app/care/presentation/views/care-workspace/care-workspace.ts` | Usa contexto fijo y `vitalink-patient`. Aislar selección/datos por sesión. |
| `src/app/iam/presentation/views/login/`, `profile/`, `src/app/app.ts` | Mantener UI; adaptar login/perfil y unificar los dos logout. |
| `public/i18n/en.json`, `es.json` | Mantener traducciones; `iam.loginUnavailable` todavía menciona el recurso `users`. |

Observaciones que afectan al contrato:

- “Solicitar acceso” es texto visual en `login.html`, no registro. No habilitar registro abierto que permita elegir rol médico.
- `care.service.ts` carga **seis** recursos con `forkJoin`: `/patients`, `/alerts`, `/records`, `/interventions`, `/caregivers`, `/measurementRanges`. Si uno falla, falla la carga. Retargetear la URL a un backend con solo login no completa integración.
- Usa además `GET /alerts/{id}`, `PATCH /alerts/{id}`, `POST /interventions`. Cambio de estado y auditoría son dos peticiones; la transacción corresponde a Fern.
- En el frontend, `PatientResponse.providerId` se usa como **médico**; TS-06 habla de **clínica**. Cuenta IAM, profesional, clínica y cuidador son identidades diferentes. `hospital` es texto visible, no criterio de autorización.
- `InterventionResponse.role = professional` es vocabulario de auditoría, no un tercer rol IAM. El servidor deriva autor y rol.
- `measurementRanges` no incluye `patientId`. Si es catálogo global, exigir autenticación; si se convierte en rango clínico por paciente, Deiby aporta vínculo y autorización.
- README enlaza `api-update/README.md`, ausente. No asumir endpoints descritos pero no presentes en código.
- `app.spec.ts` espera `Hello, vitalink`: verificar posible obsolescencia; no desactivarlo para obtener verde.

## 5. Arquitectura y cuentas

Extender el monolito; dominio independiente de HTTP/Spring Security, sin repositorios ajenos en controllers. Reutilizar convenciones; nombres siguientes orientativos.

| Ubicación bajo `com.codebrokers.vitalink` | Responsabilidad |
| --- | --- |
| `iam/domain` | `UserAccount`, `UserRole`, invariantes y puertos. |
| `iam/application` | Autenticación, consulta propia y aprovisionamiento controlado. |
| `iam/infrastructure` | Repositorio/mapeo JPA, contraseñas, principal y tokens. |
| `iam/interfaces/rest` | Controller, requests, responses y assemblers. |
| `shared/security` | Cadena de seguridad, políticas técnicas y contratos transversales mínimos. |
| `shared/exception`, `shared/configuration` | Errores, CORS, propiedades y mensajes. |
| `src/main/resources/db/migration` | Migración incremental IAM. |
| `src/test/java/com/codebrokers/vitalink` | Pruebas de comportamiento y persistencia. |

Cuenta mínima: ID estable, email normalizado/único, hash de contraseña, nombre, rol único, estado activo y timestamps. Teléfono opcional; clínica/afiliación pertenece a `profiles`. No serializar entidades JPA directamente ni exponer hashes mediante DTOs, logs o `toString`.

- Normalizar email (`trim`, minúsculas consistentes), validar formato/longitudes; no transformar contraseña.
- Restricción única en BD además del servicio; duplicado concurrente debe producir conflicto controlado.
- IDs JSON como strings opacos. UUID es una propuesta si no existe convención; no deducir relaciones por prefijos `DR-`/`FC-` o nombres.
- Usar `PasswordEncoder` con hash adaptativo; nunca texto plano, cifrado reversible o SHA simple. Si se usa BCrypt, respetar límite de entrada en bytes y evitar truncamiento silencioso.
- Migraciones compatibles con BD elegida. Si se añade Flyway/MySQL, usar soporte compatible. No editar migraciones ya aplicadas ni ejecutar `ddl-auto=create/create-drop` sobre datos persistentes.
- Transacciones en servicios de escritura, respetando límites de agregados.
- Bootstrap **explícito** `local`/`test` con usuarios ficticios y contraseñas externas; desactivado en producción. Idempotente y sin resetear contraseñas existentes al arrancar.
- No publicar alta que acepte libremente `role`, `active`, clínica o afiliaciones. No inventar `ADMIN` para evitar diseñar autorización.

### Roles

| Dominio del informe | Authority propuesta | API/UI |
| --- | --- | --- |
| `PHYSICIAN` | `ROLE_PHYSICIAN` | `doctor` |
| `FAMILY_MEMBER` | `ROLE_FAMILY_MEMBER` | `family` |
| `OLDER_ADULT` | Sin permisos habilitados en este incremento | Sin ruta ni flujo implementado. |

Implementar los dos primeros. No convertir `OLDER_ADULT` en familiar ni provisionarlo como usuario funcional sin requisito. `PRIMARY`/`SECONDARY` son roles de afiliación de cuidadores, no roles globales de login.

## 6. Contrato IAM propuesto

Base: **`/api/v1`**. Si existe contrato local vigente, conservarlo y adaptar frontend/documentación juntos, evitando aliases innecesarios.

| Operación | Acceso / resultado |
| --- | --- |
| `POST /api/v1/auth/login` | Público. `200` token/usuario; `400` formato inválido; `401` credenciales inválidas o cuenta inactiva. |
| `GET /api/v1/auth/me` | Bearer válido. `200` usuario del principal; `401` sin autenticación válida. |

No exponer `GET /users` para login ni permitir todo `/auth/**`. Migrar perfil a `/auth/me`; si se conserva `/users/{id}`, limitarlo al propio usuario. Sin `/register` ni `/refresh` sin requisito.

Login request:

```json
{"email":"doctor@example.test","password":"<provided-locally>"}
```

Login response; datos ilustrativos:

```json
{
  "accessToken": "<signed-jwt>",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": "account-id",
    "name": "Demo Doctor",
    "email": "doctor@example.test",
    "role": "doctor",
    "phone": null,
    "hospital": null,
    "careContext": {
      "providerId": "physician-profile-id",
      "familyId": null,
      "clinicId": "clinic-id"
    }
  }
}
```

`GET /auth/me` devuelve directamente ese objeto `user`. Para familiar: `role: family`, `familyId` del perfil cuidador, campos no aplicables `null`. Resolver contexto mediante relaciones persistidas; sin afiliaciones, campos nulos y ningún permiso clínico. La ausencia de perfil no debe impedir innecesariamente autenticar una cuenta válida; una caída de la fuente no equivale a ausencia de afiliaciones.

`providerId` conserva la semántica profesional del frontend; `clinicId` es institución; `id` es cuenta IAM. Si cambian los nombres, actualizar todos los consumidores. El contexto orienta la UI, no sustituye autorización en cada operación. TTL de 900 segundos es propuesta configurable. No incluir password, hash, secretos, biometría o listas completas de pacientes en estas respuestas.

Error propuesto, consistente también en filtros:

```json
{
  "status": 403,
  "code": "PATIENT_ACCESS_DENIED",
  "message": "Access to this resource is not allowed.",
  "path": "/api/v1/patients/patient-id",
  "timestamp": "2026-10-28T12:00:00Z",
  "errors": []
}
```

Reutilizar el formato común si ya existe. Códigos estables: `INVALID_CREDENTIALS`, `UNAUTHENTICATED`, `ACCESS_DENIED`, `PATIENT_ACCESS_DENIED`, `VALIDATION_ERROR`, `EMAIL_ALREADY_EXISTS`. Validación `400` por campo sin valores sensibles; `401` sin revelar si el email existe; `403` sin detalles ajenos. No convertir errores en `200` ni fallos esperados en `500`.

Mensajes localizados: `Accept-Language`, `messages*.properties`, códigos estables y funcionamiento stateless; no copiar `SessionLocaleResolver` por rutina.

## 7. Seguridad y autorización

### Configuración

- `SecurityFilterChain`, DSL lambda y `requestMatchers`; no `WebSecurityConfigurerAdapter`/`antMatchers`.
- Stateless con Bearer; sin form login/HTTP Basic no utilizados ni sesiones accidentales. Una estrategia JWT mantenida y compatible; no criptografía propia.
- Validar algoritmo permitido, firma, `exp`, `nbf` si existe, `iss`, `aud`, `sub`; rechazar `alg=none` y manipulación. Propuesta: `sub` = cuenta, `role` = enum del dominio. Verificar cuenta activa, rol y afiliaciones vigentes, incluso con token previo.
- HS256 requiere secreto aleatorio externo de al menos 256 bits, validado al iniciar. Sin claves default, logs de secretos ni regeneración silenciosa en cada arranque. JWT firmado no cifra claims; excluir salud/credenciales.
- CORS antes de autenticación: `http://localhost:4200` y orígenes explícitos por entorno. Permitir preflight sin JWT, headers/métodos necesarios; no comodín global.
- Documentar decisión CSRF para Bearer explícito sin cookies automáticas. Si se usan cookies de autenticación, reevaluar protección CSRF.
- Permitir login y recursos técnicos concretos; política cerrada para el resto, nunca `/api/v1/**` público. Swagger según perfil/política acordada.
- Entry point para `401`, access denied handler para `403`; advice de controllers no cubre todos los errores de filtros.
- Conectar variables a propiedades: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET_BASE64`, `JWT_ISSUER`, `JWT_AUDIENCE`, `JWT_TTL_SECONDS`, `CORS_ALLOWED_ORIGINS`. Documentar configuración del IDE; `.env` no se carga automáticamente en Spring Boot.
- Logs mínimos sin passwords, JWT ni datos clínicos. Evaluar límite de intentos para despliegue; memoria de una instancia no equivale a protección distribuida.

### Política por paciente

El rol no basta: autorizar por principal y relaciones activas de `profiles`.

**Política inicial conservadora propuesta:** médico con relación profesional activa al paciente y clínica correspondiente cuando esté modelada; familiar con afiliación activa como cuidador. No ampliar acceso por coincidencia del texto `hospital`. Si el equipo acuerda acceso institucional más amplio, documentarlo y probarlo antes de habilitarlo.

| Operación | Médico | Familiar |
| --- | --- | --- |
| Perfil propio | Permitido | Permitido |
| Pacientes, alertas, registros, historial | Solo pacientes autorizados | Solo vinculados y campos permitidos |
| Recurso por ID | Resolver paciente y autorizar | Mismo control; conocer ID no basta |
| Revisión/atención y nota clínica | Profesional autorizado | Denegado; confirmación familiar es otro caso de uso |
| Afiliaciones/cuidadores | Política TS-06/08 acordada con Pablo | No autoasignar pacientes ni elevar permisos |
| Telemetría/origen asistido | Contrato de Deiby | Operación autorizada, sin suplantar dispositivo o médico |

Sin JWT válido: `401`; con identidad válida sin permisos: `403`. TS-11 exige `403` para paciente existente no autorizado. Un recurso inexistente puede producir `404` según política común, sin revelar datos ajenos.

Resolver relaciones desde almacenamiento: `alertId → patientId`, `recordId → patientId`, `interventionId → alertId → patientId`. No confiar en body/query con `patientId`, `actorId`, `providerId`, `familyId`, `clinicId` o `role` para conceder permisos. Derivar autor y fecha de auditoría en servidor.

Filtrar listados, búsquedas, paginación, agregados y conteos **en servidor**, antes de devolverlos. No filtrar después de calcular totales globales. Proyectar también relaciones anidadas: un cuidador permitido no autoriza exponer todos sus `patientIds` o contactos. Evitar cachés compartidas que ignoren usuario/ámbito/revocación.

### Dependencia con perfiles y otros módulos

Definir/reutilizar un puerto como `PatientAccessReader` y política `PatientAccessPolicy`: resolver vínculos del principal, comprobar relación activa y acotar consultas. Pablo conserva la fuente de afiliaciones; IAM aporta principal y política. Deiby/Fern la aplican en casos de uso, no solo controllers o botones. Referenciar agregados por ID, sin importar entidades JPA ajenas; mantener consistencia de autorización al escribir.

**En el backend revisado estos módulos todavía están vacíos.** Si siguen así:

1. Completar IAM persistente, login, `/auth/me`, configuración y pruebas propias.
2. Implementar puerto/política y pruebas con adaptadores exclusivos de test; documentar el contrato que debe conectar Pablo.
3. En producción, falta de afiliación/fuente implica no conceder acceso. Prohibidos `return true`, fixtures clínicos o adaptadores permisivos. Una caída técnica se reporta como indisponibilidad y falla cerrada.
4. No crear controllers clínicos ficticios. **No cerrar TS-11 hasta verificar autorización con relaciones y endpoints reales.** Distinguir IAM terminado de integración pendiente.

## 8. Integración Angular

1. Sustituir GET de credenciales por POST login; eliminar password de DTOs de lectura y adaptar assemblers. No devolver contraseñas desde Java para conservar el flujo viejo.
2. Mantener UI/rutas actuales. Navegar por rol validado; rechazar roles no soportados sin convertirlos silenciosamente.
3. Centralizar token en un servicio pequeño sin `HttpClient` ni dependencia del store, evitando ciclo store → HTTP → interceptor → store.
4. Para recarga en el piloto, puede usarse `sessionStorage` con token corto, nunca password ni datos clínicos. Es accesible a JavaScript y no protege frente a XSS; documentar esa limitación, sin persistencia indefinida.
5. Restaurar como sesión **pendiente** y consultar `/auth/me`; guard espera. JSON válido o JWT decodificado en cliente no prueban autenticación. Storage corrupto/token vencido/`401`: limpiar; fallo de red: estado recuperable sin mostrar acceso validado.
6. Interceptor funcional mediante `provideHttpClient(withInterceptors([...]))`. Adjuntar Bearer solo al origen y prefijo exactos de API real; comparar URL de manera segura, no prefijos que acepten dominios parecidos. Excluir i18n/assets/fake API/terceros y login cuando corresponda.
7. `401` protegido: invalidar una vez y volver a login, sin bucles. `401` login: `invalidCredentials`. `403`: informar permiso insuficiente sin cerrar sesión. Red/5xx: indisponibilidad.
8. Unificar logout en `app.ts` y `profile.ts`: limpiar token, sesión, perfil, CareStore, contexto y `vitalink-patient`. Cancelar peticiones o ignorar respuestas de una sesión anterior para evitar que repueblen datos. Conservar idioma/preferencias no sensibles.
9. Borrar token del navegador no revoca un JWT stateless ya emitido: puede funcionar hasta expirar. No prometer revocación inmediata; si se exige, implementar mecanismo servidor y probarlo.
10. Reemplazar `CARE_CONTEXT` fijo por contexto reactivo validado; no asumir `user.id === providerId === familyId`. Los filtros UI son presentación; servidor ya debe haber autorizado.
11. Alinear seis GET y escrituras de Care antes de cambiar base global. Backlog usa `/patients/{id}/history` y `/alerts/{id}/notes`; frontend usa `/records` e `/interventions`. Requieren contrato/adaptador, no sustitución ciega. Desempaquetar paginación si reemplaza arrays.
12. Si solo IAM está listo, separar temporalmente `authApiUrl` de Care en un perfil de desarrollo explícito. Nunca enviar JWT al fake API, usarlo con datos reales ni presentar ese modo como integración segura completa. La configuración final conecta Care al backend autorizado.
13. Coordinar `IN_REVIEW` de frontend/modelo frente a `REVIEW` en criterio TS-04: un contrato único o adaptador acordado con Fern; no renombrar desde IAM unilateralmente.
14. Actualizar EN/ES, incluidos mensajes que mencionan `/users`. Evitar rediseño y cambios clínicos ajenos a la integración.

## 9. Secuencia y entregables

1. Baseline local: estado, build/configuración disponibles y discrepancias; separar fallo previo de regresión.
2. Persistencia: migración, cuenta/rol, repositorio, hash y aprovisionamiento local controlado.
3. Autenticación: login, principal, emisión/validación JWT, `/auth/me` y DTOs seguros.
4. Seguridad: cadena, errores, CORS, política por objeto y puerto de afiliaciones; probar antes de pantallas.
5. Frontend: login, interceptor, restauración, guards, perfil/logout; Care solo con contratos disponibles.
6. Verificar y documentar resultados y pendientes por responsable.

Entregar código, pruebas, README, contrato IAM (OpenAPI o equivalente), variables y ejemplos `.http` sin secretos. Documentar integración de otros módulos. Frontend ausente: parche con pasos, marcado no aplicado/no verificado.

## 10. Pruebas y definición de terminado

Probar comportamiento, no solo getters o `contextLoads`.

| Caso | Resultado requerido |
| --- | --- |
| Login válido médico/familiar | `200`, JWT verificable y rol/TTL correctos; sin password/hash. |
| Contraseña errónea/cuenta inexistente/inactiva | `401` genérico, sin enumeración de cuentas. |
| Body/email inválido | `400` consistente, sin repetir password. |
| Token ausente/vencido/firma manipulada/issuer o audience incorrectos | `401`, nunca `500` por parsing. |
| `/auth/me` válido | Identidad del principal, no de parámetros del cliente. |
| Médico con paciente propio/ajeno | Permitir propio; `403` ajeno. |
| Familiar vinculado/no vinculado | Permitir vinculado; `403` ajeno. |
| Dos clínicas, dos médicos, dos familiares, IDs de cuenta/perfil diferentes | Sin fuga por IDs fijos ni confusión de identidades. |
| IDs indirectos de alerta/registro/intervención ajenos | `403`. |
| Listados/filtros/conteos/relaciones anidadas | Solo información autorizada. |
| Familiar ejecuta revisión médica o body manipula rol/autor/ámbito | `403` o rechazo de campos; ninguna elevación ni escritura ajena. |
| Revocar afiliación/desactivar cuenta con token previo | La siguiente operación respeta el cambio. |
| Fuente de afiliación caída | Fallo cerrado e indisponibilidad controlada. |
| Preflight permitido/no permitido | Solo origen permitido recibe autorización CORS. |
| Logout, respuesta HTTP tardía y login de otra cuenta | No reaparecen datos/selección/perfil previos. |
| Storage manipulado o restauración fallida | No autoriza vistas sin servidor. |
| Interceptor ante assets, fake API y terceros | No envía Bearer. |
| `403` versus `401` | No cerrar sesión válida por permiso insuficiente. |
| Alta concurrente con email duplicado normalizado | Restricción BD y conflicto controlado. |

Usar JUnit y Security/MockMvc compatibles con Boot 4. Probar JWT realmente inválidos, sin omitir decoder. Validar migraciones/consultas con Testcontainers o BD de pruebas del motor elegido; H2 solo no demuestra compatibilidad MySQL. Nunca usar producción.

Backend desde raíz; Windows: `gradlew.bat`/`.\gradlew.bat`:

```sh
java -version
./gradlew --version
./gradlew test
./gradlew build
```

Con BD/propiedades configuradas y perfil local creado:

```sh
./gradlew bootRun --args='--spring.profiles.active=local'
```

Frontend, desde su raíz y con Node compatible:

```sh
npm ci
npm test -- --watch=false
npm run build
npm start
```

No existe script `lint`. No relajar budgets ni desactivar pruebas para ocultar fallos. Ante bloqueos, registrar comando, causa y pendiente; no declarar éxito.

Checklist de cierre:

- [ ] Cuentas persistentes, login/perfil sin secretos y JWT probado positiva/negativamente.
- [ ] `401/403` y CORS correctos.
- [ ] Autorización real por afiliación conectada/probada; si falta, TS-11 permanece pendiente de integración.
- [ ] Frontend autentica, revalida y limpia todos los estados de sesión.
- [ ] Sin confundir identidad de cuenta, profesional, clínica y cuidador.
- [ ] Contratos clínicos resueltos o pendientes concretos con responsable.
- [ ] README/configuración reflejan stack real; build/pruebas tienen evidencia.
- [ ] Reporte separa implementado, verificado y pendiente.

## 11. Estilo y evidencia

- Código, identificadores y comentarios en inglés; explicación al usuario en español.
- Comentarios solo para decisiones/invariantes/limitaciones no evidentes. Sin narrar cada línea, Javadoc trivial ni tutoriales dentro de métodos.
- Seguir convenciones del proyecto, responsabilidades pequeñas, inyección por constructor y validación explícita. No añadir dependencias/capas sin necesidad.
- No agregar firmas, banners de generación ni metacomentarios sobre herramientas o asistentes en código, UI, README técnico o mensajes de cambios. Describir comportamiento de la aplicación.
- Conservar licencias/atribuciones. No fabricar autoría, historial, commits, capturas, pruebas, despliegues o certificaciones; no cambiar identidad Git.
- Si se solicitan commits, describir cambios reales siguiendo la convención, por ejemplo `feat(iam): implement JWT authentication for TS-11`.
- Cierre breve: qué cambió, por qué, qué se verificó y qué depende de otro módulo, con archivos para que Kirk pueda revisarlo y comprenderlo.

## 12. Referencias verificables

Repositorios y fuentes ancladas a los commits revisados:

- [Frontend: código completo](https://github.com/Kirkcito/upc-pre-202620-1asi0729-7737-CodeBrokers-Frontend/tree/a51b67c1d15cbf34c73d83d85e6c1c73675c4b20).
- [Backend: estructura y reparto](https://github.com/CodeBrokers-VitaLink/upc-pre-202620-1asi0729-7723-CodeBrokers-BackEnd/blob/31a26d1dd9a0f3b599009fba596cb36cae146152/README.md).
- [Backend: dependencias](https://github.com/CodeBrokers-VitaLink/upc-pre-202620-1asi0729-7723-CodeBrokers-BackEnd/blob/31a26d1dd9a0f3b599009fba596cb36cae146152/build.gradle).

Documentación oficial consultada; comprobar versión al implementar:

- [Spring Security: autorización HTTP](https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html).
- [Spring Security: JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html).
- [Spring Security: contraseñas](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html).
- [Spring Security: CORS](https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html).
- [Angular: interceptores](https://angular.dev/guide/http/interceptors).
- [OpenAI: AGENTS.md](https://learn.chatgpt.com/docs/agent-configuration/agents-md).
