package examples;

import jpvm.buffer.Buffer;
import jpvm.Environment;
import jpvm.JPVMException;
import jpvm.tasks.TaskId;

class hello_other {
    static int num_workers = 1;

    public static void main(String args[]) {
        try {
            // Enroll in the parallel virtual machine...
            Environment jpvm = new Environment();

            // Get my parent's task id...
            TaskId parent = jpvm.pvm_parent();

            // Send a message to my parent...
            Buffer buf = new Buffer();
            buf.pack("Hello from jpvm task, id: " + jpvm.pvm_mytid().toString());
            jpvm.pvm_send(buf, parent, 12345);

            // Exit from the parallel virtual machine
            jpvm.pvm_exit();
        } catch (JPVMException ex) {
            System.out.println("Error - jpvm exception");
        }
    }
};
