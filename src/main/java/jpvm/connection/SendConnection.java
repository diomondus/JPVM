package jpvm.connection;

import jpvm.Debug;
import jpvm.JPVMException;
import jpvm.tasks.TaskId;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Created by Dmitry Butilov
 * on 14.12.17.
 */
public class SendConnection {
    public DataOutputStream dataOutputStream;
    public TaskId tid;

    public SendConnection(Socket sock, TaskId t) {
        if (sock == null || t == null) return;
        tid = t;
        try {
            OutputStream outputStream = sock.getOutputStream();
            outputStream = new BufferedOutputStream(outputStream);
            dataOutputStream = new DataOutputStream(outputStream);
        } catch (IOException ioe) {
            dataOutputStream = null;
            tid = null;
            Debug.error("SendConnection, i/o exception");
        }
    }

    public static SendConnection connect(TaskId t, TaskId f)
            throws JPVMException {
        SendConnection ret = null;
        try {
            Debug.note("SendConnection, " +
                    "connecting to " + t.toString());

            // Make the new connection...
            Socket sock = new Socket(t.getHost(), t.getPort());
            ret = new SendConnection(sock, t);

            // Send my identity to the newly connected task...
            f.send(ret.dataOutputStream);
            ret.dataOutputStream.flush();
        } catch (IOException ioe) {
            Debug.note("SendConnection, connect - " + " i/o exception");
            throw new JPVMException("SendConnection, connect - " + " i/o exception: \"" + ioe + "\"");
        }
        return ret;
    }
}