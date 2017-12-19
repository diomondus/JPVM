package jpvm.connection;

import jpvm.tasks.TaskId;

/**
 * Created by Dmitry Butilov
 * on 14.12.17.
 */
public class ConnectionSet {
    private RecvConnectionListNode recvList;
    private SendConnectionListNode sendList;
    private RecvConnectionListNode recvListIter;

    public ConnectionSet() {
        recvList = null;
        sendList = null;
    }

    public synchronized RecvConnection
    lookupRecvConnection(TaskId tid) {
        RecvConnectionListNode tmp = recvList;
        while (tmp != null) {
            if (tmp.conn.tid.equals(tid))
                return tmp.conn;
            tmp = tmp.next;
        }
        return null;
    }

    public synchronized void insertRecvConnection(RecvConnection c) {
        RecvConnectionListNode nw;
        nw = new RecvConnectionListNode(c);
        nw.next = recvList;
        recvList = nw;
    }

    public synchronized RecvConnection
    firstIterRecv() {
        recvListIter = recvList;
        if (recvListIter != null)
            return recvListIter.conn;
        return null;
    }

    public synchronized RecvConnection
    nextIterRecv() {
        if (recvListIter != null)
            recvListIter = recvListIter.next;
        if (recvListIter != null)
            return recvListIter.conn;
        return null;
    }

    public synchronized SendConnection
    lookupSendConnection(TaskId tid) {
        SendConnectionListNode tmp = sendList;
        while (tmp != null) {
            if (tmp.conn.tid.equals(tid))
                return tmp.conn;
            tmp = tmp.next;
        }
        return null;
    }

    public synchronized void insertSendConnection(SendConnection c) {
        SendConnectionListNode nw;
        nw = new SendConnectionListNode(c);
        nw.next = sendList;
        sendList = nw;
    }
}