package jpvm.connection;

/**
 * Created by Dmitry Butilov
 * on 14.12.17.
 */
public class RecvConnectionListNode {
    public RecvConnection conn;
    public RecvConnectionListNode next;

    public RecvConnectionListNode(RecvConnection c) {
        conn = c;
        next = null;
    }
}