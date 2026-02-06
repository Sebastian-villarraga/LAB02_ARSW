# Snake Race — ARSW Lab #2 (Java 21, Virtual Threads)

**Escuela Colombiana de Ingeniería – Arquitecturas de Software**  
Laboratorio de programación concurrente: condiciones de carrera, sincronización y colecciones seguras.

##Entregado por:
- Sebastian Julian Villarraga Guerrero
---

## Requisitos

- **JDK 21** (Temurin recomendado)
- **Maven 3.9+**
- SO: Windows, macOS o Linux

---


## Reglas del juego (resumen)

- **N serpientes** corren de forma autónoma (cada una en su propio hilo).
- **Ratones**: al comer uno, la serpiente **crece** y aparece un **nuevo obstáculo**.
- **Obstáculos**: si la cabeza entra en un obstáculo hay **rebote**.
- **Teletransportadores** (flechas rojas): entrar por uno te **saca por su par**.
- **Rayos (Turbo)**: al pisarlos, la serpiente obtiene **velocidad aumentada** temporal.
- Movimiento con **wrap-around** (el tablero “se repite” en los bordes).

---

## Arquitectura (carpetas)

```
co.eci.snake
├─ app/                 # Bootstrap de la aplicación (Main)
├─ core/                # Dominio: Board, Snake, Direction, Position
├─ core/engine/         # GameClock (ticks, Pausa/Reanudar)
├─ concurrency/         # SnakeRunner (lógica por serpiente con virtual threads)
└─ ui/legacy/           # UI estilo legado (Swing) con grilla y botón Action
```

---

# Actividades del laboratorio

## Parte I — (Calentamiento) `wait/notify` en un programa multi-hilo

1. Toma el programa [**PrimeFinder**](https://github.com/ARSW-ECI/wait-notify-excercise).
2. Modifícalo para que **cada _t_ milisegundos**:
   - Se **pausen** todos los hilos trabajadores.
   - Se **muestre** cuántos números primos se han encontrado.
   - El programa **espere ENTER** para **reanudar**.
3. La sincronización debe usar **`synchronized`**, **`wait()`**, **`notify()` / `notifyAll()`** sobre el **mismo monitor** (sin _busy-waiting_).
4. Entrega en el reporte de laboratorio **las observaciones y/o comentarios** explicando tu diseño de sincronización (qué lock, qué condición, cómo evitas _lost wakeups_).

Utilice un único monitor 'Control.class' compartido por todas las hebras.
La condición de sincronización es una variable booleana 'paused', que indica si el sistema debe detener el cálculo.
Cada hebra trabajadora verifica esta condición dentro de un bloque synchronized y, si está activa, se suspende mediante wait().
El hilo controlador pausa la ejecución cada T milisegundos, luego calcula el total de primos encontrados y espera la entrada del usuario.
Para evitar lost wakeups, la espera se realiza dentro de un ciclo , y la reanudación se hace usando notifyAll().

---

## Parte II — SnakeRace concurrente (núcleo del laboratorio)

### 1) Análisis de concurrencia

- Explica **cómo** el código usa hilos para dar autonomía a cada serpiente.

Cada Snake es controlada por un SnakeRunner, ejecutado en su propio hilo (virtual thread).
Todas las serpientes comparten:
el mismo Board
la UI (lecturas desde Swing)
El GameClock dispara repaints periódicos (hilo distinto).
Esto da autonomía real a cada serpiente, pero introduce estado compartido.
  
- **Identifica** y documenta en **`el reporte de laboratorio`**:
  - Posibles **condiciones de carrera**.

Board.step(Snake) modifica:
mice, obstacles, turbo, teleports
Múltiples SnakeRunner llaman step() en paralelo.
Riesgo: dos serpientes pisan el mismo mouse.
Mitigación parcial existente: step() ya es synchronized → bien.

  - **Colecciones** o estructuras **no seguras** en contexto concurrente.

| Estructura                         | Problema                            |
| ---------------------------------- | ----------------------------------- |
| `Deque<Position> body` en `Snake`  | Modificada por runner, leída por UI |
| `List<Snake> snakes` en `SnakeApp` | Leída por UI, runners activos       |
| `snapshot()`                       | Copia sin sincronización            |

  
  - Ocurrencias de **espera activa** (busy-wait) o de sincronización innecesaria.

NO hay busy-wait.
Pausa solo afecta UI 'GameClock', no detiene runners.

### 2) Correcciones mínimas y regiones críticas

- **Elimina** esperas activas reemplazándolas por **señales** / **estados** o mecanismos de la librería de concurrencia.
- Protege **solo** las **regiones críticas estrictamente necesarias** (evita bloqueos amplios).
- Justifica en **`el reporte de laboratorio`** cada cambio: cuál era el riesgo y cómo lo resuelves.

CAMBIO 1 — Estado global de ejecución
Creamos un estado compartido del juego:
RUNNING / PAUSED
Visible para runners y UI
Usamos GameClock como autoridad del estado, sin suspender hilos forzosamente.

CAMBIO 2 — SnakeRunner cooperativo
El runner respeta el estado del reloj, sin bloqueo fuerte.

CAMBIO 3 — Snake thread-safe mínimo
Protegemos solo lo necesario.

### 3) Control de ejecución seguro (UI)

- Implementa la **UI** con **Iniciar / Pausar / Reanudar** (ya existe el botón _Action_ y el reloj `GameClock`).
- Al **Pausar**, muestra de forma **consistente** (sin _tearing_):
  - La **serpiente viva más larga**.
  - La **peor serpiente** (la que **primero murió**).
- Considera que la suspensión **no es instantánea**; coordina para que el estado mostrado no quede “a medias”.

<img width="519" height="495" alt="image" src="https://github.com/user-attachments/assets/80276cef-0598-4c3a-89df-eabe0cb4aa7e" />
<img width="651" height="432" alt="image" src="https://github.com/user-attachments/assets/41544d4b-77e5-4b96-aa66-8b70a9f41cec" />


### 4) Robustez bajo carga

- Ejecuta con **N alto** (`-Dsnakes=20` o más) y/o aumenta la velocidad.
- El juego **no debe romperse**: sin `ConcurrentModificationException`, sin lecturas inconsistentes, sin _deadlocks_.
- Si habilitas **teleports** y **turbo**, verifica que las reglas no introduzcan carreras.

<img width="522" height="491" alt="image" src="https://github.com/user-attachments/assets/48ff1271-20eb-445d-b39f-a5bf91cf3cfa" />
<img width="515" height="487" alt="image" src="https://github.com/user-attachments/assets/b4c19ebc-28bd-4822-968c-0f91752f75c7" />
<img width="567" height="492" alt="image" src="https://github.com/user-attachments/assets/b05f53f3-efe8-4850-bc1d-9b0215186fc4" />
<img width="520" height="492" alt="image" src="https://github.com/user-attachments/assets/b71e8759-f545-4a70-9904-1186f1224c8e" />
<img width="519" height="493" alt="image" src="https://github.com/user-attachments/assets/2b0048f6-6440-4740-8a58-b79aa48475e2" />


> Entregables detallados más abajo.

---

## Entregables

1. **Código fuente** funcionando en **Java 21**.
2. Todo de manera clara en **`**el reporte de laboratorio**`** con:
   - Data races encontradas y su solución.
   - Colecciones mal usadas y cómo se protegieron (o sustituyeron).
   - Esperas activas eliminadas y mecanismo utilizado.
   - Regiones críticas definidas y justificación de su **alcance mínimo**.
3. UI con **Iniciar / Pausar / Reanudar** y estadísticas solicitadas al pausar.

---
