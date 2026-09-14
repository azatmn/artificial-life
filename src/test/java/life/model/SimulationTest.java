package life.model;

import life.model.TestAgents.Breeder;
import life.model.TestAgents.Eater;
import life.model.TestAgents.Recorder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SimulationTest {

    private static Settings field(int width, int height, int plants, int herbivores, int predators) {
        return Settings.builder().width(width).height(height)
                .initialPlants(plants).initialHerbivores(herbivores).initialPredators(predators).build();
    }

    /** Поле построчно: «.» — пусто, иначе буква и энергия. Одинаковые снимки — одинаковое состояние. */
    private static String snapshot(ReadOnlyWorld world) {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < world.getHeight(); y++) {
            for (int x = 0; x < world.getWidth(); x++) {
                Agent a = world.getAgent(x, y);
                sb.append(a == null ? "." : a.symbol() + String.valueOf(a.getEnergy())).append(',');
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    // ---------- начальная расстановка ----------

    @Test
    void newSimulationStartsWithConfiguredPopulation() {
        Simulation sim = new Simulation(field(10, 8, 20, 7, 3));

        assertEquals(new Statistics(0, 20, 7, 3), sim.statistics());
        assertEquals(0, sim.getStep());
        assertEquals(10, sim.getWorld().getWidth());
        assertEquals(8, sim.getWorld().getHeight());
    }

    @Test
    void startingAgentsHaveStartingEnergy() {
        Settings settings = Settings.builder().width(10).height(10)
                .initialPlants(5).initialHerbivores(5).initialPredators(5)
                .plantStartEnergy(3).herbivoreStartEnergy(11).predatorStartEnergy(22).build();
        Simulation sim = new Simulation(settings);

        int checked = 0;
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                Agent a = sim.getWorld().getAgent(x, y);
                if (a instanceof Plant) {
                    assertEquals(3, a.getEnergy());
                    checked++;
                } else if (a instanceof Herbivore) {
                    assertEquals(11, a.getEnergy());
                    checked++;
                } else if (a instanceof Predator) {
                    assertEquals(22, a.getEnergy());
                    checked++;
                }
            }
        }
        assertEquals(15, checked, "проверка не должна пройти «ни о чём» на пустом поле");
    }

    @Test
    void fieldCanBeFilledCompletely() {
        Simulation sim = new Simulation(field(5, 4, 10, 6, 4));

        assertEquals(new Statistics(0, 10, 6, 4), sim.statistics());
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 5; x++) {
                assertNotNull(sim.getWorld().getAgent(x, y));
            }
        }
    }

    @Test
    void agentsAreScatteredNotPackedIntoFirstRows() {
        Simulation sim = new Simulation(field(20, 20, 20, 0, 0));
        assertEquals(20, sim.statistics().plants(), "растения должны быть расставлены");
        int inFirstRow = 0;
        for (int x = 0; x < 20; x++) {
            if (sim.getWorld().getAgent(x, 0) != null) {
                inFirstRow++;
            }
        }
        assertTrue(inFirstRow < 20, "20 растений на поле 20×20 не должны все оказаться в первой строке");
    }

    @Test
    void emptySimulationKeepsCountingSteps() {
        Simulation sim = new Simulation(field(3, 3, 0, 0, 0));
        sim.step();
        sim.step();
        sim.step();
        assertEquals(new Statistics(3, 0, 0, 0), sim.statistics());
        assertTrue(sim.statistics().allDead());
    }

    @Test
    void stepCounterGrowsByOne() {
        Simulation sim = new Simulation(Settings.defaults());
        for (int i = 1; i <= 5; i++) {
            sim.step();
            assertEquals(i, sim.getStep());
            assertEquals(i, sim.statistics().step());
        }
    }

    // ---------- воспроизводимость ----------

    @Test
    void sameSeedGivesSameHistory() {
        Settings settings = field(20, 15, 60, 20, 5).toBuilder().seed(7).build();
        Simulation a = new Simulation(settings);
        Simulation b = new Simulation(settings);
        assertEquals(85, a.statistics().total(), "сравнивать пустые поля бессмысленно");
        for (int i = 0; i < 50; i++) {
            assertEquals(snapshot(a.getWorld()), snapshot(b.getWorld()), "разошлись на шаге " + i);
            a.step();
            b.step();
        }
    }

    @Test
    void differentSeedsGiveDifferentStart() {
        Settings settings = field(20, 15, 60, 20, 5);
        Simulation a = new Simulation(settings.toBuilder().seed(1).build());
        Simulation b = new Simulation(settings.toBuilder().seed(2).build());
        assertNotEquals(snapshot(a.getWorld()), snapshot(b.getWorld()));
    }

    @Test
    void settingsAreKept() {
        Settings settings = field(7, 7, 1, 1, 1);
        assertSame(settings, new Simulation(settings).getSettings());
    }

    // ---------- порядок ходов ----------

    @Test
    void everyLivingAgentActsExactlyOncePerStep() {
        World world = new World(4, 4);
        List<Agent> log = new ArrayList<>();
        List<Recorder> recorders = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Recorder r = new Recorder(log, 1);
            world.setAgent(i % 4, i / 4, r);
            recorders.add(r);
        }
        Simulation sim = new Simulation(Settings.defaults(), world);

        sim.step();
        sim.step();
        sim.step();

        recorders.forEach(r -> assertEquals(3, r.acts));
        assertEquals(15, log.size());
    }

    @Test
    void newbornDoesNotActInTheStepItWasBorn() {
        World world = new World(5, 5);
        List<Agent> log = new ArrayList<>();
        Breeder parent = new Breeder(log, 1000);
        world.setAgent(2, 2, parent);
        Simulation sim = new Simulation(Settings.defaults(), world);

        sim.step();

        List<Agent> all = world.getAgents();
        assertEquals(2, all.size(), "родился один ребёнок");
        Recorder child = (Recorder) all.stream().filter(a -> a != parent).findFirst().orElseThrow();
        assertEquals(1, parent.acts);
        assertEquals(0, child.acts, "ребёнок в шаг рождения не ходит");

        sim.step();
        assertEquals(1, child.acts, "а со следующего шага ходит");
    }

    @Test
    void eatenAgentDoesNotActAfterwardsAndTurnOrderIsRandom() {
        // 10 пар «едок + жертва», пары далеко друг от друга. Один шаг.
        World world = new World(30, 1);
        List<Agent> log = new ArrayList<>();
        List<Eater> eaters = new ArrayList<>();
        List<Recorder> victims = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Eater eater = new Eater(log);
            Recorder victim = new Recorder(log, 1);
            world.setAgent(i * 3, 0, eater);
            world.setAgent(i * 3 + 1, 0, victim);
            eaters.add(eater);
            victims.add(victim);
        }
        Simulation sim = new Simulation(Settings.defaults(), world);

        sim.step();

        int eaterFirst = 0;
        int victimFirst = 0;
        for (int i = 0; i < 10; i++) {
            Recorder victim = victims.get(i);
            assertFalse(victim.isAlive(), "каждую жертву съели");
            if (log.indexOf(eaters.get(i)) < log.indexOf(victim) || !log.contains(victim)) {
                eaterFirst++;
                assertEquals(0, victim.acts, "съеденная до своего хода жертва ходить не должна");
            } else {
                victimFirst++;
                assertEquals(1, victim.acts);
            }
        }
        assertTrue(eaterFirst > 0 && victimFirst > 0,
                "порядок ходов случайный: должны встретиться оба варианта, а было " + eaterFirst + " и " + victimFirst);
    }

    @Test
    void starvedAnimalsDisappearFromStatistics() {
        Settings settings = Settings.builder().herbivoreStepCost(5).build();
        World world = new World(5, 5);
        world.setAgent(1, 1, new Herbivore(settings, 5));
        world.setAgent(3, 3, new Herbivore(settings, 6));
        Simulation sim = new Simulation(settings, world);

        sim.step();

        assertEquals(1, sim.statistics().herbivores());
    }

    @Test
    void statisticsCountsEachSpeciesSeparately() {
        Settings settings = Settings.defaults();
        World world = new World(6, 6);
        world.setAgent(0, 0, new Plant(settings));
        world.setAgent(5, 5, new Plant(settings));
        world.setAgent(0, 5, new Herbivore(settings));
        world.setAgent(2, 5, new Herbivore(settings));
        world.setAgent(4, 5, new Herbivore(settings));
        world.setAgent(5, 0, new Predator(settings));

        assertEquals(new Statistics(0, 2, 3, 1), new Simulation(settings, world).statistics());
    }

    // ---------- долгий прогон: ничего не ломается ----------

    @ParameterizedTest(name = "seed {0}: 300 шагов без нарушений")
    @ValueSource(longs = {1, 2, 3, 42, -99})
    void invariantsHoldDuringLongRun(long seed) {
        Settings settings = field(30, 20, 150, 40, 10).toBuilder().seed(seed).build();
        Simulation sim = new Simulation(settings);

        for (int i = 0; i < 300; i++) {
            sim.step();
            ReadOnlyWorld world = sim.getWorld();
            int plants = 0, herbivores = 0, predators = 0;
            for (int y = 0; y < world.getHeight(); y++) {
                for (int x = 0; x < world.getWidth(); x++) {
                    Agent a = world.getAgent(x, y);
                    if (a == null) {
                        continue;
                    }
                    assertTrue(a.isAlive(), "на поле мёртвый житель");
                    assertEquals(new Position(x, y), a.getPosition(), "координаты жителя разошлись с полем");
                    assertTrue(a.getEnergy() >= 1, "живой житель без энергии");
                    switch (a) {
                        case Plant p -> {
                            plants++;
                            assertTrue(p.getEnergy() <= settings.plantMaxEnergy());
                        }
                        case Herbivore h -> herbivores++;
                        case Predator p -> predators++;
                        default -> fail("неизвестный житель " + a);
                    }
                }
            }
            assertEquals(new Statistics(i + 1, plants, herbivores, predators), sim.statistics());
        }
    }
}
