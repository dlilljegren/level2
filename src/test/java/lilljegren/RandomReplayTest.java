package lilljegren;

import lilljegren.compact.Level2ViewCompact;
import lilljegren.scalable.Level2ViewScalable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.Math.round;
import static lilljegren.Level2View.Side.ASK;
import static lilljegren.Level2View.Side.BID;

/**
 * As we have two implementation we can create random operations on the OrderBook and just compare that both implementations return the same
 */
public class RandomReplayTest {
    static Random R = new Random();

    int orderIdCounter = 1000;

    @Test
    public void compareCompactWithScalable(){
        int noOfOrders= 1000;
        int timeSlots = 500;
        Stream<Instruction> instructions = IntStream.range(0,noOfOrders)
                .boxed()
                .flatMap( i-> orderLifeCycle(i,timeSlots).stream())
                .sorted(Comparator.comparing(TimedInstruction::getTime))
                .map(TimedInstruction::getInstruction);

        var compact = new Level2ViewCompact();
        var scalable = new Level2ViewScalable();

        instructions.peek( instruction ->{
            instruction.actOn(compact);
            instruction.actOn(scalable);
        }).forEachOrdered( i->{
            //Do the tests
            Assertions.assertEquals(compact.getBookDepth(BID), scalable.getBookDepth(BID));
            Assertions.assertEquals(compact.getBookDepth(ASK), scalable.getBookDepth(ASK));
            Assertions.assertEquals(compact.getTopOfBook(BID), scalable.getTopOfBook(BID));
            Assertions.assertEquals(compact.getTopOfBook(ASK), scalable.getTopOfBook(ASK));
            //
            for(int px=0;px<9;px++) {
                var price = BigDecimal.valueOf(px);
                Assertions.assertEquals(compact.getSizeForPriceLevel(BID, price), scalable.getSizeForPriceLevel(BID, price));
                Assertions.assertEquals(compact.getSizeForPriceLevel(ASK, price), scalable.getSizeForPriceLevel(ASK, price));
            }

        });
    }


    /**
     * The events for a random order
     * @param maxTime number of time slots in the simulation
     * @return 4 operations on the order insert,replace,trade, cancel
     */
    Collection<TimedInstruction> orderLifeCycle(int orderId,int maxTime){
        var side = R.nextBoolean() ? "A" :"B";
        var startPrice = R.nextInt(3) +2;//2,3,4
        if(side.equals("A")){
            startPrice +=3;//5,6,7
        }
        var startQty = (R.nextInt(9)+1)*100;
        var replacePrice = R.nextBoolean() ? startPrice-1 : startPrice+1; // 1 to 8
        var replaceQty = round(startQty / 100.0 * R.nextInt(100) +50); //replace = start * [50% - 150%]
        var tradeQty = round(replaceQty * R.nextDouble());

        var newTemplate = "N#%s:%d:%d:%d";
        var replaceTemplate = "R#%d:%d:%d";
        var tradeTemplate = "T#%d:%d";
        var cancelTemplate = "C#%d";

        //Select 4 time slots between  0 and maxTime
        var times = new ArrayList<Integer>();
        for(int i=0;i<4;i++){
            times.add(R.nextInt(maxTime));
        }
        times.sort(Comparator.naturalOrder());

        var instructions = new ArrayList<TimedInstruction>();
        instructions.add(new TimedInstruction( Instruction.parse( String.format(newTemplate,side,startPrice,startQty,orderId)), times.get(0)-1));//-1 ensure init always first
        instructions.add(new TimedInstruction( Instruction.parse( String.format(replaceTemplate,replacePrice,replaceQty,orderId)), times.get(1)));
        instructions.add(new TimedInstruction( Instruction.parse( String.format(tradeTemplate,tradeQty,orderId)), times.get(2)));

        //In case the trade took all the volume we should not cancel as we will get an error in this implementation
        if(tradeQty != replaceQty){
            instructions.add(new TimedInstruction( Instruction.parse( String.format(cancelTemplate,orderId)), times.get(3)+1));//+1 cancel always last
        }
        return instructions;

    }

    static class TimedInstruction{
        private final Instruction instruction;
        private final int time;

        TimedInstruction(Instruction instruction, int time) {
            this.instruction = instruction;
            this.time = time;
        }


        public int getTime() {
            return time;
        }

        public Instruction getInstruction() {
            return instruction;
        }
    }
}
