package ss.tp1;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * App interactiva (punto 1 y 2 del enunciado): click en una partícula -> se
 * colorean sus vecinos y se muestra a la derecha el cálculo del CIM paso a paso.
 */
public final class InteractiveApp extends JFrame {

    private double L = 20;
    private double rc = 1.0;
    private int M = 10;
    private boolean periodic = false;
    private long seed = 42;
    private List<Particle> particles = List.of();
    private int selectedId = -1;

    private final ParticleCanvas canvas = new ParticleCanvas();
    private final CalculationPanel calcPanel = new CalculationPanel();
    private final JLabel status = new JLabel(" ");

    private final JSpinner spN = new JSpinner(new SpinnerNumberModel(150, 5, 3000, 5));
    private final JSpinner spM = new JSpinner(new SpinnerNumberModel(10, 1, 300, 1));
    private final JSpinner spRc = new JSpinner(new SpinnerNumberModel(1.0, 0.1, 10.0, 0.1));
    private final JCheckBox cbPeriodic = new JCheckBox("Condiciones periódicas", false);

    public InteractiveApp() {
        super("TP1 - Cell Index Method interactivo (Java)");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        top.add(new JLabel("N:"));
        top.add(spN);
        top.add(new JLabel("M:"));
        top.add(spM);
        top.add(new JLabel("rc:"));
        top.add(spRc);
        top.add(cbPeriodic);

        JButton regen = new JButton("Regenerar");
        regen.addActionListener(e -> regenerate());
        top.add(regen);

        JButton load = new JButton("Cargar data/");
        load.addActionListener(e -> loadFromData());
        top.add(load);

        JButton exportFig = new JButton("Exportar figura");
        exportFig.addActionListener(e -> exportFigure());
        top.add(exportFig);

        add(top, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(8, 0));
        canvas.setPreferredSize(new Dimension(620, 620));
        calcPanel.setPreferredSize(new Dimension(440, 620));
        center.add(canvas, BorderLayout.CENTER);
        center.add(calcPanel, BorderLayout.EAST);
        add(center, BorderLayout.CENTER);

        add(status, BorderLayout.SOUTH);

        canvas.setOnSelect(this::onParticleClicked);

        spM.addChangeListener(e -> {
            M = (Integer) spM.getValue();
            canvas.setGridM(M);
            canvas.clearSelection();
            selectedId = -1;
            calcPanel.showIdle();
        });
        spRc.addChangeListener(e -> {
            rc = (Double) spRc.getValue();
            canvas.clearSelection();
            selectedId = -1;
            calcPanel.showIdle();
        });
        cbPeriodic.addActionListener(e -> {
            periodic = cbPeriodic.isSelected();
            canvas.clearSelection();
            selectedId = -1;
            calcPanel.showIdle();
            status.setText(periodic ? "Modo: condiciones periódicas" : "Modo: paredes");
        });

        regenerate();
        pack();
        setLocationRelativeTo(null);
    }

    private void regenerate() {
        int N = (Integer) spN.getValue();
        M = (Integer) spM.getValue();
        rc = (Double) spRc.getValue();
        periodic = cbPeriodic.isSelected();
        try {
            particles = ParticleGenerator.generate(N, L, seed++);
            selectedId = -1;
            canvas.setSystem(L, M, particles);
            calcPanel.showIdle();
            status.setText("Sistema nuevo: N=" + N + ", L=" + L + ", M=" + M + ". Click en una partícula.");
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error al generar", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadFromData() {
        Path base = Path.of("data");
        if (!base.resolve("static.txt").toFile().exists()) {
            base = Path.of("..", "data");
        }
        try {
            SystemFiles.LoadedSystem sys = SystemFiles.load(base.resolve("static.txt"), base.resolve("dynamic.txt"));
            L = sys.L();
            particles = sys.particles();
            selectedId = -1;
            M = (Integer) spM.getValue();
            rc = (Double) spRc.getValue();
            periodic = cbPeriodic.isSelected();
            spN.setValue(particles.size());
            canvas.setSystem(L, M, particles);
            calcPanel.showIdle();
            status.setText("Cargado desde " + base.toAbsolutePath());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "No se pudo cargar", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onParticleClicked(int id) {
        M = (Integer) spM.getValue();
        rc = (Double) spRc.getValue();
        periodic = cbPeriodic.isSelected();
        try {
            CellIndexMethod cim = new CellIndexMethod(particles, L, M, rc, periodic);
            CellIndexMethod.ParticleQuery q = cim.queryParticle(id);
            selectedId = id;
            canvas.setGridM(M);
            canvas.setSelection(id, q.neighbors());
            calcPanel.showQuery(particles.get(id), L, M, rc, periodic, q);
            status.setText("Partícula " + id + ": " + q.neighbors().size() + " vecinos (d_borde < " + rc + ")");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "M inválido", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void exportFigure() {
        try {
            Path outDir = Path.of("out");
            Files.createDirectories(outDir);
            int focus = selectedId >= 0 ? selectedId : 0;
            CellIndexMethod cim = new CellIndexMethod(particles, L, M, rc, periodic);
            CellIndexMethod.ParticleQuery q = cim.queryParticle(focus);
            Path fig = outDir.resolve("neighbors_gui.png");
            NeighborRenderer.render(particles, L, focus, q.neighbors(), periodic, fig);
            status.setText("Figura exportada a " + fig.toAbsolutePath());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error al exportar", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new InteractiveApp().setVisible(true));
    }
}
