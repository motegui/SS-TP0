package ss.tp1;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** CLI: genera un sistema aleatorio sin solapamiento y escribe static.txt / dynamic.txt. */
public final class GenerateSystem {

    public static void main(String[] args) throws Exception {
        int N = 200;
        double L = 20.0;
        long seed = 42;
        Path outDir = Path.of("data");

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-N" -> N = Integer.parseInt(args[++i]);
                case "-L" -> L = Double.parseDouble(args[++i]);
                case "--seed" -> seed = Long.parseLong(args[++i]);
                case "-o" -> outDir = Path.of(args[++i]);
                default -> throw new IllegalArgumentException("Argumento desconocido: " + args[i]);
            }
        }

        Files.createDirectories(outDir);
        List<Particle> particles = ParticleGenerator.generate(N, L, seed);
        SystemFiles.writeStatic(outDir.resolve("static.txt"), L, particles);
        SystemFiles.writeDynamic(outDir.resolve("dynamic.txt"), 0.0, particles);
        System.out.printf("Generadas %d partículas en %s (L=%.2f)%n", N, outDir, L);
    }
}
