# TP1 — Búsqueda Eficiente de Partículas Vecinas (Cell Index Method), en Java

Reimplementación completa en **Java** (sin dependencias externas, solo JDK + Swing).
Incluye una **app interactiva**: hacés click en una partícula y se colorean sus
vecinos, mientras a la derecha se muestra el cálculo del CIM paso a paso.

## Requisitos

JDK 17+. Si `java`/`javac` no están en el PATH, los scripts intentan usar
`/opt/homebrew/opt/openjdk/bin` (instalación típica de `brew install openjdk`
en macOS con Apple Silicon).

## Criterio L/M con partículas de radio > 0 (pregunta del enunciado)

Para partículas puntuales el criterio clásico es **L/M > rc**: el tamaño de
celda debe superar el alcance de interacción medido entre centros, así basta
revisar la celda propia y las 8 vecinas.

Con radio `r_i > 0`, dos partículas son vecinas si su distancia borde-borde es
`< rc`, es decir si la distancia entre **centros** es `< rc + r_i + r_j`.
Además, el **borde** de una partícula puede caer en una celda vecina aunque su
**centro** siga en la celda de origen. Por eso el criterio (con una capa de
celdas vecinas) pasa a ser:

```
L/M > rc + 2*max(r_i)
```

Si `max(r_i) -> 0` se recupera `L/M > rc`. El código (`Geometry.minCellSize`,
`CellIndexMethod.maxM`) valida esto y lanza error si `M` supera el máximo.

## Build

```bash
cd /Users/motegui/ss/tp1
./build.sh
```

## 1) App interactiva (click -> vecinos + cálculo)

```bash
./run_interactive.sh
```

- Controles arriba: **N**, **M**, **rc**, casillero de **condiciones periódicas**.
- **Regenerar**: crea un sistema aleatorio nuevo sin solapamiento.
- **Cargar data/**: lee `data/static.txt` + `data/dynamic.txt` (formato del punto 5).
- **Click** en un disco: rojo = partícula seleccionada, azul = sus vecinos.
  El panel derecho muestra: celda del centro, tamaño de celda `h=L/M`, capas y
  celdas escaneadas, y para cada candidato `j` el cálculo `dx, dy, ||d||,
  d_borde = ||d|| - r_i - r_j` y si es `< rc` (vecino) o no.
- **Exportar figura**: guarda `out/neighbors_gui.png` con la selección actual.

## 2) Generar sistema (input, formato del punto 5)

```bash
./run_generate.sh -N 300 -L 20 -o data
```

Genera `data/static.txt` (N, L, radios) y `data/dynamic.txt` (t0, posiciones).

## 3) Correr CIM por línea de comandos (output del punto 1)

```bash
./run_cim.sh data/static.txt data/dynamic.txt -M 15 -rc 1 --focus 0 \
  -o out/neighbors.txt --figure out/neighbors.png

# Con condiciones periódicas:
./run_cim.sh data/static.txt data/dynamic.txt -M 15 -rc 1 --periodic --figure out/neighbors_pbc.png
```

Imprime el `M` máximo permitido, el tiempo de ejecución, escribe la lista de
vecinos (`i id1 id2 ...` por línea) y una figura PNG con la partícula foco y
sus vecinos resaltados.

## 4) Punto 3 — tiempo vs M

```bash
./run_benchmark_m.sh --repeats 100 -o figures/time_vs_M.png
```

Toma dos valores de N (intermedio y el máximo posible sin solapar en L=20),
varía M de 1 (fuerza bruta) al máximo permitido, repite la medición y grafica
media ± desvío estándar (con escalas log si el rango de valores lo amerita).
También escribe un CSV con los datos crudos.

## 5) Punto 4 — tiempo vs N

```bash
# Elegí el M óptimo según la curva anterior, p.ej. M=15:
./run_benchmark_n.sh -M 15 --repeats 100 -o figures/time_vs_N.png
```

- **4.1**: L=20 fijo, tiempo vs N ("densidad libre").
- **4.2**: se toma una densidad intermedia de 4.1 y se agranda L junto con N
  para mantenerla constante ("densidad fija"), superpuesta en el mismo gráfico.

## Estructura

| Archivo | Rol |
|---|---|
| `src/ss/tp1/Geometry.java` | Distancia borde-borde, criterio de tamaño de celda |
| `src/ss/tp1/ParticleGenerator.java` | Colocación aleatoria sin solapamiento |
| `src/ss/tp1/CellIndexMethod.java` | CIM (M=1 fuerza bruta, M>1 grilla), validación de M, consulta detallada por partícula |
| `src/ss/tp1/SystemFiles.java` | Lectura/escritura de `static.txt`, `dynamic.txt` y salida de vecinos |
| `src/ss/tp1/NeighborRenderer.java` | Figura PNG headless (foco + vecinos) |
| `src/ss/tp1/SimpleChart.java` | Gráficos XY con barras de error y escala log, sin dependencias |
| `src/ss/tp1/ParticleCanvas.java`, `CalculationPanel.java`, `InteractiveApp.java` | GUI interactiva (Swing) |
| `GenerateSystem`, `RunCim`, `BenchmarkM`, `BenchmarkN` | CLIs (`main`) para cada punto del enunciado |

## Demostración en vivo (punto 2)

Con `./run_interactive.sh` se puede variar N, M, rc y condiciones periódicas
en vivo, y hacer click sobre distintas partículas para mostrar los vecinos y
el detalle del cálculo pedido en el punto 1.
