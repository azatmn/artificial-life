package life.view.console;

import life.model.Settings;
import life.model.Simulation;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/** Точка входа консольной версии. */
public final class ConsoleMain {

    private ConsoleMain() {
    }

    public static void main(String[] args) {
        // Кодировка клавиатурного ввода консоли (на Windows это обычно не UTF-8).
        // System.out уже настроен Java на кодировку консоли сам.
        Charset inputCharset = Charset.forName(
                System.getProperty("stdin.encoding", Charset.defaultCharset().name()), Charset.defaultCharset());
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, inputCharset));
        new ConsoleApp(new Simulation(Settings.defaults()), in, System.out).run();
    }
}
