package jpvm.messages;

import jpvm.Debug;
import jpvm.JPVMException;
import jpvm.tasks.TaskId;
import jpvm.buffer.Buffer;
import jpvm.connection.RecvConnection;
import jpvm.connection.SendConnection;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by Dmitry Butilov
 * on 14.12.17.
 */
public class Message {
    public int messageTag;
    public TaskId sourceTid;
    public TaskId destTid;
    public Buffer buffer;

    public Message(Buffer buf, TaskId dest, TaskId src, int tag) {
        messageTag = tag;
        sourceTid = src;
        destTid = dest;
        buffer = buf;
    }

    public Message(RecvConnection conn) throws JPVMException {
        messageTag = -1;
        sourceTid = null;
        destTid = null;
        buffer = null;
        recv(conn);
    }

    public void send(SendConnection conn) throws JPVMException {
        DataOutputStream strm = conn.dataOutputStream;
        try {
            strm.writeInt(messageTag);
            sourceTid.send(strm);
            destTid.send(strm);
            buffer.send(conn);
            strm.flush();
        } catch (IOException ioe) {
            Debug.note("Message, send - i/o exception");
            throw new JPVMException("Message, send - " + "i/o exception");
        }
    }

    public void recv(RecvConnection conn) throws JPVMException {
        DataInputStream strm = conn.dataInputStream;
        try {
            messageTag = strm.readInt();
            sourceTid = new TaskId();
            sourceTid.recv(strm);
            destTid = new TaskId();
            destTid.recv(strm);
            buffer = new Buffer();
            buffer.recv(conn);
        } catch (IOException ioe) {
            Debug.note("Message, recv - i/o exception");
            throw new JPVMException("Message, recv - " +
                    "i/o exception");
        }
    }
}