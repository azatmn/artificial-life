package life.model;

import life.model.TestAgents.Stone;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class PlantTest {

    /** Растение, которое не размножается (порог почти на максимуме), — чтобы проверять только рост. */
    private static Settings growOnly(int growth, int max) {
        return Settings.builder().plantStartEnergy(1).plantGrowth(growth)
                .plantMaxEnergy(max).plantReproductionThreshold(max - 1).build();
    }

    private static Settings withThreshold(int growth, int threshold, int max) {
        return Settings.builder().plantStartEnergy(1).plantGrowth(growth)
                .plantMaxEnergy(max).plantReproductionThreshold(threshold).build();
    }

    // ---------- создание ----------

    @Test
    void plantSymbolIsP() {
        assertEquals('P', new Plant(Settings.defaults()).symbol());
    }

    @Test
    void newPlantStartsWithConfiguredEnergy() {
        Settings settings = Settings.builder().plantStartEnergy(7).build();
        assertEquals(7, new Plant(settings).getEnergy());
    }

    @Test
    void plantCannotBeCreatedAboveMaximum() {
        Settings settings = Settings.builder().plantStartEnergy(1).plantMaxEnergy(20).plantReproductionThreshold(10).build();
        assertThrows(IllegalArgumentException.class, () -> new Plant(settings, 21));
        assertEquals(20, new Plant(settings, 20).getEnergy());
    }

    // ---------- рост ----------

    @Test
    void plantGrowsByConfiguredAmountEachTurn() {
        Settings settings = growOnly(3, 100);
        World world = new World(3, 3);
        Plant plant = new Plant(settings, 5);
        world.setAgent(1, 1, plant);

        plant.act(world, new Random(1));
        assertEquals(8, plant.getEnergy());
        plant.act(world, new Random(1));
        assertEquals(11, plant.getEnergy());
    }

    @Test
    void growthStopsAtMaximum() {
        Settings settings = growOnly(5, 20);
        World world = new World(1, 1);
        Plant plant = new Plant(settings, 18);
        world.setAgent(0, 0, plant);

        plant.act(world, new Random(1));
        assertEquals(20, plant.getEnergy());
        plant.act(world, new Random(1));
        assertEquals(20, plant.getEnergy(), "на максимуме энергия больше не растёт");
    }

    @Test
    void plantThatReachesMaximumExactlyIsNotCut() {
        Settings settings = growOnly(2, 20);
        World world = new World(1, 1);
        Plant plant = new Plant(settings, 18);
        world.setAgent(0, 0, plant);
        plant.act(world, new Random(1));
        assertEquals(20, plant.getEnergy());
    }

    // ---------- размножение ----------

    @Test
    void plantReproducesWhenGrowthTakesItAboveThreshold() {
        Settings settings = withThreshold(1, 10, 50);
        World world = new World(3, 3);
        Plant plant = new Plant(settings, 10);
        world.setAgent(1, 1, plant);

        plant.act(world, new Random(1)); // 10 + 1 = 11 > 10

        assertEquals(2, world.count(Plant.class));
        Agent child = world.getAgents().stream().filter(a -> a != plant).findFirst().orElseThrow();
        assertInstanceOf(Plant.class, child);
        assertEquals(5, child.getEnergy());
        assertEquals(6, plant.getEnergy());
    }

    @Test
    void plantReachingExactlyThresholdDoesNotReproduce() {
        Settings settings = withThreshold(1, 10, 50);
        World world = new World(3, 3);
        Plant plant = new Plant(settings, 9);
        world.setAgent(1, 1, plant);

        plant.act(world, new Random(1)); // 9 + 1 = 10, не больше порога

        assertEquals(1, world.count(Plant.class));
        assertEquals(10, plant.getEnergy());
    }

    @Test
    void childPlantUsesSameSettings() {
        Settings settings = withThreshold(3, 10, 50);
        World world = new World(5, 5);
        Plant plant = new Plant(settings, 10);
        world.setAgent(2, 2, plant);
        plant.act(world, new Random(1));
        Agent child = world.getAgents().stream().filter(a -> a != plant).findFirst().orElseThrow();

        int before = child.getEnergy();
        child.act(world, new Random(1));
        assertEquals(before + 3, child.getEnergy(), "ребёнок растёт с тем же приростом");
    }

    @Test
    void surroundedPlantKeepsGrowingWithoutChildren() {
        Settings settings = withThreshold(4, 10, 30);
        World world = new World(3, 3);
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                if (x != 1 || y != 1) {
                    world.setAgent(x, y, new Stone());
                }
            }
        }
        Plant plant = new Plant(settings, 1);
        world.setAgent(1, 1, plant);

        for (int i = 0; i < 20; i++) {
            plant.act(world, new Random(i));
        }
        assertEquals(30, plant.getEnergy());
        assertEquals(1, world.count(Plant.class), "детей нет — родить некуда");
    }

    @Test
    void plantNeverMoves() {
        Settings settings = withThreshold(2, 10, 30);
        World world = new World(7, 7);
        Plant plant = new Plant(settings, 1);
        world.setAgent(3, 3, plant);
        for (int i = 0; i < 30; i++) {
            plant.act(world, new Random(i));
        }
        assertEquals(new Position(3, 3), plant.getPosition());
        assertSame(plant, world.getAgent(3, 3));
    }

    @Test
    void plantInOneCellWorldLivesWithoutErrors() {
        World world = new World(1, 1);
        Plant plant = new Plant(Settings.defaults());
        world.setAgent(0, 0, plant);
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 100; i++) {
                plant.act(world, new Random(i));
            }
        });
        assertEquals(Settings.defaults().plantMaxEnergy(), plant.getEnergy());
    }

    // ---------- мёртвое растение ----------

    @Test
    void eatenPlantDoesNothing() {
        Settings settings = withThreshold(5, 10, 50);
        World world = new World(3, 3);
        Plant plant = new Plant(settings, 20);
        world.setAgent(1, 1, plant);
        plant.die(world);

        plant.act(world, new Random(1));

        assertEquals(20, plant.getEnergy(), "съеденное растение не растёт");
        assertEquals(0, world.getAgents().size(), "и не размножается");
    }
}
