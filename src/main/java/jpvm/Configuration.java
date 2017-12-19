package jpvm;

import jpvm.tasks.TaskId;

/**
 * Created by Dmitry Butilov
 * on 14.12.17.
 */
public class Configuration {
    public int numHosts;
    public String hostNames[];
    public TaskId hostDaemonTids[];

    public Configuration(int n) {
        numHosts = n;
        hostNames = new String[n];
        hostDaemonTids = new TaskId[n];
    }
}
