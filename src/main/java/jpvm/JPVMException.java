package jpvm;

/**
 * Created by Dmitry Butilov
 * on 14.12.17.
 */
public class JPVMException extends Exception {
    private String val;

    public JPVMException() {
        val = "jpvm error: unknown error.";
    }

    public JPVMException(String str) {
        val = "jpvm error: " + str;
    }

    public String toString() {
        return val;
    }
}
