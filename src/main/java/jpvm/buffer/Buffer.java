package jpvm.buffer;

import jpvm.Debug;
import jpvm.JPVMException;
import jpvm.tasks.TaskId;
import jpvm.connection.RecvConnection;
import jpvm.connection.SendConnection;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by Dmitry Butilov
 * on 14.12.17.
 */
public class Buffer {
    private BufferElement list_head;
    private BufferElement list_tail;
    private BufferElement curr_elt;
    private int num_list_elts;

    public Buffer() {
        list_head = null;
        list_tail = null;
        num_list_elts = 0;
    }

    private void addElt(BufferElement nw) {
        num_list_elts++;
        if (list_head == null) {
            curr_elt = list_head = list_tail = nw;
            return;
        }
        list_tail.next = nw;
        list_tail = nw;
    }

    public void rewind() {
        curr_elt = list_head;
    }

    public void pack(int d[], int n, int stride) {
        BufferElement nw = new BufferElement(d, n, stride);
        addElt(nw);
    }

    public void pack(int d) {
        BufferElement nw = new BufferElement(d);
        addElt(nw);
    }

    public void pack(char d[], int n, int stride) {
        BufferElement nw = new BufferElement(d, n, stride);
        addElt(nw);
    }

    public void pack(char d) {
        BufferElement nw = new BufferElement(d);
        addElt(nw);
    }

    public void pack(short d[], int n, int stride) {
        BufferElement nw = new BufferElement(d, n, stride);
        addElt(nw);
    }

    public void pack(short d) {
        BufferElement nw = new BufferElement(d);
        addElt(nw);
    }

    public void pack(long d[], int n, int stride) {
        BufferElement nw = new BufferElement(d, n, stride);
        addElt(nw);
    }

    public void pack(long d) {
        BufferElement nw = new BufferElement(d);
        addElt(nw);
    }

    public void pack(byte d[], int n, int stride) {
        BufferElement nw = new BufferElement(d, n, stride);
        addElt(nw);
    }

    public void pack(byte d) {
        BufferElement nw = new BufferElement(d);
        addElt(nw);
    }

    public void pack(float d[], int n, int stride) {
        BufferElement nw = new BufferElement(d, n, stride);
        addElt(nw);
    }

    public void pack(float d) {
        BufferElement nw = new BufferElement(d);
        addElt(nw);
    }

    public void pack(double d[], int n, int stride) {
        BufferElement nw = new BufferElement(d, n, stride);
        addElt(nw);
    }

    public void pack(double d) {
        BufferElement nw = new BufferElement(d);
        addElt(nw);
    }

    public void pack(TaskId d[], int n, int stride) {
        BufferElement nw = new BufferElement(d, n, stride);
        addElt(nw);
    }

    public void pack(TaskId d) {
        BufferElement nw = new BufferElement(d);
        addElt(nw);
    }

    public void pack(String str) {
        BufferElement nw = new BufferElement(str);
        addElt(nw);
    }

    public void unpack(int d[], int n, int stride) throws JPVMException {
        if (curr_elt == null)
            throw new JPVMException("buffer empty, upkint.");
        curr_elt.unpack(d, n, stride);
        curr_elt = curr_elt.next;
    }

    public int upkint() throws JPVMException {
        int d[] = new int[1];
        unpack(d, 1, 1);
        return d[0];
    }

    public void unpack(byte d[], int n, int stride) throws JPVMException {
        if (curr_elt == null)
            throw new JPVMException("buffer empty, upkbyte.");
        curr_elt.unpack(d, n, stride);
        curr_elt = curr_elt.next;
    }

    public byte upkbyte() throws JPVMException {
        byte d[] = new byte[1];
        unpack(d, 1, 1);
        return d[0];
    }

    public void unpack(char d[], int n, int stride) throws JPVMException {
        if (curr_elt == null)
            throw new JPVMException("buffer empty, upkchar.");
        curr_elt.unpack(d, n, stride);
        curr_elt = curr_elt.next;
    }

    public char upkchar() throws JPVMException {
        char d[] = new char[1];
        unpack(d, 1, 1);
        return d[0];
    }

    public void unpack(short d[], int n, int stride) throws JPVMException {
        if (curr_elt == null)
            throw new JPVMException("buffer empty, upkshort.");
        curr_elt.unpack(d, n, stride);
        curr_elt = curr_elt.next;
    }

    public short upkshort() throws JPVMException {
        short d[] = new short[1];
        unpack(d, 1, 1);
        return d[0];
    }

    public void unpack(long d[], int n, int stride) throws JPVMException {
        if (curr_elt == null)
            throw new JPVMException("buffer empty, upklong.");
        curr_elt.unpack(d, n, stride);
        curr_elt = curr_elt.next;
    }

    public long upklong() throws JPVMException {
        long d[] = new long[1];
        unpack(d, 1, 1);
        return d[0];
    }

    public void unpack(float d[], int n, int stride) throws JPVMException {
        if (curr_elt == null)
            throw new JPVMException("buffer empty, upkfloat.");
        curr_elt.unpack(d, n, stride);
        curr_elt = curr_elt.next;
    }

    public float upkfloat() throws JPVMException {
        float d[] = new float[1];
        unpack(d, 1, 1);
        return d[0];
    }

    public void unpack(double d[], int n, int stride) throws JPVMException {
        if (curr_elt == null)
            throw new JPVMException("buffer empty, upkdouble.");
        curr_elt.unpack(d, n, stride);
        curr_elt = curr_elt.next;
    }

    public double upkdouble() throws JPVMException {
        double d[] = new double[1];
        unpack(d, 1, 1);
        return d[0];
    }

    public void unpack(TaskId d[], int n, int stride)
            throws JPVMException {
        if (curr_elt == null)
            throw new JPVMException("buffer empty, upktid.");
        curr_elt.unpack(d, n, stride);
        curr_elt = curr_elt.next;
    }

    public TaskId upktid() throws JPVMException {
        TaskId d[] = new TaskId[1];
        unpack(d, 1, 1);
        return d[0];
    }

    public String upkstr() throws JPVMException {
        if (curr_elt == null)
            throw new JPVMException("buffer empty, upkstring.");
        String ret = curr_elt.unpack();
        curr_elt = curr_elt.next;
        return ret;
    }

    public void send(SendConnection conn) throws JPVMException {
        DataOutputStream dataOutputStream = conn.dataOutputStream;
        BufferElement tmp = list_head;
        try {
            dataOutputStream.writeInt(num_list_elts);
            while (tmp != null) {
                tmp.send(conn);
                tmp = tmp.next;
            }
        } catch (IOException ioe) {
            Debug.note("Buffer, send - i/o exception");
            throw new JPVMException("Buffer, send - " + "i/o exception");
        }
    }

    public void recv(RecvConnection conn) throws JPVMException {
        int i, N;
        BufferElement tmp;

        DataInputStream dataInputStream = conn.dataInputStream;
        try {
            N = dataInputStream.readInt();
            Debug.note("Buffer, recv " + N +
                    " buffer elements.");
            for (i = 0; i < N; i++) {
                tmp = new BufferElement();
                tmp.recv(conn);
                addElt(tmp);
            }
        } catch (IOException ioe) {
            Debug.note("Buffer, recv - i/o exception");
            throw new JPVMException("Buffer, recv - " +
                    "i/o exception");
        }
    }
}
