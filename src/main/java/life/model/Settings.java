package life.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Все параметры симуляции в одном месте.
 * Объект неизменяемый: создаётся через {@link Builder} и проверяется при создании,
 * поэтому некорректные настройки не могут попасть в модель.
 */
public record Settings(
        int width,
        int height,
        int initialPlants,
        int initialHerbivores,
        int initialPredators,
        int plantStartEnergy,
        int plantGrowth,
        int plantMaxEnergy,
        int plantReproductionThreshold,
        int herbivoreStartEnergy,
        int herbivoreStepCost,
        int herbivoreFoodEnergy,
        int herbivoreReproductionThreshold,
        int predatorStartEnergy,
        int predatorStepCost,
        int predatorFoodEnergy,
        int predatorReproductionThreshold,
        long seed) {

    public static final int MAX_SIZE = 1000;
    public static final int MAX_ENERGY = 1_000_000;

    public Settings {
        Checker check = new Checker();

        boolean sizeOk = check.range("width", "Ширина поля", width, 1, MAX_SIZE)
                & check.range("height", "Высота поля", height, 1, MAX_SIZE);
        boolean countsOk = check.range("initialPlants", "Начальное число растений", initialPlants, 0, Integer.MAX_VALUE)
                & check.range("initialHerbivores", "Начальное число травоядных", initialHerbivores, 0, Integer.MAX_VALUE)
                & check.range("initialPredators", "Начальное число хищников", initialPredators, 0, Integer.MAX_VALUE);
        if (sizeOk && countsOk) {
            // long, чтобы сумма огромных чисел не переполнилась и не превратилась в «мало»
            long agents = (long) initialPlants + initialHerbivores + initialPredators;
            long cells = (long) width * height;
            if (agents > cells) {
                check.fail("initialCount", "Жителей " + agents + ", а клеток на поле только " + cells);
            }
        }

        check.range("plantStartEnergy", "Начальная энергия растения", plantStartEnergy, 1, MAX_ENERGY);
        check.range("plantGrowth", "Прирост энергии растения за шаг", plantGrowth, 1, MAX_ENERGY);
        check.range("plantMaxEnergy", "Максимум энергии растения", plantMaxEnergy, 1, MAX_ENERGY);
        check.range("plantReproductionThreshold", "Порог размножения растения", plantReproductionThreshold, 1, MAX_ENERGY);
        if (plantStartEnergy > plantMaxEnergy) {
            check.fail("plantStartEnergy", "Начальная энергия растения (" + plantStartEnergy
                    + ") больше его максимума (" + plantMaxEnergy + ")");
        }
        if (plantReproductionThreshold >= plantMaxEnergy) {
            check.fail("plantReproductionThreshold", "Порог размножения растения (" + plantReproductionThreshold
                    + ") должен быть меньше максимума энергии (" + plantMaxEnergy + "), иначе растения не размножатся");
        }

        check.range("herbivoreStartEnergy", "Начальная энергия травоядного", herbivoreStartEnergy, 1, MAX_ENERGY);
        check.range("herbivoreStepCost", "Расход энергии травоядного за шаг", herbivoreStepCost, 1, MAX_ENERGY);
        check.range("herbivoreFoodEnergy", "Энергия травоядного от растения", herbivoreFoodEnergy, 1, MAX_ENERGY);
        check.range("herbivoreReproductionThreshold", "Порог размножения травоядного", herbivoreReproductionThreshold, 1, MAX_ENERGY);

        check.range("predatorStartEnergy", "Начальная энергия хищника", predatorStartEnergy, 1, MAX_ENERGY);
        check.range("predatorStepCost", "Расход энергии хищника за шаг", predatorStepCost, 1, MAX_ENERGY);
        check.range("predatorFoodEnergy", "Энергия хищника от травоядного", predatorFoodEnergy, 1, MAX_ENERGY);
        check.range("predatorReproductionThreshold", "Порог размножения хищника", predatorReproductionThreshold, 1, MAX_ENERGY);

        check.throwIfFailed();
    }

    /** Копит найденные проблемы, чтобы сообщить обо всех сразу. */
    private static final class Checker {
        private final List<InvalidSettingsException.Problem> problems = new ArrayList<>();

        /** Проверяет, что значение в [min, max]. Возвращает true, если всё в порядке. */
        boolean range(String field, String title, int value, int min, int max) {
            if (value >= min && value <= max) {
                return true;
            }
            String allowed = max == Integer.MAX_VALUE ? "не меньше " + min : "от " + min + " до " + max;
            fail(field, title + ": допустимо " + allowed + ", указано " + value);
            return false;
        }

        void fail(String field, String message) {
            problems.add(new InvalidSettingsException.Problem(field, message));
        }

        void throwIfFailed() {
            if (!problems.isEmpty()) {
                throw new InvalidSettingsException(problems);
            }
        }
    }

    public static Settings defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Билдер с текущими значениями — удобно поменять пару полей у готовых настроек. */
    public Builder toBuilder() {
        return new Builder()
                .width(width).height(height)
                .initialPlants(initialPlants).initialHerbivores(initialHerbivores).initialPredators(initialPredators)
                .plantStartEnergy(plantStartEnergy).plantGrowth(plantGrowth)
                .plantMaxEnergy(plantMaxEnergy).plantReproductionThreshold(plantReproductionThreshold)
                .herbivoreStartEnergy(herbivoreStartEnergy).herbivoreStepCost(herbivoreStepCost)
                .herbivoreFoodEnergy(herbivoreFoodEnergy).herbivoreReproductionThreshold(herbivoreReproductionThreshold)
                .predatorStartEnergy(predatorStartEnergy).predatorStepCost(predatorStepCost)
                .predatorFoodEnergy(predatorFoodEnergy).predatorReproductionThreshold(predatorReproductionThreshold)
                .seed(seed);
    }

    /**
     * Пошаговая сборка настроек: меняем только нужные поля, остальные берутся по умолчанию.
     * Умолчания подобраны перебором так, чтобы все три вида жили долго (см. BalanceTest).
     */
    public static final class Builder {
        private int width = 100;
        private int height = 60;
        private int initialPlants = 2000;
        private int initialHerbivores = 270;
        private int initialPredators = 30;
        private int plantStartEnergy = 3;
        private int plantGrowth = 3;
        private int plantMaxEnergy = 12;
        private int plantReproductionThreshold = 9;
        private int herbivoreStartEnergy = 13;
        private int herbivoreStepCost = 1;
        private int herbivoreFoodEnergy = 14;
        private int herbivoreReproductionThreshold = 26;
        private int predatorStartEnergy = 52;
        private int predatorStepCost = 2;
        private int predatorFoodEnergy = 13;
        private int predatorReproductionThreshold = 103;
        private long seed = 42;

        private Builder() {
        }

        public Builder width(int value) { width = value; return this; }
        public Builder height(int value) { height = value; return this; }
        public Builder initialPlants(int value) { initialPlants = value; return this; }
        public Builder initialHerbivores(int value) { initialHerbivores = value; return this; }
        public Builder initialPredators(int value) { initialPredators = value; return this; }
        public Builder plantStartEnergy(int value) { plantStartEnergy = value; return this; }
        public Builder plantGrowth(int value) { plantGrowth = value; return this; }
        public Builder plantMaxEnergy(int value) { plantMaxEnergy = value; return this; }
        public Builder plantReproductionThreshold(int value) { plantReproductionThreshold = value; return this; }
        public Builder herbivoreStartEnergy(int value) { herbivoreStartEnergy = value; return this; }
        public Builder herbivoreStepCost(int value) { herbivoreStepCost = value; return this; }
        public Builder herbivoreFoodEnergy(int value) { herbivoreFoodEnergy = value; return this; }
        public Builder herbivoreReproductionThreshold(int value) { herbivoreReproductionThreshold = value; return this; }
        public Builder predatorStartEnergy(int value) { predatorStartEnergy = value; return this; }
        public Builder predatorStepCost(int value) { predatorStepCost = value; return this; }
        public Builder predatorFoodEnergy(int value) { predatorFoodEnergy = value; return this; }
        public Builder predatorReproductionThreshold(int value) { predatorReproductionThreshold = value; return this; }
        public Builder seed(long value) { seed = value; return this; }

        public Settings build() {
            return new Settings(width, height,
                    initialPlants, initialHerbivores, initialPredators,
                    plantStartEnergy, plantGrowth, plantMaxEnergy, plantReproductionThreshold,
                    herbivoreStartEnergy, herbivoreStepCost, herbivoreFoodEnergy, herbivoreReproductionThreshold,
                    predatorStartEnergy, predatorStepCost, predatorFoodEnergy, predatorReproductionThreshold,
                    seed);
        }
    }
}
