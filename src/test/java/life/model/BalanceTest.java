package life.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Главное требование лабы: с настройками по умолчанию жизнь не прекращается — все три вида выживают.
 * Seed здесь намеренно другие, чем при подборе параметров, чтобы проверка была честной.
 */
class BalanceTest {

    private static final int STEPS = 5000;

    @ParameterizedTest(name = "seed {0}: {1} шагов все виды живы")
    @ValueSource(longs = {7, 2024, 31337, 900_001, -12345})
    void defaultSettingsKeepAllSpeciesAlive(long seed) {
        Simulation sim = new Simulation(Settings.defaults().toBuilder().seed(seed).build());

        for (int i = 0; i < STEPS; i++) {
            sim.step();
            Statistics stats = sim.statistics();
            if (stats.anySpeciesExtinct()) {
                fail("вид вымер: " + stats);
            }
        }
    }
}
