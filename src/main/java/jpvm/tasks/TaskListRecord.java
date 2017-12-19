package jpvm.tasks;

/**
 * Created by Dmitry Butilov
 * on 14.12.17.
 */
public class TaskListRecord {
    public TaskId tid;
    public String name;
    public TaskListRecord next;

    public TaskListRecord(TaskId t, String n) {
        tid = t;
        name = n;
        next = null;
    }
}