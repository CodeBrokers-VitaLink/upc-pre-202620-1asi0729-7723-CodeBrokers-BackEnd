# Parche IAM para Angular

Archivo: [frontend-iam.patch](frontend-iam.patch).

Base exacta: `Kirkcito/upc-pre-202620-1asi0729-7737-CodeBrokers-Frontend`, commit `a51b67c1d15cbf34c73d83d85e6c1c73675c4b20`. La rama remota consultada seguía en ese commit.

**No aplicado al frontend de trabajo del usuario ni publicado.** Se aplicaron y verificaron los cambios en una copia aislada bajo `.gradle/frontend-iam`, con Angular 22 y el lockfile original. Las carpetas encontradas en `D:/Webstorm/.../OpenSource-Original` y `OpenSource-Copia` usan Angular 21 y no tienen metadatos Git: no se asumió que fueran el repositorio solicitado ni se modificaron.

## Aplicación

Desde un checkout del frontend en el commit base, preservar primero cualquier cambio local y usar la ruta absoluta al parche:

```powershell
git status --short
git switch -c feature/iam-security
git apply --check '<backend>/docs/frontend-iam.patch'
git apply '<backend>/docs/frontend-iam.patch'
npm ci
npm test -- --watch=false
npm run build
npm start
```

No usar `git apply` a ciegas sobre una versión distinta. No hay push ni commits incluidos. El parche incorpora archivos nuevos y cambios completos de login/perfil/interceptor/guards/logout, EN/ES y pruebas.

## Comportamiento

- POST `/auth/login` y GET `/auth/me`; sin consulta `/users` ni comparación de contraseñas en navegador. Roles externos desconocidos se rechazan.
- El token corto y su expiración se guardan en `sessionStorage`; perfil/contexto solo en memoria. La sesión restaurada está pendiente hasta `/auth/me`; el guard espera esa respuesta. JSON local y un token decodificado no autorizan vistas.
- `TokenStorage` no depende de HttpClient ni del store. El interceptor compara origen y frontera exacta de `/api/v1`; excluye login, assets, i18n, fake API y terceros. `401` protegido limpia la sesión actual; `403` conserva la sesión. Un `401` de una petición antigua no invalida una cuenta recién iniciada.
- Logout/expiración invalidan la generación de sesión, cancelan peticiones, limpian perfil, CareStore, contexto y selección. Se elimina el antiguo `vitalink-patient`; la selección queda en memoria. Idioma y preferencias no sensibles se conservan.
- El contexto reactivo distingue cuenta, profesional, cuidador y clínica. No quedan IDs `DR-01`/`FC-01` de entorno ni comparación entre el ID de cuenta y el ID profesional.
- Los errores de red dejan la restauración sin validar y permiten reintentar `ensureValidated()` o iniciar sesión nuevamente.

`sessionStorage` es accesible a JavaScript: no protege ante XSS. El logout local no revoca una copia del JWT en el servidor; rige su expiración y la comprobación de cuenta activa.

## Care pendiente

Desarrollo: `authApiUrl = http://localhost:8080/api/v1`, `apiUrl` conserva la fake API existente pero **`careEnabled=false` impide cargar o escribir datos Care**. No se envía el JWT a la fake API. Producción apunta ambas URLs a `/api/v1` bajo el mismo origen, también con Care deshabilitado. Requiere proxy inverso para la API.

Al entrar al dashboard aparece el mensaje de datos de cuidado no disponibles. Se conserva la UI sin presentar una integración clínica completa. No activar Care hasta resolver los seis GET, escrituras atómicas, auditoría del servidor, paginación y permisos descritos en [module-integration.md](module-integration.md).

Las pruebas verifican la lógica Angular con HTTP simulado y la compilación de la copia. **No se verificó un flujo de navegador conectado al backend ni endpoints clínicos reales.** El parche no modifica versiones, lockfile ni budgets.
