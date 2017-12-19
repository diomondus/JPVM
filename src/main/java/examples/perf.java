package examples;

import jpvm.*;
import jpvm.buffer.Buffer;
import jpvm.messages.Message;
import jpvm.tasks.TaskId;

import java.util.Date;

/* perf.java
 *
 * A simple test of jpvm message passing.
 *
 */
class perf {
    private static int dataSize = 1024 * 1024;
    private static int dataSizeOf = 4;
    private static int data[] = null;
    private static int dataIn[] = null;

    private static Environment jpvm = null;
    private static boolean test_spawn = false;
    private static boolean test_comm = true;
    private static int iters = 16;
    private static int test_sizes[];
    private static int num_tests;

    public static void main(String args[]) {
        if (test_comm) {
            num_tests = 5;
            test_sizes = new int[num_tests];
            test_sizes[0] = 4;
            test_sizes[1] = 1024;
            test_sizes[2] = 10 * 1024;
            test_sizes[3] = 100 * 1024;
            test_sizes[4] = 1024 * 1024;

            // Small test...
            // num_tests = 1;
            // test_sizes = new int[num_tests];
            // test_sizes[0] = 128 * 1024;
        }
        try {
            jpvm = new Environment();
            if (jpvm.pvm_parent() == null)
                master();
            else
                slave();
            jpvm.pvm_exit();
        } catch (JPVMException ex) {
            System.out.println("Error - jpvm exception");
        }
    }

    public static void master() throws JPVMException {
        int i;
        int ns = 0;
        double bef = 0.0;
        double aft = 0.0;
        TaskId tids[] = new TaskId[16];

        data = new int[dataSize / dataSizeOf];
        dataIn = new int[dataSize / dataSizeOf];
        for (i = 0; i < (dataSize / dataSizeOf); i++)
            data[i] = i;

        if (test_spawn) {
            // Spawn test
            for (i = 1; i <= 16; i *= 2) {
                bef = msecond();
                ns = jpvm.pvm_spawn("perf", i, tids);
                aft = msecond();
                if (ns != i) {
                    System.err.println("Spawn error, created " +
                            ns + " tasks, " + i + " requested!");
                }
                System.out.println("Spawn " + i + " tasks: " + (aft - bef) +
                        " msecs.");

                Buffer buf = new Buffer();
                int tmp = 0;
                buf.pack(tmp);
                jpvm.pvm_mcast(buf, tids, i, 111);
                sleep(2);
            }
        }

        if (test_comm) {
            ns = jpvm.pvm_spawn("perf", 1, tids);
            if (ns != 1) {
                System.err.println("Spawn error, created " +
                        ns + " tasks, " + 1 + " requested!");
            }
            Buffer buf = new Buffer();
            buf.pack(iters);
            jpvm.pvm_send(buf, tids[0], 111);

            double avg_pack = 0.0;
            double avg_comm = 0.0;
            double avg_unpack = 0.0;

            for (int tst = 0; tst < num_tests; tst++) {
                for (int iter = 0; iter < iters; iter++) {
                    buf = new Buffer();
                    bef = msecond();
                    buf.pack(data,
                            (test_sizes[tst] / dataSizeOf), 1);
                    aft = msecond();
                    avg_pack += (aft - bef);

                    bef = msecond();
                    jpvm.pvm_send(buf, tids[0], 222);
                    Message message;
                    message = jpvm.pvm_recv();
                    aft = msecond();
                    avg_comm += (aft - bef);

                    bef = msecond();
                    buf.unpack(dataIn,
                            (test_sizes[tst] / dataSizeOf), 1);
                    aft = msecond();
                    avg_unpack += (aft - bef);

                    for (i = 0; i < (test_sizes[tst] / dataSizeOf);
                         i++) {
                        if (data[i] != dataIn[i]) {
                            error("Bad data " +
                                    dataIn[i] +
                                    "!=" +
                                    data[i] +
                                    " iter=" +
                                    iter +
                                    " i=" + i);
                        }
                    }

                }
                avg_pack /= iters;
                avg_comm /= iters;
                avg_unpack /= iters;
                System.out.println("\nPack " + test_sizes[tst] +
                        " bytes: " + avg_pack + " msecs.");
                System.out.println("Comm " + test_sizes[tst] +
                        " bytes: " + avg_comm + " msecs.");
                System.out.println("Unpk " + test_sizes[tst] +
                        " bytes: " + avg_unpack + " msecs.\n");
                sleep(4);
            }
        }
    }

    public static void slave() throws JPVMException {
        TaskId parent = jpvm.pvm_parent();
        Message message = jpvm.pvm_recv();
        int doRecv = message.buffer.upkint();

        if (doRecv == 0) return;

        for (int tst = 0; tst < num_tests; tst++) {
            for (int iter = 0; iter < iters; iter++) {
                Message msg = jpvm.pvm_recv();
                jpvm.pvm_send(msg.buffer, parent, 333);
            }
        }
    }

    public static double msecond() {
        Date d = new Date();
        double msec = (double) d.getTime();
        return msec;
    }

    public static void sleep(int secs) {
        try {
            Thread.sleep(secs * 1000);
        } catch (InterruptedException ie) {
        }
    }

    public static void error(String str) {
        System.err.println("perf: error \"" + str + "\"");
    }
};
