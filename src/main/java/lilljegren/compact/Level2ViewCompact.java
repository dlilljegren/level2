package lilljegren.compact;

import lilljegren.Level2View;

import java.math.BigDecimal;
import java.util.*;

import static java.util.stream.Collectors.toCollection;

/**
 * Simple implementation using a single Map, inserts and cancels are O(1) while queries are O(N)
 *
 * Edge cases are just marked with assertions, how to treat them would depend on the context.
 * If this component is called by code in the same module that would make the checks ( or simply ensuring consistency) then asserts may be sufficient ( especially if performance is important)
 * Otherwise either log or throw Exceptions ( e.g. IllegalArgumentException("Order with id=302 doesnt exist")
 *
 */
public class Level2ViewCompact implements Level2View {
    //We assume single threaded otherwise use ConcurrentMap
    private final Map<Long, Order> id2Orders = new HashMap<>();

    public void onNewOrder(Side side, BigDecimal price, long quantity, long orderId) {
        var newOrder = new Order(side,price,quantity,orderId);
        var prev = id2Orders.put(orderId,newOrder);
        assert prev == null : "Order with id "+orderId+" already existed";
    }

    public void onCancelOrder(long orderId) {
        //Just remove
        var prev = id2Orders.remove(orderId);
        assert prev != null : "Order with id "+orderId+" doesn't exist";
    }

    public void onReplaceOrder(BigDecimal price, long quantity, long orderId) {
        //Check that we didn't already have it
        var prevVersion = id2Orders.get(orderId);
        assert prevVersion != null : "No order with id "+orderId+" exist. Can't replace";
        //Replace with new version
        id2Orders.put(orderId,prevVersion.with(price,quantity));
    }

    public void onTrade(long quantity, long restingOrderId) {
        var prevVersion = id2Orders.get(restingOrderId);
        assert prevVersion != null : "No order with id "+restingOrderId+" exist. Can't trade";

        var remaining = prevVersion.getQuantity()-quantity;
        assert remaining >=0 : "Can't trade more than quantity:"+quantity+">"+prevVersion.getQuantity();
        if(remaining >0) {
            onReplaceOrder(prevVersion.getPrice(), remaining, restingOrderId);//Re-use
        }
        else {//Can discuss the <
            onCancelOrder(restingOrderId);
        }

    }

    public long getSizeForPriceLevel(Side side, BigDecimal price) {
        return id2Orders.values().stream()
                .filter(o->o.isAtPrice(price))//Get all orders at the level
                .filter(o->o.hasSide(side))//Make sure they are on the correct side
                .mapToLong(Order::getQuantity)//Get the quantity
                .sum();//Sum
    }

    public long getBookDepth(Side side) {
        return id2Orders.values().stream()
                .filter(o->o.hasSide(side))//All orders of the side
                .map(Order::getPrice)
                //.distinct().count();//neater, but doesn't work well on BigDecimal 2.00 and 2.000 are NOT distinctm would be ok if we new the scale of the BDs are always the same
                .collect(toCollection(TreeSet::new))//Tree set works because it uses compare and not equals (see java doc NavigableSet)
                .size();
    }

    public BigDecimal getTopOfBook(Side side) {
        return id2Orders.values().stream()
                .filter(o->o.hasSide(side))//Make sure they are on the correct side
                .max(Order.bestComparatorFor(side))
                .map(Order::getPrice)
                .orElse(null);//This to discuss interface not clear, null or error or change interface to Optional

    }
}
