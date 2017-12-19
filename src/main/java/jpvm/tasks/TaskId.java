package jpvm.tasks;

import jpvm.Debug;
import jpvm.JPVMException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by Dmitry Butilov
 * on 14.12.17.
 */
public class TaskId implements Serializable {
    private String taskHost;
    private int taskConnectPort;

    public TaskId() {
        taskHost = null;
        taskConnectPort = 0;
    }

    public TaskId(int my_port) {
        taskHost = null;
        taskConnectPort = 0;
        try {
            InetAddress taskAddr = InetAddress.getLocalHost();
            taskHost = taskAddr.getHostName();
            taskConnectPort = my_port;
        } catch (UnknownHostException uhe) {
            Debug.error("TaskId, unknown host exception");
        }
    }

    public TaskId(String host, int port) {
        taskHost = host;
        taskConnectPort = port;
    }

    public String getHost() {
        return taskHost;
    }

    public int getPort() {
        return taskConnectPort;
    }

    public String toString() {
        return ((taskHost != null ? taskHost : "(null)") + ", port #" + taskConnectPort);
    }

    public boolean equals(TaskId tid) {
        return tid != null && taskConnectPort == tid.taskConnectPort && tid.taskHost != null
                && tid.taskHost.equalsIgnoreCase(taskHost);
    }

    public void send(DataOutputStream stream) throws JPVMException {
        int i;
        try {
            int len = 0;
            if (taskHost != null) {
                len = taskHost.length();
                stream.writeInt(len);
                char hname[] = new char[len];
                taskHost.getChars(0, len, hname, 0);
                for (i = 0; i < len; i++) {
                    stream.writeChar(hname[i]);
                }
            } else {
                stream.writeInt(len);
            }
            stream.writeInt(taskConnectPort);
        } catch (IOException ioe) {
            Debug.note("TaskId, send - i/o exception");
            throw new JPVMException("TaskId, send - i/o exception");
        }
    }

    public void recv(DataInputStream stream) throws JPVMException {
        int i;
        try {
            int len = stream.readInt();
            if (len > 0) {
                char hname[] = new char[len];
                for (i = 0; i < len; i++) {
                    hname[i] = stream.readChar();
                }
                taskHost = new String(hname);
            } else {
                taskHost = null;
            }
            taskConnectPort = stream.readInt();
        } catch (IOException ioe) {
            Debug.note("TaskId, recv - i/o exception");
            throw new JPVMException("TaskId, recv - i/o exception");
        }
    }
}
