package jpvm.daemon;

/**
 * Created by Dmitry Butilov
 * on 14.12.17.
 */
public class DaemonWaiter extends Thread {
    private Process process;

    DaemonWaiter(Process p) {
        process = p;
    }

    public void run() {
        boolean wait = true;
        while (wait) {
            try {
                process.waitFor();
                wait = false;
            } catch (InterruptedException ignored) {

            }
        }
    }
}