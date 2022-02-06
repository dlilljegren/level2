package lilljegren.compact;

import lilljegren.AbstractLevel2ViewTester;
import lilljegren.Level2View;

public class Level2ViewCompactTest extends AbstractLevel2ViewTester {
    @Override
    public Level2View createUnderTest() {
        return new Level2ViewCompact();
    }
}
