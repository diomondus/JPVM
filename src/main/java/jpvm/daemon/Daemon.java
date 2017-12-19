package jpvm.daemon;

import jpvm.Debug;
import jpvm.Environment;
import jpvm.JPVMException;
import jpvm.buffer.Buffer;
import jpvm.messages.Message;
import jpvm.tasks.TaskExecutorThread;
import jpvm.tasks.TaskId;
import jpvm.tasks.TaskList;
import jpvm.tasks.TaskListRecord;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Dmitry Butilov
 * on 14.12.17.
 */
public class Daemon {
    public static final String SPACE = " ";
    private static Environment jpvm = null;
    private static TaskId myTid = null;

    private static TaskList tasks = null;
    private static TaskList hosts = null;
    private static SpawnWorkOrderList spawnOrders = null;
    private static int maxCreateOrders = 256;
    private static CreateWorkOrder createOrders[];
    private static int nextCreateOrder = 0;

    private static boolean log_on = true;
    private static boolean debug_on = true;
    private static String my_host_name = null;

    // Which version of the JVM should be used to host tasks?
//    private static String java_exec = "/Library/Java/JavaVirtualMachines/jdk1.8.0_121.jdk/Contents/Home/bin/java";
    private static String java_exec = "java";

    public static void main(String args[]) {
        try {
            int i;
            // Initialize data structures
            jpvm = new Environment(true);
            myTid = jpvm.pvm_mytid();
            tasks = new TaskList();
            hosts = new TaskList();
            spawnOrders = new SpawnWorkOrderList();
            createOrders = new CreateWorkOrder[maxCreateOrders];

            // Announce location
            log(jpvm.pvm_mytid().toString());
            my_host_name = jpvm.pvm_mytid().getHost();
            hosts.addTask(jpvm.pvm_mytid(), my_host_name);

            writeDaemonFile();

            // Main server loop...
            while (true) {
                Message req = jpvm.pvm_recv();
                TaskId client = req.sourceTid;
                int request = req.messageTag;
                String reqName;

                switch (request) {
                    case (DaemonMessageTag.jpvmdPingRequest):
                        reqName = "Ping";
                        break;
                    case (DaemonMessageTag.jpvmdRegisterTask):
                        reqName = "RegisterTask";
                        break;
                    case (DaemonMessageTag.jpvmdRegisterChild):
                        reqName = "RegisterChild";
                        break;
                    case DaemonMessageTag.jpvmdSpawnTask:
                        reqName = "SpawnTask";
                        break;
                    case DaemonMessageTag.jpvmdCreateTask:
                        reqName = "CreateTask";
                        break;
                    case DaemonMessageTag.jpvmdCreatedTask:
                        reqName = "CreateTaskReturn";
                        break;
                    case DaemonMessageTag.jpvmdDeleteTask:
                        reqName = "DeleteTask";
                        break;
                    case DaemonMessageTag.jpvmdTaskStatus:
                        reqName = "TaskStatus";
                        break;
                    case DaemonMessageTag.jpvmdAddHost:
                        reqName = "AddHost";
                        break;
                    case DaemonMessageTag.jpvmdAddHostBcast:
                        reqName = "AddHostNotify";
                        break;
                    case DaemonMessageTag.jpvmdDeleteHost:
                        reqName = "DeleteHost";
                        break;
                    case DaemonMessageTag.jpvmdDeleteHostBcast:
                        reqName = "DeleteHostNotify";
                        break;
                    case DaemonMessageTag.jpvmdHostStatus:
                        reqName = "HostStatus";
                        break;
                    case DaemonMessageTag.jpvmdHostHalt:
                        reqName = "HostHalt";
                        break;
                    case DaemonMessageTag.jpvmdHalt:
                        reqName = "Halt";
                        break;
                    default:
                        reqName = "Unknown Request";
                }

                if (debug_on) {
                    log("new request type=" + request + ",\"" + reqName + "\", from " + client.toString());
                }

                switch (request) {
                    case (DaemonMessageTag.jpvmdPingRequest):
                        Ping(client, req.buffer);
                        break;
                    case (DaemonMessageTag.jpvmdRegisterTask):
                        RegisterTask(client, req.buffer);
                        break;
                    case (DaemonMessageTag.jpvmdRegisterChild):
                        RegisterChild(client, req.buffer);
                        break;
                    case DaemonMessageTag.jpvmdSpawnTask:
                        SpawnTask(client, req.buffer);
                        break;
                    case DaemonMessageTag.jpvmdCreateTask:
                        CreateTask(client, req.buffer);
                        break;
                    case DaemonMessageTag.jpvmdCreatedTask:
                        CreatedTask(client, req.buffer);
                        break;
                    case DaemonMessageTag.jpvmdDeleteTask:
                        DeleteTask(client, req.buffer);
                        break;
                    case DaemonMessageTag.jpvmdTaskStatus:
                        TaskStatus(client, req.buffer);
                        break;
                    case DaemonMessageTag.jpvmdAddHost:
                        AddHost(client, req.buffer);
                        break;
                    case DaemonMessageTag.jpvmdAddHostBcast:
                        AddHostBcast(client, req.buffer);
                        break;
                    case DaemonMessageTag.jpvmdDeleteHost:
                        DeleteHost(client, req.buffer);
                        break;
                    case DaemonMessageTag.jpvmdDeleteHostBcast:
                        DeleteHostBcast(client, req.buffer);
                        break;
                    case DaemonMessageTag.jpvmdHostStatus:
                        HostStatus(client, req.buffer);
                        break;
                    case DaemonMessageTag.jpvmdHostHalt:
                        HostHalt(client, req.buffer);
                        break;
                    case DaemonMessageTag.jpvmdHalt:
                        Halt(client, req.buffer);
                        break;
                    default:
                        perror("unknown request type");
                }
            }
        } catch (JPVMException ex) {
            Debug.note("Daemon, internal jpvm error.");
        }
    }

    private static void daemonBcast(Buffer buf, int tag) {
        TaskListRecord tmp = hosts.firstIter();
        while (tmp != null) {
            try {
                jpvm.pvm_send(buf, tmp.tid, tag);
                tmp = hosts.nextIter();
            } catch (JPVMException ex) {
                perror("problem sending to daemon " + tmp.tid.toString());
                hosts.deleteTask(tmp.tid);
            }
        }
    }

    private static void RegisterTask(TaskId client, Buffer req) {
        try {
            String name = req.upkstr();
            tasks.addTask(client, name);
        } catch (JPVMException ex) {
            log("bad RegisterTask invocation");
        }
    }

    private static void RegisterChild(TaskId client, Buffer req) {
        int regNum = -1;

        // Child process reporting in. Notify the remote client that
        // requested this local task creation.

        try {
            regNum = req.upkint();
        } catch (JPVMException ex) {
            perror("bad RegisterChild invocation");
            return;
        }

        if (regNum < 0 || regNum >= maxCreateOrders) {
            perror("RegisterChild, child registration number " +
                    regNum + "out of bounds");
            return;
        }

        CreateWorkOrder order = createOrders[regNum];
        if (order == null) {
            perror("RegisterChild, child registration number " +
                    regNum + "not expected");
            return;
        }
        if (!order.outstanding) {
            perror("RegisterChild, child registration number " +
                    regNum + "unexpected");
            return;
        }
        order.outstanding = false;

        // Return the blessed event to the requester
        try {
            Buffer buf = new Buffer();
            buf.pack(1);
            buf.pack(client);
            buf.pack(order.order);
            jpvm.pvm_send(buf, order.client,
                    DaemonMessageTag.jpvmdCreatedTask);
        } catch (JPVMException ex) {
            perror("RegisterChild, \"" + ex + "\" sending to client " + order.client.toString());
        }
    }

    private static void SpawnTask(TaskId client, Buffer req) {
        int num = 0;
        String name = null;
        Buffer buf = new Buffer();
        try {
            num = req.upkint();
            name = req.upkstr();
        } catch (JPVMException ex) {
            perror("bad SpawnTask invocation");
            return;
        }
        if (num == 0) {
            buf.pack(num);
            try {
                jpvm.pvm_send(buf, client,
                        DaemonMessageTag.jpvmdSpawnTask);
            } catch (JPVMException ex) {
                perror("SpawnTask, problem sending "
                        + "to client " +
                        client.toString());
            }
            return;
        }

        // Create a work order for the spawn
        SpawnWorkOrder order;
        order = spawnOrders.newOrder(num);
        order.client = client;

        // Create a request to create a task on a remote host
        Buffer creq = new Buffer();
        creq.pack(name); // Pack the class to create
        creq.pack(client); // Pack parent of the created tasks
        creq.pack(order.order);

        // Schedule on known hosts round robin style
        TaskListRecord target = null;
        for (int i = 0; i < num; i++) {
            if (target == null) target = hosts.firstIter();
            if (target == null) {
                perror("no hosts in SpawnTask invocation");
                return;
            }
            if (target.tid.equals(jpvm.pvm_mytid())) {
                creq.rewind();
                CreateTask(jpvm.pvm_mytid(), creq);
            } else {
                try {
                    jpvm.pvm_send(creq, target.tid,
                            DaemonMessageTag.jpvmdCreateTask);
                } catch (JPVMException ex) {
                    perror("SpawnTask, error scheduling " +
                            "on host " + target.tid.toString());
                }
            }
            target = hosts.nextIter();
        }
    }

    private static void CreatedTask(TaskId client, Buffer req) {
        int count = -1;
        TaskId child = null;
        int orderNum = -1;

        try {
            count = req.upkint();
            if (count == 1) {
                child = req.upktid();
                orderNum = req.upkint();
            }
        } catch (JPVMException ex) {
            perror("CreatedTask, bad report from " +
                    client.toString());
        }

        // Look up which spawn order this is in regards to
        SpawnWorkOrder order;
        order = spawnOrders.lookup(orderNum);

        if (order == null) {
            perror("CreatedTask, order number " + orderNum +
                    " is not valid");
            return;
        }

        // Update the status of the order
        order.tids[order.numDone] = child;
        order.numDone++;

        if (order.numDone == order.num) {
            // The order is complete - return the good
            // news to the original client
            Buffer buf = new Buffer();
            buf.pack(order.numDone);
            buf.pack(order.tids, order.numDone, 1);
            try {
                jpvm.pvm_send(buf, order.client,
                        DaemonMessageTag.jpvmdSpawnTask);
            } catch (JPVMException ex) {
                perror("CreatedTask, \"" + ex + "\" sending to client " + order.client.toString());
            }

            // Throw away the order
            spawnOrders.doneOrder(order);
        }
    }

    private static void CreateTask(TaskId client, Buffer req) {
        String name = null;
        TaskId parent = null;
        int order;

        try {
            name = req.upkstr();
            parent = req.upktid();
            order = req.upkint();
        } catch (JPVMException ex) {
            perror("bad CreateTask invocation");
            return;
        }

        if (createOrders[nextCreateOrder] == null)
            createOrders[nextCreateOrder] = new CreateWorkOrder();

        if (createOrders[nextCreateOrder].outstanding) {
            perror("too many outstanding task creation requests");
            return;
        }

        // Log the task creation request so when the task reports
        // in we'll know it was expected
        createOrders[nextCreateOrder].order = order;
        createOrders[nextCreateOrder].client = client;
        createOrders[nextCreateOrder].outstanding = true;

        // Create a thread to execute the new task
        String command = java_exec +
                " -cp /Users/mitryl/Dev/jpvm/build/libs/*" +
                " -Djpvm.daemon=" + jpvm.pvm_mytid().getPort() +
                " -Djpvm.parhost=" + parent.getHost() +
                " -Djpvm.parport=" + parent.getPort() +
                " -Djpvm.taskname=" + name +
                " -Djpvm.regnum=" + nextCreateOrder +
                SPACE + name;
        if (debug_on) {
            log("exec( " + command + " )");
        }
        TaskExecutorThread spawnThread;
        spawnThread = new TaskExecutorThread(jpvm, client, command, createOrders[nextCreateOrder]);
        nextCreateOrder++;
        spawnThread.run();
    }

    private static void DeleteTask(TaskId client, Buffer req) {
        tasks.deleteTask(client);
    }

    private static void TaskStatus(TaskId client, Buffer req) {
        Buffer buf = new Buffer();
        int n = tasks.numTasks();
        TaskId tids[] = new TaskId[n];
        buf.pack(n);
        TaskListRecord tmp = tasks.firstIter();
        int i = 0;
        while (tmp != null) {
            tids[i] = tmp.tid;
            buf.pack(tmp.name);
            tmp = tasks.nextIter();
            i++;
        }
        buf.pack(tids, n, 1);
        try {
            jpvm.pvm_send(buf, client, DaemonMessageTag.jpvmdTaskStatus);
        } catch (JPVMException ex) {
            perror("TaskStatus, \"" + ex + "\" sending to client " +
                    client.toString());
        }
    }

    private static void AddHost(TaskId client, Buffer req) {
        Buffer buf = internalAddHosts(req);
        if (buf == null) {
            perror("AddHost, problem adding hosts");
            return;
        }
        daemonBcast(buf, DaemonMessageTag.jpvmdAddHostBcast);
    }

    private static void Ping(TaskId client, Buffer req) {
        try {
            jpvm.pvm_send(req, client,
                    DaemonMessageTag.jpvmdPingReply);
        } catch (JPVMException ex) {
            perror("ping, \"" + ex + "\" sending to client " +
                    client.toString());
        }
    }

    private static void AddHostBcast(TaskId client, Buffer req) {
        if (client.equals(myTid)) return;
        try {
            int i;
            int num = req.upkint();
            String names[] = new String[num];
            TaskId daemonTids[] = new TaskId[num];
            for (i = 0; i < num; i++)
                names[i] = req.upkstr();
            req.unpack(daemonTids, num, 1);
            for (i = 0; i < num; i++)
                hosts.addTask(daemonTids[i], names[i]);
        } catch (JPVMException ex) {
            log("bad AddHost invocation");
        }
    }

    private static Buffer internalAddHosts(Buffer req) {
        int i, j;
        Buffer ret = new Buffer();

        int newNum = 0;
        String newNames[] = null;
        TaskId newTids[] = null;
        boolean newValid[] = null;
        int newValidNum = 0;
        try {
            // First, get the addresses of all new daemons
            newNum = req.upkint();
            newNames = new String[newNum];
            newTids = new TaskId[newNum];
            newValid = new boolean[newNum];
            for (i = 0; i < newNum; i++) newNames[i] = req.upkstr();
            req.unpack(newTids, newNum, 1);
        } catch (JPVMException ex) {
            log("bad AddHost call");
            perror("internalAddHost, " + ex);
            return null;
        }

        // Check the validity of all new names
        Buffer pingBuf = new Buffer();
        newValidNum = newNum;
        for (i = 0; i < newNum; i++) {
            boolean valid = true;
            try {
                jpvm.pvm_send(pingBuf, newTids[i], DaemonMessageTag.jpvmdPingRequest);
                Message pingMess = jpvm.pvm_recv(newTids[i], DaemonMessageTag.jpvmdPingReply);
            } catch (JPVMException ex) {
                valid = false;
                newValidNum--;
                perror("internalAddHost, ping, " + ex);
            }
            newValid[i] = valid;
            if (valid)
                hosts.addTask(newTids[i], newNames[i]);
        }

        if (newValidNum < 1) {
            // no hosts to add!
            ret = null;
            perror("internalAddHost, no hosts added");
            return ret;
        }

        // Create the message to add all daemons, new and old
        int oldNum = hosts.numTasks();
        int totalNum = oldNum + newValidNum;
        TaskId allTids[] = new TaskId[totalNum];
        TaskListRecord tmp = hosts.firstIter();

        // Pack in the old names...
        ret.pack(totalNum);
        i = 0;
        while (tmp != null) {
            allTids[i] = tmp.tid;
            ret.pack(tmp.name);
            tmp = hosts.nextIter();
            i++;
        }
        // Pack in the old names...
        for (j = 0; j < newNum; j++)
            if (newValid[j]) {
                allTids[i] = newTids[j];
                ret.pack(newNames[j]);
                i++;
            }

        // Pack in all of the tids...
        ret.pack(allTids, totalNum, 1);
        return ret;
    }

    private static void DeleteHost(TaskId client, Buffer req) {
    }

    private static void DeleteHostBcast(TaskId client, Buffer req) {
    }

    private static void HostStatus(TaskId client, Buffer req) {
        Buffer buf = new Buffer();
        int nhosts = hosts.numTasks();
        buf.pack(nhosts);
        TaskId dTids[] = new TaskId[nhosts];
        TaskListRecord tmp = hosts.firstIter();
        int i = 0;
        while (tmp != null) {
            dTids[i] = tmp.tid;
            buf.pack(tmp.name);
            tmp = hosts.nextIter();
            i++;
        }
        buf.pack(dTids, nhosts, 1);
        try {
            jpvm.pvm_send(buf, client, DaemonMessageTag.jpvmdHostStatus);
        } catch (JPVMException ex) {
            perror("HostStatus, \"" + ex + "\" sending to client " +
                    client.toString());
        }
    }

    private static void HostHalt(TaskId client, Buffer req) {
        log("shutting down");
        System.exit(0);
    }

    private static void Halt(TaskId client, Buffer req) {
        Buffer buf = new Buffer();
        daemonBcast(buf, DaemonMessageTag.jpvmdHostHalt);
    }

    private static void log(String message) {
        if (log_on) {
            System.out.println("jpvm daemon: " + message);
            System.out.flush();
        }
    }

    private static void perror(String message) {
        System.err.println("jpvm daemon: " + message);
        System.err.flush();
    }

    private static void writeDaemonFile() {
        String fileName = Environment.pvm_daemon_file_name();
        try {
            File f = new File(fileName);
            FileOutputStream fout = new FileOutputStream(f);
            DataOutputStream dout = new DataOutputStream(fout);
            int port = myTid.getPort();
            dout.writeInt(port);
            dout.flush();
        } catch (IOException ioe) {
            perror("error writing \"" + fileName + "\"");
        }
    }
}