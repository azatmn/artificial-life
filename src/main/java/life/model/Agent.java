package life.model;

import java.util.Random;

/**
 * Житель поля: растение или животное.
 * Хранит общие для всех данные — где стоит и сколько у него энергии.
 * Что именно житель делает за ход, решает каждый наследник сам (метод {@link #act}).
 */
public abstract class Agent {

    /** Клетка, где стоит житель; null — его нет на поле. Меняет только {@link World}. */
    private Position position;
    private int energy;

    protected Agent(int energy) {
        this.energy = energy;
    }

    public Position getPosition() {
        return position;
    }

    public int getEnergy() {
        return energy;
    }

    /** Один ход жителя. */
    public abstract void act(World world, Random random);

    /** Буква для консоли: P — растение, T — травоядное, X — хищник. */
    public abstract char symbol();

    void setPosition(Position position) {
        this.position = position;
    }
}
