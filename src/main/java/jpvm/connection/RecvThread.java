package jpvm.connection;

import jpvm.Debug;
import jpvm.JPVMException;
import jpvm.connection.RecvConnection;
import jpvm.messages.Message;
import jpvm.messages.MessageQueue;

/**
 * Created by Dmitry Butilov
 * on 14.12.17.
 */
public class RecvThread extends Thread {
    private RecvConnection conn;
    private MessageQueue queue;
    int my_num;
    static int num = 0;


    public RecvThread(RecvConnection c, MessageQueue q) {
        conn = c;
        queue = q;
        num++;
        my_num = num;
    }

    public void run() {
        boolean alive = true;
        while (alive) {
            try {
                Debug.note("RecvThread (" + my_num + ") - blocking " + "for a message.");
                Message nw = new Message(conn);
                Debug.note("RecvThread (" + my_num + ") - got a " + "new message.");
                queue.enqueue(nw);
                Thread.yield();
            } catch (JPVMException jpe) {
                Debug.note("RecvThread, " + "connection closed");
                alive = false;
            }
        }
    }
}
