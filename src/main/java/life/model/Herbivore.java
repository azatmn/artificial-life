package life.model;

import java.util.List;
import java.util.Random;

/** Травоядное: ест растения и убегает от хищников. */
public class Herbivore extends Animal {

    public Herbivore(Settings settings) {
        this(settings, settings.herbivoreStartEnergy());
    }

    public Herbivore(Settings settings, int energy) {
        super(settings, energy);
    }

    /**
     * Ход травоядного:
     * ЕСЛИ видит хищника — убегает в противоположную сторону;
     * ИНАЧЕ ЕСЛИ видит растение — идёт к нему (и съедает, если дошло);
     * ИНАЧЕ бродит случайно.
     * Перед этим тратит энергию, после — размножается, если энергии хватает.
     */
    @Override
    public void act(World world, Random random) {
        if (!isAlive() || !spendEnergy(world)) {
            return;
        }
        List<Agent> seen = look(world);
        Agent predator = nearest(seen, other -> other instanceof Predator, random);
        if (predator != null) {
            moveAwayFrom(world, predator.getPosition(), random);
        } else {
            Agent plant = nearest(seen, this::isFood, random);
            if (plant != null) {
                moveTowards(world, plant.getPosition(), random);
            } else {
                wander(world, random);
            }
        }
        tryReproduce(world, random);
    }

    @Override
    public char symbol() {
        return 'T';
    }

    @Override
    protected int stepCost() {
        return settings.herbivoreStepCost();
    }

    @Override
    protected int foodEnergy() {
        return settings.herbivoreFoodEnergy();
    }

    @Override
    protected boolean isFood(Agent other) {
        return other instanceof Plant;
    }

    @Override
    protected int reproductionThreshold() {
        return settings.herbivoreReproductionThreshold();
    }

    @Override
    protected Agent createChild(int energy) {
        return new Herbivore(settings, energy);
    }
}
