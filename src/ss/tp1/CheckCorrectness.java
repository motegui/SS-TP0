package ss.tp1;

import java.util.ArrayList;
import java.util.List;

/**
 * Verificación de correctitud del CIM: para todo M válido el resultado debe
 * coincidir exactamente con la fuerza bruta (M=1), con y sin condiciones
 * periódicas. Chequea además la simetría de la relación de vecindad y que la
 * consulta por partícula (usada por la GUI) coincida con el cálculo global.
 *
 * Uso: ./run_check.sh
 */
public final class CheckCorrectness {

    public static void main(String[] args) {
        double L = 20.0;
        double rc = 1.0;
        int fails = 0;

        // 1) CIM vs fuerza bruta, para todo M válido
        for (boolean periodic : new boolean[] {false, true}) {
            for (int N : new int[] {50, 200, 400}) {
                List<Particle> ps = ParticleGenerator.generate(N, L, 42 + N);
                double maxR = ps.stream().mapToDouble(Particle::r).max().orElse(0);
                int mMax = CellIndexMethod.maxM(L, rc, maxR);
                List<List<Integer>> ref = CellIndexMethod.run(ps, L, 1, rc, periodic).neighbors();
                for (int M = 2; M <= mMax; M++) {
                    List<List<Integer>> got = CellIndexMethod.run(ps, L, M, rc, periodic).neighbors();
                    if (!ref.equals(got)) {
                        fails++;
                        System.out.printf("FALLA: periodic=%s N=%d M=%d difiere de fuerza bruta%n",
                                periodic, N, M);
                        for (int i = 0, shown = 0; i < ref.size() && shown < 3; i++) {
                            if (!ref.get(i).equals(got.get(i))) {
                                System.out.println("   i=" + i + " bruta=" + ref.get(i) + " cim=" + got.get(i));
                                shown++;
                            }
                        }
                    }
                }
                System.out.printf("OK  periodic=%-5s N=%-4d  M=2..%d coinciden con fuerza bruta%n",
                        periodic, N, mMax);
            }
        }

        List<Particle> ps = ParticleGenerator.generate(300, L, 7);
        int mMax = CellIndexMethod.maxM(L, rc, ParticleGenerator.R_MAX);

        // 2) Simetría: si j es vecino de i, i debe ser vecino de j
        List<List<Integer>> nb = CellIndexMethod.run(ps, L, mMax, rc, true).neighbors();
        int asym = 0;
        for (int i = 0; i < nb.size(); i++) {
            for (int j : nb.get(i)) {
                if (!nb.get(j).contains(i)) {
                    asym++;
                }
            }
        }
        if (asym > 0) {
            System.out.println("FALLA: " + asym + " pares asimétricos");
            fails++;
        } else {
            System.out.println("OK  relación de vecindad simétrica");
        }

        // 3) queryParticle (GUI) vs computeAll
        for (boolean periodic : new boolean[] {false, true}) {
            CellIndexMethod cim = new CellIndexMethod(ps, L, mMax, rc, periodic);
            List<List<Integer>> all = cim.computeAll();
            int diffs = 0;
            for (int i = 0; i < ps.size(); i++) {
                if (!new ArrayList<>(cim.queryParticle(i).neighbors()).equals(all.get(i))) {
                    diffs++;
                }
            }
            if (diffs > 0) {
                System.out.println("FALLA: queryParticle difiere de computeAll en " + diffs
                        + " partículas (periodic=" + periodic + ")");
                fails++;
            } else {
                System.out.println("OK  queryParticle == computeAll (periodic=" + periodic + ")");
            }
        }

        // 4) maxM debe devolver siempre un M aceptado por el constructor
        for (double r : new double[] {0.0, 0.1, 0.26, 0.5}) {
            int m = CellIndexMethod.maxM(L, rc, r);
            if (m >= 2) {
                List<Particle> pts = List.of(new Particle(0, 1, 1, r), new Particle(1, 5, 5, r));
                try {
                    new CellIndexMethod(pts, L, m, rc, false);
                    System.out.printf("OK  maxM(L=%.0f, rc=%.0f, r=%.2f)=%d es válido%n", L, rc, r, m);
                } catch (IllegalArgumentException e) {
                    System.out.printf("FALLA: maxM devolvió M=%d inválido para r=%.2f: %s%n", m, r, e.getMessage());
                    fails++;
                }
            }
        }

        System.out.println(fails == 0 ? "\nTODO OK" : "\n" + fails + " FALLAS");
        if (fails > 0) {
            System.exit(1);
        }
    }
}
