package life.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PositionTest {

    @Test
    void translateShiftsEachAxisSeparately() {
        Position p = new Position(3, 7);
        assertEquals(new Position(5, 7), p.translate(2, 0));
        assertEquals(new Position(3, 4), p.translate(0, -3));
        assertEquals(new Position(2, 8), p.translate(-1, 1));
        assertEquals(new Position(3, 7), p, "исходная позиция не меняется");
    }

    @Test
    void positionsWithSameCoordinatesAreEqual() {
        assertEquals(new Position(1, 2), new Position(1, 2));
        assertNotEquals(new Position(1, 2), new Position(2, 1));
    }

    @Test
    void distanceSquaredUsesBothAxes() {
        assertEquals(25, new Position(1, 2).distanceSquaredTo(new Position(4, 6)));
        assertEquals(9, new Position(1, 2).distanceSquaredTo(new Position(4, 2)));
        assertEquals(16, new Position(1, 2).distanceSquaredTo(new Position(1, 6)));
    }

    @Test
    void distanceIsSymmetricAndZeroToItself() {
        Position a = new Position(-3, 5);
        Position b = new Position(2, -1);
        assertEquals(a.distanceSquaredTo(b), b.distanceSquaredTo(a));
        assertEquals(61, a.distanceSquaredTo(b));
        assertEquals(0, a.distanceSquaredTo(a));
    }

    @Test
    void distanceDoesNotOverflow() {
        long max = Integer.MAX_VALUE;
        assertEquals(max * max, new Position(0, 0).distanceSquaredTo(new Position(Integer.MAX_VALUE, 0)));
    }
}
