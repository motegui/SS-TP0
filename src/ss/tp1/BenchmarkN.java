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
        Path csv = withExtension(out, ".csv");
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
        System.out.println("Calentando la JVM (repeticiones por punto=" + repeats + ")...");
        Bench.warmupJvm(L0, rc, periodic);

        // 4.1: densidad libre (L0 fijo)
        double[] meansFree = new double[nValues.length];
        double[] stdsFree = new double[nValues.length];
        for (int k = 0; k < nValues.length; k++) {
            int N = nValues[k];
            List<Particle> particles = ParticleGenerator.generate(N, L0, seed + N);
            double[] times = Bench.measure(particles, L0, M, rc, periodic, repeats);
            meansFree[k] = Stats.mean(times);
            stdsFree[k] = Stats.stddev(times, meansFree[k]);
            System.out.printf("[libre] N=%d -> %.6f s (+/- %.6f)%n", N, meansFree[k], stdsFree[k]);
        }

        // Densidad intermedia elegida de 4.1
        int nStar = nValues[nValues.length / 2];
        double rho = nStar / (L0 * L0);

        // 4.2: densidad fija (L escala con N).
        //
        // Acá M NO puede quedar fijo. Lo que el punto 3 optimiza no es el número
        // de celdas sino su tamaño h=L/M: es h frente a rc lo que fija cuántas
        // partículas se revisan por celda. Si se agranda L dejando M=13, h crece
        // con L, cada celda acumula O(L^2) partículas y el CIM degenera a O(N^2)
        // -- además, para los L chicos M=13 viola L/M > rc+2r y esos puntos se
        // perdían. Manteniendo h constante (M proporcional a L) la comparación
        // es a igual carga por celda y se recupera el comportamiento lineal.
        double h0 = L0 / M;
        List<Double> xFixed = new ArrayList<>();
        List<Double> meanFixed = new ArrayList<>();
        List<Double> stdFixed = new ArrayList<>();
        List<Integer> mFixed = new ArrayList<>();
        for (int N : nValues) {
            double L = Math.sqrt(N / rho);
            int mL = Math.max(1, (int) Math.floor(L / h0));
            int mMaxL = CellIndexMethod.maxM(L, rc, ParticleGenerator.R_MAX);
            if (mMaxL >= 1) {
                mL = Math.min(mL, mMaxL);
            }
            try {
                List<Particle> particles = ParticleGenerator.generate(N, L, seed + 10_000 + N);
                double[] times = Bench.measure(particles, L, mL, rc, periodic, repeats);
                double mean = Stats.mean(times);
                xFixed.add((double) N);
                meanFixed.add(mean);
                stdFixed.add(Stats.stddev(times, mean));
                mFixed.add(mL);
                System.out.printf("[fija]  N=%d L=%.3f M=%d (h=%.3f) -> %.6f s%n", N, L, mL, L / mL, mean);
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
                "densidad libre (L=" + L0 + ", M=" + M + ")",
                new Color(69, 123, 157), xFreeArr, meansFree, stdsFree));
        seriesList.add(new SimpleChart.Series(
                String.format("densidad fija (rho=%.4f, h=%.3f)", rho, h0),
                new Color(230, 57, 70),
                toArray(xFixed), toArray(meanFixed), toArray(stdFixed)));

        List<String> csvLines = new ArrayList<>();
        csvLines.add("N,M,mean_free_s,std_free_s");
        for (int i = 0; i < nValues.length; i++) {
            csvLines.add(nValues[i] + "," + M + "," + meansFree[i] + "," + stdsFree[i]);
        }
        csvLines.add("");
        csvLines.add("N,L,M,mean_fixed_density_s,std_fixed_density_s,rho=" + rho);
        for (int i = 0; i < xFixed.size(); i++) {
            csvLines.add(xFixed.get(i).intValue()
                    + "," + Math.sqrt(xFixed.get(i) / rho)
                    + "," + mFixed.get(i)
                    + "," + meanFixed.get(i) + "," + stdFixed.get(i));
        }

        Files.createDirectories(out.toAbsolutePath().getParent());
        Files.createDirectories(csv.toAbsolutePath().getParent());
        Files.write(csv, csvLines);

        boolean logX = (double) nValues[nValues.length - 1] / nValues[0] > 30;
        double minVal = Math.min(Stats.minPositive(meansFree), Stats.minPositive(toArray(meanFixed)));
        double maxVal = Math.max(Stats.max(meansFree), Stats.max(toArray(meanFixed)));
        boolean logY = maxVal / Math.max(minVal, 1e-12) > 30;

        SimpleChart.save(out,
                "CIM: tiempo medio vs N (" + repeats + " repeticiones, tamaño de celda h=" + String.format("%.3f", h0) + ")",
                "N", "Tiempo (s)", logX, logY, seriesList);
        System.out.println("Guardado " + out + " y " + csv + "  (N*=" + nStar + ", rho=" + rho + ")");
    }

    /** Mismo path que la figura pero con otra extensión, para que -o mueva ambos archivos. */
    private static Path withExtension(Path path, String ext) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String base = (dot > 0) ? name.substring(0, dot) : name;
        Path parent = path.getParent();
        return (parent == null) ? Path.of(base + ext) : parent.resolve(base + ext);
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
