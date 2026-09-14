package life.model;

import life.model.TestAgents.Stone;
import life.model.TestAgents.Tree;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class WorldTest {

    private static Position at(int x, int y) {
        return new Position(x, y);
    }

    // ---------- создание ----------

    @Test
    void smallestWorldIsOneCell() {
        World world = new World(1, 1);
        assertEquals(1, world.getWidth());
        assertEquals(1, world.getHeight());
    }

    @ParameterizedTest(name = "поле {0}×{1} создать нельзя")
    @CsvSource({"0,1", "1,0", "-5,5", "5,-5"})
    void worldSizeMustBePositive(int width, int height) {
        assertThrows(IllegalArgumentException.class, () -> new World(width, height));
    }

    @Test
    void newWorldIsEmpty() {
        World world = new World(4, 3);
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 4; x++) {
                assertNull(world.getAgent(x, y));
                assertTrue(world.isEmpty(at(x, y)));
            }
        }
        assertEquals(List.of(), world.getAgents());
    }

    // ---------- границы поля ----------

    @ParameterizedTest(name = "({0},{1}) внутри поля 5×3")
    @CsvSource({"0,0", "4,0", "0,2", "4,2", "2,1"})
    void cellsInsideWorld(int x, int y) {
        assertTrue(new World(5, 3).isInside(at(x, y)));
    }

    @ParameterizedTest(name = "({0},{1}) снаружи поля 5×3")
    @CsvSource({"-1,0", "5,0", "0,-1", "0,3", "5,3", "-1,-1"})
    void cellsOutsideWorld(int x, int y) {
        World world = new World(5, 3);
        assertFalse(world.isInside(at(x, y)));
        assertFalse(world.isEmpty(at(x, y)), "клетка за краем не считается свободной");
        assertThrows(IllegalArgumentException.class, () -> world.getAgent(x, y));
        assertThrows(IllegalArgumentException.class, () -> world.setAgent(x, y, new Stone()));
    }

    @Test
    void widthAndHeightAreNotSwapped() {
        World world = new World(5, 2); // 5 столбцов, 2 строки
        Stone stone = new Stone();
        world.setAgent(4, 1, stone);
        assertSame(stone, world.getAgent(at(4, 1)));
        assertThrows(IllegalArgumentException.class, () -> world.getAgent(1, 4));
    }

    // ---------- размещение и удаление ----------

    @Test
    void placedAgentCanBeFoundAndKnowsItsPosition() {
        World world = new World(5, 5);
        Stone stone = new Stone();
        world.setAgent(2, 3, stone);

        assertSame(stone, world.getAgent(2, 3));
        assertSame(stone, world.getAgent(at(2, 3)));
        assertEquals(at(2, 3), stone.getPosition());
        assertFalse(world.isEmpty(at(2, 3)));
        assertTrue(world.isEmpty(at(3, 2)), "соседняя клетка должна остаться пустой");
    }

    @Test
    void agentNotOnWorldHasNoPosition() {
        assertNull(new Stone().getPosition());
    }

    @Test
    void cellHoldsOnlyOneAgent() {
        World world = new World(3, 3);
        Stone first = new Stone();
        Stone second = new Stone();
        world.setAgent(1, 1, first);

        assertThrows(IllegalStateException.class, () -> world.setAgent(1, 1, second));
        assertSame(first, world.getAgent(1, 1), "первый житель должен остаться на месте");
        assertNull(second.getPosition(), "второго жителя не должно быть на поле");
        assertEquals(List.of(first), world.getAgents());
    }

    @Test
    void agentCannotStandInTwoCells() {
        World world = new World(3, 3);
        Stone stone = new Stone();
        world.setAgent(0, 0, stone);

        assertThrows(IllegalStateException.class, () -> world.setAgent(2, 2, stone));
        assertTrue(world.isEmpty(at(2, 2)));
        assertEquals(at(0, 0), stone.getPosition());
    }

    @Test
    void placingSameAgentIntoItsOwnCellIsAlsoRejected() {
        World world = new World(3, 3);
        Stone stone = new Stone();
        world.setAgent(1, 1, stone);
        assertThrows(IllegalStateException.class, () -> world.setAgent(1, 1, stone));
        assertSame(stone, world.getAgent(1, 1));
    }

    @Test
    void settingNullClearsTheCell() {
        World world = new World(3, 3);
        Stone stone = new Stone();
        world.setAgent(1, 1, stone);
        world.setAgent(1, 1, null);

        assertTrue(world.isEmpty(at(1, 1)));
        assertNull(stone.getPosition());
        assertEquals(List.of(), world.getAgents());
    }

    @Test
    void clearingEmptyCellDoesNothing() {
        World world = new World(3, 3);
        assertDoesNotThrow(() -> world.setAgent(1, 1, null));
        assertTrue(world.isEmpty(at(1, 1)));
    }

    @Test
    void removedAgentLeavesTheWorld() {
        World world = new World(3, 3);
        Stone stone = new Stone();
        Stone other = new Stone();
        world.setAgent(1, 1, stone);
        world.setAgent(2, 2, other);

        world.removeAgent(stone);

        assertTrue(world.isEmpty(at(1, 1)));
        assertNull(stone.getPosition());
        assertEquals(List.of(other), world.getAgents(), "остальные жители не должны пострадать");
    }

    @Test
    void removingAgentThatIsNotOnWorldFails() {
        World world = new World(3, 3);
        assertThrows(IllegalStateException.class, () -> world.removeAgent(new Stone()));
    }

    @Test
    void removedAgentCanBePlacedAgain() {
        World world = new World(3, 3);
        Stone stone = new Stone();
        world.setAgent(0, 0, stone);
        world.removeAgent(stone);
        world.setAgent(2, 2, stone);
        assertEquals(at(2, 2), stone.getPosition());
    }

    @Test
    void agentFromAnotherWorldIsRejected() {
        World first = new World(3, 3);
        World second = new World(3, 3);
        Stone stone = new Stone();
        first.setAgent(1, 1, stone);

        assertThrows(IllegalStateException.class, () -> second.removeAgent(stone));
        assertThrows(IllegalStateException.class, () -> second.moveAgent(stone, at(0, 0)));
        assertSame(stone, first.getAgent(1, 1));
    }

    @Test
    void agentFromAnotherWorldDoesNotRemoveWhoeverStandsAtSameCoordinates() {
        World first = new World(3, 3);
        World second = new World(3, 3);
        Stone stranger = new Stone();
        Stone local = new Stone();
        first.setAgent(1, 1, stranger);
        second.setAgent(1, 1, local);

        assertThrows(IllegalStateException.class, () -> second.removeAgent(stranger));
        assertThrows(IllegalStateException.class, () -> second.moveAgent(stranger, at(0, 0)));
        assertSame(local, second.getAgent(1, 1), "чужой житель не должен выселить местного");
        assertEquals(at(1, 1), local.getPosition());
        assertTrue(second.isEmpty(at(0, 0)));
    }

    @Test
    void agentFromBiggerWorldGivesClearErrorInsteadOfCrash() {
        World big = new World(10, 10);
        World small = new World(3, 3);
        Stone stone = new Stone();
        big.setAgent(7, 8, stone);

        assertThrows(IllegalStateException.class, () -> small.removeAgent(stone));
        assertThrows(IllegalStateException.class, () -> small.moveAgent(stone, at(0, 0)));
    }

    // ---------- перемещение ----------

    @Test
    void movedAgentChangesCell() {
        World world = new World(4, 4);
        Stone stone = new Stone();
        world.setAgent(1, 1, stone);

        world.moveAgent(stone, at(2, 3));

        assertTrue(world.isEmpty(at(1, 1)), "старая клетка освободилась");
        assertSame(stone, world.getAgent(2, 3));
        assertEquals(at(2, 3), stone.getPosition());
        assertEquals(1, world.getAgents().size());
    }

    @Test
    void cannotMoveIntoOccupiedCell() {
        World world = new World(4, 4);
        Stone mover = new Stone();
        Stone blocker = new Stone();
        world.setAgent(1, 1, mover);
        world.setAgent(2, 2, blocker);

        assertThrows(IllegalStateException.class, () -> world.moveAgent(mover, at(2, 2)));
        assertSame(mover, world.getAgent(1, 1));
        assertSame(blocker, world.getAgent(2, 2));
        assertEquals(at(1, 1), mover.getPosition());
    }

    @Test
    void cannotMoveOutsideWorld() {
        World world = new World(4, 4);
        Stone stone = new Stone();
        world.setAgent(0, 0, stone);

        assertThrows(IllegalArgumentException.class, () -> world.moveAgent(stone, at(-1, 0)));
        assertSame(stone, world.getAgent(0, 0));
        assertEquals(at(0, 0), stone.getPosition());
    }

    @Test
    void cannotMoveAgentThatIsNotOnWorld() {
        World world = new World(4, 4);
        assertThrows(IllegalStateException.class, () -> world.moveAgent(new Stone(), at(1, 1)));
        assertTrue(world.isEmpty(at(1, 1)));
    }

    // ---------- соседи (зрение) ----------

    @Test
    void neighborsInRadiusTwoAreExactlyTheFiveByFiveSquare() {
        World world = new World(9, 9);
        Stone center = new Stone();
        world.setAgent(4, 4, center);

        Set<Agent> inside = new HashSet<>();
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                if (dx != 0 || dy != 0) {
                    Stone s = new Stone();
                    world.setAgent(4 + dx, 4 + dy, s);
                    inside.add(s);
                }
            }
        }
        // Кольцо на расстоянии 3 — его видно быть не должно.
        for (int d = -3; d <= 3; d++) {
            world.setAgent(4 + d, 1, new Stone());
            world.setAgent(4 + d, 7, new Stone());
            if (d != -3 && d != 3) {
                world.setAgent(1, 4 + d, new Stone());
                world.setAgent(7, 4 + d, new Stone());
            }
        }

        List<Agent> seen = world.getNeighbors(4, 4, 2);

        assertEquals(24, seen.size());
        assertEquals(inside, new HashSet<>(seen));
        assertFalse(seen.contains(center), "себя житель не видит");
    }

    @Test
    void neighborsAreNotDuplicated() {
        World world = new World(5, 5);
        world.setAgent(2, 2, new Stone());
        world.setAgent(3, 3, new Stone());
        List<Agent> seen = world.getNeighbors(2, 2, 2);
        assertEquals(1, seen.size());
    }

    @Test
    void neighborsNearCornerStayInsideWorld() {
        World world = new World(6, 6);
        world.setAgent(0, 0, new Stone());
        Stone near = new Stone();
        Stone far = new Stone();
        world.setAgent(2, 2, near);
        world.setAgent(3, 0, far);

        List<Agent> seen = assertDoesNotThrow(() -> world.getNeighbors(0, 0, 2));
        assertEquals(List.of(near), seen);
    }

    @Test
    void neighborsNearOppositeCornerOfRectangularWorld() {
        World world = new World(3, 7);
        Stone corner = new Stone();
        world.setAgent(2, 6, corner);
        Stone seenOne = new Stone();
        world.setAgent(0, 4, seenOne);

        assertEquals(List.of(seenOne), world.getNeighbors(2, 6, 2));
    }

    @Test
    void emptyCellCanAlsoLookAround() {
        World world = new World(5, 5);
        Stone stone = new Stone();
        world.setAgent(3, 3, stone);
        assertEquals(List.of(stone), world.getNeighbors(2, 2, 1));
    }

    @Test
    void radiusZeroSeesNothing() {
        World world = new World(3, 3);
        world.setAgent(1, 1, new Stone());
        world.setAgent(1, 2, new Stone());
        assertEquals(List.of(), world.getNeighbors(1, 1, 0));
    }

    @Test
    void hugeRadiusSeesWholeWorldWithoutOverflow() {
        World world = new World(4, 4);
        world.setAgent(3, 3, new Stone());
        world.setAgent(0, 3, new Stone());
        world.setAgent(3, 0, new Stone());
        assertEquals(3, world.getNeighbors(1, 1, Integer.MAX_VALUE).size());
    }

    @Test
    void negativeRadiusIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new World(3, 3).getNeighbors(1, 1, -1));
    }

    @Test
    void lookingFromOutsideIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new World(3, 3).getNeighbors(3, 0, 1));
    }

    // ---------- свободные клетки вокруг ----------

    @Test
    void emptyCellsAroundAreTheFreeOnesOfEightNeighbors() {
        World world = new World(5, 5);
        world.setAgent(2, 2, new Stone());
        world.setAgent(1, 1, new Stone());
        world.setAgent(3, 2, new Stone());
        world.setAgent(4, 4, new Stone()); // на расстоянии 2 — не касается

        Set<Position> expected = Set.of(at(2, 1), at(3, 1), at(1, 2), at(1, 3), at(2, 3), at(3, 3));
        List<Position> free = world.getEmptyCellsAround(at(2, 2));

        assertEquals(expected.size(), free.size(), "без повторов");
        assertEquals(expected, new HashSet<>(free));
    }

    @Test
    void emptyCellsAroundCornerAreOnlyInsideWorld() {
        World world = new World(4, 4);
        assertEquals(Set.of(at(1, 0), at(0, 1), at(1, 1)), new HashSet<>(world.getEmptyCellsAround(at(0, 0))));
        assertEquals(Set.of(at(2, 3), at(3, 2), at(2, 2)), new HashSet<>(world.getEmptyCellsAround(at(3, 3))));
    }

    @Test
    void centerIsNeverReturnedAsEmptyNeighbor() {
        World world = new World(3, 3);
        assertFalse(world.getEmptyCellsAround(at(1, 1)).contains(at(1, 1)));
        assertEquals(8, world.getEmptyCellsAround(at(1, 1)).size());
    }

    @Test
    void surroundedCellHasNoEmptyNeighbors() {
        World world = new World(3, 3);
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                world.setAgent(x, y, new Stone());
            }
        }
        assertEquals(List.of(), world.getEmptyCellsAround(at(1, 1)));
    }

    @Test
    void oneCellWorldHasNoNeighbors() {
        World world = new World(1, 1);
        assertEquals(List.of(), world.getEmptyCellsAround(at(0, 0)));
        assertEquals(List.of(), world.getNeighbors(0, 0, 2));
    }

    // ---------- список и подсчёт ----------

    @Test
    void agentsListContainsEveryoneOnce() {
        World world = new World(4, 4);
        Stone a = new Stone();
        Tree b = new Tree();
        Stone c = new Stone();
        world.setAgent(3, 0, a);
        world.setAgent(0, 2, b);
        world.setAgent(1, 3, c);

        List<Agent> all = world.getAgents();
        assertEquals(3, all.size());
        assertEquals(Set.of(a, b, c), new HashSet<>(all));
    }

    @Test
    void agentsListIsACopy() {
        World world = new World(3, 3);
        world.setAgent(1, 1, new Stone());
        List<Agent> all = world.getAgents();
        assertThrows(UnsupportedOperationException.class, () -> all.clear());
        assertEquals(1, world.getAgents().size());
    }

    @Test
    void countsAgentsByType() {
        World world = new World(4, 4);
        world.setAgent(0, 0, new Stone());
        world.setAgent(1, 0, new Stone());
        world.setAgent(2, 0, new Tree());

        assertEquals(2, world.count(Stone.class));
        assertEquals(1, world.count(Tree.class));
        assertEquals(3, world.count(Agent.class));
    }

    @Test
    void countChangesAfterRemoval() {
        World world = new World(4, 4);
        Stone stone = new Stone();
        world.setAgent(0, 0, stone);
        world.setAgent(1, 0, new Stone());
        world.removeAgent(stone);
        assertEquals(1, world.count(Stone.class));
    }
}
