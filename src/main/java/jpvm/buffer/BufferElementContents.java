package jpvm.buffer;

import jpvm.JPVMException;
import jpvm.tasks.TaskId;

import java.io.Serializable;

/**
 * Created by Dmitry Butilov
 * on 14.12.17.
 */
public class BufferElementContents implements Serializable {
    public int dataType;
    public int arraySize;
    public byte byteArray[];
    public char charArray[];
    public int intArray[];
    public short shortArray[];
    public long longArray[];
    public float floatArray[];
    public double doubleArray[];
    public TaskId taskArray[];

    public BufferElementContents(TaskId d[], int n, int stride, boolean inPlace) {
        init();
        dataType = BufferDataType.jpvmTid;
        if (stride == 1) {
            if (inPlace)
                taskArray = d;
            else {
                taskArray = new TaskId[n];
                System.arraycopy(d, 0, taskArray, 0, n);
            }
        } else {
            taskArray = new TaskId[n];
            int i, j;
            for (i = 0, j = 0; i < n; i++, j += stride)
                taskArray[i] = d[j];
        }
    }

    public BufferElementContents(short d[], int n, int stride, boolean inPlace) {
        init();
        dataType = BufferDataType.jpvmShort;
        if (stride == 1) {
            if (inPlace)
                shortArray = d;
            else {
                shortArray = new short[n];
                System.arraycopy(d, 0, shortArray, 0, n);
            }
        } else {
            shortArray = new short[n];
            int i, j;
            for (i = 0, j = 0; i < n; i++, j += stride)
                shortArray[i] = d[j];
        }
    }

    public BufferElementContents(int d[], int n, int stride, boolean inPlace) {
        init();
        dataType = BufferDataType.jpvmInteger;
        if (stride == 1) {
            if (inPlace)
                intArray = d;
            else {
                intArray = new int[n];
                System.arraycopy(d, 0, intArray, 0, n);
            }
        } else {
            intArray = new int[n];
            int i, j;
            for (i = 0, j = 0; i < n; i++, j += stride)
                intArray[i] = d[j];
        }
    }

    public BufferElementContents(long d[], int n, int stride, boolean inPlace) {
        init();
        dataType = BufferDataType.jpvmLong;
        if (stride == 1) {
            if (inPlace)
                longArray = d;
            else {
                longArray = new long[n];
                System.arraycopy(d, 0, longArray, 0, n);
            }
        } else {
            longArray = new long[n];
            int i, j;
            for (i = 0, j = 0; i < n; i++, j += stride)
                longArray[i] = d[j];
        }
    }

    public BufferElementContents(char d[], int n, int stride, boolean inPlace) {
        init();
        dataType = BufferDataType.jpvmChar;
        if (stride == 1) {
            if (inPlace)
                charArray = d;
            else {
                charArray = new char[n];
                System.arraycopy(d, 0, charArray, 0, n);
            }
        } else {
            charArray = new char[n];
            int i, j;
            for (i = 0, j = 0; i < n; i++, j += stride)
                charArray[i] = d[j];
        }
    }

    public BufferElementContents(float d[], int n, int stride, boolean inPlace) {
        init();
        dataType = BufferDataType.jpvmFloat;
        if (stride == 1) {
            if (inPlace)
                floatArray = d;
            else {
                floatArray = new float[n];
                System.arraycopy(d, 0, floatArray, 0, n);
            }
        } else {
            floatArray = new float[n];
            int i, j;
            for (i = 0, j = 0; i < n; i++, j += stride)
                floatArray[i] = d[j];
        }
    }

    public BufferElementContents(double d[], int n, int stride, boolean inPlace) {
        init();
        dataType = BufferDataType.jpvmDouble;
        if (stride == 1) {
            if (inPlace)
                doubleArray = d;
            else {
                doubleArray = new double[n];
                System.arraycopy(d, 0, doubleArray, 0, n);
            }
        } else {
            doubleArray = new double[n];
            int i, j;
            for (i = 0, j = 0; i < n; i++, j += stride)
                doubleArray[i] = d[j];
        }
    }

    public BufferElementContents(byte d[], int n, int stride, boolean inPlace) {
        init();
        dataType = BufferDataType.jpvmByte;
        if (stride == 1) {
            if (inPlace)
                byteArray = d;
            else {
                byteArray = new byte[n];
                System.arraycopy(d, 0, byteArray, 0, n);
            }
        } else {
            byteArray = new byte[n];
            int i, j;
            for (i = 0, j = 0; i < n; i++, j += stride)
                byteArray[i] = d[j];
        }
    }

    private void init() {
        dataType = BufferDataType.jpvmNull;
        arraySize = 0;
        byteArray = null;
        charArray = null;
        shortArray = null;
        intArray = null;
        longArray = null;
        floatArray = null;
        doubleArray = null;
        taskArray = null;
    }

    public void unpack(int d[], int n, int stride)
            throws JPVMException {
        if (dataType != BufferDataType.jpvmInteger) {
            throw new JPVMException("buffer type mismatch, upkint.");
        }
        if (stride == 1) {
            System.arraycopy(intArray, 0, d, 0, n);
        } else {
            int i, j;
            for (i = 0, j = 0; i < n; i++, j += stride)
                d[j] = intArray[i];
        }
    }

    public void unpack(short d[], int n, int stride) throws JPVMException {
        if (dataType != BufferDataType.jpvmShort) {
            throw new JPVMException("buffer type mismatch, upkshort.");
        }
        if (stride == 1) {
            System.arraycopy(shortArray, 0, d, 0, n);
        } else {
            int i, j;
            for (i = 0, j = 0; i < n; i++, j += stride)
                d[j] = shortArray[i];
        }
    }

    public void unpack(byte d[], int n, int stride) throws JPVMException {
        if (dataType != BufferDataType.jpvmByte) {
            throw new JPVMException("buffer type mismatch, upkbyte.");
        }
        if (stride == 1)
            System.arraycopy(byteArray, 0, d, 0, n);
        else {
            int i, j;
            for (i = 0, j = 0; i < n; i++, j += stride)
                d[j] = byteArray[i];
        }
    }

    public void unpack(char d[], int n, int stride) throws JPVMException {
        if (dataType != BufferDataType.jpvmChar) {
            throw new JPVMException("buffer type mismatch, upkchar.");
        }
        if (stride == 1) {
            System.arraycopy(charArray, 0, d, 0, n);
        } else {
            int i, j;
            for (i = 0, j = 0; i < n; i++, j += stride)
                d[j] = charArray[i];
        }
    }

    public void unpack(long d[], int n, int stride) throws JPVMException {
        if (dataType != BufferDataType.jpvmLong) {
            throw new JPVMException("buffer type mismatch, upklong.");
        }
        if (stride == 1) {
            System.arraycopy(longArray, 0, d, 0, n);
        } else {
            int i, j;
            for (i = 0, j = 0; i < n; i++, j += stride)
                d[j] = longArray[i];
        }
    }

    public void unpack(double d[], int n, int stride) throws JPVMException {
        if (dataType != BufferDataType.jpvmDouble) {
            throw new JPVMException("buffer type mismatch, upkdouble.");
        }
        if (stride == 1) {
            System.arraycopy(doubleArray, 0, d, 0, n);
        } else {
            int i, j;
            for (i = 0, j = 0; i < n; i++, j += stride)
                d[j] = doubleArray[i];
        }
    }

    public void unpack(float d[], int n, int stride) throws JPVMException {
        if (dataType != BufferDataType.jpvmFloat) {
            throw new JPVMException("buffer type mismatch, upkfloat.");
        }
        if (stride == 1) {
            System.arraycopy(floatArray, 0, d, 0, n);
        } else {
            int i, j;
            for (i = 0, j = 0; i < n; i++, j += stride)
                d[j] = floatArray[i];
        }
    }

    public void unpack(TaskId d[], int n, int stride)
            throws JPVMException {
        if (dataType != BufferDataType.jpvmTid) {
            throw new JPVMException("buffer type mismatch, upktid.");
        }
        if (stride == 1) {
            System.arraycopy(taskArray, 0, d, 0, n);
        } else {
            int i, j;
            for (i = 0, j = 0; i < n; i++, j += stride)
                d[j] = taskArray[i];
        }
    }

    public String unpack() throws JPVMException {
        if (dataType != BufferDataType.jpvmString) {
            throw new JPVMException("buffer type mismatch, upkstring.");
        }
        return new String(charArray);
    }
}

