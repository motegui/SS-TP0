package ss.tp1;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Generación aleatoria de partículas sin solapamiento dentro de [0,L]x[0,L]. */
public final class ParticleGenerator {

    public static final double R_MIN = 0.23;
    public static final double R_MAX = 0.26;
    private static final int MAX_ATTEMPTS_PER_PARTICLE = 50_000;

    private ParticleGenerator() {}

    public static List<Particle> generate(int N, double L, long seed) {
        return generate(N, L, seed, R_MIN, R_MAX);
    }

    public static List<Particle> generate(int N, double L, long seed, double rMin, double rMax) {
        Random rng = new Random(seed);
        List<Particle> out = new ArrayList<>(N);
        for (int n = 0; n < N; n++) {
            boolean placed = false;
            for (int attempt = 0; attempt < MAX_ATTEMPTS_PER_PARTICLE; attempt++) {
                double r = rMin + rng.nextDouble() * (rMax - rMin);
                if (2 * r >= L) {
                    continue;
                }
                double x = r + rng.nextDouble() * (L - 2 * r);
                double y = r + rng.nextDouble() * (L - 2 * r);
                if (!overlaps(x, y, r, out, L)) {
                    out.add(new Particle(n, x, y, r));
                    placed = true;
                    break;
                }
            }
            if (!placed) {
                throw new IllegalStateException(
                        "No se pudo colocar la partícula " + (n + 1) + "/" + N
                                + " sin solapamiento (L=" + L + "). Probá con N menor o L mayor.");
            }
        }
        return out;
    }

    private static boolean overlaps(double x, double y, double r, List<Particle> others, double L) {
        for (Particle p : others) {
            if (Geometry.edgeDistance(x, y, r, p.x(), p.y(), p.r(), L, false) <= 0.0) {
                return true;
            }
        }
        return false;
    }

    /** Busca por bisección el N máximo colocable sin solapamiento en [0,L]x[0,L]. */
    public static int maxFeasibleN(double L, long seed) {
        int lo = 10;
        int hi = 10;
        while (true) {
            try {
                generate(hi, L, seed);
                lo = hi;
                if (hi >= 20_000) {
                    break;
                }
                int next = Math.min(hi * 2, 20_000);
                if (next == hi) {
                    break;
                }
                hi = next;
            } catch (IllegalStateException e) {
                break;
            }
        }
        while (lo + 1 < hi) {
            int mid = (lo + hi) / 2;
            try {
                generate(mid, L, seed);
                lo = mid;
            } catch (IllegalStateException e) {
                hi = mid;
            }
        }
        return lo;
    }
}
