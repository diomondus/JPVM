package jpvm.tasks;

import jpvm.Environment;
import jpvm.JPVMException;
import jpvm.daemon.CreateWorkOrder;
import jpvm.daemon.DaemonMessageTag;
import jpvm.buffer.Buffer;

import java.io.IOException;
import java.util.Scanner;

/**
 * Created by Dmitry Butilov
 * on 14.12.17.
 */
public class TaskExecutorThread extends Thread {
    private Environment jpvm = null;
    private Process process;
    private TaskId client;
    private String command;
    private boolean log_on = true;
    private boolean debug_on = false;
    private CreateWorkOrder order;

    public TaskExecutorThread(Environment j, TaskId c, String aCommand, CreateWorkOrder o) {
        jpvm = j;
        client = c;
        command = aCommand;
        order = o;
    }

    private boolean doExec() {
        try {
            process = Runtime.getRuntime().exec(command);
        } catch (IOException ioe) {
            perror("i/o exception on exec");
            order.outstanding = false;
            Buffer buf = new Buffer();
            buf.pack(order.order);
            buf.pack(-1);
            try {
                jpvm.pvm_send(buf, client, DaemonMessageTag.jpvmdCreatedTask);
            } catch (JPVMException jpe) {
                perror("CreateTask, \"" + jpe + " sending " + "to client " + client.toString());
            }
            return false;
        }
        return true;
    }

    public void run() {
        boolean wait = doExec();
        while (wait) {
            try {
                process.waitFor();
                Scanner s = new Scanner(process.getInputStream()).useDelimiter("\\A");
                String result = s.hasNext() ? s.next() : "";
                s = new Scanner(process.getErrorStream()).useDelimiter("\\A");
                result += s.hasNext() ? s.next() : "";
                System.out.println(result);
                wait = false;
            } catch (InterruptedException ignored) {

            }
        }
    }

    private void log(String message) {
        if (log_on) {
            System.out.println("jpvm daemon: " + message);
            System.out.flush();
        }
    }

    private void perror(String message) {
        System.err.println("jpvm daemon: " + message);
        System.err.flush();
    }
}