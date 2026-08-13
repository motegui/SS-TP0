package ss.tp1;

/** Distancias entre discos, con y sin condiciones periódicas de contorno (PBC). */
public final class Geometry {

    private Geometry() {}

    /** Distancia borde a borde entre dos discos: ||centro_j - centro_i|| - r_i - r_j. */
    public static double edgeDistance(
            double xi, double yi, double ri,
            double xj, double yj, double rj,
            double L, boolean periodic) {
        double dx = xj - xi;
        double dy = yj - yi;
        if (periodic) {
            dx -= L * Math.round(dx / L);
            dy -= L * Math.round(dy / L);
        }
        double center = Math.hypot(dx, dy);
        return center - ri - rj;
    }

    /**
     * Tamaño mínimo de celda para que baste revisar una capa de celdas vecinas
     * (Cell Index Method) cuando las partículas tienen radio r_i > 0.
     *
     * Para partículas puntuales el criterio clásico es L/M > rc. Con radio,
     * el borde de una partícula puede estar en una celda vecina aunque su
     * centro esté en la celda actual, por lo que la condición pasa a ser:
     *
     *   L/M > rc + 2*max(r_i)
     *
     * (si max(r_i) -> 0 se recupera el criterio clásico L/M > rc).
     */
    public static double minCellSize(double rc, double maxRadius) {
        return rc + 2.0 * maxRadius;
    }
}
