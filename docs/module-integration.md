# Integración de autorización TS-11

**Pendiente: relaciones persistidas y endpoints clínicos reales.** No se deben interpretar las pruebas con adaptadores exclusivos de test como cierre de TS-11. IAM no crea tablas, fixtures ni controllers clínicos en producción.

## Pablo: profiles

Implementar beans de `IdentityContextReader` y `PatientAccessReader`. Las configuraciones de respaldo se desactivan al existir esos beans; no se requiere `@Primary` ni permitir acceso por defecto.

- `contextFor(AccountIdentity)`: resolver cuenta → profesional/cuidador → clínica mediante relaciones persistidas activas. Sin perfil, devolver `CareContext.empty()`; una caída técnica lanza `ApplicationFailure("AFFILIATIONS_UNAVAILABLE")` o `DataAccessException`. No confiar en IDs de navegador ni en el texto hospital.
- `authorizedPatientIds(AccountIdentity)`: conjunto inmutable de IDs de pacientes con vínculo activo. Médico: relación profesional–paciente activa y clínica correspondiente cuando se modele. Familiar: relación cuidador–paciente activa. Cuenta y perfil tienen IDs diferentes. No inferir identidades por prefijos.
- Relación inexistente/revocada: conjunto vacío. Fuente caída: excepción de indisponibilidad; nunca conjunto universal, `return true` ni silenciamiento de fallos.
- Acordar política TS-06/08 antes de habilitar afiliaciones/cuidadores. Este incremento no concede gestión de vínculos ni autoasignación. No se habilita acceso institucional amplio sin acuerdo y pruebas.

El adaptador actual de contexto vacío permite login sin perfiles. El adaptador actual de acceso lanza `503 AFFILIATIONS_UNAVAILABLE`, distinguiendo falta de integración de una relación inexistente. La aplicación puede arrancar con IAM, pero no concede permisos clínicos.

## Deiby y Fern: casos de uso

Recibir `AccountIdentity` desde el principal autenticado, nunca desde el body. Aplicar `PatientAccessPolicy` dentro del caso de uso, antes de cargar/devolver datos y antes de escribir. Para escrituras, incluir autorización y mutación en una transacción y coordinar con profiles el aislamiento/bloqueo necesario ante revocaciones concurrentes.

| Caso | Control |
| --- | --- |
| Paciente por ID | `requireRead(identity, patientId)` |
| Alerta/registro/intervención por ID | Resolver en almacenamiento `alert → patient`, `record → patient`, `intervention → alert → patient`; luego `requireRead` |
| Revisión/nota clínica | `requireClinicalWrite(identity, patientId)`; familiar recibe 403 |
| Listados y búsqueda | Obtener `scope(identity)` y aplicar `WHERE patient_id IN (...)` antes de paginar, contar o agregar; scope vacío devuelve colección vacía |
| Relaciones anidadas | Proyectar solo datos/contactos/patientIds permitidos, incluso en cuidadores compartidos |
| Catálogo global de rangos | Autenticación; si pasa a ser clínico por paciente, requiere vínculo y política |

El conjunto de IDs es el contrato inicial para el piloto. Si el volumen exige consultas por join/predicados, ampliar el puerto junto con profiles; nunca descargar todos los datos para filtrarlos después. No reutilizar resultados entre usuarios, ámbitos o cambios de afiliación.

Derivar autor, rol y fecha de auditoría del servidor. Rechazar propiedades de autor/ámbito aportadas por clientes. Fern debe unificar cambio de estado y nota en su transacción y resolver `IN_REVIEW` frente a `REVIEW`. Deiby debe definir origen de telemetría y autorización sin suplantación de dispositivos.

## Contratos Angular pendientes

Care utiliza seis GET: `/patients`, `/alerts`, `/records`, `/interventions`, `/caregivers`, `/measurementRanges`; además GET/PATCH `/alerts/{id}` y POST `/interventions`. Deben retornar solo datos autorizados. El backlog usa `/patients/{id}/history` y `/alerts/{id}/notes`; acordar adaptación/paginación antes de activar `careEnabled`.

La lógica de escritura de demostración sigue presente pero deshabilitada en el parche. No activarla contra datos reales: debe sustituirse por la operación atómica de Fern y eliminar IDs/autor/fecha del payload. Las políticas conservadoras descritas son la propuesta de AGENTS.md; el acuerdo clínico definitivo y los campos visibles a familiares siguen pendientes del equipo.

Para cerrar TS-11: ejecutar escenarios con dos clínicas, dos médicos, dos familiares y cuenta/perfil distintos sobre BD y endpoints reales; comprobar recursos indirectos, paginación/conteos, proyecciones anidadas, revocación y fuente caída. Las pruebas actuales cubren IAM real y el contrato de la política, no esos endpoints inexistentes.
