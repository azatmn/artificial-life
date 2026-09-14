package life.model;

import java.util.Random;

/** Хищник: охотится на травоядных. */
public class Predator extends Animal {

    public Predator(Settings settings) {
        this(settings, settings.predatorStartEnergy());
    }

    public Predator(Settings settings, int energy) {
        super(settings, energy);
    }

    /**
     * Ход хищника:
     * ЕСЛИ видит травоядное — идёт к нему (и съедает, если дошёл);
     * ИНАЧЕ бродит случайно.
     * Перед этим тратит энергию, после — размножается, если энергии хватает.
     */
    @Override
    public void act(World world, Random random) {
        if (!isAlive() || !spendEnergy(world)) {
            return;
        }
        Agent prey = nearest(look(world), this::isFood, random);
        if (prey != null) {
            moveTowards(world, prey.getPosition(), random);
        } else {
            wander(world, random);
        }
        tryReproduce(world, random);
    }

    @Override
    public char symbol() {
        return 'X';
    }

    @Override
    protected int stepCost() {
        return settings.predatorStepCost();
    }

    @Override
    protected int foodEnergy() {
        return settings.predatorFoodEnergy();
    }

    @Override
    protected boolean isFood(Agent other) {
        return other instanceof Herbivore;
    }

    @Override
    protected int reproductionThreshold() {
        return settings.predatorReproductionThreshold();
    }

    @Override
    protected Agent createChild(int energy) {
        return new Predator(settings, energy);
    }
}
