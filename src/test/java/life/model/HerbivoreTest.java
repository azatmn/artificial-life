package life.model;

import life.model.TestAgents.Stone;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class HerbivoreTest {

    private static Position at(int x, int y) {
        return new Position(x, y);
    }

    /** Травоядное с заданным расходом, едой и порогом размножения. */
    private static Settings herbivoreSettings(int cost, int food, int threshold) {
        return Settings.builder().herbivoreStepCost(cost).herbivoreFoodEnergy(food)
                .herbivoreReproductionThreshold(threshold).build();
    }

    /** Настройки, при которых травоядное не размножается и почти не устаёт. */
    private static final Settings CALM = herbivoreSettings(1, 5, Settings.MAX_ENERGY);

    private static Herbivore place(World world, int x, int y, int energy) {
        Herbivore h = new Herbivore(CALM, energy);
        world.setAgent(x, y, h);
        return h;
    }

    private static void fillWithStonesExcept(World world, Position... free) {
        List<Position> keep = List.of(free);
        for (int y = 0; y < world.getHeight(); y++) {
            for (int x = 0; x < world.getWidth(); x++) {
                if (!keep.contains(at(x, y)) && world.isEmpty(at(x, y))) {
                    world.setAgent(x, y, new Stone());
                }
            }
        }
    }

    // ---------- создание ----------

    @Test
    void symbolIsT() {
        assertEquals('T', new Herbivore(Settings.defaults()).symbol());
    }

    @Test
    void startsWithConfiguredEnergy() {
        assertEquals(17, new Herbivore(Settings.builder().herbivoreStartEnergy(17).build()).getEnergy());
    }

    // ---------- голод ----------

    @Test
    void spendsEnergyEveryTurn() {
        Settings settings = herbivoreSettings(3, 5, Settings.MAX_ENERGY);
        World world = new World(5, 5);
        Herbivore h = new Herbivore(settings, 10);
        world.setAgent(2, 2, h);

        h.act(world, new Random(1));
        assertEquals(7, h.getEnergy());
        h.act(world, new Random(2));
        assertEquals(4, h.getEnergy());
    }

    @Test
    void diesWhenEnergyRunsOutExactly() {
        Settings settings = herbivoreSettings(3, 5, Settings.MAX_ENERGY);
        World world = new World(5, 5);
        Herbivore h = new Herbivore(settings, 3);
        world.setAgent(2, 2, h);

        h.act(world, new Random(1));

        assertFalse(h.isAlive());
        assertNull(h.getPosition());
        assertEquals(List.of(), world.getAgents());
    }

    @Test
    void survivesWithOneEnergyLeft() {
        Settings settings = herbivoreSettings(3, 5, Settings.MAX_ENERGY);
        World world = new World(5, 5);
        Herbivore h = new Herbivore(settings, 4);
        world.setAgent(2, 2, h);

        h.act(world, new Random(1));

        assertTrue(h.isAlive());
        assertEquals(1, h.getEnergy());
    }

    @Test
    void starvingAnimalDiesBeforeItCanEat() {
        World world = new World(5, 5);
        Herbivore h = place(world, 2, 2, 1);
        Plant plant = new Plant(Settings.defaults());
        world.setAgent(3, 2, plant);

        h.act(world, new Random(1));

        assertFalse(h.isAlive(), "сначала тратится энергия — на еду сил уже не хватило");
        assertTrue(plant.isAlive());
        assertSame(plant, world.getAgent(3, 2));
    }

    @Test
    void deadHerbivoreDoesNothing() {
        World world = new World(5, 5);
        Herbivore h = place(world, 2, 2, 10);
        world.setAgent(3, 2, new Plant(Settings.defaults()));
        h.die(world);

        h.act(world, new Random(1));

        assertEquals(10, h.getEnergy());
        assertEquals(1, world.getAgents().size(), "растение цело, травоядного нет");
    }

    @Test
    void livesAndDiesInOneCellWorld() {
        World world = new World(1, 1);
        Herbivore h = place(world, 0, 0, 3);
        Random random = new Random(1);
        h.act(world, random);
        h.act(world, random);
        assertTrue(h.isAlive());
        h.act(world, random);
        assertFalse(h.isAlive());
    }

    // ---------- еда ----------

    @Test
    void eatsAdjacentPlantAndTakesItsCell() {
        World world = new World(5, 5);
        Herbivore h = place(world, 2, 2, 10);
        Plant plant = new Plant(Settings.defaults());
        world.setAgent(3, 3, plant);

        h.act(world, new Random(1));

        assertFalse(plant.isAlive());
        assertEquals(at(3, 3), h.getPosition());
        assertSame(h, world.getAgent(3, 3));
        assertTrue(world.isEmpty(at(2, 2)));
        assertEquals(10 - 1 + 5, h.getEnergy());
        assertEquals(1, world.getAgents().size());
    }

    @Test
    void walksTowardsPlantItSeesButCannotReachYet() {
        World world = new World(5, 5);
        Herbivore h = place(world, 2, 2, 10);
        Plant plant = new Plant(Settings.defaults());
        world.setAgent(4, 4, plant);

        h.act(world, new Random(1));

        assertEquals(at(3, 3), h.getPosition(), "шаг по диагонали прямо к растению");
        assertTrue(plant.isAlive());
        assertEquals(9, h.getEnergy(), "не поел — только потратил");
    }

    @Test
    void choosesTheStraightestStepToTarget() {
        World world = new World(5, 5);
        Herbivore h = place(world, 2, 2, 10);
        world.setAgent(4, 2, new Plant(Settings.defaults()));

        h.act(world, new Random(1));

        assertEquals(at(3, 2), h.getPosition());
    }

    @Test
    void goesForTheNearestPlant() {
        World world = new World(5, 5);
        Herbivore h = place(world, 2, 2, 10);
        Plant near = new Plant(Settings.defaults());
        Plant far = new Plant(Settings.defaults());
        world.setAgent(2, 3, near);
        world.setAgent(4, 0, far);

        h.act(world, new Random(1));

        assertFalse(near.isAlive());
        assertTrue(far.isAlive());
        assertEquals(at(2, 3), h.getPosition());
    }

    @Test
    void doesNotEatOtherHerbivores() {
        World world = new World(2, 1);
        Herbivore h = place(world, 0, 0, 10);
        Herbivore other = place(world, 1, 0, 10);

        h.act(world, new Random(1));

        assertTrue(other.isAlive());
        assertEquals(at(0, 0), h.getPosition());
        assertEquals(at(1, 0), other.getPosition());
    }

    @Test
    void cannotWalkThroughObstaclesToPlant() {
        World world = new World(3, 1);
        Herbivore h = place(world, 0, 0, 10);
        Stone wall = new Stone();
        world.setAgent(1, 0, wall);
        Plant plant = new Plant(Settings.defaults());
        world.setAgent(2, 0, plant);

        h.act(world, new Random(1));

        assertEquals(at(0, 0), h.getPosition());
        assertTrue(plant.isAlive());
        assertSame(wall, world.getAgent(1, 0));
    }

    // ---------- зрение 5×5 ----------

    @Test
    void doesNotSeePlantThreeCellsAway() {
        // Коридор 7×1: слева камень, значит «бродить» можно только вправо.
        // Если бы травоядное видело растение слева, оно осталось бы стоять (вправо — дальше от цели).
        World world = new World(7, 1);
        world.setAgent(0, 0, new Plant(Settings.defaults()));
        world.setAgent(2, 0, new Stone());
        Herbivore h = place(world, 3, 0, 10);

        h.act(world, new Random(1));

        assertEquals(at(4, 0), h.getPosition());
    }

    @Test
    void seesPlantTwoCellsAway() {
        World world = new World(7, 1);
        world.setAgent(1, 0, new Plant(Settings.defaults()));
        world.setAgent(2, 0, new Stone());
        Herbivore h = place(world, 3, 0, 10);

        h.act(world, new Random(1));

        assertEquals(at(3, 0), h.getPosition(), "видит растение и не уходит от него");
    }

    @Test
    void seesTheCornerOfFiveByFiveSquare() {
        Random random = new Random(7);
        for (int attempt = 0; attempt < 20; attempt++) {
            World world = new World(5, 5);
            Herbivore h = place(world, 2, 2, 10);
            world.setAgent(4, 4, new Plant(Settings.defaults()));
            fillWithStonesExcept(world, at(1, 1), at(3, 3));

            h.act(world, random);

            assertEquals(at(3, 3), h.getPosition(), "угол квадрата 5×5 тоже в поле зрения");
        }
    }

    // ---------- бегство ----------

    @Test
    void runsAwayFromPredator() {
        World world = new World(7, 7);
        Herbivore h = place(world, 3, 3, 10);
        world.setAgent(1, 3, new Predator(Settings.defaults()));

        h.act(world, new Random(1));

        assertTrue(Set.of(at(4, 2), at(4, 4)).contains(h.getPosition()),
                "уходит в самую дальнюю от хищника клетку, а оказался в " + h.getPosition());
    }

    @Test
    void equallyGoodEscapeRoutesAreChosenRandomly() {
        Random random = new Random(11);
        Set<Position> used = new HashSet<>();
        for (int attempt = 0; attempt < 30; attempt++) {
            World world = new World(7, 7);
            Herbivore h = place(world, 3, 3, 10);
            world.setAgent(1, 3, new Predator(Settings.defaults()));
            h.act(world, random);
            used.add(h.getPosition());
        }
        assertEquals(Set.of(at(4, 2), at(4, 4)), used, "обе одинаково дальние клетки должны встречаться");
    }

    @Test
    void doesNotSidestepWhenItCannotGetCloser() {
        // До растения (1,1) квадрат расстояния 5. Все более близкие клетки заняты камнями,
        // клетка (3,2) на том же расстоянии 5 — шагать туда смысла нет.
        World world = new World(5, 5);
        world.setAgent(1, 1, new Plant(Settings.defaults()));
        world.setAgent(1, 2, new Stone());
        world.setAgent(2, 2, new Stone());
        world.setAgent(1, 3, new Stone());
        Herbivore h = place(world, 2, 3, 10);

        h.act(world, new Random(1));

        assertEquals(at(2, 3), h.getPosition());
    }

    @Test
    void runningAwayBeatsEating() {
        World world = new World(7, 7);
        Herbivore h = place(world, 3, 3, 10);
        world.setAgent(1, 3, new Predator(Settings.defaults()));
        Plant plant = new Plant(Settings.defaults());
        world.setAgent(2, 3, plant);

        h.act(world, new Random(1));

        assertTrue(plant.isAlive(), "при виде хищника не до еды");
        assertEquals(4, h.getPosition().x());
    }

    @Test
    void doesNotRunOntoPlantWhileFleeing() {
        World world = new World(7, 1);
        world.setAgent(1, 0, new Predator(Settings.defaults()));
        Herbivore h = place(world, 3, 0, 10);
        Plant plant = new Plant(Settings.defaults());
        world.setAgent(4, 0, plant);

        h.act(world, new Random(1));

        assertEquals(at(3, 0), h.getPosition(), "убегать некуда: влево — к хищнику, вправо — занято");
        assertTrue(plant.isAlive());
    }

    @Test
    void corneredHerbivoreStaysPut() {
        // В углу любая соседняя клетка ближе к хищнику, чем текущая, — лучше стоять.
        World world = new World(5, 5);
        Herbivore h = place(world, 0, 0, 10);
        world.setAgent(2, 2, new Predator(Settings.defaults()));

        h.act(world, new Random(1));

        assertEquals(at(0, 0), h.getPosition());
    }

    @Test
    void doesNotRunFromOtherHerbivores() {
        // Сосед-травоядное слева вплотную, растение слева через клетку.
        // Идёт к растению (влево). Если бы боялся соседа — ушёл бы вправо.
        World world = new World(7, 1);
        place(world, 0, 0, 10);
        world.setAgent(1, 0, new Plant(Settings.defaults()));
        Herbivore h = place(world, 3, 0, 10);

        h.act(world, new Random(1));

        assertEquals(at(2, 0), h.getPosition());
    }

    @Test
    void runsFromTheNearestPredator() {
        World world = new World(9, 9);
        Herbivore h = place(world, 4, 4, 10);
        world.setAgent(3, 4, new Predator(Settings.defaults())); // вплотную слева
        world.setAgent(6, 4, new Predator(Settings.defaults())); // через клетку справа

        h.act(world, new Random(1));

        assertTrue(Set.of(at(5, 3), at(5, 5)).contains(h.getPosition()),
                "убегает от ближнего хищника, а оказался в " + h.getPosition());
    }

    // ---------- блуждание ----------

    @Test
    void wandersToNeighborCellsInDifferentDirections() {
        Random random = new Random(3);
        Set<Position> visited = new HashSet<>();
        for (int attempt = 0; attempt < 40; attempt++) {
            World world = new World(5, 5);
            Herbivore h = place(world, 2, 2, 10);
            h.act(world, random);
            Position p = h.getPosition();
            assertTrue(Math.abs(p.x() - 2) <= 1 && Math.abs(p.y() - 2) <= 1 && !p.equals(at(2, 2)),
                    "за ход — ровно на соседнюю клетку, а оказался в " + p);
            visited.add(p);
        }
        assertTrue(visited.size() >= 6, "бродит в разные стороны, а побывал только в " + visited);
    }

    @Test
    void surroundedHerbivoreStaysButStillGetsHungry() {
        World world = new World(3, 3);
        Herbivore h = place(world, 1, 1, 10);
        fillWithStonesExcept(world);

        h.act(world, new Random(1));

        assertEquals(at(1, 1), h.getPosition());
        assertEquals(9, h.getEnergy());
    }

    // ---------- размножение ----------

    @Test
    void reproducesWhenMealTakesItAboveThreshold() {
        Settings settings = herbivoreSettings(1, 8, 10);
        World world = new World(5, 5);
        Herbivore h = new Herbivore(settings, 5);
        world.setAgent(2, 2, h);
        world.setAgent(3, 2, new Plant(Settings.defaults()));

        h.act(world, new Random(1)); // 5 - 1 + 8 = 12 > 10

        assertEquals(2, world.count(Herbivore.class));
        assertEquals(0, world.count(Plant.class));
        assertEquals(6, h.getEnergy());
    }

    @Test
    void childHerbivoreUsesSameSettings() {
        // Ребёнок получит 15; после хода 15 - 3 = 12 — не выше порога 14, сам делиться не станет.
        Settings settings = herbivoreSettings(3, 8, 14);
        World world = new World(5, 5);
        Herbivore h = new Herbivore(settings, 30);
        world.setAgent(2, 2, h);
        Agent child = h.tryReproduce(world, new Random(1));

        assertInstanceOf(Herbivore.class, child);
        int before = child.getEnergy();
        child.act(world, new Random(1));
        assertEquals(before - 3, child.getEnergy(), "ребёнок тратит столько же, сколько родитель");
    }
}
