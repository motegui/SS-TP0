package ss.tp1;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

import javax.imageio.ImageIO;

/** Gráfico XY minimalista (líneas + barras de error, escalas lineal/log) sin dependencias externas. */
public final class SimpleChart {

    public record Series(String label, Color color, double[] x, double[] y, double[] yErr) {}

    private static final int W = 900;
    private static final int H = 620;
    private static final int PAD_L = 95;
    private static final int PAD_R = 40;
    private static final int PAD_T = 60;
    private static final int PAD_B = 70;

    private SimpleChart() {}

    public static void save(
            Path path, String title, String xLabel, String yLabel,
            boolean logX, boolean logY, List<Series> series) throws IOException {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, W, H);

        double[] bounds = computeBounds(series, logX, logY);
        double xMin = bounds[0], xMax = bounds[1], yMin = bounds[2], yMax = bounds[3];

        int plotW = W - PAD_L - PAD_R;
        int plotH = H - PAD_T - PAD_B;

        g.setColor(Color.BLACK);
        g.drawRect(PAD_L, PAD_T, plotW, plotH);

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        FontMetrics fmTitle = g.getFontMetrics();
        g.drawString(title, W / 2 - fmTitle.stringWidth(title) / 2, 28);

        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        String xLab = xLabel + (logX ? " (escala log)" : "");
        String yLab = yLabel + (logY ? " (escala log)" : "");
        FontMetrics fm = g.getFontMetrics();
        g.drawString(xLab, W / 2 - fm.stringWidth(xLab) / 2, H - 18);
        Graphics2D gRot = (Graphics2D) g.create();
        gRot.rotate(-Math.PI / 2);
        gRot.drawString(yLab, -(H / 2 + fm.stringWidth(yLab) / 2), 22);
        gRot.dispose();

        final double fXMin = xMin, fXMax = xMax, fYMin = yMin, fYMax = yMax;
        DoubleUnaryOperator toPx = xv -> PAD_L + (xv - fXMin) / (fXMax - fXMin) * plotW;
        DoubleUnaryOperator toPy = yv -> PAD_T + plotH - (yv - fYMin) / (fYMax - fYMin) * plotH;

        g.setColor(new Color(235, 235, 235));
        for (int i = 0; i <= 5; i++) {
            double fx = fXMin + i * (fXMax - fXMin) / 5;
            double fy = fYMin + i * (fYMax - fYMin) / 5;
            int px = (int) Math.round(toPx.applyAsDouble(fx));
            int py = (int) Math.round(toPy.applyAsDouble(fy));
            g.draw(new Line2D.Double(px, PAD_T, px, PAD_T + plotH));
            g.draw(new Line2D.Double(PAD_L, py, PAD_L + plotW, py));
        }
        g.setColor(Color.BLACK);
        for (int i = 0; i <= 5; i++) {
            double fx = fXMin + i * (fXMax - fXMin) / 5;
            double fy = fYMin + i * (fYMax - fYMin) / 5;
            int px = (int) Math.round(toPx.applyAsDouble(fx));
            int py = (int) Math.round(toPy.applyAsDouble(fy));
            double xv = logX ? Math.pow(10, fx) : fx;
            double yv = logY ? Math.pow(10, fy) : fy;
            g.drawString(fmt(xv), px - 15, PAD_T + plotH + 15);
            g.drawString(fmt(yv), PAD_L - 75, py + 4);
        }

        int legendY = PAD_T + 18;
        for (Series s : series) {
            double[] xs = s.x();
            double[] ys = s.y();
            double[] err = s.yErr();
            Integer prevPx = null;
            Integer prevPy = null;
            g.setColor(s.color());
            for (int i = 0; i < xs.length; i++) {
                double xv = logX ? Math.log10(Math.max(xs[i], 1e-12)) : xs[i];
                double yv = logY ? Math.log10(Math.max(ys[i], 1e-12)) : ys[i];
                int px = (int) Math.round(toPx.applyAsDouble(xv));
                int py = (int) Math.round(toPy.applyAsDouble(yv));

                if (err != null && err[i] > 0) {
                    double floor = logY ? Math.max(ys[i] * 1e-2, 1e-12) : -Double.MAX_VALUE;
                    double lowRaw = Math.max(ys[i] - err[i], floor);
                    double highRaw = ys[i] + err[i];
                    double lowV = logY ? Math.log10(Math.max(lowRaw, 1e-12)) : lowRaw;
                    double highV = logY ? Math.log10(Math.max(highRaw, 1e-12)) : highRaw;
                    int pyLow = (int) Math.round(toPy.applyAsDouble(lowV));
                    int pyHigh = (int) Math.round(toPy.applyAsDouble(highV));
                    g.drawLine(px, pyLow, px, pyHigh);
                    g.drawLine(px - 3, pyLow, px + 3, pyLow);
                    g.drawLine(px - 3, pyHigh, px + 3, pyHigh);
                }
                g.fillOval(px - 3, py - 3, 6, 6);
                if (prevPx != null) {
                    g.drawLine(prevPx, prevPy, px, py);
                }
                prevPx = px;
                prevPy = py;
            }
            g.fillRect(PAD_L + plotW - 180, legendY - 9, 10, 10);
            g.drawString(s.label(), PAD_L + plotW - 165, legendY);
            legendY += 18;
        }

        g.dispose();
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        ImageIO.write(img, "png", path.toFile());
    }

    private static double[] computeBounds(List<Series> series, boolean logX, boolean logY) {
        double xMin = Double.MAX_VALUE, xMax = -Double.MAX_VALUE;
        double yMin = Double.MAX_VALUE, yMax = -Double.MAX_VALUE;
        for (Series s : series) {
            for (int i = 0; i < s.x().length; i++) {
                double xv = logX ? Math.log10(Math.max(s.x()[i], 1e-12)) : s.x()[i];
                xMin = Math.min(xMin, xv);
                xMax = Math.max(xMax, xv);
                double err = s.yErr() != null ? s.yErr()[i] : 0.0;
                // Si el desvío es mayor que la media (N chico, pocas repeticiones), evitar que
                // la barra inferior colapse a un valor absurdo en escala log: no bajar de un
                // 1% del propio valor de la serie en ese punto.
                double floor = logY ? Math.max(s.y()[i] * 1e-2, 1e-12) : -Double.MAX_VALUE;
                double lowRaw = Math.max(s.y()[i] - err, floor);
                double highRaw = s.y()[i] + err;
                double lowV = logY ? Math.log10(Math.max(lowRaw, 1e-12)) : lowRaw;
                double highV = logY ? Math.log10(Math.max(highRaw, 1e-12)) : highRaw;
                yMin = Math.min(yMin, lowV);
                yMax = Math.max(yMax, highV);
            }
        }
        if (xMin == xMax) {
            xMin -= 1;
            xMax += 1;
        }
        if (yMin == yMax) {
            yMin -= 1;
            yMax += 1;
        }
        double xPad = (xMax - xMin) * 0.06;
        double yPad = (yMax - yMin) * 0.10;
        return new double[] {xMin - xPad, xMax + xPad, yMin - yPad, yMax + yPad};
    }

    private static String fmt(double v) {
        if (Math.abs(v) >= 1000 || (Math.abs(v) < 0.001 && v != 0)) {
            return String.format("%.1e", v);
        }
        return String.format("%.3g", v);
    }

    static double[] flatten(List<double[]> arrs) {
        return arrs.stream().flatMapToDouble(Arrays::stream).toArray();
    }
}
