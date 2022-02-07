package lilljegren.compact;

import lilljegren.Level2View;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

final class Order {

    static Comparator<Order> BEST_BID = Comparator.comparing(Order::getPrice);//These need tests easy to confuse
    static Comparator<Order> BEST_ASK = Comparator.comparing(Order::getPrice).reversed();

    private final long orderId;
    private final Level2View.Side side;
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



    /**
     *
     * @param side the side
     * @return a comparator that for a given list of orders ( all of the same side ) sorts the best on top
     */
    public static Comparator<? super Order> bestComparatorFor(Level2View.Side side) {
        if(Objects.requireNonNull(side)==Level2View.Side.ASK){
            return BEST_ASK;
        }
        else {
            return BEST_BID;
        }
    }

    public long getOrderId() {
        return orderId;
    }

    public Level2View.Side getSide() {
        return side;
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

    /**
     *
     * @param aPrice price level
     * @return true if this orders price matches a price
     */
    public boolean isAtPrice(BigDecimal aPrice) {
        //Note that equals is not a good way to compare BigDecimals as depending on the mantissa,scale
        //two BigDecimals representing the same value may not be equal
        return requireNonNull(aPrice).compareTo(this.price) == 0;
    }

    public boolean hasSide(Level2View.Side side) {
        return this.side == side;//ok to compare enums by reference
    }
}
