package jpvm.connection;

/**
 * Created by Dmitry Butilov
 * on 14.12.17.
 */
public class SendConnectionListNode {
    public SendConnection conn;
    public SendConnectionListNode next;

    public SendConnectionListNode(SendConnection c) {
        conn = c;
        next = null;
    }
}