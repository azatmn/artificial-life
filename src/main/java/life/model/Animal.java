package life.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;

/**
 * Животное: видит квадрат 5×5 вокруг себя, ходит на одну клетку за ход и тратит энергию.
 * Здесь собраны общие для всех животных умения; какую еду искать и кого бояться,
 * решают наследники в своём методе {@link #act}.
 */
public abstract class Animal extends Agent {

    /** Радиус зрения: 2 клетки в каждую сторону — квадрат 5×5 с животным в центре. */
    public static final int VISION_RADIUS = 2;

    protected final Settings settings;

    protected Animal(Settings settings, int energy) {
        super(energy);
        this.settings = settings;
    }

    /** Сколько энергии тратится за ход. */
    protected abstract int stepCost();

    /** Сколько энергии даёт одна съеденная еда. */
    protected abstract int foodEnergy();

    /** Можно ли это съесть. */
    protected abstract boolean isFood(Agent other);

    /**
     * Тратит энергию за ход. Если энергия кончилась — животное погибает.
     *
     * @return true, если животное выжило
     */
    protected boolean spendEnergy(World world) {
        changeEnergy(-stepCost());
        if (getEnergy() == 0) {
            die(world);
            return false;
        }
        return true;
    }

    /** Все, кого животное видит в квадрате 5×5 вокруг себя. */
    protected List<Agent> look(World world) {
        Position here = getPosition();
        return world.getNeighbors(here.x(), here.y(), VISION_RADIUS);
    }

    /** Ближайший из увиденных, кто подходит под условие; при равном расстоянии — случайный. null — таких нет. */
    protected Agent nearest(List<Agent> seen, Predicate<Agent> filter, Random random) {
        List<Agent> closest = new ArrayList<>();
        long bestDistance = Long.MAX_VALUE;
        for (Agent other : seen) {
            if (!filter.test(other)) {
                continue;
            }
            long distance = getPosition().distanceSquaredTo(other.getPosition());
            if (distance < bestDistance) {
                closest.clear();
                bestDistance = distance;
            }
            if (distance == bestDistance) {
                closest.add(other);
            }
        }
        return closest.isEmpty() ? null : closest.get(random.nextInt(closest.size()));
    }

    /** Шаг к цели. Если на выбранной клетке еда — съедает её. Если ближе подойти нельзя — стоит. */
    protected void moveTowards(World world, Position target, Random random) {
        Position step = chooseStep(world, true, cell -> cell.distanceSquaredTo(target), random);
        if (step != null) {
            stepTo(world, step);
        }
    }

    /** Шаг в свободную клетку, самую дальнюю от угрозы. Если дальше уйти нельзя — стоит. */
    protected void moveAwayFrom(World world, Position threat, Random random) {
        Position step = chooseStep(world, false, cell -> -cell.distanceSquaredTo(threat), random);
        if (step != null) {
            stepTo(world, step);
        }
    }

    /** Шаг в случайную свободную соседнюю клетку. Если все заняты — стоит. */
    protected void wander(World world, Random random) {
        List<Position> free = world.getEmptyCellsAround(getPosition());
        if (!free.isEmpty()) {
            world.moveAgent(this, free.get(random.nextInt(free.size())));
        }
    }

    /**
     * Выбирает соседнюю клетку с наименьшей «оценкой» — но только если она лучше, чем остаться на месте.
     * Проходимы пустые клетки, а если mayEat — ещё и клетки с едой.
     *
     * @return клетка для шага или null — лучше стоять
     */
    private Position chooseStep(World world, boolean mayEat, ToLongFunction<Position> score, Random random) {
        Position here = getPosition();
        long stayScore = score.applyAsLong(here);
        long bestScore = stayScore;
        List<Position> bestCells = new ArrayList<>();
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                Position cell = here.translate(dx, dy);
                if ((dx == 0 && dy == 0) || !world.isInside(cell)) {
                    continue;
                }
                Agent occupant = world.getAgent(cell);
                if (occupant != null && !(mayEat && isFood(occupant))) {
                    continue;
                }
                long cellScore = score.applyAsLong(cell);
                if (cellScore < bestScore) {
                    bestCells.clear();
                    bestScore = cellScore;
                }
                if (cellScore == bestScore && bestScore < stayScore) {
                    bestCells.add(cell);
                }
            }
        }
        return bestCells.isEmpty() ? null : bestCells.get(random.nextInt(bestCells.size()));
    }

    /** Переходит в клетку; если там еда — сначала съедает её. */
    private void stepTo(World world, Position cell) {
        Agent food = world.getAgent(cell);
        if (food != null) {
            changeEnergy(foodEnergy());
            food.die(world);
        }
        world.moveAgent(this, cell);
    }
}
