package life.controller;

import life.model.ReadOnlyWorld;
import life.model.Settings;
import life.model.Simulation;
import life.model.Statistics;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Управление симуляцией для оконной версии: старт, пауза, шаг, сброс, скорость.
 * Не знает ничего про JavaFX — окно только вызывает методы и рисует состояние,
 * поэтому всю логику кнопок можно проверить обычными тестами.
 * <p>
 * Время приходит снаружи через {@link #tick(long)} (окно вызывает его на каждом кадре),
 * а контроллер сам решает, сколько шагов сделать, исходя из скорости.
 */
public class SimulationController {

    public static final int MIN_SPEED = 1;
    public static final int MAX_SPEED = 500;
    public static final int DEFAULT_SPEED = 20;
    /** Больше шагов за один кадр не делаем, чтобы окно не подвисало. */
    public static final int MAX_STEPS_PER_TICK = 20;
    /** Сколько последних точек помнить для графика. */
    public static final int HISTORY_LIMIT = 1000;

    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    private Settings settings;
    private Simulation simulation;
    private final Deque<Statistics> history = new ArrayDeque<>();

    private boolean running;
    private int speed = DEFAULT_SPEED;
    /** Время прошлого кадра; пока его нет (сразу после старта), первый кадр только запоминает время. */
    private boolean hasLastTick;
    private long lastTick;
    /** Накопленное время × скорость: целая часть от деления на секунду — сколько шагов пора сделать. */
    private long stepDebt;

    public SimulationController(Settings settings) {
        applySettings(settings);
    }

    /** Запустить. Повторное нажатие ничего не меняет; вымершее поле не запускается. */
    public void start() {
        if (running || simulation.statistics().allDead()) {
            return;
        }
        running = true;
        hasLastTick = false; // время, проведённое на паузе, не должно превратиться в шаги
        stepDebt = 0;
    }

    public void pause() {
        running = false;
    }

    /** Одна кнопка «Старт/Пауза». */
    public void toggleRunning() {
        if (running) {
            pause();
        } else {
            start();
        }
    }

    /** Кнопка «Шаг»: ставит на паузу и делает ровно один шаг. */
    public void stepOnce() {
        pause();
        if (!simulation.statistics().allDead()) {
            makeStep();
        }
    }

    /** Кнопка «Сброс»: то же поле с теми же настройками заново, на паузе. */
    public void reset() {
        pause();
        simulation = new Simulation(settings);
        history.clear();
        history.addLast(simulation.statistics());
    }

    /** Новые настройки из окна параметров: создаётся новое поле. */
    public void applySettings(Settings newSettings) {
        settings = newSettings;
        reset();
    }

    /** Скорость в шагах в секунду; значения вне допустимого диапазона прижимаются к границе. */
    public void setSpeed(int stepsPerSecond) {
        speed = Math.clamp(stepsPerSecond, MIN_SPEED, MAX_SPEED);
    }

    /** Очередной кадр окна. nowNanos — текущее время в наносекундах. */
    public void tick(long nowNanos) {
        if (!running) {
            return;
        }
        if (!hasLastTick || nowNanos < lastTick) {
            hasLastTick = true;
            lastTick = nowNanos;
            return;
        }
        // Больше секунды за кадр не учитываем: иначе после долгого «замерзания» набежит гигантский долг
        long elapsed = Math.min(nowNanos - lastTick, NANOS_PER_SECOND);
        lastTick = nowNanos;
        stepDebt += elapsed * speed;

        long due = stepDebt / NANOS_PER_SECOND;
        stepDebt %= NANOS_PER_SECOND;
        if (due > MAX_STEPS_PER_TICK) {
            due = MAX_STEPS_PER_TICK;
            stepDebt = 0; // не успели — не догоняем, просто идём медленнее
        }
        for (int i = 0; i < due; i++) {
            makeStep();
            if (simulation.statistics().allDead()) {
                pause();
                return;
            }
        }
    }

    public boolean isRunning() {
        return running;
    }

    public int getSpeed() {
        return speed;
    }

    public Settings getSettings() {
        return settings;
    }

    public ReadOnlyWorld getWorld() {
        return simulation.getWorld();
    }

    public Statistics getStatistics() {
        return simulation.statistics();
    }

    /** Последние точки численности для графика, от старых к новым. */
    public List<Statistics> getHistory() {
        return List.copyOf(history);
    }

    /** Что сообщить пользователю: жизнь прекратилась, какие виды вымерли или ничего. */
    public String getStatusMessage() {
        Statistics stats = simulation.statistics();
        if (stats.allDead()) {
            return "Жизнь прекратилась на шаге " + stats.step();
        }
        List<String> extinct = new ArrayList<>();
        if (stats.plants() == 0) {
            extinct.add("растения");
        }
        if (stats.herbivores() == 0) {
            extinct.add("травоядные");
        }
        if (stats.predators() == 0) {
            extinct.add("хищники");
        }
        return extinct.isEmpty() ? "" : "Вымерли: " + String.join(", ", extinct);
    }

    private void makeStep() {
        simulation.step();
        history.addLast(simulation.statistics());
        if (history.size() > HISTORY_LIMIT) {
            history.removeFirst();
        }
    }
}
