# Contrato IAM v1

Base `/api/v1`. JSON UTF-8. IDs opacos de cuenta como strings UUID. El email se normaliza con trim y minúsculas; la contraseña se conserva exactamente. No hay registro, refresh, recuperación, OAuth ni MFA en este incremento.

## POST /auth/login

Público, sin enviar Authorization. Request:

```json
{"email":"doctor@example.test","password":"<provided-locally>"}
```

Email obligatorio, formato válido, hasta 254 caracteres después del trim. Contraseña obligatoria, hasta 72 caracteres; además se rechazan entradas mayores de 72 bytes UTF-8 antes de BCrypt. Campos adicionales son rechazados, incluidos `role`, `active` y `clinicId`.

Respuesta `200` (IDs ilustrativos):

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
    "careContext": {"providerId": null, "familyId": null, "clinicId": null}
  }
}
```

`400 VALIDATION_ERROR`: JSON/formato inválido; `401 INVALID_CREDENTIALS`: contraseña incorrecta, cuenta inexistente/inactiva, o contraseña fuera del límite BCrypt en bytes. La respuesta no revela si existe el email. La comparación usa un hash ficticio del mismo coste cuando la cuenta no existe.

## GET /auth/me

`Authorization: Bearer <accessToken>`. Devuelve directamente el objeto `user`. No acepta un ID de usuario para seleccionar la cuenta; deriva identidad del principal. Sin token válido: `401 UNAUTHENTICATED`. Nunca devuelve contraseña/hash. Login y perfil incluyen `Cache-Control: no-store`.

`role` solo puede ser `doctor` o `family`. La cuenta IAM, el profesional (`providerId`), el cuidador (`familyId`) y la clínica (`clinicId`) son identidades distintas. `hospital` queda nulo hasta disponer de información de perfiles. Actualmente el adaptador sin perfiles devuelve contexto vacío y no concede acceso clínico. Un adaptador real debe obtener contexto de relaciones persistidas y propagar indisponibilidad, nunca convertir errores en contexto vacío.

## Errores

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

| Código | HTTP | Significado |
| --- | --- | --- |
| INVALID_CREDENTIALS | 401 | Login rechazado |
| UNAUTHENTICATED | 401 | Autenticación ausente/inválida |
| ACCESS_DENIED | 403 | Operación no permitida para el rol |
| PATIENT_ACCESS_DENIED | 403 | Paciente fuera de relaciones autorizadas |
| VALIDATION_ERROR | 400 | Request inválido; `errors` puede contener `{field, message: "INVALID_VALUE"}` |
| EMAIL_ALREADY_EXISTS | 409 | Conflicto de aprovisionamiento interno, sin endpoint público |
| AFFILIATIONS_UNAVAILABLE | 503 | Falta o caída de fuente de autorización |
| SERVICE_UNAVAILABLE | 503 | Persistencia no disponible |
| NOT_FOUND | 404 | Recurso inexistente |

Mensajes por `Accept-Language`: español (`es`, `es-PE`, etc.) o inglés por defecto. Sin sesión HTTP; códigos y mensajes por campo no contienen los valores rechazados. Filtros Bearer y autorización usan el mismo formato que controllers.

## Seguridad

- JWT con Nimbus/Spring Security Resource Server: solo HS256, clave externa Base64 de 256 bits o más. Claims: `sub`, `role`, `iss`, `aud`, `iat`, `nbf`, `exp`; sin datos de salud ni credenciales. Se exige expiración, sujeto no vacío y rol conocido, además de firma, issuer y audiencia correctos. Sin tolerancia temporal adicional; sincronizar relojes de las instancias.
- TTL configurable de 60 a 3600 segundos, por defecto 900. La cuenta activa y su rol se consultan en cada petición; una desactivación o cambio de rol invalida el uso de tokens anteriores. Las afiliaciones se resuelven en cada operación clínica, sin caché compartida.
- BCrypt coste 12. Normalización y unicidad del email en servicio y BD. El bootstrap es local, explícito e idempotente; el aprovisionamiento no se expone por HTTP.
- Stateless, sin HTTP Basic, form login o cookies de autenticación. CSRF está deshabilitado porque solo se aceptan Bearer enviados explícitamente; reevaluar antes de incorporar cookies. CORS permite únicamente orígenes exactos configurados, sin credenciales de navegador.
- El logout del cliente borra el token y sus datos locales; no revoca inmediatamente una copia del JWT. Una copia puede funcionar hasta expirar si la cuenta sigue activa.
- No hay limitador distribuido de intentos en este incremento. Antes de despliegue, acordar cuotas y protección en el ingress/proxy, observabilidad sin tokens y administración/rotación de secretos. JWT no acredita certificación HIPAA/GDPR.

Referencias: [Resource Server JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html), [requisitos de Spring Boot](https://docs.spring.io/spring-boot/system-requirements.html), [interceptores Angular](https://angular.dev/guide/http/interceptors).
