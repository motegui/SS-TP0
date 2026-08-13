package ss.tp1;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Formato de archivos del enunciado (punto 5):
 *
 * Estático: N / L / r1 pr1 / ... / rN prN
 * Dinámico: t0 / x1 y1 vx1 vy1 / ... / xN yN vxN vyN
 * Salida de vecinos: una línea por partícula: "i id_vecino1 id_vecino2 ..."
 */
public final class SystemFiles {

    private SystemFiles() {}

    public record LoadedSystem(double L, List<Particle> particles) {}

    public static LoadedSystem load(Path staticFile, Path dynamicFile) throws IOException {
        List<String> st = Files.readAllLines(staticFile);
        int N = Integer.parseInt(firstToken(st.get(0)));
        double L = Double.parseDouble(firstToken(st.get(1)));
        double[] radii = new double[N];
        for (int i = 0; i < N; i++) {
            String[] parts = st.get(2 + i).trim().split("\\s+");
            radii[i] = Double.parseDouble(parts[0]);
        }

        List<String> dyn = Files.readAllLines(dynamicFile);
        List<Particle> particles = new ArrayList<>(N);
        for (int i = 0; i < N; i++) {
            String[] parts = dyn.get(1 + i).trim().split("\\s+");
            double x = Double.parseDouble(parts[0]);
            double y = Double.parseDouble(parts[1]);
            particles.add(new Particle(i, x, y, radii[i]));
        }
        return new LoadedSystem(L, particles);
    }

    private static String firstToken(String line) {
        return line.trim().split("\\s+")[0];
    }

    public static void writeStatic(Path path, double L, List<Particle> particles) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(particles.size()).append('\n');
        sb.append(L).append('\n');
        for (Particle p : particles) {
            sb.append(p.r()).append(" 0\n");
        }
        Files.writeString(path, sb.toString());
    }

    public static void writeDynamic(Path path, double t, List<Particle> particles) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(t).append('\n');
        for (Particle p : particles) {
            sb.append(p.x()).append(' ').append(p.y()).append(" 0 0\n");
        }
        Files.writeString(path, sb.toString());
    }

    public static void writeNeighbors(Path path, List<List<Integer>> neighbors) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < neighbors.size(); i++) {
            sb.append(i);
            for (int j : neighbors.get(i)) {
                sb.append(' ').append(j);
            }
            sb.append('\n');
        }
        Files.writeString(path, sb.toString());
    }
}
