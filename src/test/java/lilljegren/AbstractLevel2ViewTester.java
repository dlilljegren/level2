package lilljegren;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;

import static java.lang.String.format;
import static lilljegren.Instruction.parse;
import static lilljegren.Level2View.Side.ASK;
import static lilljegren.Level2View.Side.BID;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test helper for Level2 object
 */
@SuppressWarnings("BigDecimalMethodWithoutRoundingCalled")
public abstract class AbstractLevel2ViewTester
{
    protected abstract Level2View createUnderTest();




    @Test
    public void getBookDepth(){
        var underTest = createUnderTest();
        assertEquals(0,underTest.getBookDepth(ASK));
        assertEquals(0,underTest.getBookDepth(BID));

        parse("N#B:1.00:1000:10").actOn(underTest);
        assertEquals(1,underTest.getBookDepth(BID));
        parse("N#B:1.05:1000:12").actOn(underTest);
        assertEquals(2,underTest.getBookDepth(BID));
        parse("N#A:2.00:1000:11").actOn(underTest);
        parse("N#A:2.00:1000:13").actOn(underTest);


        assertEquals(1,underTest.getBookDepth(ASK));
        assertEquals(2,underTest.getBookDepth(BID));

        //Tricky case when the BigDecimals are not equal
        parse("N#A:3.000:1000:15").actOn(underTest);
        parse("N#A:3.00:1000:17").actOn(underTest);
        //A "bad" implementation distinguishes between 3.00 and 3.000
        assertEquals(2,underTest.getBookDepth(ASK));//Two levels 2.0 and 3.0

    }

    @Test
    public void getSizeForPriceLevel(){
        var underTest = createUnderTest();
        //Check empty book doesnt give error
        assertEquals(0,underTest.getSizeForPriceLevel(ASK,BigDecimal.TEN));
        assertEquals(0,underTest.getSizeForPriceLevel(BID,BigDecimal.TEN));

        parse("N#B:1.00:1000:10").actOn(underTest);
        parse("N#B:1.05:2000:12").actOn(underTest);

        parse("N#A:2.00:3000:11").actOn(underTest);
        parse("N#A:2.000:4000:13").actOn(underTest);

        assertEquals(1000,underTest.getSizeForPriceLevel(BID, new BigDecimal("1.000")));
        assertEquals(2000,underTest.getSizeForPriceLevel(BID, new BigDecimal("1.05")));

        assertEquals(0,underTest.getSizeForPriceLevel(ASK, new BigDecimal("1.000")));
        assertEquals(7000,underTest.getSizeForPriceLevel(ASK, new BigDecimal("2.000")));

        //Bid on same level as ask should not matter
        parse("N#B:2.00:5000:14").actOn(underTest);
        assertEquals(7000,underTest.getSizeForPriceLevel(ASK, new BigDecimal("2.000")));
        assertEquals(5000,underTest.getSizeForPriceLevel(BID, new BigDecimal("2.000")));
    }

    @Test
    public void getTopOfBook(){
        var underTest = createUnderTest();
        assertNull(underTest.getTopOfBook(ASK));
        assertNull(underTest.getTopOfBook(BID));

        parse("N#B:1.00:1000:10").actOn(underTest);
        parse("N#B:1.05:2000:12").actOn(underTest);

        parse("N#A:2.00:3000:11").actOn(underTest);
        parse("N#A:2.00:4000:13").actOn(underTest);


        //The scale is needed for equals to work
        assertEquals(new BigDecimal("1.05").setScale(5),(underTest.getTopOfBook(BID).setScale(5)));
        assertEquals(new BigDecimal("2.0000").setScale(5), underTest.getTopOfBook(ASK).setScale(5));
    }



    @Test
    public void insert(){
        var underTest = createUnderTest();
        parse("N#B:1.00:1000:10").actOn(underTest);
        parse("N#B:1.05:2000:12").actOn(underTest);

        //Check we can't insert order with same id
        assertThrows(Throwable.class, ()->parse("N#B:1.00:1000:10").actOn(underTest));
        //Even if we try the other side
        assertThrows(Throwable.class, ()->parse("N#A:1.00:1000:10").actOn(underTest));

        //Check inserting bad price won't work
        var pt = assertThrows(Throwable.class, () -> parse("N#A:-1.00:1000:14").actOn(underTest));
        assertEquals("Price must be greater or equal to 0, was:-1.00",pt.getMessage());

        //Check inserting bad quantity won't work
        var t = assertThrows(Throwable.class, () -> parse("N#A:1.00:-1000:14").actOn(underTest));
        assertEquals("Quantity must be greater than 0, was:-1000",t.getMessage());

    }

    @ParameterizedTest
    @EnumSource(Level2View.Side.class)
    public void insertAndCancel(Level2View.Side side){
        String s = side==ASK ? "A" :"B";

        var underTest = createUnderTest();
        parse(format("N#%s:1.00:1000:10",s)).actOn(underTest);
        parse(format("N#%s:1.05:2000:12",s)).actOn(underTest);
        assertEquals(2, underTest.getBookDepth(side));

        parse("C#10").actOn(underTest);

        assertEquals(1, underTest.getBookDepth(side));
        parse(format("N#%s:1.00:2000:10",s)).actOn(underTest);//Check we can re-use id 10
        assertEquals(2, underTest.getBookDepth(side));

        //Check something is thrown if we try to delete non existing order
        //Don't check specific as not quite specified in API
        assertThrows(Throwable.class, ()->parse(format("N#%s:1.00:2000:10",s)).actOn(underTest));
    }

    @ParameterizedTest
    @EnumSource(Level2View.Side.class)
    public void insertAndReplace(Level2View.Side side){
        String s = side==ASK ? "A" :"B";

        var underTest = createUnderTest();
        //Insert at same level
        parse(format("N#%s:1.00:1000:10",s)).actOn(underTest);
        parse(format("N#%s:1.00:2000:12",s)).actOn(underTest);
        assertEquals(1000+2000, underTest.getSizeForPriceLevel(side, BigDecimal.ONE));
        assertEquals(1, underTest.getBookDepth(side));


        //Change the quantity
        parse("R#1.00:600:12").actOn(underTest);
        assertEquals(1000+600, underTest.getSizeForPriceLevel(side, BigDecimal.ONE));
        assertEquals(1, underTest.getBookDepth(side));

        //Changing quantity to 0 not allowed no implicit delete
        assertThrows(Throwable.class, ()->parse("R#1.00:0:12").actOn(underTest));
        //But state should be the same after error
        assertEquals(1000+600, underTest.getSizeForPriceLevel(side, BigDecimal.ONE));
        assertEquals(1, underTest.getBookDepth(side));

        //Change the price
        parse("R#2.00:600:12").actOn(underTest);
        //Size at level 1 is now only 1000
        assertEquals(1000, underTest.getSizeForPriceLevel(side, BigDecimal.ONE));
        //Size is 600 at level 2
        assertEquals(600, underTest.getSizeForPriceLevel(side, new BigDecimal("2")));
        //Total levels now 2
        assertEquals(2, underTest.getBookDepth(side));


        //Error if replacing order id that doesn't exist
        assertThrows(Throwable.class, ()->parse("R#1.00:0:42").actOn(underTest));
    }

    @ParameterizedTest
    @EnumSource(Level2View.Side.class)
    public void trade(Level2View.Side side) {
        String s = side == ASK ? "A" : "B";
        var underTest = createUnderTest();
        //Add order id=10
        parse(format("N#%s:1.00:1000:10", s)).actOn(underTest);

        //Error if quantity to large ??
        //assertThrows(Throwable.class, ()->parse("T#1001:10").actOn(underTest));

        //Trade 600
        parse("T#600:10").actOn(underTest);
        //Volume on level 1.00 should now be 400
        assertEquals(400, underTest.getSizeForPriceLevel(side, BigDecimal.ONE));

        //Trade remaining
        parse("T#400:10").actOn(underTest);
        //Book should now be empty
        assertEquals(0, underTest.getBookDepth(side));

    }
}
