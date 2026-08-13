package ss.tp1;

import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Punto 4: tiempo de cómputo vs N para el M óptimo hallado en el punto 3.
 * 4.1: L=20 fijo (densidad libre). 4.2: L crece con N a densidad constante (superpuesto).
 */
public final class BenchmarkN {

    public static void main(String[] args) throws Exception {
        int M = -1;
        double L0 = 20.0;
        double rc = 1.0;
        int repeats = 100;
        long seed = 0;
        boolean periodic = false;
        Path out = Path.of("figures/time_vs_N.png");
        Path csv = Path.of("figures/time_vs_N.csv");

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-M" -> M = Integer.parseInt(args[++i]);
                case "-L0" -> L0 = Double.parseDouble(args[++i]);
                case "-rc" -> rc = Double.parseDouble(args[++i]);
                case "--repeats" -> repeats = Integer.parseInt(args[++i]);
                case "--seed" -> seed = Long.parseLong(args[++i]);
                case "--periodic" -> periodic = true;
                case "-o" -> out = Path.of(args[++i]);
                default -> throw new IllegalArgumentException("Argumento desconocido: " + args[i]);
            }
        }
        if (M < 1) {
            System.err.println("Debe indicar -M <valor óptimo obtenido con BenchmarkM>");
            System.exit(1);
        }

        double mMaxAtL0 = CellIndexMethod.maxM(L0, rc, ParticleGenerator.R_MAX);
        if (M > mMaxAtL0) {
            System.err.println("M=" + M + " no es válido para L=" + L0 + " (máximo aprox. " + mMaxAtL0 + ")");
            System.exit(1);
        }

        int nMax = ParticleGenerator.maxFeasibleN(L0, seed);
        int[] nValues = geomSpace(10, nMax, 12);

        // 4.1: densidad libre (L0 fijo)
        double[] meansFree = new double[nValues.length];
        double[] stdsFree = new double[nValues.length];
        for (int k = 0; k < nValues.length; k++) {
            int N = nValues[k];
            List<Particle> particles = ParticleGenerator.generate(N, L0, seed + N);
            double[] times = timeRepeated(particles, L0, M, rc, periodic, repeats);
            meansFree[k] = Stats.mean(times);
            stdsFree[k] = Stats.stddev(times, meansFree[k]);
            System.out.printf("[libre] N=%d -> %.6f s (+/- %.6f)%n", N, meansFree[k], stdsFree[k]);
        }

        // Densidad intermedia elegida de 4.1
        int nStar = nValues[nValues.length / 2];
        double rho = nStar / (L0 * L0);

        // 4.2: densidad fija (L escala con N)
        List<Double> xFixed = new ArrayList<>();
        List<Double> meanFixed = new ArrayList<>();
        List<Double> stdFixed = new ArrayList<>();
        for (int N : nValues) {
            double L = Math.sqrt(N / rho);
            try {
                List<Particle> particles = ParticleGenerator.generate(N, L, seed + 10_000 + N);
                double[] times = timeRepeated(particles, L, M, rc, periodic, repeats);
                double mean = Stats.mean(times);
                xFixed.add((double) N);
                meanFixed.add(mean);
                stdFixed.add(Stats.stddev(times, mean));
                System.out.printf("[fija]  N=%d L=%.3f -> %.6f s%n", N, L, mean);
            } catch (IllegalArgumentException | IllegalStateException ex) {
                System.err.println("Aviso: se omite N=" + N + " en densidad fija (L=" + String.format("%.3f", L)
                        + "): " + ex.getMessage());
            }
        }

        double[] xFreeArr = new double[nValues.length];
        for (int i = 0; i < nValues.length; i++) {
            xFreeArr[i] = nValues[i];
        }

        List<SimpleChart.Series> seriesList = new ArrayList<>();
        seriesList.add(new SimpleChart.Series(
                "densidad libre (L=" + L0 + ")", new Color(69, 123, 157), xFreeArr, meansFree, stdsFree));
        seriesList.add(new SimpleChart.Series(
                String.format("densidad fija (rho=%.4f)", rho),
                new Color(230, 57, 70),
                toArray(xFixed), toArray(meanFixed), toArray(stdFixed)));

        List<String> csvLines = new ArrayList<>();
        csvLines.add("N,mean_free_s,std_free_s");
        for (int i = 0; i < nValues.length; i++) {
            csvLines.add(nValues[i] + "," + meansFree[i] + "," + stdsFree[i]);
        }
        csvLines.add("");
        csvLines.add("N,mean_fixed_density_s,std_fixed_density_s,rho=" + rho);
        for (int i = 0; i < xFixed.size(); i++) {
            csvLines.add(xFixed.get(i).intValue() + "," + meanFixed.get(i) + "," + stdFixed.get(i));
        }

        Files.createDirectories(out.toAbsolutePath().getParent());
        Files.createDirectories(csv.toAbsolutePath().getParent());
        Files.write(csv, csvLines);

        boolean logX = (double) nValues[nValues.length - 1] / nValues[0] > 30;
        double minVal = Math.min(Stats.minPositive(meansFree), Stats.minPositive(toArray(meanFixed)));
        double maxVal = Math.max(Stats.max(meansFree), Stats.max(toArray(meanFixed)));
        boolean logY = maxVal / Math.max(minVal, 1e-12) > 30;

        SimpleChart.save(out, "CIM: tiempo medio vs N (M=" + M + ")", "N", "Tiempo (s)", logX, logY, seriesList);
        System.out.println("Guardado " + out + " y " + csv + "  (N*=" + nStar + ", rho=" + rho + ")");
    }

    private static double[] timeRepeated(
            List<Particle> particles, double L, int M, double rc, boolean periodic, int repeats) {
        double[] times = new double[repeats];
        for (int rep = 0; rep < repeats; rep++) {
            times[rep] = CellIndexMethod.run(particles, L, M, rc, periodic).elapsedNanos() / 1e9;
        }
        return times;
    }

    private static int[] geomSpace(int lo, int hi, int count) {
        TreeSet<Integer> set = new TreeSet<>();
        double logLo = Math.log(lo);
        double logHi = Math.log(Math.max(hi, lo + 1));
        for (int i = 0; i < count; i++) {
            double t = i / (double) (count - 1);
            int v = (int) Math.round(Math.exp(logLo + t * (logHi - logLo)));
            set.add(Math.max(lo, Math.min(hi, v)));
        }
        int[] arr = new int[set.size()];
        int i = 0;
        for (int v : set) {
            arr[i++] = v;
        }
        return arr;
    }

    private static double[] toArray(List<Double> list) {
        double[] arr = new double[list.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }
}
