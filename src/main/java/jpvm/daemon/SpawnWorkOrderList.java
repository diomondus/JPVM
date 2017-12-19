package jpvm.daemon;

/**
 * Created by Dmitry Butilov
 * on 14.12.17.
 */
public class SpawnWorkOrderList {
    private SpawnWorkOrder list = null;
    private int nextOrder = 1;

    SpawnWorkOrderList() {
        list = null;
        nextOrder = 1;
    }

    public SpawnWorkOrder newOrder(int num) {
        SpawnWorkOrder ret;

        ret = new SpawnWorkOrder(nextOrder, num);
        nextOrder++;
        ret.next = list;
        list = ret;
        return ret;
    }

    public SpawnWorkOrder lookup(int order) {
        SpawnWorkOrder ret;

        ret = list;

        while (ret != null) {
            if (ret.order == order)
                return ret;
            ret = ret.next;
        }
        return null;
    }

    public void doneOrder(SpawnWorkOrder order) {
        SpawnWorkOrder tmp;
        if (list == null || order == null)
            return;
        if (order == list) {
            list = list.next;
            return;
        }
        tmp = list;
        while (tmp.next != null) {
            if (tmp.next == order) {
                tmp.next = order.next;
                return;
            }
            tmp = tmp.next;
        }
    }

    public void doneOrder(int order) {
        SpawnWorkOrder tmp;
        if (list == null)
            return;
        if (order == list.order) {
            list = list.next;
            return;
        }
        tmp = list;
        while (tmp.next != null) {
            if (tmp.next.order == order) {
                tmp.next = tmp.next.next;
                return;
            }
            tmp = tmp.next;
        }
    }
}