package ss.tp1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Cell Index Method (CIM): búsqueda de vecinos a distancia borde-borde &lt; rc.
 *
 * M=1 se resuelve por fuerza bruta (todas las parejas). M&gt;1 usa una grilla
 * de M x M celdas y revisa, para cada partícula, una cantidad de "capas" de
 * celdas vecinas suficiente para no perder interacciones dado el radio máximo.
 */
public final class CellIndexMethod {

    public record CellCoord(int cx, int cy) {}

    public record CandidateResult(
            int j, CellCoord fromCell, double dx, double dy,
            double centerDist, double edgeDist, boolean neighbor) {}

    public record ParticleQuery(
            int particleId, double h, int layers, CellCoord homeCell,
            List<CellCoord> scannedCells, List<CandidateResult> candidates,
            List<Integer> neighbors, long elapsedNanos) {}

    public record NeighborResult(List<List<Integer>> neighbors, long elapsedNanos) {}

    private final double L;
    private final int M;
    private final double rc;
    private final boolean periodic;
    private final List<Particle> particles;
    private final double rMax;
    private final double h;
    private final int layers;
    private final List<Integer>[] grid; // null si M == 1 (fuerza bruta)

    @SuppressWarnings("unchecked")
    public CellIndexMethod(List<Particle> particles, double L, int M, double rc, boolean periodic) {
        if (M < 1) {
            throw new IllegalArgumentException("M debe ser >= 1");
        }
        this.particles = List.copyOf(particles);
        this.L = L;
        this.M = M;
        this.rc = rc;
        this.periodic = periodic;
        this.rMax = this.particles.stream().mapToDouble(Particle::r).max().orElse(0.0);

        if (M > 1) {
            double hMin = Geometry.minCellSize(rc, rMax);
            double hCandidate = L / M;
            if (hCandidate <= hMin) {
                throw new IllegalArgumentException(String.format(
                        "M=%d inválido: L/M=%.6f debe ser > rc + 2*max(r) = %.6f",
                        M, hCandidate, hMin));
            }
        }
        this.h = L / M;
        this.layers = (M == 1) ? 0 : Math.max(1, (int) Math.ceil((rMax + rc + rMax) / h));

        if (M == 1) {
            this.grid = null;
        } else {
            List<Integer>[] cells = new List[M * M];
            for (int k = 0; k < cells.length; k++) {
                cells[k] = new ArrayList<>();
            }
            for (Particle p : this.particles) {
                CellCoord c = cellOf(p.x(), p.y());
                cells[index(c)].add(p.id());
            }
            this.grid = cells;
        }
    }

    /** M máximo tal que L/M &gt; rc + 2*maxRadius (0 si ni M=1 alcanza, en cuyo caso solo vale M=1). */
    public static int maxM(double L, double rc, double maxRadius) {
        double hMin = Geometry.minCellSize(rc, maxRadius);
        if (L <= hMin) {
            return 0;
        }
        return (int) Math.floor(L / hMin);
    }

    /** Corre una búsqueda completa de vecinos, midiendo el tiempo total (incluye construir la grilla). */
    public static NeighborResult run(List<Particle> particles, double L, int M, double rc, boolean periodic) {
        long t0 = System.nanoTime();
        CellIndexMethod cim = new CellIndexMethod(particles, L, M, rc, periodic);
        List<List<Integer>> out = cim.computeAll();
        long elapsed = System.nanoTime() - t0;
        return new NeighborResult(out, elapsed);
    }

    public CellCoord cellOf(double x, double y) {
        int cx = (int) Math.floor(x / h);
        int cy = (int) Math.floor(y / h);
        cx = Math.min(Math.max(cx, 0), M - 1);
        cy = Math.min(Math.max(cy, 0), M - 1);
        return new CellCoord(cx, cy);
    }

    private int index(CellCoord c) {
        return c.cx() + M * c.cy();
    }

    private CellCoord wrapCell(int cx, int cy) {
        if (periodic) {
            return new CellCoord(Math.floorMod(cx, M), Math.floorMod(cy, M));
        }
        if (cx < 0 || cy < 0 || cx >= M || cy >= M) {
            return null;
        }
        return new CellCoord(cx, cy);
    }

    /** Lista de vecinos para todas las partículas (para benchmarking / salida a archivo). */
    public List<List<Integer>> computeAll() {
        int n = particles.size();
        List<List<Integer>> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            out.add(new ArrayList<>());
        }

        if (M == 1) {
            for (int i = 0; i < n; i++) {
                Particle pi = particles.get(i);
                for (int j = i + 1; j < n; j++) {
                    Particle pj = particles.get(j);
                    double d = Geometry.edgeDistance(pi.x(), pi.y(), pi.r(), pj.x(), pj.y(), pj.r(), L, periodic);
                    if (d < rc) {
                        out.get(i).add(j);
                        out.get(j).add(i);
                    }
                }
            }
        } else {
            for (int i = 0; i < n; i++) {
                Particle pi = particles.get(i);
                CellCoord home = cellOf(pi.x(), pi.y());
                Set<Integer> seen = new HashSet<>();
                for (int dx = -layers; dx <= layers; dx++) {
                    for (int dy = -layers; dy <= layers; dy++) {
                        CellCoord c = wrapCell(home.cx() + dx, home.cy() + dy);
                        if (c == null) {
                            continue;
                        }
                        for (int j : grid[index(c)]) {
                            if (j <= i || !seen.add(j)) {
                                continue;
                            }
                            Particle pj = particles.get(j);
                            double d = Geometry.edgeDistance(pi.x(), pi.y(), pi.r(), pj.x(), pj.y(), pj.r(), L, periodic);
                            if (d < rc) {
                                out.get(i).add(j);
                                out.get(j).add(i);
                            }
                        }
                    }
                }
            }
        }
        for (List<Integer> l : out) {
            Collections.sort(l);
        }
        return out;
    }

    /**
     * Consulta detallada para UNA partícula: qué celdas se escanean y, para
     * cada candidato, el cálculo completo de distancia (para mostrar en la UI).
     */
    public ParticleQuery queryParticle(int i) {
        long t0 = System.nanoTime();
        Particle pi = particles.get(i);
        List<CellCoord> scanned = new ArrayList<>();
        List<CandidateResult> candidates = new ArrayList<>();
        List<Integer> neighbors = new ArrayList<>();
        CellCoord home;

        if (M == 1) {
            home = new CellCoord(0, 0);
            scanned.add(home);
            for (Particle pj : particles) {
                if (pj.id() == i) {
                    continue;
                }
                addCandidate(pi, pj, home, candidates, neighbors);
            }
        } else {
            home = cellOf(pi.x(), pi.y());
            Set<Integer> seen = new HashSet<>();
            for (int dx = -layers; dx <= layers; dx++) {
                for (int dy = -layers; dy <= layers; dy++) {
                    CellCoord c = wrapCell(home.cx() + dx, home.cy() + dy);
                    if (c == null) {
                        continue;
                    }
                    scanned.add(c);
                    for (int j : grid[index(c)]) {
                        if (j == i || !seen.add(j)) {
                            continue;
                        }
                        addCandidate(pi, particles.get(j), c, candidates, neighbors);
                    }
                }
            }
        }

        Collections.sort(neighbors);
        long elapsed = System.nanoTime() - t0;
        return new ParticleQuery(i, h, layers, home, scanned, candidates, neighbors, elapsed);
    }

    private void addCandidate(
            Particle pi, Particle pj, CellCoord fromCell,
            List<CandidateResult> candidates, List<Integer> neighbors) {
        double dx = pj.x() - pi.x();
        double dy = pj.y() - pi.y();
        if (periodic) {
            dx -= L * Math.round(dx / L);
            dy -= L * Math.round(dy / L);
        }
        double center = Math.hypot(dx, dy);
        double edge = center - pi.r() - pj.r();
        boolean isNeighbor = edge < rc;
        candidates.add(new CandidateResult(pj.id(), fromCell, dx, dy, center, edge, isNeighbor));
        if (isNeighbor) {
            neighbors.add(pj.id());
        }
    }

    public double cellSize() {
        return h;
    }

    public int layers() {
        return layers;
    }
}
