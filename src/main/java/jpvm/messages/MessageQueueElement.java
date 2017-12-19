package jpvm.messages;

/**
 * Created by Dmitry Butilov
 * on 14.12.17.
 */
public class MessageQueueElement {
    public Message message;
    public MessageQueueElement next;

    public MessageQueueElement(Message m) {
        message = m;
        next = null;
    }
}
