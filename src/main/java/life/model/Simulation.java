package life.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Симуляция целиком: поле с жителями, генератор случайных чисел и счётчик шагов.
 * Снаружи доступны только «сделать шаг», статистика и поле для чтения.
 */
public class Simulation {

    private final Settings settings;
    private final World world;
    /** Один генератор на всю симуляцию: так ход игры полностью определяется seed из настроек. */
    private final Random random;
    private long step;

    /** Новая симуляция: поле нужного размера со случайно расставленными жителями. */
    public Simulation(Settings settings) {
        this(settings, new World(settings.width(), settings.height()));
        populate();
    }

    /** Симуляция на готовом поле (для тестов): жители не расставляются. */
    Simulation(Settings settings, World world) {
        this.settings = settings;
        this.world = world;
        this.random = new Random(settings.seed());
    }

    /**
     * Один шаг времени: каждый житель, живой на начало шага, ходит один раз, в случайном порядке.
     * Съеденный до своей очереди не ходит; родившийся в этом шаге ждёт следующего.
     */
    public void step() {
        List<Agent> order = new ArrayList<>(world.getAgents());
        Collections.shuffle(order, random);
        for (Agent agent : order) {
            if (agent.isAlive()) {
                agent.act(world, random);
            }
        }
        step++;
    }

    public Statistics statistics() {
        return new Statistics(step,
                world.count(Plant.class),
                world.count(Herbivore.class),
                world.count(Predator.class));
    }

    public ReadOnlyWorld getWorld() {
        return world;
    }

    public Settings getSettings() {
        return settings;
    }

    public long getStep() {
        return step;
    }

    /** Расставляет начальных жителей по случайным различным клеткам. */
    private void populate() {
        List<Position> cells = new ArrayList<>(settings.width() * settings.height());
        for (int y = 0; y < settings.height(); y++) {
            for (int x = 0; x < settings.width(); x++) {
                cells.add(new Position(x, y));
            }
        }
        Collections.shuffle(cells, random);
        Iterator<Position> free = cells.iterator();
        for (int i = 0; i < settings.initialPlants(); i++) {
            world.setAgent(free.next(), new Plant(settings));
        }
        for (int i = 0; i < settings.initialHerbivores(); i++) {
            world.setAgent(free.next(), new Herbivore(settings));
        }
        for (int i = 0; i < settings.initialPredators(); i++) {
            world.setAgent(free.next(), new Predator(settings));
        }
    }
}
