# Evidencia de implementación y verificación

Fecha: 2026-10-28. Trabajo local en `feature/iam-security`, desde backend `31a26d1dd9a0f3b599009fba596cb36cae146152`. No se hicieron commits, push, merge ni despliegues. Se conservó `AGENTS.md`, que ya estaba agregado al índice antes del trabajo.

## Baseline

- Paquete y módulos confirmados: `com.codebrokers.vitalink`, iam/profiles/monitoring/alerts/shared. Solo existían la aplicación principal, una prueba `contextLoads` y `.gitkeep`.
- Java 21.0.11; Gradle 9.8.2 resolvió correctamente; Spring Boot 4.1.2 se conservó. No fue necesario modificar versiones ni introducir Maven/PostgreSQL.
- La primera ejecución de `gradlew.bat test` falló al crear el datasource: no había URL ni base de pruebas configurada.
- Gradle y Git requirieron ejecución fuera del sandbox para caché/referencias. Docker estaba detenido; se inició para Testcontainers. Los contenedores de pruebas fueron temporales.

## Incrementos

| Incremento | Verificación | Resultado |
| --- | --- | --- |
| Cuentas, BCrypt y migración | 3 pruebas iniciales en H2 | Correcto |
| Login, JWT, CORS, política | Suite incremental, tokens firmados y decoder real | Correcto tras corregir selección de bean CORS y fallback de idioma del sistema |
| Persistencia MySQL | Flyway/JPA sobre `mysql:8.4`, dos inserts concurrentes | Correcto: un alta y un conflicto controlado |
| Aislamiento de pruebas | BD H2 distinta por contexto | Corregido conflicto entre fixtures de clases distintas |
| Backend final | `gradlew.bat test build` | **29 pruebas, 0 fallos, 0 omitidas; JAR generado** |
| Angular | `npm ci`, `npm test -- --watch=false` | **16 pruebas correctas** |
| Angular producción | `npm run build` | Correcto; advertencia CSS existente, sin alterar budgets |
| Parche entregado | `git apply --check` y aplicación sobre otra copia limpia del commit base | Correcto; sin errores de whitespace |

Reportes backend: `build/reports/tests/test/index.html` y `build/test-results/test/`. Artefacto: `build/libs/vitalink-backend-0.0.1-SNAPSHOT.jar`. Los reportes y copias de trabajo quedan ignorados por Git; no son código fuente de entrega.

### Cobertura comprobada

Login de médico/familiar, email normalizado, BCrypt, credenciales inválidas/inactivas/inexistentes, validación sin eco de contraseña, rechazo de campos extra, perfil derivado del principal, ausencia de sesión HTTP, tokens ausentes/malformados/vencidos/con firma incorrecta/algoritmo distinto/`none`/issuer o audience erróneos/claims obligatorios ausentes/`nbf` futuro, desactivación/cambio de rol con token previo, CORS permitido/rechazado, mensajes ES/EN y configuración JWT inválida.

Política: acceso propio/ajeno de médico y familiar, denegación de escritura familiar, scope acotado, revocación y fuente caída. Los escenarios con IDs indirectos y redes separadas son **adaptadores exclusivos de test**, no conexiones a perfiles reales ni evidencia de filtrado de endpoints clínicos.

Angular: login servidor, guard pendiente, restauración válida/fallida, storage corrupto/legado, separación cuenta/perfil, rechazo de roles desconocidos, origen/ruta del interceptor, `401` frente a `403`, respuesta antigua después de otro login, cancelación de login/restauración/Care y limpieza de estado. La prueba inicial del frontend falló por falta de `TranslateService` y esperaba un título obsoleto; se actualizaron providers y aserciones a la UI real sin desactivar pruebas.

### Entorno frontend y límites

Node 24.15.0; npm instalado 12.0.2 (el repositorio declara 11.6.4). Se respetó el lockfile y no se actualizaron dependencias. `npm ci` informó 7 vulnerabilidades de dependencias (1 baja, 1 moderada, 5 altas); no se ejecutó una actualización general automática. Los scripts de instalación bloqueados por la configuración local no impidieron tests/build.

La compilación advierte que `care-workspace.css` ocupa 6.72 kB frente al umbral de advertencia de 4 kB; queda por debajo del error de 8 kB. Ese CSS no fue modificado. No se ha realizado una sesión manual de navegador ni `bootRun` con una BD persistente del usuario; la verificación HTTP backend fue MockMvc y la persistencia MySQL fue Testcontainers.

## Pendientes concretos

- **Pablo:** fuentes persistidas de contexto/afiliaciones, política definitiva de gestión TS-06/08 y visibilidad familiar.
- **Deiby:** vincular recursos/historial/rangos con paciente, filtrar consultas/paginación/conteos y aplicar la política.
- **Fern:** resolver paciente real desde alertas/intervenciones, autorización y auditoría en transacción, contrato de estados y notas.
- **Integración frontend:** aplicar el parche al checkout elegido, comprobar flujo real contra IAM y después conectar Care autorizado. La copia aislada verificada no sustituye esa integración.

**IAM implementado y verificado en el alcance descrito. TS-11 no se cierra hasta probar afiliaciones y endpoints clínicos reales.**
