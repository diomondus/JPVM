package jpvm.buffer;

import jpvm.Debug;
import jpvm.JPVMException;
import jpvm.tasks.TaskId;
import jpvm.connection.RecvConnection;
import jpvm.connection.SendConnection;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Created by Dmitry Butilov
 * on 14.12.17.
 */
public class BufferElement {
    public BufferElementContents contents;
    public BufferElement next; // Linked structure
    private boolean inPlace;

    public BufferElement() {
        init();
        inPlace = false;
    }

    public BufferElement(boolean dataInPlace) {
        init();
        inPlace = dataInPlace;
    }

    public BufferElement(TaskId d[], int n, int stride) {
        init();
        contents = new BufferElementContents(d, n, stride, inPlace);
    }

    public BufferElement(TaskId d) {
        init();
        TaskId a[] = new TaskId[1];
        a[0] = d;
        contents = new BufferElementContents(a, 1, 1, true);
    }

    public BufferElement(byte d[], int n, int stride) {
        init();
        contents = new BufferElementContents(d, n, stride, inPlace);
    }

    public BufferElement(byte d) {
        init();
        byte a[] = new byte[1];
        a[0] = d;
        contents = new BufferElementContents(a, 1, 1, true);
    }

    public BufferElement(short d[], int n, int stride) {
        init();
        contents = new BufferElementContents(d, n, stride, inPlace);
    }

    public BufferElement(short d) {
        init();
        short a[] = new short[1];
        a[0] = d;
        contents = new BufferElementContents(a, 1, 1, true);
    }

    public BufferElement(char d[], int n, int stride) {
        init();
        contents = new BufferElementContents(d, n, stride, inPlace);
    }

    public BufferElement(char d) {
        init();
        char a[] = new char[1];
        a[0] = d;
        contents = new BufferElementContents(a, 1, 1, true);
    }

    public BufferElement(long d[], int n, int stride) {
        init();
        contents = new BufferElementContents(d, n, stride, inPlace);
    }


    public BufferElement(long d) {
        init();
        long a[] = new long[1];
        a[0] = d;
        contents = new BufferElementContents(a, 1, 1, true);
    }

    public BufferElement(int d[], int n, int stride) {
        init();
        contents = new BufferElementContents(d, n, stride, inPlace);
    }


    public BufferElement(int d) {
        init();
        int a[] = new int[1];
        a[0] = d;
        contents = new BufferElementContents(a, 1, 1, true);
    }

    public BufferElement(float d[], int n, int stride) {
        init();
        contents = new BufferElementContents(d, n, stride, inPlace);
    }

    public BufferElement(float d) {
        init();
        float a[] = new float[1];
        a[0] = d;
        contents = new BufferElementContents(a, 1, 1, true);
    }

    public BufferElement(double d[], int n, int stride) {
        init();
        contents = new BufferElementContents(d, n, stride, inPlace);
    }

    public BufferElement(double d) {
        init();
        double a[] = new double[1];
        a[0] = d;
        contents = new BufferElementContents(a, 1, 1, true);
    }

    public BufferElement(String str) {
        init();
        int n = str.length();
        char a[] = new char[n];
        str.getChars(0, n, a, 0);
        contents = new BufferElementContents(a, n, 1, true);
        contents.dataType = BufferDataType.jpvmString;
    }

    public void init() {
        contents = null;
        next = null;
    }

    public void unpack(int d[], int n, int stride) throws JPVMException {
        contents.unpack(d, n, stride);
    }

    public void unpack(short d[], int n, int stride) throws JPVMException {
        contents.unpack(d, n, stride);
    }

    public void unpack(byte d[], int n, int stride) throws JPVMException {
        contents.unpack(d, n, stride);
    }

    public void unpack(char d[], int n, int stride) throws JPVMException {
        contents.unpack(d, n, stride);
    }

    public void unpack(long d[], int n, int stride) throws JPVMException {
        contents.unpack(d, n, stride);
    }

    public void unpack(double d[], int n, int stride) throws JPVMException {
        contents.unpack(d, n, stride);
    }

    public void unpack(float d[], int n, int stride) throws JPVMException {
        contents.unpack(d, n, stride);
    }

    public void unpack(TaskId d[], int n, int stride)
            throws JPVMException {
        contents.unpack(d, n, stride);
    }

    public String unpack() throws JPVMException {
        return contents.unpack();
    }

    public void send(SendConnection conn) throws JPVMException {
        int i;
        try {
            ObjectOutputStream out;
            out = new ObjectOutputStream(conn.dataOutputStream);
            out.writeObject(contents);
            //out.flush();
        } catch (IOException ioe) {
            System.err.println("I/O exception - " + ioe);
            Debug.note("BufferElement, " +
                    "send - i/o exception");
            throw new JPVMException("BufferElement, " +
                    "send - i/o exception");
        }
    }

    public void recv(RecvConnection conn) throws JPVMException {
        int i;
        try {
            ObjectInputStream in;
            in = new ObjectInputStream(conn.dataInputStream);
            try {
                contents = (BufferElementContents) in.readObject();
            } catch (ClassNotFoundException cnf) {
                throw new JPVMException("BufferElement, " +
                        "recv - can't find class " +
                        "BufferElementContents");
            }
        } catch (IOException ioe) {
            Debug.note("BufferElement, " +
                    "recv - i/o exception");
            throw new JPVMException("BufferElement, " +
                    "recv - i/o exception");
        }
    }
}
