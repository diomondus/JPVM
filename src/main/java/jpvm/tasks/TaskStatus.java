package jpvm.tasks;

/**
 * Created by Dmitry Butilov
 * on 14.12.17.
 */
public class TaskStatus {
    public String hostName;
    public int numTasks;
    public String taskNames[];
    public TaskId taskTids[];

    public TaskStatus() {
        hostName = null;
        numTasks = 0;
        taskNames = null;
        taskTids = null;
    }
}
