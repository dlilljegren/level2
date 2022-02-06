package lilljegren;

import java.math.BigDecimal;

abstract class Instruction {
    final String[] parts;

    protected Instruction(String parts) {
        this.parts = parts.split(":");
    }

    abstract void actOn(Level2View underTest);

    Level2View.Side parseSide(int i) {
        switch (parts[i]){
            case "A": return Level2View.Side.ASK;
            case "B": return Level2View.Side.BID;
            default:
                throw new IllegalArgumentException(parts[i]);
        }
    }
    BigDecimal parsePx(int i){
        return new BigDecimal(parts[i]);
    }

    long parseLong(int i){
        return Long.parseLong(parts[i]);
    }

    static Instruction parse(String s){
        try {
            var parts = s.split("#");
            switch (parts[0]) {
                case "N":
                case "New":
                    return new NewOrder(parts[1]);
                case "C":
                case "Cancel":
                    return new CancelOrder(parts[1]);
                case "R":
                case "Replace":
                    return new ReplaceOrder(parts[1]);
                case "T":
                case "Trade":
                    return new Trade(parts[1]);
                default:
                    throw new IllegalArgumentException("Can't parse:" + parts[0]);
            }
        }catch (Exception e){
            throw new IllegalArgumentException("Can't parse "+s,e);
        }

    }

    static class NewOrder extends Instruction{
        NewOrder(String str){
            super(str);
        }

        @Override
        void actOn(Level2View underTest) {
            var side = parseSide(0);
            var px = parsePx(1);
            var qty = parseLong(2);
            var oId = parseLong(3);
            underTest.onNewOrder(side,px,qty,oId);
        }
    }

    static class CancelOrder extends Instruction{
        CancelOrder(String str){
            super(str);
        }

        @Override
        void actOn(Level2View underTest) {
            var oId = parseLong(0);
            underTest.onCancelOrder(oId);
        }
    }

    static class ReplaceOrder extends Instruction{
        ReplaceOrder(String str){
            super(str);
        }

        @Override
        void actOn(Level2View underTest) {
            var px = parsePx(0);
            var qty = parseLong(1);
            var oId = parseLong(2);
            underTest.onReplaceOrder(px,qty,oId);
        }
    }

    static class Trade extends Instruction{
        Trade(String str){
            super(str);
        }

        @Override
        void actOn(Level2View underTest) {
            var qty = parseLong(0);
            var oId = parseLong(1);
            underTest.onTrade(qty,oId);
        }
    }


}
