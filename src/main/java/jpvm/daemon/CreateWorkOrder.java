package jpvm.daemon;

import jpvm.tasks.TaskId;

/**
 * Created by Dmitry Butilov
 * on 14.12.17.
 */
public class CreateWorkOrder {
    public int order;
    public TaskId client;
    public boolean outstanding;

    CreateWorkOrder() {
        order = 0;
        client = null;
        outstanding = false;
    }
}
