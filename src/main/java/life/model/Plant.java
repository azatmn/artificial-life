package life.model;

import java.util.Random;

/** Растение: не двигается, само набирает энергию «от солнца» и разрастается в соседние клетки. */
public class Plant extends Agent {

    private final Settings settings;

    public Plant(Settings settings) {
        this(settings, settings.plantStartEnergy());
    }

    public Plant(Settings settings, int energy) {
        super(energy);
        if (energy > settings.plantMaxEnergy()) {
            throw new IllegalArgumentException("Энергия растения " + energy
                    + " больше максимума " + settings.plantMaxEnergy());
        }
        this.settings = settings;
    }

    /** Ход растения: подрасти (не выше максимума), затем размножиться, если энергии хватает. */
    @Override
    public void act(World world, Random random) {
        if (!isAlive()) {
            return;
        }
        int room = settings.plantMaxEnergy() - getEnergy();
        changeEnergy(Math.min(settings.plantGrowth(), room));
        tryReproduce(world, random);
    }

    @Override
    public char symbol() {
        return 'P';
    }

    @Override
    protected int reproductionThreshold() {
        return settings.plantReproductionThreshold();
    }

    @Override
    protected Agent createChild(int energy) {
        return new Plant(settings, energy);
    }
}
