package ss.tp1;

import java.util.List;

/**
 * Medición de tiempos con calentamiento previo de la JVM.
 *
 * Sin warm-up, las primeras corridas se ejecutan interpretadas (antes de que
 * el JIT compile el kernel del CIM) y quedan uno o dos órdenes de magnitud por
 * encima del tiempo real: en la versión anterior el punto N=10 medía 8.5e-4 s,
 * 14 veces más que N=15. Esas corridas se descartan.
 */
public final class Bench {

    /** Corridas descartadas antes de medir, para dejar el código ya compilado por el JIT. */
    public static final int WARMUP_RUNS = 30;

    /** Corridas de calentamiento global, sobre un sistema mediano, antes del primer punto. */
    private static final int GLOBAL_WARMUP_RUNS = 2_000;

    private Bench() {}

    /**
     * Calienta el kernel al arrancar el proceso. El warm-up por punto no alcanza
     * cuando el primer punto es muy chico (N=10): son tan pocas invocaciones que
     * el JIT todavía no compiló, y ese punto quedaba inflado respecto de N=15.
     */
    public static void warmupJvm(double L, double rc, boolean periodic) {
        List<Particle> ps = ParticleGenerator.generate(200, L, 999);
        int m = Math.max(1, CellIndexMethod.maxM(L, rc, ParticleGenerator.R_MAX));
        for (int i = 0; i < GLOBAL_WARMUP_RUNS; i++) {
            CellIndexMethod.run(ps, L, m, rc, periodic);
        }
    }

    /** Duración mínima de un bloque de medición: por debajo domina el ruido del reloj. */
    private static final long MIN_BLOCK_NANOS = 1_000_000L; // 1 ms

    /**
     * Devuelve `repeats` tiempos (en segundos) de una búsqueda de vecinos.
     *
     * Para los sistemas chicos una búsqueda dura ~1 us, y a esa escala la
     * resolución del reloj y el ruido del sistema operativo son del orden de la
     * medición misma (las barras de error tapaban la curva). Por eso cada
     * muestra cronometra un bloque de corridas de al menos 1 ms y divide por la
     * cantidad. Cuando una sola corrida ya supera ese umbral el bloque es de una
     * corrida y la medición es directa.
     */
    public static double[] measure(
            List<Particle> particles, double L, int M, double rc, boolean periodic, int repeats) {
        for (int i = 0; i < WARMUP_RUNS; i++) {
            CellIndexMethod.run(particles, L, M, rc, periodic);
        }

        long probe = CellIndexMethod.run(particles, L, M, rc, periodic).elapsedNanos();
        int block = (probe <= 0) ? 1000 : (int) Math.max(1, Math.ceil((double) MIN_BLOCK_NANOS / probe));

        double[] times = new double[repeats];
        for (int rep = 0; rep < repeats; rep++) {
            long t0 = System.nanoTime();
            for (int b = 0; b < block; b++) {
                CellIndexMethod.run(particles, L, M, rc, periodic);
            }
            times[rep] = (System.nanoTime() - t0) / 1e9 / block;
        }
        return times;
    }
}
