package life.view.console;

import life.model.Settings;
import life.model.Simulation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.function.LongSupplier;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

/** Консоль целиком: подаём текст как будто с клавиатуры, читаем, что вывелось. */
@Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD) // зависание в цикле — тоже ошибка
class ConsoleAppTest {

    private static String run(Simulation simulation, Reader input, LongSupplier clock) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bytes, true, UTF_8);
        new ConsoleApp(simulation, new BufferedReader(input), out, clock).run();
        return bytes.toString(UTF_8);
    }

    /** Часы стоят на месте — прогресс никогда не печатается, остальные тесты от времени не зависят. */
    private static String run(Simulation simulation, Reader input) {
        return run(simulation, input, () -> 0L);
    }

    private static String run(Simulation simulation, String input) {
        return run(simulation, new StringReader(input));
    }

    private static String run(Simulation simulation, String input, LongSupplier clock) {
        return run(simulation, new StringReader(input), clock);
    }

    /** Поддельные часы: первый вызов показывает 0, каждый следующий — на millis миллисекунд позже. */
    private static LongSupplier fakeClock(long millis) {
        long tick = millis * 1_000_000;
        long[] now = {-tick};
        return () -> now[0] += tick;
    }

    /** Строки прогресса без приглашения «> » перед ними. */
    private static List<String> progressLines(String output) {
        return output.lines()
                .filter(line -> line.contains("выполнено"))
                .map(line -> line.substring(line.indexOf("...")))
                .toList();
    }

    private static Simulation small() {
        return new Simulation(Settings.builder().width(10).height(6)
                .initialPlants(15).initialHerbivores(5).initialPredators(2).build());
    }

    private static int occurrences(String text, String part) {
        return text.split(java.util.regex.Pattern.quote(part), -1).length - 1;
    }

    @Test
    void showsFieldAndHelpAtStart() {
        String output = run(small(), "q\n");
        assertTrue(output.contains("Шаг 0 |"), output);
        assertTrue(output.contains("Команды"), output);
    }

    @Test
    void quitsOnQ() {
        Simulation sim = small();
        String output = run(sim, "q\n");
        assertEquals(0, sim.getStep());
        assertTrue(output.contains("До свидания"), output);
    }

    @Test
    void endOfInputQuitsCleanly() {
        Simulation sim = small();
        String output = run(sim, "");
        assertEquals(0, sim.getStep());
        assertTrue(output.contains("До свидания"), output);
    }

    @Test
    void enterMakesOneStep() {
        Simulation sim = small();
        String output = run(sim, "\nq\n");
        assertEquals(1, sim.getStep());
        assertTrue(output.contains("Шаг 1 |"), output);
    }

    @Test
    void numberMakesThatManyStepsAndDrawsOnlyTheResult() {
        Simulation sim = small();
        String output = run(sim, "5\nq\n");
        assertEquals(5, sim.getStep());
        assertTrue(output.contains("Шаг 5 |"), output);
        assertFalse(output.contains("Шаг 3 |"), "промежуточные шаги не печатаются");
    }

    @Test
    void commandsWorkOneAfterAnother() {
        Simulation sim = small();
        run(sim, "2\n\n3\nq\n");
        assertEquals(6, sim.getStep());
    }

    @Test
    void garbageInputDoesNotCrashAndDoesNotStep() {
        Simulation sim = small();
        String input = String.join("\n", "abc", "-1", "0", "2.5", "99999999999999999999", "\u001b[2J", "5 5", "q") + "\n";

        String output = run(sim, input);

        assertEquals(0, sim.getStep());
        assertEquals(7, occurrences(output, "Ошибка:"), output);
        assertFalse(output.contains("Exception"), "никаких стек-трейсов");
        assertFalse(output.contains("\u001b"), "управляющие символы не выводятся обратно");
        assertTrue(output.contains("До свидания"));
    }

    @Test
    void helpCanBeShownAgain() {
        String output = run(small(), "h\nq\n");
        assertEquals(2, occurrences(output, "Команды"), output);
    }

    @Test
    void emptyWorldRefusesToStep() {
        Simulation sim = new Simulation(Settings.builder().width(3).height(3)
                .initialPlants(0).initialHerbivores(0).initialPredators(0).build());

        String output = run(sim, "3\nq\n");

        assertEquals(0, sim.getStep());
        assertTrue(output.contains("Жизнь прекратилась"), output);
    }

    @Test
    void manyStepsStopWhenLifeEnds() {
        // Только травоядные без еды: энергия 4, расход 2 — все умирают на шаге 2.
        Simulation sim = new Simulation(Settings.builder().width(5).height(5)
                .initialPlants(0).initialHerbivores(3).initialPredators(0)
                .herbivoreStartEnergy(4).herbivoreStepCost(2).build());

        String output = run(sim, "100\nq\n");

        assertEquals(2, sim.getStep(), "дальше шагать бессмысленно");
        assertTrue(output.contains("Жизнь прекратилась на шаге 2"), output);
    }

    @Test
    void stepsAfterLifeEndedAreRefusedPolitely() {
        Simulation sim = new Simulation(Settings.builder().width(5).height(5)
                .initialPlants(0).initialHerbivores(3).initialPredators(0)
                .herbivoreStartEnergy(4).herbivoreStepCost(2).build());

        String output = run(sim, "100\n\n5\nq\n");

        assertEquals(2, sim.getStep());
        assertTrue(occurrences(output, "Жизнь прекратилась") >= 3, output);
        assertFalse(output.contains("Exception"));
    }

    // ---------- прогресс долгих команд ----------

    @Test
    void quickCommandShowsNoProgress() {
        String output = run(small(), "50\nq\n", fakeClock(0));
        assertEquals(List.of(), progressLines(output));
    }

    @Test
    void longCommandReportsProgressEverySecond() {
        // Часы: старт 0, после шагов 1, 2, 3… — 0,5 с, 1 с, 1,5 с…  Отчёт, как только прошла секунда.
        Simulation sim = small();
        String output = run(sim, "10\nq\n", fakeClock(500));

        assertEquals(List.of(
                "...выполнено 2 из 10",
                "...выполнено 4 из 10",
                "...выполнено 6 из 10",
                "...выполнено 8 из 10"), progressLines(output));
        assertEquals(10, sim.getStep());
        assertTrue(output.contains("Шаг 10 |"), "в конце, как обычно, поле");
    }

    @Test
    void lastStepIsNotReportedAsProgress() {
        String output = run(small(), "2\nq\n", fakeClock(1000));
        assertEquals(List.of("...выполнено 1 из 2"), progressLines(output), "про последний шаг скажет само поле");
    }

    @Test
    void everyCommandStartsItsOwnProgressTimer() {
        // Если бы отсчёт не начинался заново, вторая команда отчиталась бы уже после первого шага.
        String output = run(small(), "3\n3\nq\n", fakeClock(500));
        assertEquals(List.of("...выполнено 2 из 3", "...выполнено 2 из 3"), progressLines(output));
    }

    @Test
    void readErrorEndsProgramWithMessageInsteadOfCrash() {
        Reader broken = new Reader() {
            @Override
            public int read(char[] buffer, int offset, int length) throws IOException {
                throw new IOException("диск отвалился");
            }

            @Override
            public void close() {
            }
        };

        String output = assertDoesNotThrow(() -> run(small(), broken));

        assertTrue(output.contains("Не удалось прочитать ввод"), output);
        assertTrue(output.contains("До свидания"), output);
    }
}
