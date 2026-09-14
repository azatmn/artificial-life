package life.model;

import life.model.TestAgents.Stone;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class PredatorTest {

    private static Position at(int x, int y) {
        return new Position(x, y);
    }

    private static Settings predatorSettings(int cost, int food, int threshold) {
        return Settings.builder().predatorStepCost(cost).predatorFoodEnergy(food)
                .predatorReproductionThreshold(threshold).build();
    }

    private static final Settings CALM = predatorSettings(1, 10, Settings.MAX_ENERGY);

    private static Predator place(World world, int x, int y, int energy) {
        Predator p = new Predator(CALM, energy);
        world.setAgent(x, y, p);
        return p;
    }

    /** Травоядное-мишень: в этих тестах оно не ходит, его только едят. */
    private static Herbivore prey(World world, int x, int y) {
        Herbivore h = new Herbivore(Settings.defaults());
        world.setAgent(x, y, h);
        return h;
    }

    // ---------- создание ----------

    @Test
    void symbolIsX() {
        assertEquals('X', new Predator(Settings.defaults()).symbol());
    }

    @Test
    void startsWithConfiguredEnergy() {
        assertEquals(23, new Predator(Settings.builder().predatorStartEnergy(23).build()).getEnergy());
    }

    // ---------- голод ----------

    @Test
    void spendsConfiguredEnergyEveryTurn() {
        World world = new World(5, 5);
        Predator p = new Predator(predatorSettings(4, 10, Settings.MAX_ENERGY), 20);
        world.setAgent(2, 2, p);

        p.act(world, new Random(1));

        assertEquals(16, p.getEnergy());
    }

    @Test
    void diesOfHungerAtZero() {
        World world = new World(5, 5);
        Predator p = new Predator(predatorSettings(4, 10, Settings.MAX_ENERGY), 4);
        world.setAgent(2, 2, p);
        Herbivore h = prey(world, 3, 2);

        p.act(world, new Random(1));

        assertFalse(p.isAlive());
        assertEquals(List.of(h), world.getAgents(), "умер раньше, чем успел съесть");
    }

    @Test
    void deadPredatorDoesNothing() {
        World world = new World(5, 5);
        Predator p = place(world, 2, 2, 20);
        Herbivore h = prey(world, 3, 2);
        p.die(world);

        p.act(world, new Random(1));

        assertTrue(h.isAlive());
        assertEquals(20, p.getEnergy());
    }

    // ---------- охота ----------

    @Test
    void eatsAdjacentHerbivore() {
        World world = new World(5, 5);
        Predator p = place(world, 2, 2, 20);
        Herbivore h = prey(world, 2, 3);

        p.act(world, new Random(1));

        assertFalse(h.isAlive());
        assertNull(h.getPosition());
        assertEquals(at(2, 3), p.getPosition());
        assertEquals(20 - 1 + 10, p.getEnergy());
        assertEquals(List.of(p), world.getAgents());
    }

    @Test
    void chasesHerbivoreItSees() {
        World world = new World(5, 5);
        Predator p = place(world, 0, 0, 20);
        Herbivore h = prey(world, 2, 2);

        p.act(world, new Random(1));

        assertEquals(at(1, 1), p.getPosition());
        assertTrue(h.isAlive());
    }

    @Test
    void doesNotSeeHerbivoreThreeCellsAway() {
        World world = new World(7, 1);
        prey(world, 0, 0);
        world.setAgent(2, 0, new Stone());
        Predator p = place(world, 3, 0, 20);

        p.act(world, new Random(1));

        assertEquals(at(4, 0), p.getPosition(), "добычи не видит — бродит (вправо, слева камень)");
    }

    @Test
    void seesHerbivoreTwoCellsAway() {
        World world = new World(7, 1);
        prey(world, 1, 0);
        world.setAgent(2, 0, new Stone());
        Predator p = place(world, 3, 0, 20);

        p.act(world, new Random(1));

        assertEquals(at(3, 0), p.getPosition(), "видит добычу и не уходит от неё");
    }

    @Test
    void ignoresPlants() {
        World world = new World(3, 1);
        Predator p = place(world, 0, 0, 100);
        Plant plant = new Plant(Settings.defaults());
        world.setAgent(1, 0, plant);

        Random random = new Random(1);
        for (int i = 0; i < 20; i++) {
            p.act(world, random);
        }

        assertTrue(plant.isAlive(), "хищник траву не ест");
        assertEquals(at(0, 0), p.getPosition(), "и не ходит сквозь неё");
    }

    @Test
    void prefersHerbivoreEvenIfPlantIsCloser() {
        World world = new World(5, 5);
        Predator p = place(world, 2, 2, 20);
        Plant plant = new Plant(Settings.defaults());
        world.setAgent(3, 2, plant);
        Herbivore h = prey(world, 4, 4);

        p.act(world, new Random(1));

        assertEquals(at(3, 3), p.getPosition());
        assertTrue(plant.isAlive());
        assertTrue(h.isAlive());
    }

    @Test
    void doesNotEatOtherPredators() {
        World world = new World(2, 1);
        Predator p = place(world, 0, 0, 20);
        Predator other = place(world, 1, 0, 20);

        p.act(world, new Random(1));

        assertTrue(other.isAlive());
        assertEquals(at(0, 0), p.getPosition());
    }

    @Test
    void doesNotRunFromAnything() {
        // Рядом другой хищник, добычи нет: хищник просто бродит (единственная свободная клетка — к соседу).
        World world = new World(4, 1);
        place(world, 3, 0, 20);
        Predator p = place(world, 1, 0, 20);
        world.setAgent(0, 0, new Stone());

        p.act(world, new Random(1));

        assertEquals(at(2, 0), p.getPosition());
    }

    // ---------- размножение ----------

    @Test
    void reproducesAfterGoodMeal() {
        Settings settings = predatorSettings(1, 20, 25);
        World world = new World(5, 5);
        Predator p = new Predator(settings, 10);
        world.setAgent(2, 2, p);
        prey(world, 3, 2);

        p.act(world, new Random(1)); // 10 - 1 + 20 = 29 > 25

        assertEquals(2, world.count(Predator.class));
        assertEquals(0, world.count(Herbivore.class));
        assertEquals(15, p.getEnergy());
    }

    @Test
    void childPredatorUsesSameSettings() {
        // Ребёнок получит 30; после хода 30 - 4 = 26 — не выше порога 27, сам делиться не станет.
        Settings settings = predatorSettings(4, 20, 27);
        World world = new World(5, 5);
        Predator p = new Predator(settings, 60);
        world.setAgent(2, 2, p);
        Agent child = p.tryReproduce(world, new Random(1));

        assertInstanceOf(Predator.class, child);
        int before = child.getEnergy();
        child.act(world, new Random(1));
        assertEquals(before - 4, child.getEnergy());
    }
}
