package life.model;

import life.model.TestAgents.Blob;
import life.model.TestAgents.Stone;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AgentTest {

    private static Position at(int x, int y) {
        return new Position(x, y);
    }

    // ---------- создание ----------

    @ParameterizedTest(name = "энергия {0} при рождении недопустима")
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void agentMustBeBornWithPositiveEnergy(int energy) {
        assertThrows(IllegalArgumentException.class, () -> new Blob(energy, 10));
    }

    @Test
    void newAgentIsAliveWithGivenEnergyAndNotOnWorld() {
        Blob blob = new Blob(1, 10);
        assertTrue(blob.isAlive());
        assertEquals(1, blob.getEnergy());
        assertNull(blob.getPosition());
    }

    // ---------- энергия ----------

    @Test
    void energyGoesUpAndDown() {
        Blob blob = new Blob(10, 100);
        blob.changeEnergy(5);
        assertEquals(15, blob.getEnergy());
        blob.changeEnergy(-12);
        assertEquals(3, blob.getEnergy());
    }

    @Test
    void energyNeverBecomesNegative() {
        Blob blob = new Blob(5, 100);
        blob.changeEnergy(-10);
        assertEquals(0, blob.getEnergy());
    }

    @Test
    void energyCanDropExactlyToZero() {
        Blob blob = new Blob(5, 100);
        blob.changeEnergy(-5);
        assertEquals(0, blob.getEnergy());
    }

    @Test
    void energyDoesNotOverflowIntoNegative() {
        Blob blob = new Blob(Integer.MAX_VALUE - 1, 100);
        blob.changeEnergy(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, blob.getEnergy());
    }

    @Test
    void hugeNegativeChangeDoesNotOverflow() {
        Blob blob = new Blob(1, 100);
        blob.changeEnergy(Integer.MIN_VALUE);
        assertEquals(0, blob.getEnergy());
    }

    // ---------- гибель ----------

    @Test
    void deadAgentLeavesTheWorld() {
        World world = new World(3, 3);
        Blob blob = new Blob(5, 100);
        world.setAgent(1, 1, blob);

        blob.die(world);

        assertFalse(blob.isAlive());
        assertNull(blob.getPosition());
        assertTrue(world.isEmpty(at(1, 1)));
    }

    @Test
    void dyingTwiceIsHarmless() {
        World world = new World(3, 3);
        Blob blob = new Blob(5, 100);
        world.setAgent(1, 1, blob);
        blob.die(world);
        assertDoesNotThrow(() -> blob.die(world));
        assertFalse(blob.isAlive());
    }

    @Test
    void agentThatNeverStoodOnWorldCanStillDie() {
        Blob blob = new Blob(5, 100);
        assertDoesNotThrow(() -> blob.die(new World(3, 3)));
        assertFalse(blob.isAlive());
    }

    @Test
    void dyingDoesNotTouchNeighbors() {
        World world = new World(3, 3);
        Blob blob = new Blob(5, 100);
        Stone neighbor = new Stone();
        world.setAgent(1, 1, blob);
        world.setAgent(1, 2, neighbor);
        blob.die(world);
        assertSame(neighbor, world.getAgent(1, 2));
        assertTrue(neighbor.isAlive());
    }

    // ---------- размножение ----------

    @Test
    void agentAboveThresholdSplitsInHalf() {
        World world = new World(3, 3);
        Blob parent = new Blob(11, 10);
        world.setAgent(1, 1, parent);

        Agent child = parent.tryReproduce(world, new Random(1));

        assertNotNull(child);
        assertNotSame(parent, child);
        assertInstanceOf(Blob.class, child);
        assertTrue(child.isAlive());
        assertEquals(5, child.getEnergy());
        assertEquals(6, parent.getEnergy());
        assertEquals(2, world.getAgents().size());
        assertSame(child, world.getAgent(child.getPosition()), "ребёнок стоит на поле");
        Position c = child.getPosition();
        assertTrue(Math.abs(c.x() - 1) <= 1 && Math.abs(c.y() - 1) <= 1, "ребёнок в соседней клетке: " + c);
        assertEquals(at(1, 1), parent.getPosition(), "родитель остаётся на месте");
    }

    @Test
    void agentExactlyAtThresholdDoesNotSplit() {
        World world = new World(3, 3);
        Blob parent = new Blob(10, 10);
        world.setAgent(1, 1, parent);

        assertNull(parent.tryReproduce(world, new Random(1)));
        assertEquals(10, parent.getEnergy());
        assertEquals(1, world.getAgents().size());
    }

    @ParameterizedTest(name = "энергия {0} делится без потерь")
    @ValueSource(ints = {2, 3, 7, 100, 1_000_001})
    void splittingKeepsTotalEnergy(int energy) {
        World world = new World(3, 3);
        Blob parent = new Blob(energy, 1);
        world.setAgent(1, 1, parent);

        Agent child = parent.tryReproduce(world, new Random(1));

        assertNotNull(child);
        assertEquals(energy, parent.getEnergy() + child.getEnergy());
        assertTrue(child.getEnergy() >= 1 && parent.getEnergy() >= 1);
        assertEquals(energy / 2, child.getEnergy(), "ребёнку достаётся половина (с округлением вниз)");
    }

    @Test
    void noFreeCellMeansNoChild() {
        World world = new World(3, 3);
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                if (x != 1 || y != 1) {
                    world.setAgent(x, y, new Stone());
                }
            }
        }
        Blob parent = new Blob(50, 10);
        world.setAgent(1, 1, parent);

        assertNull(parent.tryReproduce(world, new Random(1)));
        assertEquals(50, parent.getEnergy(), "энергия не тратится, если родить некуда");
        assertEquals(9, world.getAgents().size());
    }

    @Test
    void childTakesTheOnlyFreeCell() {
        World world = new World(3, 3);
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                if ((x != 1 || y != 1) && (x != 2 || y != 0)) {
                    world.setAgent(x, y, new Stone());
                }
            }
        }
        Blob parent = new Blob(50, 10);
        world.setAgent(1, 1, parent);

        Agent child = parent.tryReproduce(world, new Random(1));

        assertNotNull(child);
        assertEquals(at(2, 0), child.getPosition());
    }

    @Test
    void childCellIsChosenRandomlyNotAlwaysTheSame() {
        Set<Position> places = new HashSet<>();
        for (long seed = 0; seed < 50; seed++) {
            World world = new World(3, 3);
            Blob parent = new Blob(50, 10);
            world.setAgent(1, 1, parent);
            places.add(parent.tryReproduce(world, new Random(seed)).getPosition());
        }
        assertTrue(places.size() >= 6, "дети должны появляться в разных сторонах, а появились только в " + places);
    }

    @Test
    void deadAgentDoesNotReproduce() {
        World world = new World(3, 3);
        Blob parent = new Blob(50, 10);
        world.setAgent(1, 1, parent);
        parent.die(world);

        assertNull(parent.tryReproduce(world, new Random(1)));
        assertEquals(0, world.getAgents().size());
        assertEquals(50, parent.getEnergy());
    }

    @Test
    void childCanReproduceOnItsOwnLater() {
        World world = new World(5, 5);
        Blob parent = new Blob(100, 10);
        world.setAgent(2, 2, parent);

        Agent child = parent.tryReproduce(world, new Random(3));
        Agent grandChild = child.tryReproduce(world, new Random(4));

        assertNotNull(grandChild);
        assertEquals(3, world.getAgents().size());
        assertEquals(100, parent.getEnergy() + child.getEnergy() + grandChild.getEnergy());
    }
}
