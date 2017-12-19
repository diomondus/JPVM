package jpvm.connection;

import jpvm.Debug;
import jpvm.JPVMException;
import jpvm.tasks.TaskId;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

/**
 * Created by Dmitry Butilov
 * on 14.12.17.
 */
public class RecvConnection {
    private InputStream inputStream;
    public DataInputStream dataInputStream;
    public TaskId tid;

    public RecvConnection(Socket socket) {
        if (socket == null) return;
        try {
            inputStream = socket.getInputStream();
            inputStream = new BufferedInputStream(inputStream);
            dataInputStream = new DataInputStream(inputStream);
            tid = new TaskId();
            try {
                tid.recv(dataInputStream);
            } catch (JPVMException jpe) {
                dataInputStream = null;
                tid = null;
                Debug.error("RecvConnection, internal" +
                        " error");
            }
            Debug.note("RecvConnection, connect to "
                    + tid.toString() + " established");
        } catch (IOException ioe) {
            dataInputStream = null;
            tid = null;
            Debug.error("RecvConnection, i/o exception");
        }
        if (dataInputStream == null) return;
    }

    public boolean hasMessagesQueued() {
        boolean ret = false;
        if (inputStream != null) {
            try {
                if (inputStream.available() > 0)
                    ret = true;
            } catch (IOException ioe) {
                ret = false;
                Debug.error("RecvConnection, " + "hasMessagesQueued - i/o exception");
            }
        }
        return ret;
    }
}