package life.model;

import java.util.List;
import java.util.Random;

/**
 * Житель поля: растение или животное.
 * Хранит общие для всех данные — где стоит, сколько энергии, жив ли —
 * и общие действия: изменение энергии, гибель, размножение делением.
 * Что именно житель делает за ход, решает каждый наследник сам (метод {@link #act}).
 */
public abstract class Agent {

    /** Клетка, где стоит житель; null — его нет на поле. Меняет только {@link World}. */
    private Position position;
    private int energy;
    private boolean alive = true;

    protected Agent(int energy) {
        if (energy < 1) {
            throw new IllegalArgumentException("Житель должен рождаться с положительной энергией, указано " + energy);
        }
        this.energy = energy;
    }

    public Position getPosition() {
        return position;
    }

    public int getEnergy() {
        return energy;
    }

    public boolean isAlive() {
        return alive;
    }

    /** Один ход жителя. */
    public abstract void act(World world, Random random);

    /** Буква для консоли: P — растение, T — травоядное, X — хищник. */
    public abstract char symbol();

    /** Порог размножения: при энергии строго больше порога житель делится. */
    protected abstract int reproductionThreshold();

    /** Новый житель того же вида с заданной энергией. */
    protected abstract Agent createChild(int energy);

    /** Прибавляет (или отнимает) энергию. Энергия не бывает меньше нуля и не переполняется. */
    protected void changeEnergy(int delta) {
        energy = Math.clamp((long) energy + delta, 0, Integer.MAX_VALUE);
    }

    /** Житель погибает и исчезает с поля. Повторный вызов ничего не делает. */
    protected void die(World world) {
        alive = false;
        if (position != null) {
            world.removeAgent(this);
        }
    }

    /**
     * Размножение делением: если энергии больше порога и рядом есть свободная клетка,
     * в случайной свободной соседней клетке появляется ребёнок с половиной энергии родителя.
     *
     * @return ребёнок или null, если размножиться не получилось
     */
    protected Agent tryReproduce(World world, Random random) {
        if (!alive || energy <= reproductionThreshold()) {
            return null;
        }
        List<Position> free = world.getEmptyCellsAround(position);
        if (free.isEmpty()) {
            return null;
        }
        Position place = free.get(random.nextInt(free.size()));
        Agent child = createChild(energy / 2);
        energy -= child.getEnergy();
        world.setAgent(place, child);
        return child;
    }

    void setPosition(Position position) {
        this.position = position;
    }
}
