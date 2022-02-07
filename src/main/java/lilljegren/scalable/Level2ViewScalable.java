package lilljegren.scalable;

import lilljegren.Level2View;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * <pre>
 * This implementation is scalable for very large orderbooks all operations ought to be O(LogN)
 * The downside compared to the Compact version is that we maintain two data structures one map to find the orders based on id
 * and another nested structure in the Page class keyed on the price level. Having many structures makes it hard to be thread safe, without excessive synchronization especially handling errors could become quite tricky
 * Still normally one would manage the threading so it's partitioned over the instruments associated with the books, single threaded almost always both faster and safer
 *
 * Each side of the book is managed by an instance of the Page class
 *
 * This version will throw IllegalArgumentException for bad arguments, care is taken to only modify data structure if all args are ok
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
        return sides.get(requireNonNull(side));
    }

    @Override
    public void onNewOrder(Side side, BigDecimal price, long quantity, long orderId) {
        if (id2Order.containsKey(orderId)) {
            throw new IllegalArgumentException(format("An order with id:[%d] already exist", orderId));
        }
        var newOrder = new Order(side, price, quantity, orderId);//Args are checked in order constructor
        id2Order.put(orderId, newOrder);
        getPage(side).addOrder(newOrder);
    }

    @Override
    public void onCancelOrder(long orderId) {
        //Remove from outer mapping
        var prev = id2Order.remove(orderId);
        if (prev == null) {
            throw createOrderMissingException(orderId);
        }
        //Remove from page
        getPage(prev.getSide()).removeOrder(prev);
    }

    @Override
    public void onReplaceOrder(BigDecimal price, long quantity, long orderId) {
        //Check that we didn't already have it
        var prev = id2Order.get(orderId);
        if (prev == null) {
            throw createOrderMissingException(orderId);
        }
        var nextVersion = prev.with(price, quantity);//px and qty args will be checked here
        id2Order.put(orderId, nextVersion);

        var page = getPage(prev.getSide());
        page.removeOrder(prev);
        page.addOrder(nextVersion);
    }

    @Override
    public void onTrade(long quantity, long restingOrderId) {
        if (quantity < 0) {
            throw new IllegalArgumentException(format("quantity can't be less than 0 was:[%d]", quantity));
        }
        //Same as compact version
        var prev = id2Order.get(restingOrderId);
        if (prev == null) {
            throw createOrderMissingException(restingOrderId);
        }

        var remaining = prev.getQuantity() - quantity;
        if (remaining > 0) {
            onReplaceOrder(prev.getPrice(), remaining, restingOrderId);//Re-use
        } else {//Can discuss the if one should throw exception if trade quantity is higher than remaining
            onCancelOrder(restingOrderId);
        }
    }


    @Override
    public long getSizeForPriceLevel(Side side, BigDecimal price) {

        return getPage(side).getSizeForPriceLevel(requireNonNull(price));
    }

    @Override
    public long getBookDepth(Side side) {
        return getPage(side).getBookDepth();
    }

    @Override
    public BigDecimal getTopOfBook(Side side) {
        return getPage(side).getTopOfBook();

    }

    private IllegalArgumentException createOrderMissingException(long orderId) {
        return new IllegalArgumentException(format("No order with id:[%d] exist", orderId));
    }
}
