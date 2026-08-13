package ss.tp1;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;

import javax.swing.JPanel;

/** Panel de dibujo: grilla del CIM + partículas. Click en un disco dispara onSelect(id). */
public class ParticleCanvas extends JPanel {

    private static final int PAD = 30;

    private double L = 20;
    private int M = 10;
    private List<Particle> particles = List.of();
    private int selected = -1;
    private Set<Integer> highlightNeighbors = Set.of();
    private IntConsumer onSelect;

    public ParticleCanvas() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(620, 620));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int hit = hitTest(e.getX(), e.getY());
                if (hit >= 0 && onSelect != null) {
                    onSelect.accept(hit);
                }
            }
        });
    }

    public void setOnSelect(IntConsumer onSelect) {
        this.onSelect = onSelect;
    }

    public void setSystem(double L, int M, List<Particle> particles) {
        this.L = L;
        this.M = M;
        this.particles = particles;
        this.selected = -1;
        this.highlightNeighbors = Set.of();
        repaint();
    }

    public void setGridM(int M) {
        this.M = M;
        repaint();
    }

    public void setSelection(int id, List<Integer> neighbors) {
        this.selected = id;
        this.highlightNeighbors = new HashSet<>(neighbors);
        repaint();
    }

    public void clearSelection() {
        this.selected = -1;
        this.highlightNeighbors = Set.of();
        repaint();
    }

    public int getSelected() {
        return selected;
    }

    private double scale() {
        int side = Math.min(getWidth(), getHeight()) - 2 * PAD;
        return Math.max(side, 10) / L;
    }

    private double toWorldX(int px) {
        return (px - PAD) / scale();
    }

    private double toWorldY(int py) {
        return (PAD + L * scale() - py) / scale();
    }

    private int hitTest(int px, int py) {
        double wx = toWorldX(px);
        double wy = toWorldY(py);
        int best = -1;
        double bestDist = Double.MAX_VALUE;
        for (Particle p : particles) {
            double d = Math.hypot(p.x() - wx, p.y() - wy);
            if (d <= p.r() && d < bestDist) {
                bestDist = d;
                best = p.id();
            }
        }
        return best;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double s = scale();
        double side = L * s;
        int x0 = PAD;
        int yTop = PAD;
        int yBase = (int) Math.round(PAD + side);

        g2.setColor(new Color(248, 248, 252));
        g2.fillRect(x0, yTop, (int) Math.round(side), (int) Math.round(side));
        g2.setColor(new Color(180, 180, 190));
        g2.drawRect(x0, yTop, (int) Math.round(side), (int) Math.round(side));

        g2.setColor(new Color(215, 215, 225));
        g2.setStroke(new BasicStroke(1f));
        double h = L / M;
        for (int k = 0; k <= M; k++) {
            double w = k * h;
            int gx = x0 + (int) Math.round(w * s);
            int gy = yBase - (int) Math.round(w * s);
            g2.draw(new Line2D.Double(gx, yTop, gx, yBase));
            g2.draw(new Line2D.Double(x0, gy, x0 + side, gy));
        }

        for (Particle p : particles) {
            Color fill;
            if (p.id() == selected) {
                fill = new Color(230, 57, 70);
            } else if (highlightNeighbors.contains(p.id())) {
                fill = new Color(69, 123, 157);
            } else {
                fill = new Color(215, 215, 215);
            }
            double cx = x0 + p.x() * s;
            double cy = yBase - p.y() * s;
            double d = 2 * p.r() * s;
            Ellipse2D disk = new Ellipse2D.Double(cx - p.r() * s, cy - p.r() * s, d, d);
            g2.setColor(fill);
            g2.fill(disk);
            g2.setColor(Color.DARK_GRAY);
            g2.draw(disk);
        }

        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        g2.setColor(Color.BLACK);
        g2.drawString("Click en una partícula para ver sus vecinos", x0, yTop - 10);
        g2.dispose();
    }
}
