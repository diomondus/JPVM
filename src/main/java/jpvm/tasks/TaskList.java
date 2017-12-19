package jpvm.tasks;

/**
 * Created by Dmitry Butilov
 * on 14.12.17.
 */
public class TaskList {
    TaskListRecord tasks;
    int num_tasks;
    TaskListRecord iter;

    public TaskList() {
        tasks = null;
        num_tasks = 0;
        iter = null;
    }

    public int numTasks() {
        return num_tasks;
    }

    public void addTask(TaskId tid, String name) {
        if (find(tid) != null) {
            // Already know about this task...
            return;
        }
        TaskListRecord nw = new TaskListRecord(tid, name);
        nw.next = tasks;
        tasks = nw;
        num_tasks++;
    }

    public void deleteTask(TaskId tid) {
        if (tasks == null) return;
        TaskListRecord tmp = tasks;

        // Check head
        if (tmp.tid.equals(tid)) {
            if (iter == tmp) iter = tmp.next;
            tasks = tasks.next;
            num_tasks--;
            return;
        }
        // Check body
        while (tmp.next != null) {
            if (tmp.next.tid.equals(tid)) {
                if (iter == tmp.next) iter = tmp.next.next;
                tmp.next = tmp.next.next;
                num_tasks--;
                return;
            }
            tmp = tmp.next;
        }
    }

    public TaskListRecord find(TaskId tid) {
        TaskListRecord tmp = tasks;
        while (tmp != null) {
            if (tmp.tid.equals(tid))
                return tmp;
            tmp = tmp.next;
        }
        return tmp;
    }

    public TaskListRecord firstIter() {
        if (tasks == null) return null;
        TaskListRecord ret = tasks;
        iter = tasks.next;
        return ret;
    }

    public TaskListRecord nextIter() {
        if (iter == null) return null;
        TaskListRecord ret = iter;
        iter = iter.next;
        return ret;
    }
}