package ss.tp1;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** CLI: corre CIM sobre un sistema, escribe lista de vecinos + tiempo + figura (punto 1). */
public final class RunCim {

    public static void main(String[] args) throws Exception {
        int M = 10;
        int focus = 0;
        double rc = 1.0;
        boolean periodic = false;
        Path output = Path.of("out/neighbors.txt");
        Path figure = Path.of("out/neighbors.png");
        List<String> pos = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-M" -> M = Integer.parseInt(args[++i]);
                case "-rc" -> rc = Double.parseDouble(args[++i]);
                case "--focus" -> focus = Integer.parseInt(args[++i]);
                case "--periodic" -> periodic = true;
                case "-o" -> output = Path.of(args[++i]);
                case "--figure" -> figure = Path.of(args[++i]);
                default -> pos.add(args[i]);
            }
        }
        if (pos.size() < 2) {
            System.err.println(
                    "Uso: RunCim <static.txt> <dynamic.txt> -M <int> [-rc <double>] "
                            + "[--focus <id>] [--periodic] [-o out.txt] [--figure fig.png]");
            System.exit(1);
        }

        Path staticFile = Path.of(pos.get(0));
        Path dynamicFile = Path.of(pos.get(1));

        SystemFiles.LoadedSystem sys = SystemFiles.load(staticFile, dynamicFile);
        double maxR = sys.particles().stream().mapToDouble(Particle::r).max().orElse(0);
        int mMax = CellIndexMethod.maxM(sys.L(), rc, maxR);
        System.out.println("Max M permitido (L/(rc+2*max(r))): " + mMax);

        CellIndexMethod.NeighborResult result = CellIndexMethod.run(sys.particles(), sys.L(), M, rc, periodic);

        if (output.toAbsolutePath().getParent() != null) {
            Files.createDirectories(output.toAbsolutePath().getParent());
        }
        if (figure.toAbsolutePath().getParent() != null) {
            Files.createDirectories(figure.toAbsolutePath().getParent());
        }

        SystemFiles.writeNeighbors(output, result.neighbors());
        System.out.println("Lista de vecinos escrita en " + output);
        System.out.printf("Tiempo de ejecución: %.6f s%n", result.elapsedNanos() / 1e9);

        NeighborRenderer.render(sys.particles(), sys.L(), focus, result.neighbors().get(focus), periodic, figure);
        System.out.println("Figura guardada en " + figure);
    }
}
