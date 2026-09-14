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
}
