package lilljegren.scalable;

import lilljegren.Level2View;


import java.math.BigDecimal;
import java.util.*;

/**
 * <pre>
 * This implementation is scalable for very large orderbooks all operations ought to be O(LogN)
 * The downside compared to the Compact version is that we maintain two data structures one map to find the orders based on id
 * and another nested structure in the Page class keyed on the price level. Having many structures makes it hard to be thread safe, without excessive synchronization especially handling errors could become quite tricky
 * Still normally one would manage the threading so it's partitioned over the instruments associated with the books, single threaded almost always both faster and safer
 *
 * As with Compact there are no special handling of invalid arguments, they are just marked with asserts
 * </pre>
 */
public class Level2ViewScalable implements Level2View {

    //Queries always depend on the side, bid and ask the same expect the sorting
    private final EnumMap<Side,Page> sides;

    //To be able to deal with cancel and replace we need a mapping to the order id
    private final Map<Long,Order> id2Order= new HashMap<>();

    public Level2ViewScalable(){
        sides = new EnumMap<>(Side.class);
        for(var s : Side.values()){
            sides.put(s, new Page(s));
        }
        assert sides.size() == 2;
    }

    private Page getPage(Side side){
        return sides.get(side);
    }

    public void onNewOrder(Side side, BigDecimal price, long quantity, long orderId) {
        var newOrder = new Order(side,price,quantity,orderId);

        //Only keep track if order existed
        var prev = id2Order.put(orderId,newOrder);
        assert prev == null : "Order with id "+orderId+" already existed";
        id2Order.put(orderId,newOrder);

        getPage(side).addOrder(newOrder);
    }

    public void onCancelOrder(long orderId) {
        //Remove from outer mapping
        var prev = id2Order.remove(orderId);
        assert prev != null : "Order with id "+orderId+" doesn't exist";
        //Remove from page
        getPage(prev.getSide()).removeOrder(prev);
    }

    public void onReplaceOrder(BigDecimal price, long quantity, long orderId) {
        //Check that we didn't already have it
        var prevVersion = id2Order.get(orderId);
        assert prevVersion != null : "No order with id "+orderId+" exist. Can't replace";
        var nextVersion = prevVersion.with(price,quantity);
        id2Order.put(orderId,nextVersion);

        var page = getPage(prevVersion.getSide());
        page.removeOrder(prevVersion);
        page.addOrder(nextVersion);
    }

    public void onTrade(long quantity, long restingOrderId) {
        //Same as compact version
        var prevVersion = id2Order.get(restingOrderId);
        assert prevVersion != null : "No order with id "+restingOrderId+" exist. Can't trade";

        var remaining = prevVersion.getQuantity()-quantity;
        assert remaining >= 0 : "Can't trade more than quantity:"+quantity+">"+prevVersion.getQuantity();
        if(remaining >0) {
            onReplaceOrder(prevVersion.getPrice(), remaining, restingOrderId);//Re-use
        }
        else {//Can discuss the <
            onCancelOrder(restingOrderId);
        }

    }

    public long getSizeForPriceLevel(Side side, BigDecimal price) {
        return getPage(side).getSizeForPriceLevel(price);
    }

    public long getBookDepth(Side side) {
        return getPage(side).getBookDepth();
    }

    public BigDecimal getTopOfBook(Side side) {
        return getPage(side).getTopOfBook();

    }
}
