package lilljegren.scalable;

import lilljegren.Level2View;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Order with special implementation of equals and hashcode suited for the Page's data structure
 */
class Order {



    private final Level2View.Side side;
    private final long orderId;
    private final BigDecimal price;
    private final long quantity;

    Order(Level2View.Side side, BigDecimal price, long quantity, long orderId) {
        this.side = requireNonNull(side);
        this.price = requireNonNull(price);
        this.quantity = quantity;
        this.orderId = orderId;

        assert price.compareTo(BigDecimal.ZERO) >= 0 :"Price must be greater or equal to 0, was:"+price;
        assert quantity > 0:"Quantity must be greater than 0, was:"+quantity;
    }



    public Level2View.Side getSide() {
        return side;
    }

    /**
     * Not used but likely useful for debugging
     * @return the order id
     */
    public long getOrderId() {
        return orderId;
    }


    public BigDecimal getPrice() {
        return price;
    }

    public long getQuantity() {
        return quantity;
    }

    public Order with(BigDecimal price, long quantity) {
        return new Order(this.side,price,quantity,this.orderId);
    }



    //We determine equality based only on the order id, so that it works in the Page class
    //As this class is only used in this package we can allow for this but one should be careful
    @Override
    public boolean equals(Object o){
        assert o instanceof Order;
        return ((Order)o).orderId == this.orderId;
    }
    @Override
    public int hashCode(){
        return Long.hashCode(orderId);
    }

}
