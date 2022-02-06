package lilljegren.scalable;

import lilljegren.Level2View;

import java.math.BigDecimal;
import java.util.*;

/**
 * <pre>
 * Maintain the orders for a given side
 * Orders are kept in a kind of MultiMap keyed on the price, with a set of orders ( keyed only on the order id )
 * This makes it very easy to implement the get methods, but some extra care is required when adding and removing the orders
 * </pre>
 */
class Page {

    //Note as TreeMap use compare and not equals we can use BigDecimal as a key without having to worry about the scaling
    private final TreeMap<BigDecimal, Set<Order>> level2Order;

    Page(Level2View.Side side){
        Comparator<BigDecimal> c = Comparator.naturalOrder();//lowest ask is best
        if(side == Level2View.Side.BID){
            c= c.reversed();
        }
        this.level2Order = new TreeMap<>(c);
    }

    void addOrder(Order order){
        var ordersOnLevel =level2Order.computeIfAbsent(order.getPrice(), o->new HashSet<>());//see equals and hashcode in Order class
        ordersOnLevel.add(order);
    }

    void removeOrder(Order order){
        var ordersOnLevel =level2Order.get(order.getPrice());
        assert ordersOnLevel != null;
        ordersOnLevel.remove(order);

        //If the removed order was the last we must remove the level from the TreeMap, in order to keep book depth simple
        if(ordersOnLevel.isEmpty()){
            level2Order.remove(order.getPrice());
        }
    }



    long getSizeForPriceLevel(BigDecimal price){
        return level2Order.getOrDefault(price,Set.of()).stream().mapToLong(Order::getQuantity).sum();
    }

    // get the number of price levels on the specified side
    long getBookDepth(){
        return level2Order.size();
    }

    BigDecimal getTopOfBook(){
        if(level2Order.isEmpty()){
            return null;
        }
        return level2Order.firstKey();
    }
}
