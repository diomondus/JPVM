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

    public synchronized RecvConnection lookupRecvConnection(TaskId tid) {
        RecvConnectionListNode list = recvList;
        while (list != null) {
            if (list.conn.tid.equals(tid)) {
                return list.conn;
            }
            list = list.next;
        }
        return null;
    }

    public synchronized void insertRecvConnection(RecvConnection c) {
        RecvConnectionListNode list = new RecvConnectionListNode(c);
        list.next = recvList;
        recvList = list;
    }

    public synchronized RecvConnection firstIterRecv() {
        recvListIter = recvList;
        if (recvListIter != null)
            return recvListIter.conn;
        return null;
    }

    public synchronized RecvConnection nextIterRecv() {
        if (recvListIter != null) {
            recvListIter = recvListIter.next;
        }
        if (recvListIter != null) {
            return recvListIter.conn;
        }
        return null;
    }

    public synchronized SendConnection lookupSendConnection(TaskId tid) {
        SendConnectionListNode list = sendList;
        while (list != null) {
            if (list.conn.tid.equals(tid)) {
                return list.conn;
            }
            list = list.next;
        }
        return null;
    }

    public synchronized void insertSendConnection(SendConnection c) {
        SendConnectionListNode list;
        list = new SendConnectionListNode(c);
        list.next = sendList;
        sendList = list;
    }
}