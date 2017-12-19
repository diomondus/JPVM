package jpvm;

import jpvm.buffer.Buffer;
import jpvm.connection.ConnectionServer;
import jpvm.connection.ConnectionSet;
import jpvm.connection.SendConnection;
import jpvm.daemon.DaemonMessageTag;
import jpvm.messages.Message;
import jpvm.messages.MessageQueue;
import jpvm.tasks.TaskId;
import jpvm.tasks.TaskStatus;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by Dmitry Butilov
 * on 14.12.17.
 */
public class Environment {
    public static final TaskId PvmNoParent = null;

    private static TaskId myTid;
    private static TaskId parentTid;
    private static TaskId daemonTid;
    private static MessageQueue myMessageQueue;
    private static ConnectionSet myConnectionSet;
    private static ConnectionServer connectionServer;
    private static String myName;
    private static int registrationNumber = -1;

    public Environment() throws JPVMException {
        init(false, null);
    }

    public Environment(String taskName) throws JPVMException {
        init(false, taskName);
    }

    public Environment(boolean isDaemon) throws JPVMException {
        init(isDaemon, null);
    }

    public Environment(boolean isDaemon, String taskName)
            throws JPVMException {
        init(isDaemon, taskName);
    }

    public TaskId pvm_mytid() {
        return myTid;
    }

    public TaskId pvm_parent() {
        return parentTid;
    }

    public int pvm_spawn(String task_name, int num, TaskId tids[])
            throws JPVMException {
        int ret = 0;
        Buffer buf = new Buffer();
        buf.pack(num);
        buf.pack(task_name);
        pvm_send(buf, daemonTid, DaemonMessageTag.jpvmdSpawnTask);
        try {
            Message m = pvm_recv(DaemonMessageTag.jpvmdSpawnTask);
            ret = m.buffer.upkint();
            m.buffer.unpack(tids, ret, 1);
        } catch (JPVMException ex) {
            Debug.error("pvm_spawn, internal error");
        }
        return ret;
    }

    public synchronized void pvm_send(Buffer buf, TaskId tid, int tag)
            throws JPVMException {
        Debug.note("pvm_send, sending message to " + tid.toString());
        Message message = new Message(buf, tid, myTid, tag);
        if (myTid.equals(tid)) {
            // Just enqueue the message
            myMessageQueue.enqueue(message);
        } else {
            SendConnection conn = getConnection(tid);
            message.send(conn);
        }
    }

    public synchronized void pvm_mcast(Buffer buf, TaskId tids[], int ntids, int tag)
            throws JPVMException {
        int exceptions = 0;
        Message message = new Message(buf, null, myTid, tag);
        for (int i = 0; i < ntids; i++) {
            TaskId tid = tids[i];
            message.destTid = tid;
            Debug.note("pvm_mcast, sending message to " + tid.toString());
            try {
                SendConnection conn = getConnection(tid);
                message.send(conn);
            } catch (JPVMException ex) {
                exceptions++;
            }
        }
        if (exceptions > 0) {
            throw new JPVMException("pvm_mcast, some messages " + "failed");
        }
    }

    public Message pvm_recv(TaskId tid, int tag)
            throws JPVMException {
        return myMessageQueue.dequeue(tid, tag);
    }

    public Message pvm_recv(TaskId tid) throws JPVMException {
        return myMessageQueue.dequeue(tid);
    }

    public Message pvm_recv(int tag) throws JPVMException {
        return myMessageQueue.dequeue(tag);
    }

    public Message pvm_recv() throws JPVMException {
        return myMessageQueue.dequeue();
    }

    public Message pvm_nrecv(TaskId tid, int tag)
            throws JPVMException {
        return myMessageQueue.dequeueNonBlock(tid, tag);
    }

    public Message pvm_nrecv(TaskId tid) throws JPVMException {
        return myMessageQueue.dequeueNonBlock(tid);
    }

    public Message pvm_nrecv(int tag) throws JPVMException {
        return myMessageQueue.dequeueNonBlock(tag);
    }

    public Message pvm_nrecv() throws JPVMException {
        return myMessageQueue.dequeueNonBlock();
    }

    public boolean pvm_probe(TaskId tid, int tag) throws JPVMException {
        return myMessageQueue.probe(tid, tag);
    }

    public boolean pvm_probe(TaskId tid) throws JPVMException {
        return myMessageQueue.probe(tid);
    }

    public boolean pvm_probe(int tag) throws JPVMException {
        return myMessageQueue.probe(tag);
    }

    public boolean pvm_probe() throws JPVMException {
        return myMessageQueue.probe();
    }

    public void pvm_exit() throws JPVMException {
        Buffer buf = new Buffer();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {
        }
        pvm_send(buf, daemonTid, DaemonMessageTag.jpvmdDeleteTask);
    }

    public Configuration pvm_config() {
        Buffer buffer = new Buffer();
        Configuration configuration = null;
        try {
            pvm_send(buffer, daemonTid, DaemonMessageTag.jpvmdHostStatus);
            Message m = pvm_recv(DaemonMessageTag.jpvmdHostStatus);
            int n = m.buffer.upkint();
            configuration = new Configuration(n);
            for (int i = 0; i < n; i++)
                configuration.hostNames[i] = m.buffer.upkstr();
            m.buffer.unpack(configuration.hostDaemonTids, n, 1);
        } catch (JPVMException ex) {
            Debug.error("pvm_config, internal error");
        }
        return configuration;
    }

    public TaskStatus pvm_tasks(Configuration configuration, int which) {
        TaskStatus taskStatus = null;
        if (configuration == null || which < 0 || which >= configuration.numHosts)
            return null;
        try {
            Buffer buf = new Buffer();
            pvm_send(buf, configuration.hostDaemonTids[which], DaemonMessageTag.jpvmdTaskStatus);
            Message m = pvm_recv(DaemonMessageTag.jpvmdTaskStatus);
            taskStatus = new TaskStatus();
            taskStatus.hostName = configuration.hostNames[which];
            taskStatus.numTasks = m.buffer.upkint();
            if (taskStatus.numTasks == 0) {
                taskStatus.taskNames = null;
                taskStatus.taskTids = null;
                return taskStatus;
            }
            taskStatus.taskNames = new String[taskStatus.numTasks];
            taskStatus.taskTids = new TaskId[taskStatus.numTasks];
            for (int i = 0; i < taskStatus.numTasks; i++) {
                taskStatus.taskNames[i] = m.buffer.upkstr();
            }
            m.buffer.unpack(taskStatus.taskTids, taskStatus.numTasks, 1);
        } catch (JPVMException ex) {
            Debug.error("pvm_tasks, internal error");
        }
        return taskStatus;
    }

    public void pvm_halt() throws JPVMException {
        Buffer buf = new Buffer();
        pvm_send(buf, daemonTid, DaemonMessageTag.jpvmdHalt);
    }

    public void pvm_addhosts(int nhosts, String hostnames[], TaskId daemonTids[]) throws JPVMException {
        Buffer buffer = new Buffer();
        buffer.pack(nhosts);
        for (int i = 0; i < nhosts; i++) {
            buffer.pack(hostnames[i]);
        }
        buffer.pack(daemonTids, nhosts, 1);
        pvm_send(buffer, daemonTid, DaemonMessageTag.jpvmdAddHost);
    }

    // Internal methods:
    private void init(boolean isDaemon, String taskName)
            throws JPVMException {
        myMessageQueue = new MessageQueue();
        myConnectionSet = new ConnectionSet();
        connectionServer = new ConnectionServer(myConnectionSet, myMessageQueue);
        myTid = new TaskId(connectionServer.getConnectionPort());
        connectionServer.setDaemon(true);
        connectionServer.start();
        if (!isDaemon) {
            findDaemon();
            findParent();
            registerDaemon(taskName);
        }
    }

    private SendConnection getConnection(TaskId tid)
            throws JPVMException {
        SendConnection connection;
        connection = myConnectionSet.lookupSendConnection(tid);
        if (connection != null) {
            // Had a cached connection...
            return connection;
        }
        // Must establish a new connection.
        connection = SendConnection.connect(tid, myTid);
        if (connection != null) {
            myConnectionSet.insertSendConnection(connection);
            return connection;
        }
        throw new JPVMException("getConnection, connect failed");
    }

    private void findDaemon() {
        int daemonPort;
        String daemonPortStr = System.getProperty("jpvm.daemon");
        if (daemonPortStr != null) {
            try {
                daemonPort = Integer.valueOf(daemonPortStr);
            } catch (NumberFormatException nfe) {
                Debug.error("couldn't bind to daemon, " + "jpvm.daemon not an integer");
                daemonPort = -1;
            }
        } else {
            daemonPort = readDaemonFile();
        }
        if (daemonPort == -1) {
            Debug.error("couldn't bind to daemon, " + "jpvm.daemon not defined");
        }
        daemonTid = new TaskId(daemonPort);
    }

    private void findParent()
            throws JPVMException {
        String parentHost = System.getProperty("jpvm.parhost");
        int parentPort = 0;
        if (parentHost == null) {
            parentTid = null;
            return;
        }
        String parentPortStr = System.getProperty("jpvm.parport");
        try {
            parentPort = Integer.valueOf(parentPortStr);
        } catch (NumberFormatException nfe) {
            Debug.error("couldn't bind to parent, " + "jpvm.parport not an integer");
        }
        parentTid = new TaskId(parentHost, parentPort);

        // Since we have a parent, register with the daemon
        String regStr = System.getProperty("jpvm.regnum");
        if (regStr == null) {
            Debug.error("no task registration number");
        } else {
            try {
                registrationNumber = Integer.valueOf(regStr);
            } catch (NumberFormatException nfe) {
                Debug.error("invalid task registration number");
            }
        }
        Buffer buffer = new Buffer();
        buffer.pack(registrationNumber);
        pvm_send(buffer, daemonTid, DaemonMessageTag.jpvmdRegisterChild);
    }

    private void registerDaemon(String taskName)
            throws JPVMException {
        // Find out the name of this task
        if (taskName == null) {
            myName = System.getProperty("jpvm.taskname");
            if (myName == null) {
                myName = "(command line jpvm task)";
            }
        } else {
            myName = taskName;
        }

        // Register this task with the daemon
        Buffer buf = new Buffer();
        buf.pack(myName);
        pvm_send(buf, daemonTid, DaemonMessageTag.jpvmdRegisterTask);
    }

    private int readDaemonFile() {
        int port;
        String fileName = pvm_daemon_file_name();
        try {
            File f = new File(fileName);
            FileInputStream fin = new FileInputStream(f);
            DataInputStream din = new DataInputStream(fin);
            port = din.readInt();
        } catch (IOException ioe) {
            Debug.error("error writing \"" + fileName + "\"");
            port = -1;
        }
        return port;
    }

    public static String pvm_daemon_file_name() {
        String osName = System.getProperty("os.name");
        String userName = System.getProperty("user.name");
        String fileName = null;
        if (osName.toLowerCase().contains("windows")) {
            fileName = "c:\\Users\\" + userName + "\\jpvmd-" + userName + ".txt";
        } else {
            fileName = "/tmp/jpvmd." + userName;
        }
        return fileName;
    }
}