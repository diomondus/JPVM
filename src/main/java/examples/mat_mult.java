package examples;

import jpvm.Environment;
import jpvm.JPVMException;
import jpvm.buffer.Buffer;
import jpvm.messages.Message;
import jpvm.tasks.TaskId;

import java.util.Date;

/* mat_mult.java
 *
 * A simple parallel matrix-matrix multiply example using jpvm.
 *
 */
class mat_mult {
    static final int ParamTag = 11;
    static final int DoneTag = 22;
    static final int PipeTag = 33;
    static final int RollTag = 44;
    static int numTasks = 0;
    static int matDim = 0;
    static int taskMeshDim = 0;
    static int localPartitionDim = 0;
    static int localPartitionSize = 0;
    static int taskMeshRow = 0;
    static int taskMeshCol = 0;
    static int localTaskIndex = 0;
    static boolean debug = false;
    static TaskId myTaskId = null;
    static TaskId masterTaskId = null;
    static Environment jpvm = null;
    static TaskId tids[];
    static float C[], A[], B[], tempA[];

    public static void main(String args[]) {
        double mmstart = 0.0;
        double start = 0.0;
        double end = 0.0;
        boolean cmd = false;

        try {
            jpvm = new Environment();
            myTaskId = jpvm.pvm_mytid();
            masterTaskId = jpvm.pvm_parent();
            if (masterTaskId == jpvm.PvmNoParent) {
                if (args.length != 2) usage();
                cmd = true;
                try {
                    numTasks = Integer.parseInt(args[0]);
                    matDim = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    usage();
                }
                System.out.println("" + matDim + "x" + matDim +
                        " matrix multiply, " + numTasks + " tasks");

                start = msecond();
                taskMeshDim = getTaskMeshDim(numTasks);

                tids = new TaskId[numTasks];
                if (numTasks > 1)
                    jpvm.pvm_spawn("mat_mult", numTasks - 1, tids);
                tids[numTasks - 1] = tids[0];
                tids[0] = myTaskId;
                localTaskIndex = 0;

			    /* Broadcast parameters to all tasks */
                mmstart = msecond();
                Buffer buf = new Buffer();
                buf.pack(taskMeshDim);
                buf.pack(numTasks);
                buf.pack(tids, numTasks, 1);
                buf.pack(matDim);
                jpvm.pvm_mcast(buf, tids, numTasks, ParamTag);
            } else {
                // Normal worker task - get parameters...
                Message m = jpvm.pvm_recv(ParamTag);
                taskMeshDim = m.buffer.upkint();
                numTasks = m.buffer.upkint();
                tids = new TaskId[numTasks];
                m.buffer.unpack(tids, numTasks, 1);
                for (localTaskIndex = 0; localTaskIndex < numTasks;
                     localTaskIndex++)
                    if (myTaskId.equals(tids[localTaskIndex]))
                        break;
                matDim = m.buffer.upkint();
            }

			/* Do the matrix multiplication */
            matmul();

            if (cmd) {
                end = msecond();
                System.out.println("Total time: " + (end - start) +
                        " (mult: " + (end - mmstart) + ")");
            }
            jpvm.pvm_exit();
        } catch (JPVMException ex) {
            error("jpvm Exception - " + ex.toString(), true);
        }
    }

    public static void matmul() throws JPVMException {
        int i, j, k;

        localPartitionDim = matDim / taskMeshDim;
        localPartitionSize = localPartitionDim * localPartitionDim;
        taskMeshRow = localTaskIndex / taskMeshDim;
        taskMeshCol = localTaskIndex % taskMeshDim;
        A = new float[localPartitionSize];
        B = new float[localPartitionSize];
        C = new float[localPartitionSize];
        tempA = new float[localPartitionSize];

        if (debug) {
            System.out.println("localTaskIndex\t= " + localTaskIndex);
            System.out.println("matDim\t= " + matDim);
            System.out.println("taskMeshDim\t= " + taskMeshDim);
            System.out.println("localPartitionDim\t= " +
                    localPartitionDim);
            System.out.println("localPartitionSize\t= " +
                    localPartitionSize);
            System.out.println("taskMeshRow\t= " + taskMeshRow);
            System.out.println("taskMeshCol\t= " + taskMeshCol);
        }

        for (i = 0; i < localPartitionSize; i++) {
            C[i] = (float) 0.0;
            A[i] = (float) (i + localTaskIndex * (taskMeshRow + 1));
            B[i] = (float) (i - localTaskIndex * (taskMeshRow + 1));
        }

        for (i = 0; i < taskMeshDim; i++) {
            Pipe(i);
            Multiply();
            if (i < (taskMeshDim - 1)) Roll();
        }
    }

    public static void Pipe(int iter) throws JPVMException {
        int i;
        if (taskMeshCol == (taskMeshRow + iter) % taskMeshDim) {
            Buffer buf = new Buffer();
            buf.pack(A, localPartitionSize, 1);
            for (i = 0; i < taskMeshDim; i++)
                if (localTaskIndex != taskMeshRow * taskMeshDim + i) {
                    jpvm.pvm_send(buf,
                            tids[taskMeshRow * taskMeshDim + i], PipeTag);
                }
        } else {
            Message m = jpvm.pvm_recv(PipeTag);
            m.buffer.unpack(tempA, localPartitionSize, 1);
        }
    }

    public static void Multiply() throws JPVMException {
        int i, j, k;
        float temp;
        for (i = 0; i < localPartitionDim; i++)
            for (j = 0; j < localPartitionDim; j++) {
                temp = 0;
                for (k = 0; k < localPartitionDim; k++)
                    temp += A[i * localPartitionDim + k] *
                            B[k * localPartitionDim + j];
                C[i * localPartitionDim + j] += temp;
            }
    }

    public static void Roll() throws JPVMException {
        int who = ((taskMeshRow != 0) ?
                (taskMeshRow - 1) * taskMeshDim + taskMeshCol :
                (taskMeshDim - 1) * taskMeshDim + taskMeshCol);
        Buffer buf = new Buffer();
        buf.pack(B, localPartitionSize, 1);
        jpvm.pvm_send(buf, tids[who], RollTag);
        Message m = jpvm.pvm_recv(RollTag);
        m.buffer.unpack(B, localPartitionSize, 1);
    }

    public static double msecond() {
        Date d = new Date();
        double msec = (double) d.getTime();
        return msec;
    }

    public static void error(String message, boolean die) {
        System.err.println("mat_mult: " + message);
        if (die) {
            if (jpvm != null) {
                try {
                    jpvm.pvm_exit();
                } catch (JPVMException ex) {
                }
                System.exit(1);
            }
        }
    }

    public static void usage() {
        error("usage -  java mat_mult <tasks> <mat dim>", true);
    }

    public static int getTaskMeshDim(int n) {
        int dim;
        for (dim = 1; dim <= n && n > 0; dim++) {
            if (dim * dim == n) return dim;
        }
        error("Number of tasks must be an even square", true);
        return -1;
    }
};
