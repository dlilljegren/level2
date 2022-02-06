package lilljegren.scalable;

import lilljegren.Level2View;

import java.math.BigDecimal;
import java.util.*;

class Page {

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

        //If the removed order was the last we must remove the level from the TreeMap, to keep book depth simple
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
