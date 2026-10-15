# Cambios del refactor de grupos-service

Este documento explica **qué cambió** respecto al código anterior de `main` y **por qué**, para que todo el equipo entienda la nueva estructura antes de seguir con los siguientes endpoints.

> **Lo que estaba bien y se mantuvo como idea:** la separación por capas, el uso de clientes HTTP con timeout para hablar con otros servicios, las validaciones con `@Valid`, el `GlobalExceptionHandler`, la tabla `integrantes` con el rol `lider`/`miembro`, el documento de contrato con tareas (`CONTRATO_TAREAS_GRUPOS.md`) y la documentación de la Entrega 2. El refactor **parte de esa base**: no la descarta.

---

## Resumen en una tabla

| Tema | Antes (`main`) | Ahora (`develop`) | Por qué |
|---|---|---|---|
| **Autenticación** | "Modo demo": `Authorization: Bearer 692464`, el token **era** el número del usuario | **JWT real** firmado por users-auth y validado con el mismo secreto | Con el modo demo, cualquiera podía hacerse pasar por cualquier usuario cambiando un número |
| **Base de datos** | H2 en memoria (local) / Postgres en un compose propio | **PostgreSQL** (`postgres-grupos`) dentro del compose **global** de `infra-campusstudy` | Todos los servicios en la misma red de Docker. H2 se borra al reiniciar |
| **Otros servicios** | **Stubs**: materias, usuarios, tareas y notificaciones **simulados** (`stubs.enabled: true`) | Llamada **real** a materias-service. Lo que aún no existe no se simula | Con stubs nunca se probó la integración real. El puerto de materias además era `8083` y el real es `8006` |
| **Validar la materia** | `GET /materias/{id}` (stub) → 422 si no existe | `GET /materias` **con el token del usuario** → 400 si no existe **o no es suya** | Las materias son **personales**: no basta con que exista, tiene que ser del usuario |
| **Estructura** | Clases concretas | **Interfaz + `impl`** en controller, service y client | Cada capa expone su firma; quien consume depende de la interfaz |
| **Boilerplate** | Getters, setters y constructores escritos a mano | **Lombok** (`@RequiredArgsConstructor`, `@Getter`, `@Setter`, `@Builder`, `@Slf4j`) | Menos código y menos errores |
| **DTOs** | 12 clases, con campos tipo `getMateria_id()` | **3 records**: `CrearGrupoRequest`, `GrupoResponse`, `IntegranteResponse` | El contrato usa la misma forma de grupo para crear, listar y detalle |
| **snake_case** | Forzado con nombres de campo Java (`materia_id`) | **Global**: `spring.jackson.property-naming-strategy: SNAKE_CASE` | Java en camelCase, JSON en snake_case, sin ensuciar el código |
| **Formato de error** | `{ status, error, message, path, details }` | `{ error, errors: [{ campo, mensaje }] }` | Es el formato del contrato, igual en todos los servicios |
| **Detalle de grupo** | Devolvía **cualquier** grupo a **cualquier** usuario | **404** si no existe **o si no perteneces** | Fuga de información: cualquiera veía los grupos de otros |
| **Resumen** | `GET /resumen?usuario_id=X` sin verificar que X sea el usuario del token | Pendiente de rehacer | Cualquiera podía ver el resumen de otro usuario |
| **Nombre duplicado** | 400 | **409 Conflict** | 409 es el código correcto para "ya existe" |
| **Fechas** | `LocalDateTime.now()` con milisegundos, hora del servidor | Hora de **Colombia** (`TZ=America/Bogota`), **sin milisegundos** | Lo pide el contrato: `YYYY-MM-DDTHH:MM:SS` |
| **`target/`** | Subido al repo | En `.gitignore` y fuera del repo | Son archivos compilados; se generan solos |

---

## 1. Autenticación: del "modo demo" a JWT real

**Antes**
```
Authorization: Bearer 692464     ← el "token" es el usuario_id en texto plano
```
`AuthFilter` hacía `Long.parseLong(token)`. Si `demo-mode` estaba en `false`, **rechazaba todo**: no había forma de validar un token real.

**Ahora**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI1Ii...   ← JWT firmado por users-auth
```
- `JwtService` valida la **firma** (HS256 con el mismo `JWT_SECRET` que users-auth) y la **expiración**, y lee el claim **`sub`** → `usuario_id`.
- `JwtAuthFilter` responde **401** si no hay token o no es válido.
- El controller recibe el usuario con una anotación propia:
  ```java
  ResponseEntity<GrupoResponse> crear(@UsuarioAutenticado Long usuarioId, @Valid @RequestBody CrearGrupoRequest request);
  ```
  En vez de `(Long) httpRequest.getAttribute(...)` en cada método.

**Para probar:** ya no se inventa un número. Se hace login en el gateway (`POST /api/v1/auth/login`) y se usa el token que devuelve.

---

## 2. Infraestructura: un solo compose para todo el sistema

**Antes:** `grupos-service/docker-compose.yml` levantaba grupos y su base **aislados**, con Postgres en el puerto **5432**, que choca con la base de users-auth.

**Ahora:** todo se levanta desde el repo **`infra-campusstudy`**:

```
campusstudy (red de Docker)
├── postgres-auth       :5432  → users-auth        :8081
├── sqlserver-materias  :1433  → materias-service  :8006
├── postgres-grupos     :5433* → grupos-service    :8082   ← nuevo
└── api-gateway         :8000
* 5433 en tu máquina, 5432 dentro de la red
```

- `grupos-service` encuentra a su base como `postgres-grupos` y a materias como `materias-service`, por **nombre de servicio**. Dentro de un contenedor, `localhost` es el propio contenedor.
- Las credenciales salen del `.env` de infra (`GRUPOS_DB_*`, `JWT_SECRET`).
- `Dockerfile.dev` corre con DevTools: los cambios de código se recargan sin reconstruir la imagen.
- El `Dockerfile` multi-stage original **se conserva** para producción.
- El `docker-compose.yml` propio de grupos **ya no se usa**: se recomienda borrarlo para evitar confusiones y choques de puertos.

---

## 3. Sin stubs: integración real con materias

**Antes:** con `stubs.enabled: true`, `MateriasClient` **inventaba** respuestas (12 = "Sistemas Distribuidos", 999 = no existe, 998 = caído). Nunca se llamó a materias-service de verdad.

**Ahora:**
```
grupos ──GET /api/v1/materias (Bearer del usuario)──► materias-service
       ◄── [{ "materia_id": 1, ... }, { "materia_id": 2, ... }]
¿materia_id está en la lista? → sí: crea el grupo · no: 400 · materias caído: 503
```

- Las materias son **personales** (cada estudiante crea las suyas). Por eso no basta con que la materia exista: debe ser **del usuario**. Como materias filtra por el `sub` del token, una sola llamada valida las dos cosas.
- `RestClientConfig` tiene un interceptor que **reenvía el token** del usuario automáticamente (*token relay*).
- Usuarios, tareas y notificaciones **no se simulan**: se integran cuando existan. Mientras tanto, grupos no depende de ellos para crear, listar ni consultar.

---

## 4. Estructura: interfaces, Lombok y menos DTOs

**Antes**
```
controller/GrupoController.java              (clase)
service/GrupoService.java                    (clase con 8 dependencias)
service/GrupoMapper.java
dto/ (12 clases)
client/ (4 clientes + RetrySupport, con stubs)
```

**Ahora**
```
controller/GrupoController.java              ← interfaz (rutas)
controller/impl/GrupoControllerImpl.java     ← delega
service/GrupoService.java                    ← interfaz (casos de uso)
service/impl/GrupoServiceImpl.java           ← reglas de negocio
client/MateriasClient.java                   ← interfaz
client/impl/MateriasClientImpl.java          ← HTTP real
mapper/GrupoMapper.java                      ← paquete propio
dto/request + dto/response                   ← 3 records + ErrorResponse
security/                                    ← todo lo de JWT
```

Ejemplo de la diferencia en una entidad:

```java
// Antes: ~80 líneas de getters/setters a mano
public Long getId() { return id; }
public void setId(Long id) { this.id = id; }
// ...

// Ahora
@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Grupo { /* solo los campos */ }
```

> En entidades **no se usa `@Data`**: genera `toString`/`hashCode` con la relación `Grupo ↔ Integrante` y provoca bucles infinitos y consultas escondidas.

---

## 5. Seguridad de los datos: "solo veo lo mío"

| Endpoint | Antes | Ahora |
|---|---|---|
| `GET /grupos/{id}` | Cualquier usuario veía cualquier grupo | Solo si eres integrante; si no → **404** |
| `GET /grupos` | Ya filtraba por integrante ✅ | Igual, pero trae **todos** los integrantes de cada grupo en **una sola consulta** (`JOIN FETCH`) |
| `GET /grupos/resumen?usuario_id=X` | X lo ponía el cliente | Se rehará tomando el usuario **del token** |

Se responde **404 y no 403** cuando no perteneces, para no revelar que el grupo existe.

---

## 6. Punto para decidir en equipo: ¿nombre único global o por materia?

En la última versión de `main` el nombre del grupo pasó a ser **único en toda la plataforma** (`existsByNombreIgnoreCase` + `UNIQUE(nombre)`), y la colección de Postman lo prueba.

En `develop` es **único por materia** (`UNIQUE(nombre, materia_id)`), que además es lo que dice la propia Entrega 2 (sección 4.5: *"nombre único por materia"*).

| | Único global | Único por materia (actual en `develop`) |
|---|---|---|
| Dos estudiantes, cada uno con un grupo "Equipo A" | ❌ El segundo recibe 409 | ✅ Permitido |
| Revela información de otros | ⚠️ El 409 le dice a un estudiante que **otro** ya usa ese nombre | ✅ No |
| Evita confusión en la lista de un estudiante | ✅ | ✅ (no puede haber dos "Equipo A" en la misma materia) |

**Propuesta:** mantener **único por materia**. Si el equipo tenía una razón para el global, la discutimos y se ajusta: es un cambio de una línea en el repository y en la entidad.

---

## 7. Lo que queda pendiente (y cómo seguir)

| Endpoint / tema | Estado | Comentario |
|---|---|---|
| `POST /grupos/{id}/integrantes` | Siguiente | Solo el líder invita; el invitado entra como `MIEMBRO` |
| `GET /grupos/resumen` | Pendiente | Definir con estadísticas qué es un grupo "activo" |
| Documentos | **Propuesta** | Pasar de "solo URLs" a archivos en **Azure Blob Storage** + **Azure Function**. Cambia el alcance de la Entrega 2, así que hay que acordarlo como **contrato v1.1** |
| Zoom/Jam | Fuera por ahora | No están en el contrato J |
| Calendario (tareas) | Pendiente | Cuando exista tareas-service; el `CONTRATO_TAREAS_GRUPOS.md` sigue siendo la referencia |
| Tests | Pendiente | `GrupoControllerTest` probaba el código anterior (modo demo y stubs); hay que reescribirlos |

---

## 8. Cómo trabajar a partir de ahora

1. **Ramas desde `develop`**, no desde `main`:
   ```bash
   git checkout develop && git pull
   git checkout -b feature/grupos-integrantes
   ```
2. **No seguir desarrollando en `main`** con la estructura anterior: los cambios no se van a poder mezclar limpiamente.
3. Levantar el sistema completo desde `infra-campusstudy` y probar **por el gateway** con un token real.
4. Seguir el checklist de la sección *"Cómo agregar un endpoint nuevo"* del README.