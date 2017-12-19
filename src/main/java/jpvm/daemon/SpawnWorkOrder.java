package jpvm.daemon;

import jpvm.tasks.TaskId;

/**
 * Created by Dmitry Butilov
 * on 14.12.17.
 */
public class SpawnWorkOrder {
    public int order;    // Which partially completed spawn
    public TaskId tids[]; // Tids spawned
    public int num;    // Number to spawn
    public int numDone;// Number actually done
    public TaskId client; // Who placed the order?
    SpawnWorkOrder next;   // Linked list

    SpawnWorkOrder(int o, int n) {
        order = 0;
        num = n;
        tids = new TaskId[n];
        numDone = 0;
        client = null;
        next = null;
    }
}