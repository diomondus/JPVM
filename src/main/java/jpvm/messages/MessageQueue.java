package jpvm.messages;

import jpvm.tasks.TaskId;

/**
 * Created by Dmitry Butilov
 * on 14.12.17.
 */
public class MessageQueue {
    private MessageQueueElement list_head;
    private MessageQueueElement list_tail;

    private synchronized void addElement(MessageQueueElement nw) {
        if (list_head == null) {
            list_head = list_tail = nw;
            return;
        }
        list_tail.next = nw;
        list_tail = nw;
    }

    private synchronized void deleteElement(MessageQueueElement d) {
        if (list_head == null) return;
        if (list_head == d) {
            // Deleting head element.
            list_head = d.next;
            if (list_tail == d) list_tail = null;
            return;
        }
        MessageQueueElement tmp = list_head;
        while (tmp.next != d) {
            tmp = tmp.next;
            if (tmp == null) {
                // Element wasn't in the list
                return;
            }
        }
        tmp.next = d.next;
        if (list_tail == d) list_tail = tmp;
    }

    private synchronized MessageQueueElement find() {
        return list_head;
    }

    private synchronized MessageQueueElement find(int tag) {
        MessageQueueElement tmp = list_head;
        while (tmp != null) {
            if (tmp.message.messageTag == tag) return tmp;
            tmp = tmp.next;
        }
        return null;
    }

    private synchronized MessageQueueElement find(TaskId tid) {
        MessageQueueElement tmp = list_head;
        while (tmp != null) {
            if (tmp.message.sourceTid.equals(tid)) return tmp;
            tmp = tmp.next;
        }
        return null;
    }

    private synchronized MessageQueueElement find(TaskId tid,
                                                  int tag) {
        MessageQueueElement tmp = list_head;
        while (tmp != null) {
            if ((tmp.message.sourceTid.equals(tid)) &&
                    (tmp.message.messageTag == tag))
                return tmp;
            tmp = tmp.next;
        }
        return null;
    }

    public MessageQueue() {
        list_head = list_tail = null;
    }

    public synchronized void enqueue(Message m) {
        MessageQueueElement nw = new MessageQueueElement(m);
        addElement(nw);
        notifyAll();
    }

    public synchronized boolean probe() {
        MessageQueueElement tmp = find();
        return (tmp != null);
    }

    public synchronized boolean probe(int tag) {
        MessageQueueElement tmp = find(tag);
        return (tmp != null);
    }

    public synchronized boolean probe(TaskId tid) {
        MessageQueueElement tmp = find(tid);
        return (tmp != null);
    }

    public synchronized boolean probe(TaskId tid, int tag) {
        MessageQueueElement tmp = find(tid, tag);
        return (tmp != null);
    }

    public synchronized Message dequeue() {
        MessageQueueElement tmp = null;
        while (true) {
            if ((tmp = find()) != null) {
                deleteElement(tmp);
                return tmp.message;
            }
            try {
                wait();
            } catch (InterruptedException ie) {
            }
        }
    }

    public synchronized Message dequeue(int tag) {
        MessageQueueElement tmp = null;
        while (true) {
            if ((tmp = find(tag)) != null) {
                deleteElement(tmp);
                return tmp.message;
            }
            try {
                wait();
            } catch (InterruptedException ie) {
            }
        }
    }

    public synchronized Message dequeue(TaskId tid) {
        MessageQueueElement tmp = null;
        while (true) {
            if ((tmp = find(tid)) != null) {
                deleteElement(tmp);
                return tmp.message;
            }
            try {
                wait();
            } catch (InterruptedException ie) {
            }
        }
    }

    public synchronized Message dequeue(TaskId tid, int tag) {
        MessageQueueElement tmp = null;
        while (true) {
            if ((tmp = find(tid, tag)) != null) {
                deleteElement(tmp);
                return tmp.message;
            }
            try {
                wait();
            } catch (InterruptedException ie) {
            }
        }
    }

    public synchronized Message dequeueNonBlock() {
        MessageQueueElement tmp = find();
        if (tmp != null) {
            deleteElement(tmp);
            return tmp.message;
        }
        return null;
    }

    public synchronized Message dequeueNonBlock(int tag) {
        MessageQueueElement tmp = find(tag);
        if (tmp != null) {
            deleteElement(tmp);
            return tmp.message;
        }
        return null;
    }

    public synchronized Message dequeueNonBlock(TaskId tid) {
        MessageQueueElement tmp = find(tid);
        if (tmp != null) {
            deleteElement(tmp);
            return tmp.message;
        }
        return null;
    }

    public synchronized Message dequeueNonBlock(TaskId tid, int tag) {
        MessageQueueElement tmp = find(tid, tag);
        if (tmp != null) {
            deleteElement(tmp);
            return tmp.message;
        }
        return null;
    }
}