package ss.tp1;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

/** Renderiza (sin necesidad de pantalla) una figura PNG con la partícula foco y sus vecinos. */
public final class NeighborRenderer {

    private NeighborRenderer() {}

    public static void render(
            List<Particle> particles, double L, int focus, List<Integer> neighbors,
            boolean periodic, Path outPng) throws IOException {
        int size = 900;
        int pad = 40;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, size, size);

        double s = (size - 2.0 * pad) / L;
        int yBase = size - pad;

        g.setColor(new Color(230, 230, 230));
        g.drawRect(pad, pad, (int) Math.round(L * s), (int) Math.round(L * s));

        Set<Integer> nbSet = new HashSet<>(neighbors);
        for (Particle p : particles) {
            Color c;
            if (p.id() == focus) {
                c = new Color(230, 57, 70);
            } else if (nbSet.contains(p.id())) {
                c = new Color(69, 123, 157);
            } else {
                c = new Color(210, 210, 210);
            }
            double cx = pad + p.x() * s;
            double cy = yBase - p.y() * s;
            double d = 2 * p.r() * s;
            Ellipse2D disk = new Ellipse2D.Double(cx - p.r() * s, cy - p.r() * s, d, d);
            g.setColor(c);
            g.fill(disk);
            g.setColor(Color.DARK_GRAY);
            g.draw(disk);
        }

        g.setColor(Color.BLACK);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        g.drawString(
                "Foco: " + focus + "  Vecinos: " + neighbors.size()
                        + (periodic ? "  (BC periódicas)" : "  (paredes)"),
                pad, 24);
        g.dispose();

        Path parent = outPng.toAbsolutePath().getParent();
        if (parent != null) {
            Files_createDirectoriesQuiet(parent);
        }
        ImageIO.write(img, "png", outPng.toFile());
    }

    private static void Files_createDirectoriesQuiet(Path dir) throws IOException {
        java.nio.file.Files.createDirectories(dir);
    }
}
