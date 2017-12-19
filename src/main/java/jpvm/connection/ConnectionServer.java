package jpvm.connection;

import jpvm.Debug;
import jpvm.messages.MessageQueue;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Dmitry Butilov
 * on 14.12.17.
 */
public class ConnectionServer extends Thread {
    private ServerSocket connectionSock;
    private int connectionPort;
    private ConnectionSet connectionSet;
    private MessageQueue queue;

    public ConnectionServer(ConnectionSet c, MessageQueue q) {
        connectionSet = c;
        connectionSock = null;
        connectionPort = 0;
        queue = q;
        try {
            connectionSock = new ServerSocket(0);
            connectionPort = connectionSock.getLocalPort();
        } catch (IOException ioe) {
            Debug.error("ConnectionServer, i/o exception");
        }
    }

    public int getConnectionPort() {
        return connectionPort;
    }

    public void run() {
        while (true) {
            try {
                Debug.note("ConnectionServer, blocking on port " + connectionSock.getLocalPort());
                Socket newConnSock = connectionSock.accept();
                Debug.note("ConnectionServer, new connection.");
                RecvConnection nw = new RecvConnection(newConnSock);
                connectionSet.insertRecvConnection(nw);

                // Start a thread to recv on this pipe
                RecvThread rt = new RecvThread(nw, queue);
                rt.start();
            } catch (IOException ioe) {
                Debug.error("ConnectionServer, run - " + "i/o exception");
            }
        }
    }
}