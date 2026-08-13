package ss.tp1;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/** Panel de texto: muestra el cálculo paso a paso del CIM para la partícula clickeada. */
public class CalculationPanel extends JPanel {

    private final JTextArea text = new JTextArea();

    public CalculationPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel title = new JLabel("Cálculos CIM (partícula seleccionada)");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        add(title, BorderLayout.NORTH);

        text.setEditable(false);
        text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        add(new JScrollPane(text), BorderLayout.CENTER);
        showIdle();
    }

    public void showIdle() {
        text.setText(
                "Seleccioná una partícula en el dibujo (click).\n\n"
                        + "Se va a mostrar:\n"
                        + "  - Celda (cx, cy) del centro y tamaño de celda h = L/M\n"
                        + "  - Capas de celdas vecinas escaneadas\n"
                        + "  - Para cada candidato j: dx, dy, distancia entre centros,\n"
                        + "    distancia borde-borde = ||d|| - r_i - r_j, y si es < rc -> vecino\n"
                        + "  - Lista final de vecinos y tiempo de la consulta");
        text.setCaretPosition(0);
    }

    public void showQuery(
            Particle p, double L, int M, double rc, boolean periodic,
            CellIndexMethod.ParticleQuery q) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Partícula i = ").append(p.id()).append(" ===\n");
        sb.append(String.format("Posición: (%.4f, %.4f)   radio r_i = %.4f%n", p.x(), p.y(), p.r()));
        sb.append(String.format(
                "Parámetros: L=%.2f  M=%d  rc=%.2f  %s%n%n",
                L, M, rc, periodic ? "BC periódicas" : "Paredes"));

        sb.append("--- Grilla (Cell Index Method) ---\n");
        sb.append(String.format("h = L/M = %.6f%n", q.h()));
        sb.append(String.format("Celda del centro: (cx=%d, cy=%d)%n", q.homeCell().cx(), q.homeCell().cy()));
        sb.append(String.format("Capas de celdas vecinas revisadas: %d%n", q.layers()));
        String cells = q.scannedCells().stream()
                .map(c -> "(" + c.cx() + "," + c.cy() + ")")
                .collect(Collectors.joining(" "));
        sb.append("Celdas visitadas: ").append(cells).append("\n\n");

        sb.append("--- Criterio borde-borde ---\n");
        sb.append("d_borde = ||centro_j - centro_i|| - r_i - r_j\n");
        sb.append("j es vecino de i  <=>  d_borde < rc\n\n");

        sb.append("--- Candidatos evaluados (").append(q.candidates().size()).append(") ---\n");
        if (q.candidates().isEmpty()) {
            sb.append("(ninguno)\n");
        } else {
            for (CellIndexMethod.CandidateResult c : q.candidates()) {
                sb.append(String.format(
                        "j=%d  celda(%d,%d)%n"
                                + "  dx=%+.4f  dy=%+.4f  ||d||=%.4f%n"
                                + "  d_borde = %.4f - r_i - r_j = %.4f   %s rc  ->  %s%n%n",
                        c.j(), c.fromCell().cx(), c.fromCell().cy(),
                        c.dx(), c.dy(), c.centerDist(),
                        c.centerDist(), c.edgeDist(),
                        c.neighbor() ? "<" : ">=",
                        c.neighbor() ? "VECINO" : "no vecino"));
            }
        }

        sb.append("--- Resultado ---\n");
        sb.append("Vecinos de ").append(p.id()).append(": ").append(formatList(q.neighbors())).append("\n");
        sb.append(String.format("Tiempo de esta consulta: %.4f ms%n", q.elapsedNanos() / 1_000_000.0));

        text.setText(sb.toString());
        text.setCaretPosition(0);
    }

    private static String formatList(List<Integer> ids) {
        if (ids.isEmpty()) {
            return "[]";
        }
        return ids.stream().map(String::valueOf).collect(Collectors.joining(", ", "[", "]"));
    }
}
