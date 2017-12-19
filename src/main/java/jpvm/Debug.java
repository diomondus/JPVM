package jpvm;

/**
 * Created by Dmitry Butilov
 * on 14.12.17.
 */
public final class Debug {
    public static final boolean on = false;

    public static void note(String message) {
        if (on) {
            System.out.println("Debug: " + message);
            System.out.flush();
        }
    }

    public static void error(String message) {
        System.err.println("Debug: " + message);
        System.err.flush();
        System.exit(1);
    }
}
