package life.controller;

import life.model.Agent;
import life.model.ReadOnlyWorld;
import life.model.Settings;
import life.model.Simulation;
import life.model.Statistics;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Логика кнопок окна. Время подаётся вручную (в наносекундах), поэтому тесты не ждут и не зависят от скорости машины. */
class SimulationControllerTest {

    private static final long SECOND = 1_000_000_000L;

    /** Маленькое поле: растения не вымирают, поэтому жизнь не прекращается. */
    private static Settings small() {
        return Settings.builder().width(10).height(6)
                .initialPlants(15).initialHerbivores(5).initialPredators(2).build();
    }

    /** Одни голодные травоядные: все умирают на шаге 2. */
    private static Settings doomed() {
        return Settings.builder().width(5).height(5)
                .initialPlants(0).initialHerbivores(3).initialPredators(0)
                .herbivoreStartEnergy(4).herbivoreStepCost(2).build();
    }

    private static String snapshot(ReadOnlyWorld world) {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < world.getHeight(); y++) {
            for (int x = 0; x < world.getWidth(); x++) {
                Agent a = world.getAgent(x, y);
                sb.append(a == null ? "." : a.symbol() + String.valueOf(a.getEnergy())).append(',');
            }
        }
        return sb.toString();
    }

    private static long step(SimulationController c) {
        return c.getStatistics().step();
    }

    // ---------- начальное состояние ----------

    @Test
    void startsPausedAtStepZeroWithDefaultSpeed() {
        SimulationController c = new SimulationController(small());
        assertFalse(c.isRunning());
        assertEquals(0, step(c));
        assertEquals(SimulationController.DEFAULT_SPEED, c.getSpeed());
        assertEquals(small(), c.getSettings());
    }

    @Test
    void pausedControllerIgnoresTicks() {
        SimulationController c = new SimulationController(small());
        c.tick(0);
        c.tick(5 * SECOND);
        assertEquals(0, step(c));
    }

    // ---------- старт и пауза ----------

    @Test
    void runningControllerMakesSpeedStepsPerSecond() {
        SimulationController c = new SimulationController(small());
        c.setSpeed(12);
        c.start();
        c.tick(0);          // первый кадр только запоминает время
        c.tick(SECOND);

        assertTrue(c.isRunning());
        assertEquals(12, step(c));
    }

    @Test
    void pressingStartTwiceDoesNotDoubleTheSpeed() {
        SimulationController c = new SimulationController(small());
        c.setSpeed(12);
        c.start();
        c.tick(0);
        c.start();
        c.tick(SECOND);

        assertEquals(12, step(c));
    }

    @Test
    void pauseStopsSteps() {
        SimulationController c = new SimulationController(small());
        c.start();
        c.tick(0);
        c.tick(SECOND / 2);
        long before = step(c);
        c.pause();
        c.tick(SECOND);
        c.tick(2 * SECOND);

        assertFalse(c.isRunning());
        assertEquals(before, step(c));
    }

    @Test
    void pausingTwiceIsHarmless() {
        SimulationController c = new SimulationController(small());
        c.pause();
        c.pause();
        assertFalse(c.isRunning());
    }

    @Test
    void timeSpentOnPauseIsNotCaughtUpAfterStart() {
        SimulationController c = new SimulationController(small());
        c.setSpeed(10);
        c.start();
        c.tick(0);
        c.tick(SECOND);                 // 10 шагов
        c.pause();
        c.tick(100 * SECOND);           // долгая пауза
        c.start();
        c.tick(101 * SECOND);           // первый кадр после паузы только запоминает время
        assertEquals(10, step(c), "время на паузе не должно превращаться в шаги");
        c.tick(102 * SECOND);
        assertEquals(20, step(c));
    }

    @Test
    void toggleSwitchesBetweenRunningAndPaused() {
        SimulationController c = new SimulationController(small());
        c.toggleRunning();
        assertTrue(c.isRunning());
        c.toggleRunning();
        assertFalse(c.isRunning());
    }

    // ---------- скорость и кадры ----------

    @Test
    void fractionalStepsAccumulateAcrossFrames() {
        // 3 шага в секунду при 60 кадрах: за кадр 0,05 шага — дробные доли не должны теряться.
        SimulationController c = new SimulationController(small());
        c.setSpeed(3);
        c.start();
        long frame = SECOND / 60 + 1;
        for (int i = 0; i <= 60; i++) {
            c.tick(i * frame);
        }
        assertEquals(3, step(c));
    }

    @Test
    void longFreezeDoesNotCauseAnAvalancheOfSteps() {
        SimulationController c = new SimulationController(small());
        c.setSpeed(SimulationController.MAX_SPEED);
        c.start();
        c.tick(0);
        c.tick(10 * SECOND);  // окно «замёрзло» на 10 секунд

        assertEquals(SimulationController.MAX_STEPS_PER_TICK, step(c), "не больше лимита за кадр");

        c.tick(10 * SECOND + SECOND / 100); // следующий обычный кадр: 500 × 0,01 = 5 шагов
        assertEquals(SimulationController.MAX_STEPS_PER_TICK + 5, step(c), "и долг не догоняется потом");
    }

    @Test
    void timeGoingBackwardsDoesNothing() {
        SimulationController c = new SimulationController(small());
        c.start();
        c.tick(5 * SECOND);
        assertDoesNotThrow(() -> c.tick(SECOND));
        assertEquals(0, step(c));
    }

    @Test
    void speedIsClampedToAllowedRange() {
        SimulationController c = new SimulationController(small());
        c.setSpeed(0);
        assertEquals(SimulationController.MIN_SPEED, c.getSpeed());
        c.setSpeed(-100);
        assertEquals(SimulationController.MIN_SPEED, c.getSpeed());
        c.setSpeed(Integer.MAX_VALUE);
        assertEquals(SimulationController.MAX_SPEED, c.getSpeed());
        c.setSpeed(SimulationController.MAX_SPEED);
        assertEquals(SimulationController.MAX_SPEED, c.getSpeed());
        c.setSpeed(SimulationController.MIN_SPEED);
        assertEquals(SimulationController.MIN_SPEED, c.getSpeed());
        c.setSpeed(137);
        assertEquals(137, c.getSpeed());
    }

    @Test
    void speedChangeWorksWhileRunning() {
        SimulationController c = new SimulationController(small());
        c.setSpeed(5);
        c.start();
        c.tick(0);
        c.tick(SECOND);
        c.setSpeed(40);
        c.tick(2 * SECOND);
        assertEquals(5 + 20, step(c), "за кадр больше 20 шагов не делается");
    }

    @Test
    void maximumSpeedWithSmoothFramesGivesMaximumSteps() {
        SimulationController c = new SimulationController(small());
        c.setSpeed(SimulationController.MAX_SPEED);
        c.start();
        for (int i = 0; i <= 100; i++) {
            c.tick(i * (SECOND / 100));
        }
        assertEquals(SimulationController.MAX_SPEED, step(c), "при частых кадрах лимит за кадр не мешает");
    }

    // ---------- шаг ----------

    @Test
    void stepOnceWhilePausedMakesExactlyOneStep() {
        SimulationController c = new SimulationController(small());
        c.stepOnce();
        c.stepOnce();
        assertEquals(2, step(c));
        assertFalse(c.isRunning());
    }

    @Test
    void stepOnceWhileRunningPausesAndMakesOneStep() {
        SimulationController c = new SimulationController(small());
        c.start();
        c.tick(0);
        c.stepOnce();
        c.tick(SECOND);

        assertFalse(c.isRunning(), "после «Шаг» симуляция на паузе");
        assertEquals(1, step(c));
    }

    // ---------- сброс и новые настройки ----------

    @Test
    void resetStartsTheSameWorldAgainAndPauses() {
        SimulationController c = new SimulationController(small());
        c.start();
        c.tick(0);
        c.tick(SECOND);
        c.reset();

        assertFalse(c.isRunning());
        assertEquals(0, step(c));
        assertEquals(snapshot(new Simulation(small()).getWorld()), snapshot(c.getWorld()));
    }

    @Test
    void applyingSettingsCreatesNewWorld() {
        SimulationController c = new SimulationController(small());
        c.stepOnce();
        Settings bigger = small().toBuilder().width(20).height(9).seed(77).build();

        c.applySettings(bigger);

        assertEquals(bigger, c.getSettings());
        assertEquals(20, c.getWorld().getWidth());
        assertEquals(9, c.getWorld().getHeight());
        assertEquals(0, step(c));
        assertFalse(c.isRunning());
        assertEquals(snapshot(new Simulation(bigger).getWorld()), snapshot(c.getWorld()));
    }

    @Test
    void resetAfterApplyingSettingsKeepsNewSettings() {
        SimulationController c = new SimulationController(small());
        Settings other = small().toBuilder().seed(5).build();
        c.applySettings(other);
        c.stepOnce();
        c.reset();
        assertEquals(other, c.getSettings());
        assertEquals(snapshot(new Simulation(other).getWorld()), snapshot(c.getWorld()));
    }

    // ---------- конец жизни ----------

    @Test
    void controllerPausesItselfWhenLifeEnds() {
        SimulationController c = new SimulationController(doomed());
        c.setSpeed(10);
        c.start();
        c.tick(0);
        c.tick(SECOND);

        assertFalse(c.isRunning());
        assertEquals(2, step(c), "дальше вымершего поля не шагаем");
        assertTrue(c.getStatusMessage().contains("Жизнь прекратилась на шаге 2"), c.getStatusMessage());
    }

    @Test
    void deadWorldCannotBeStartedOrStepped() {
        SimulationController c = new SimulationController(doomed());
        c.stepOnce();
        c.stepOnce();
        c.start();
        c.stepOnce();

        assertFalse(c.isRunning());
        assertEquals(2, step(c));
    }

    @Test
    void resetRevivesDeadWorld() {
        SimulationController c = new SimulationController(doomed());
        c.stepOnce();
        c.stepOnce();
        c.reset();
        c.start();
        assertTrue(c.isRunning());
        assertEquals(3, c.getStatistics().herbivores());
    }

    @Test
    void statusListsExtinctSpecies() {
        SimulationController c = new SimulationController(small().toBuilder().initialPredators(0).build());
        assertEquals("Вымерли: хищники", c.getStatusMessage());
    }

    @Test
    void statusIsEmptyWhileEveryoneLives() {
        assertEquals("", new SimulationController(small()).getStatusMessage());
    }

    // ---------- история для графика ----------

    @Test
    void historyStartsWithInitialStatistics() {
        SimulationController c = new SimulationController(small());
        assertEquals(List.of(c.getStatistics()), c.getHistory());
    }

    @Test
    void historyRecordsEveryStep() {
        SimulationController c = new SimulationController(small());
        c.stepOnce();
        c.stepOnce();
        c.setSpeed(5);
        c.start();
        c.tick(0);
        c.tick(SECOND);

        List<Statistics> history = c.getHistory();
        assertEquals(8, history.size(), "шаг 0 + 2 ручных + 5 по таймеру");
        for (int i = 0; i < history.size(); i++) {
            assertEquals(i, history.get(i).step());
        }
        assertEquals(c.getStatistics(), history.getLast());
    }

    @Test
    void historyKeepsOnlyTheLatestPoints() {
        SimulationController c = new SimulationController(small());
        for (int i = 0; i < SimulationController.HISTORY_LIMIT + 50; i++) {
            c.stepOnce();
        }
        List<Statistics> history = c.getHistory();
        assertEquals(SimulationController.HISTORY_LIMIT, history.size());
        assertEquals(c.getStatistics(), history.getLast());
        assertEquals(51, history.getFirst().step(), "самые старые точки выброшены");
    }

    @Test
    void historyIsClearedOnReset() {
        SimulationController c = new SimulationController(small());
        c.stepOnce();
        c.stepOnce();
        c.reset();
        assertEquals(1, c.getHistory().size());
        assertEquals(0, c.getHistory().getFirst().step());
    }

    @Test
    void historyCannotBeChangedFromOutside() {
        SimulationController c = new SimulationController(small());
        assertThrows(UnsupportedOperationException.class, () -> c.getHistory().clear());
    }
}
