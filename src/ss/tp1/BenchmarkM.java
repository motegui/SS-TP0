package ss.tp1;

import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Punto 3: tiempo de cómputo vs M, para dos valores de N (intermedio y máximo). */
public final class BenchmarkM {

    public static void main(String[] args) throws Exception {
        double L = 20.0;
        double rc = 1.0;
        int repeats = 100;
        long seed = 0;
        boolean periodic = false;
        Path out = Path.of("figures/time_vs_M.png");

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-L" -> L = Double.parseDouble(args[++i]);
                case "-rc" -> rc = Double.parseDouble(args[++i]);
                case "--repeats" -> repeats = Integer.parseInt(args[++i]);
                case "--seed" -> seed = Long.parseLong(args[++i]);
                case "--periodic" -> periodic = true;
                case "-o" -> out = Path.of(args[++i]);
                default -> throw new IllegalArgumentException("Argumento desconocido: " + args[i]);
            }
        }
        Path csv = withExtension(out, ".csv");

        int nMax = ParticleGenerator.maxFeasibleN(L, seed);
        int nMid = Math.max(50, nMax / 2);
        System.out.println("N intermedio=" + nMid + "  N máximo=" + nMax
                + "  (repeticiones por punto=" + repeats + ", warm-up=" + Bench.WARMUP_RUNS + ")");
        System.out.println("Calentando la JVM...");
        Bench.warmupJvm(L, rc, periodic);

        List<SimpleChart.Series> seriesList = new ArrayList<>();
        Color[] colors = {new Color(69, 123, 157), new Color(230, 57, 70)};
        int colorIdx = 0;
        List<String> csvLines = new ArrayList<>();
        csvLines.add("N,M,mean_s,std_s");

        for (int N : new int[] {nMid, nMax}) {
            List<Particle> particles = ParticleGenerator.generate(N, L, seed + N);
            double maxR = particles.stream().mapToDouble(Particle::r).max().orElse(0);
            int mMax = CellIndexMethod.maxM(L, rc, maxR);
            if (mMax < 1) {
                mMax = 1;
            }
            double[] Ms = new double[mMax];
            double[] means = new double[mMax];
            double[] stds = new double[mMax];

            for (int M = 1; M <= mMax; M++) {
                double[] times = Bench.measure(particles, L, M, rc, periodic, repeats);
                double mean = Stats.mean(times);
                double std = Stats.stddev(times, mean);
                Ms[M - 1] = M;
                means[M - 1] = mean;
                stds[M - 1] = std;
                csvLines.add(N + "," + M + "," + mean + "," + std);
                System.out.printf("  N=%d M=%d%s -> %.6f s (+/- %.6f)%n",
                        N, M, (M == 1 ? " (fuerza bruta)" : ""), mean, std);
            }

            int best = 0;
            for (int k = 1; k < mMax; k++) {
                if (means[k] < means[best]) {
                    best = k;
                }
            }
            System.out.printf("  -> M óptimo para N=%d: M=%d (%.6f s); M máximo permitido: %d%n",
                    N, (int) Ms[best], means[best], mMax);

            seriesList.add(new SimpleChart.Series("N=" + N, colors[colorIdx++ % colors.length], Ms, means, stds));
        }

        Files.createDirectories(out.toAbsolutePath().getParent());
        Files.createDirectories(csv.toAbsolutePath().getParent());
        Files.write(csv, csvLines);

        double xSpan = seriesList.stream()
                .mapToDouble(s -> s.x()[s.x().length - 1] / Math.max(s.x()[0], 1))
                .max().orElse(1);
        boolean logX = xSpan > 50;

        double minVal = seriesList.stream()
                .flatMapToDouble(s -> java.util.Arrays.stream(s.y()))
                .filter(v -> v > 0).min().orElse(1e-9);
        double maxVal = seriesList.stream()
                .flatMapToDouble(s -> java.util.Arrays.stream(s.y()))
                .max().orElse(1);
        boolean logY = maxVal / Math.max(minVal, 1e-12) > 50;

        SimpleChart.save(out,
                "CIM: tiempo medio vs M (" + repeats + " repeticiones; M=1 es fuerza bruta)",
                "M (celdas por lado)", "Tiempo (s)", logX, logY, true, seriesList);
        System.out.println("Guardado " + out + " y " + csv);
    }

    /** Mismo path que la figura pero con otra extensión, para que -o mueva ambos archivos. */
    private static Path withExtension(Path path, String ext) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String base = (dot > 0) ? name.substring(0, dot) : name;
        Path parent = path.getParent();
        return (parent == null) ? Path.of(base + ext) : parent.resolve(base + ext);
    }
}
