package life.view.console;

import life.model.Simulation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.function.LongSupplier;

/** Консольная версия: показывает поле, читает команды и выполняет их, пока пользователь не выйдет. */
public class ConsoleApp {

    static final String HELP = """
            Команды:
              Enter     - один шаг
              число N   - N шагов подряд (от 1 до %d)
              h или ?   - эта справка
              q         - выход
            """.formatted(CommandParser.MAX_STEPS);

    /** Как часто печатать прогресс долгой команды: раз в секунду. */
    private static final long PROGRESS_INTERVAL_NANOS = 1_000_000_000L;

    private final Simulation simulation;
    private final BufferedReader in;
    private final PrintStream out;
    /** Текущее время в наносекундах. Снаружи, чтобы тесты могли подставить поддельные часы. */
    private final LongSupplier clock;

    public ConsoleApp(Simulation simulation, BufferedReader in, PrintStream out) {
        this(simulation, in, out, System::nanoTime);
    }

    ConsoleApp(Simulation simulation, BufferedReader in, PrintStream out, LongSupplier clock) {
        this.simulation = simulation;
        this.in = in;
        this.out = out;
        this.clock = clock;
    }

    public void run() {
        out.println("Искусственная жизнь - консольная версия");
        out.print(HELP);
        show();
        while (true) {
            out.print("> ");
            out.flush();
            String line = readLine();
            switch (CommandParser.parse(line)) {
                case Command.Quit quit -> {
                    if (line == null) {
                        out.println(); // после «> » без перевода строки
                    }
                    out.println("До свидания!");
                    return;
                }
                case Command.Help help -> out.print(HELP);
                case Command.Invalid invalid -> out.println("Ошибка: " + invalid.message());
                case Command.Step step -> makeSteps(step.count());
            }
        }
    }

    /** Следующая строка ввода или null, если ввод закончился или сломался. */
    private String readLine() {
        try {
            return in.readLine();
        } catch (IOException e) {
            out.println();
            out.println("Не удалось прочитать ввод: " + e.getMessage());
            return null;
        }
    }

    /** Делает до count шагов, но останавливается, как только на поле никого не осталось. */
    private void makeSteps(int count) {
        if (simulation.statistics().allDead()) {
            out.println("Жизнь прекратилась - шагать больше некуда. Выход: q");
            return;
        }
        // Долгая команда не должна выглядеть зависшей: раз в секунду сообщаем, сколько сделано.
        long lastReport = clock.getAsLong();
        for (int i = 1; i <= count && !simulation.statistics().allDead(); i++) {
            simulation.step();
            long now = clock.getAsLong();
            if (i < count && now - lastReport >= PROGRESS_INTERVAL_NANOS) {
                out.println("...выполнено " + i + " из " + count);
                lastReport = now;
            }
        }
        show();
        if (simulation.statistics().allDead()) {
            out.println("Жизнь прекратилась на шаге " + simulation.getStep() + ".");
        }
    }

    private void show() {
        out.print(ConsoleRenderer.render(simulation.getWorld(), simulation.statistics()));
    }
}
