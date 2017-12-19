package examples;

import jpvm.Environment;
import jpvm.JPVMException;
import jpvm.messages.Message;
import jpvm.tasks.TaskId;

class hello {
    static int num_workers = 1;

    public static void main(String args[]) {
        try {
            // Enroll in the parallel virtual machine...
            Environment jpvm = new Environment();

            // Get my task id...
            TaskId mytid = jpvm.pvm_mytid();
            System.out.println("Task Id: " + mytid.toString());

            // Spawn som>e workers...
            TaskId tids[] = new TaskId[num_workers];
            jpvm.pvm_spawn("examples.hello_other", num_workers, tids);

            System.out.println("Worker tasks: ");
            int i;
            for (i = 0; i < num_workers; i++)
                System.out.println("\t" + tids[i].toString());

            // Receive a message from each worker...
            for (i = 0; i < num_workers; i++) {
                // Receive a message...
                Message message = jpvm.pvm_recv();
                System.out.println("Got message tag " +
                        message.messageTag + " from " +
                        message.sourceTid.toString());

                // Unpack the message...
                String str = message.buffer.upkstr();

                System.out.println("Received: " + str);
            }

            // Exit from the parallel virtual machine
            jpvm.pvm_exit();
        } catch (JPVMException jpe) {
            System.out.println("Error - jpvm exception");
        }
    }
};
