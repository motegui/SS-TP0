package ss.tp1;

/** Media y desvío estándar muestral. */
public final class Stats {
    private Stats() {}

    public static double mean(double[] a) {
        double s = 0;
        for (double v : a) {
            s += v;
        }
        return s / a.length;
    }

    public static double stddev(double[] a, double mean) {
        if (a.length < 2) {
            return 0.0;
        }
        double s = 0;
        for (double v : a) {
            s += (v - mean) * (v - mean);
        }
        return Math.sqrt(s / (a.length - 1));
    }

    public static double minPositive(double[] a) {
        double m = Double.MAX_VALUE;
        for (double v : a) {
            if (v > 0) {
                m = Math.min(m, v);
            }
        }
        return m == Double.MAX_VALUE ? 1e-9 : m;
    }

    public static double max(double[] a) {
        double m = -Double.MAX_VALUE;
        for (double v : a) {
            m = Math.max(m, v);
        }
        return m;
    }
}
