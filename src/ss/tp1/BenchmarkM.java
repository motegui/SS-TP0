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
        Path csv = Path.of("figures/time_vs_M.csv");

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

        int nMax = ParticleGenerator.maxFeasibleN(L, seed);
        int nMid = Math.max(50, nMax / 4);
        System.out.println("N intermedio=" + nMid + "  N máximo=" + nMax);

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
                double[] times = new double[repeats];
                for (int rep = 0; rep < repeats; rep++) {
                    times[rep] = CellIndexMethod.run(particles, L, M, rc, periodic).elapsedNanos() / 1e9;
                }
                double mean = Stats.mean(times);
                double std = Stats.stddev(times, mean);
                Ms[M - 1] = M;
                means[M - 1] = mean;
                stds[M - 1] = std;
                csvLines.add(N + "," + M + "," + mean + "," + std);
                System.out.printf("  N=%d M=%d -> %.6f s (+/- %.6f)%n", N, M, mean, std);
            }
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

        SimpleChart.save(out, "CIM: tiempo medio vs M", "M (celdas por lado)", "Tiempo (s)", logX, logY, seriesList);
        System.out.println("Guardado " + out + " y " + csv);
    }
}
