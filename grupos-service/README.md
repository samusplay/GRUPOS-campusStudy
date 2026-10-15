# grupos-service — CampusStudy

Microservicio de **CampusStudy** que administra los **grupos de trabajo** que los estudiantes crean dentro de sus materias, y sus **integrantes**.

- **Puerto:** `8082`
- **Base de datos:** PostgreSQL 16 (`grupos_db`, propia del servicio)
- **Autenticación:** JWT emitido por `users-auth` (HS256, secreto compartido)
- **Contrato:** sección **J** del documento *Contrato Microservicios CampusStudy*
- **Rama de trabajo:** `develop`

> ¿Venías del código anterior (modo demo, H2, stubs)? Lee primero **[docs/CAMBIOS_REFACTOR.md](grupos-service/docs/CAMBIOS_REFACTOR.md)**: explica qué cambió y por qué.

---

## Integrantes

| Nombre | ID |
|--------|-----|
| Miguel Angel Beltran Rache | 1048977 |
| Alma Carina Hernandez Fernandez | 964358 |
| Samuel Orlando Caro Brahoma | — |

---

## Tabla de contenido

1. [Lugar en el sistema](#1-lugar-en-el-sistema)
2. [Stack](#2-stack)
3. [Arquitectura por capas](#3-arquitectura-por-capas)
4. [Estructura de carpetas](#4-estructura-de-carpetas)
5. [Modelo de datos](#5-modelo-de-datos)
6. [Configuración](#6-configuración)
7. [Cómo correrlo](#7-cómo-correrlo)
8. [Endpoints](#8-endpoints)
9. [Formato de errores](#9-formato-de-errores)
10. [Seguridad: JWT](#10-seguridad-jwt)
11. [Integración con materias-service](#11-integración-con-materias-service)
12. [Cómo agregar un endpoint nuevo](#12-cómo-agregar-un-endpoint-nuevo)
13. [Hoja de ruta](#13-hoja-de-ruta)
14. [Flujo de Git](#14-flujo-de-git)
15. [Problemas comunes](#15-problemas-comunes)

---

## 1. Lugar en el sistema

```
Frontend (Next.js :3000)
      │  Authorization: Bearer <JWT>
      ▼
API Gateway :8000 ──────────────► users-auth :8081          (login, emite el JWT)
      │
      ├─────────────────────────► materias-service :8006
      │
      └─────────────────────────► grupos-service :8082  ◄── ESTE SERVICIO
                                       │
                                       ├──► postgres-grupos :5432   (su propia base)
                                       └──► materias-service :8006  (valida materia_id con el token del usuario)
```

- Todo corre en la red de Docker del repo **`infra-campusstudy`**.
- Cada servicio tiene **su propia base**: grupos **nunca** se conecta a la base de materias ni a la de usuarios. Solo guarda los ids (`materia_id`, `usuario_id`) y, cuando necesita validar algo, **pregunta por HTTP**.

---

## 2. Stack

| Tecnología | Versión | Para qué |
|---|---|---|
| Java | 17 | Lenguaje |
| Spring Boot | 3.3.4 | Web, Validation, Data JPA, DevTools |
| PostgreSQL | 16 | Base de datos del servicio |
| Lombok | (gestionada por Spring Boot) | Constructores, getters/setters, builders, logs |
| JJWT | 0.12.6 | Validar la firma del JWT y leer el `sub` |
| `RestClient` | Spring 6.1 | Llamadas HTTP a materias-service |

---

## 3. Arquitectura por capas

Cada capa **firma** su contrato con una **interfaz** y lo implementa en `impl/`.

```
Petición HTTP
   │
   ▼
JwtAuthFilter              ← valida el token. Si falla → 401. Si pasa, guarda usuarioId en la request
   │
   ▼
GrupoController (interfaz) ← rutas, verbos, @Valid
GrupoControllerImpl        ← solo delega. Recibe el usuario con @UsuarioAutenticado
   │
   ▼
GrupoService (interfaz)    ← casos de uso
GrupoServiceImpl           ← reglas de negocio, @Transactional
   │            │
   │            └──► MateriasClient (interfaz) → MateriasClientImpl → HTTP a materias-service
   ▼
GrupoRepository            ← JPA + consultas personalizadas
   │
   ▼
PostgreSQL
```

### Reglas del proyecto

| Regla | Por qué |
|---|---|
| **Controller, service y client = interfaz + `impl`** | La interfaz es la firma: qué hace. El `impl` dice cómo. Quien consume depende de la interfaz |
| **Inyección por constructor** con `@RequiredArgsConstructor` y campos `final` | Nada de `@Autowired` en campos |
| **DTOs como `record`** | Inmutables y sin código repetido |
| **Entidades con `@Getter @Setter`, no `@Data`** | `@Data` en entidades JPA con relaciones provoca bucles infinitos en `toString`/`hashCode` y consultas escondidas |
| **El controller nunca toca entidades** | Recibe y devuelve DTOs. Convierte el `GrupoMapper` |
| **Errores = excepciones** | El service lanza `NotFoundException`, `ConflictException`... y el `GlobalExceptionHandler` las traduce a HTTP |
| **El `usuario_id` sale del token**, nunca del body ni de la URL | Así nadie puede actuar en nombre de otro |

---

## 4. Estructura de carpetas

```
grupos-service/src/main/java/com/campusstudy/grupos/
├── client/
│   ├── MateriasClient.java              # firma: ¿la materia es del usuario?
│   ├── impl/MateriasClientImpl.java     # GET /api/v1/materias con RestClient
│   └── dto/MateriaResumen.java          # lo que leemos de la respuesta de materias
├── config/
│   ├── RestClientConfig.java            # RestClient: URL, timeouts, reenvío del token
│   └── WebConfig.java                   # registra el resolver de @UsuarioAutenticado
├── controller/
│   ├── GrupoController.java             # firma: rutas, @Valid, @PathVariable
│   └── impl/GrupoControllerImpl.java    # delega al service
├── dto/
│   ├── request/CrearGrupoRequest.java
│   └── response/GrupoResponse.java, IntegranteResponse.java, ErrorResponse.java
├── exception/
│   ├── NotFoundException.java           # 404
│   ├── ConflictException.java           # 409
│   ├── BadRequestException.java         # 400 sobre un campo
│   ├── ServiceUnavailableException.java # 503
│   └── GlobalExceptionHandler.java      # excepción → formato del contrato
├── mapper/GrupoMapper.java              # entidad ↔ DTO
├── model/
│   ├── Grupo.java
│   ├── Integrante.java
│   └── RolIntegrante.java               # LIDER / MIEMBRO → "lider" / "miembro" en JSON
├── repository/GrupoRepository.java
├── security/
│   ├── JwtService.java                  # valida firma/expiración, extrae el sub
│   ├── JwtAuthFilter.java               # filtro HTTP: 401 si no hay token válido
│   ├── UsuarioAutenticado.java          # anotación para el controller
│   └── UsuarioAutenticadoResolver.java  # inyecta el usuario_id en el parámetro anotado
├── service/
│   ├── GrupoService.java
│   └── impl/GrupoServiceImpl.java
└── GruposServiceApplication.java
```

---

## 5. Modelo de datos

```
grupos                                         integrantes
──────────────────────────────                 ──────────────────────────────
id              bigint  PK (identity)    ┌──── grupo_id     bigint  FK → grupos.id
nombre          varchar(100)             │     id           bigint  PK (identity)
materia_id      bigint   (sin FK)        │     usuario_id   bigint   (sin FK)
creado_por      bigint   (sin FK)        │     rol          varchar(20)  LIDER | MIEMBRO
fecha_creacion  timestamp                │     unido_en     timestamp
                 1 ──────────────────── N┘
```

| Restricción / índice | Columnas | Regla |
|---|---|---|
| `uk_grupo_nombre_materia` (UNIQUE) | `nombre, materia_id` | No hay dos grupos con el mismo nombre **en la misma materia** |
| `uk_integrante_grupo_usuario` (UNIQUE) | `grupo_id, usuario_id` | Nadie está dos veces en el mismo grupo |
| `idx_integrante_usuario` (INDEX) | `usuario_id` | "Mis grupos" busca por usuario |
| `idx_grupo_materia` (INDEX) | `materia_id` | Búsquedas por materia |

- `materia_id`, `creado_por` y `usuario_id` **no tienen FK**: apuntan a datos de **otros** servicios.
- `integrantes.grupo_id` **sí** tiene FK: está en la misma base.
- El **creador** entra automáticamente como `LIDER`. Los invitados entran como `MIEMBRO`.
- `creado_por` es historia (quién lo fundó) y nunca cambia. El `LIDER` es el rol actual.
- Fechas en **hora de Colombia** (`TZ=America/Bogota`), sin milisegundos.
- Las tablas las crea Hibernate (`ddl-auto: update`) al arrancar.

---

## 6. Configuración

`src/main/resources/application.yml` lee todo lo que cambia entre ambientes de **variables de entorno**, con un valor por defecto para correr local:

| Propiedad | Variable de entorno | Por defecto (local) | En Docker (compose de infra) |
|---|---|---|---|
| `server.port` | `SERVER_PORT` | `8082` | `8082` |
| `spring.datasource.url` | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5433/grupos_db` | `jdbc:postgresql://postgres-grupos:5432/grupos_db` |
| `spring.datasource.username` | `SPRING_DATASOURCE_USERNAME` | `campus` | `${GRUPOS_DB_USER}` |
| `spring.datasource.password` | `SPRING_DATASOURCE_PASSWORD` | `campus123` | `${GRUPOS_DB_PASSWORD}` |
| `app.jwt.secret` | `APP_JWT_SECRET` | **sin valor: obligatorio** | `${JWT_SECRET}` |
| `app.clients.materias-url` | `MATERIAS_URL` | `http://localhost:8006` | `http://materias-service:8006` |
| `app.clients.timeout-ms` | — | `3000` | `3000` |

- `spring.jackson.property-naming-strategy: SNAKE_CASE` → **todo** el JSON sale y entra en snake_case (`grupoId` ⇄ `grupo_id`), sin anotar cada DTO.
- `APP_JWT_SECRET` **no tiene valor por defecto a propósito**: si falta, el servicio no arranca. Así nunca queda un secreto escrito en el repo.
- **Dentro de Docker, `localhost` es el propio contenedor.** Entre contenedores se usa el nombre del servicio (`postgres-grupos`, `materias-service`) y el puerto **interno**.

---

## 7. Cómo correrlo

### Con todo el sistema (recomendado)

El servicio se levanta desde el repo **`infra-campusstudy`**, junto con auth, materias y el gateway. Ahí está `postgres-grupos` y las variables del `.env`.

Estructura esperada de carpetas:
```
campusstudy-backend/
├── infra-campusstudy/          ← docker-compose.yml + .env
├── Users_Auth/
├── MateriasService/
├── Api-Gateway-CampusStudy/
└── GRUPOS-campusStudy/         ← este repo
    └── grupos-service/
```

Variables que deben estar en `infra-campusstudy/.env`:
```env
GRUPOS_DB_NAME=grupos_db
GRUPOS_DB_USER=campus
GRUPOS_DB_PASSWORD=campus123
JWT_SECRET=<el mismo secreto que usa users-auth>
```

Comandos (desde `infra-campusstudy`):

| Acción | Comando |
|---|---|
| Levantar todo | `docker compose up -d` |
| Levantar solo grupos (y su base) | `docker compose up -d --build grupos-service` |
| Ver logs | `docker compose logs -f grupos-service` |
| Cambiaste `pom.xml` o `Dockerfile.dev` | `docker compose up -d --build grupos-service` |
| Cambiaste `environment:` en el compose | `docker compose up -d` (recrea solo lo que cambió) |
| Solo la base | `docker compose up -d postgres-grupos` |

> El código está montado como volumen y corre con DevTools: al recompilar (`Ctrl + F9` en IntelliJ), el servicio se reinicia solo dentro del contenedor.

> Si ves clases viejas o comportamientos raros tras un refactor grande: borra `target/` y reinicia el contenedor.

> 🚫 **No uses `docker compose down -v`**: borra **todos** los volúmenes del sistema, incluidas las bases de auth y materias.

### Conectarse a la base (DBeaver / pgAdmin)

| Campo | Valor |
|---|---|
| Host | `localhost` |
| Puerto | **`5433`** (el 5432 es de auth) |
| Base | `grupos_db` |
| Usuario / contraseña | `campus` / `campus123` |

### Archivos de Docker del repo

| Archivo | Uso |
|---|---|
| `Dockerfile.dev` | **Desarrollo** (lo usa el compose de infra): `mvn spring-boot:run` con el código montado |
| `Dockerfile` | Imagen de **producción** (multi-stage → `.jar`). No se usa en desarrollo |

---

## 8. Endpoints

Todos requieren `Authorization: Bearer <token>`. El token se obtiene con `POST /api/v1/auth/login` **en el gateway**.

| Método | Ruta | Estado | Respuestas |
|---|---|---|---|
| `POST` | `/api/v1/grupos` | ✅ | 201 · 400 · 401 · 409 · 503 |
| `GET` | `/api/v1/grupos` | ✅ | 200 · 401 |
| `GET` | `/api/v1/grupos/{grupo_id}` | ✅ | 200 · 400 · 401 · 404 |
| `POST` | `/api/v1/grupos/{grupo_id}/integrantes` | ⏳ siguiente | — |
| `GET` | `/api/v1/grupos/resumen` | ⏳ | Pendiente: definir con estadísticas qué es un grupo "activo" |
| — | Documentos Word/Excel | 💡 propuesta | Ver [Hoja de ruta](#13-hoja-de-ruta) |

### POST `/api/v1/grupos`

```json
// Request
{ "nombre": "Equipo A", "materia_id": 1 }
```
```json
// 201 Created   +   Location: /api/v1/grupos/1
{
  "grupo_id": 1,
  "nombre": "Equipo A",
  "materia_id": 1,
  "creado_por": 5,
  "integrantes": [
    { "usuario_id": 5, "rol": "lider", "unido_en": "2026-10-14T22:05:12" }
  ],
  "fecha_creacion": "2026-10-14T22:05:12"
}
```

Reglas, en orden:
1. `nombre` obligatorio (máx. 100) y `materia_id` entero positivo → si no, **400**.
2. La materia debe **existir y ser del usuario**: se pregunta a materias-service → si no, **400** en `materia_id`. Si materias no responde → **503**.
3. No puede existir otro grupo con el mismo nombre (sin importar mayúsculas) **en la misma materia** → **409**.
4. El creador queda como `lider`.

### GET `/api/v1/grupos`

Arreglo con los grupos donde el usuario es **integrante** (líder o miembro), del más reciente al más antiguo. Cada grupo trae **todos** sus integrantes. Sin grupos → `200 []`.

### GET `/api/v1/grupos/{grupo_id}`

El grupo con todos sus integrantes. **404** si el grupo no existe **o si el usuario no pertenece a él**: es la misma respuesta a propósito, para no revelar que el grupo existe.

### Probar en Postman (siempre por el gateway)

1. Environment con `gateway = http://localhost:8000` y `token` vacío.
2. `POST {{gateway}}/api/v1/auth/login` con un script **Post-response**:
   ```javascript
   if (pm.response.code === 200) {
       pm.environment.set("token", pm.response.json().token);
   }
   ```
3. Colección → **Authorization** → `Bearer Token` → `{{token}}` (los requests heredan).
4. Crea una materia primero (`POST {{gateway}}/api/v1/materias`) y usa su `materia_id` para crear el grupo.

| Prueba | Esperado |
|---|---|
| Crear con materia propia | 201 |
| Mismo nombre, misma materia | 409 |
| Mismo nombre, otra materia propia | 201 |
| `materia_id: 999` (no existe / no es tuya) | 400 en `materia_id` |
| `{ "nombre": "", "materia_id": -1 }` | 400 con 2 errores |
| `GET /grupos/999` | 404 |
| Sin token | 401 |
| `docker compose stop materias-service` y crear | 503 |

---

## 9. Formato de errores

```json
{ "error": "Datos de entrada inválidos",
  "errors": [ { "campo": "materia_id", "mensaje": "La materia no existe o no te pertenece" } ] }
```
```json
{ "error": "El grupo no existe" }
```

| Situación | Código |
|---|---|
| `@Valid` falló, JSON mal formado, `grupo_id` no numérico, materia inválida | 400 |
| Sin token, token inválido o expirado | 401 |
| Grupo no existe o no perteneces | 404 |
| Nombre repetido en la materia (o choque con un UNIQUE de la base) | 409 |
| Error no controlado (el detalle va **solo** al log) | 500 |
| materias-service caído, lento (> 3 s) o respondió con error | 503 |

`errors[].campo` sale en **snake_case** (`materia_id`), igual que lo envió el cliente.

---

## 10. Seguridad: JWT

```
Authorization: Bearer eyJ...
   │
JwtAuthFilter ── ¿header Bearer? ── no ──► 401 { "error": "No autorizado" }
   │
JwtService ── valida firma (HS256, APP_JWT_SECRET) y expiración ── falla ──► 401
   │           └─ sub = "5" → 5L
   ▼
request.setAttribute("usuarioId", 5)
   │
UsuarioAutenticadoResolver ──► controller: crear(@UsuarioAutenticado Long usuarioId, ...)
```

- El token lo **firma users-auth**. Grupos solo lo **valida** con el **mismo** secreto (`JWT_SECRET` del `.env` de infra).
- El `usuario_id` sale del claim **`sub`**.
- `@UsuarioAutenticado` es una anotación propia. Spring la conoce porque `WebConfig` registra su resolver.

---

## 11. Integración con materias-service

Al crear un grupo, grupos llama a `GET /api/v1/materias` **con el token del usuario** y revisa si `materia_id` está en la lista. Como materias filtra por el `sub` del token, una sola llamada confirma que la materia **existe** y que **es del usuario**.

- **Token relay**: `RestClientConfig` tiene un interceptor que copia el `Authorization` de la petición entrante a la petición hacia materias. `MateriasClient` no maneja tokens.
- **Timeout de 3 s** (conexión y lectura).
- **Errores**: no conecta, timeout o 4xx/5xx de materias → `ServiceUnavailableException` → **503**.
- `MateriaResumen` es un `record` propio de grupos con **solo** los campos que usa (`materia_id`, `nombre`). Los microservicios **no comparten código**: solo el contrato JSON.

> Cuando materias-service implemente `GET /api/v1/materias/{materia_id}`, se puede cambiar `MateriasClientImpl` para consultar solo esa materia. La interfaz no cambia.

---

## 12. Cómo agregar un endpoint nuevo

Ejemplo: `POST /api/v1/grupos/{grupo_id}/integrantes`.

1. **DTO** en `dto/request` (record con validaciones) y, si hace falta, en `dto/response`.
2. **Repository**: método derivado o `@Query` (si necesitas joins, subqueries o controlar el número de consultas).
3. **Service**: agregar la firma en `GrupoService` → implementar en `GrupoServiceImpl`.
4. **Mapper**: conversión entidad → DTO, si es nueva.
5. **Controller**: firma con la ruta en `GrupoController` → delegar en `GrupoControllerImpl`.
6. **Gateway**: agregar la ruta en `routers/grupos.py` del API Gateway (usa `Token` de `core/dependencies.py`).
7. **Postman**: probar por el gateway.

### Checklist

- [ ] El usuario sale de `@UsuarioAutenticado`, **nunca** del body.
- [ ] Toda consulta valida que el usuario **pertenece** al grupo (si no → 404).
- [ ] Reglas de negocio en el service; el controller solo delega.
- [ ] Errores con excepciones (`NotFoundException`, `ConflictException`, `BadRequestException`).
- [ ] `@Transactional` en los métodos que escriben (la clase es `readOnly = true` por defecto).
- [ ] Consultas que devuelven grupos con integrantes usan `JOIN FETCH` (evita N+1 y `LazyInitializationException`).
- [ ] Respuestas en snake_case y fechas `YYYY-MM-DDTHH:MM:SS`.

---

## 13. Hoja de ruta

| Fase | Qué | Notas |
|---|---|---|
| **0** | `POST /grupos/{id}/integrantes` | Solo el **líder** invita. Invitado → `MIEMBRO`. Repetido → 409 |
| **1** | Documentos en **Azure Blob Storage** | Patrón *Valet Key*: grupos entrega una URL SAS temporal y el front sube directo a Blob. En desarrollo con **Azurite** (emulador en Docker) |
| **2** | **Azure Function** (Blob Trigger) | Al subir un archivo: valida tipo/tamaño → documento `PENDIENTE → DISPONIBLE` → deja un mensaje en una **cola** para notificaciones (formato del contrato K) |
| **3** | Despliegue en Azure real | Storage Account + Function App |
| — | `GET /grupos/resumen` | Cuando estadísticas defina qué es un grupo "activo" |
| — | Calendario de tareas en el detalle | Cuando exista tareas-service (ver `docs/CONTRATO_TAREAS_GRUPOS.md`) |

> Documentos **no está en el contrato J** y cambia el alcance de la Entrega 2 ("solo URLs"). Se debe acordar con el equipo y con el front como **contrato v1.1** antes de implementarlo.

---

## 14. Flujo de Git

| Rama | Uso |
|---|---|
| `main` | Estable |
| `develop` | Integración: **aquí está la arquitectura nueva** |
| `feature/<tarea>` | Una rama por tarea, creada **desde `develop`** |

```bash
git checkout develop
git pull
git checkout -b feature/grupos-integrantes
# ... trabajo ...
git push -u origin feature/grupos-integrantes
# Pull Request → develop
```

> No sigan desarrollando sobre `main` con la estructura anterior: el merge con `develop` va a chocar.
> No se suben `target/`, `.idea/` ni `.env` (están en `.gitignore`).

---

## 15. Problemas comunes

| Síntoma | Causa | Solución |
|---|---|---|
| **401** con un token recién sacado | `APP_JWT_SECRET` distinto al de users-auth | Revisar `JWT_SECRET` en `infra-campusstudy/.env` |
| No arranca: `Could not resolve placeholder 'app.jwt.secret'` | Falta `APP_JWT_SECRET` | Agregarla al compose / variables de entorno |
| **503** al crear grupo con materias arriba | `MATERIAS_URL` apunta a `localhost` dentro de Docker | Debe ser `http://materias-service:8006` |
| **400** "La materia no existe o no te pertenece" con una materia que sí es tuya | El token es de otro usuario, o la materia es de otra cuenta | Revisar en materias quién es el dueño (`UsuarioId`) |
| `Connection refused` a la base desde DBeaver | Puerto equivocado | Es **5433**, no 5432 |
| `HikariPool ... Connection refused` en los logs | `postgres-grupos` no está healthy o URL mal | `docker compose ps postgres-grupos` |
| Arranca con clases que ya borraste | `target/` con compilados viejos | Borrar `target/` y reiniciar el contenedor |
| `No qualifying bean of type 'GrupoService'` | Falta `@Service` en el `impl` | Anotar la implementación |
| `LazyInitializationException` | Se accede a `integrantes` fuera de la transacción | Mapear dentro del service y usar consultas con `JOIN FETCH` |
| `StackOverflowError` al imprimir una entidad | `@Data`/`@ToString` en entidades con relación bidireccional | Usar `@Getter @Setter` |
| WARN `constraint "uk_..." does not exist, skipping` al primer arranque | Hibernate intenta borrar el constraint antes de crearlo | Inofensivo |