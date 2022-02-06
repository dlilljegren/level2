package lilljegren.scalable;

import lilljegren.AbstractLevel2ViewTester;
import lilljegren.Level2View;

class Level2ViewScalableTest extends AbstractLevel2ViewTester {
    @Override
    public Level2View createUnderTest() {
        return new Level2ViewScalable();
    }
}
