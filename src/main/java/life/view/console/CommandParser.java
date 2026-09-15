package life.view.console;

import life.util.SafeText;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Разбирает строку, введённую пользователем, в команду. Никогда не бросает исключений:
 * на любой мусор отвечает {@link Command.Invalid} с понятным объяснением.
 * В сообщениях только ASCII-знаки препинания — их покажет любая консоль, в том числе Windows.
 */
public final class CommandParser {

    public static final int MAX_STEPS = 100_000;

    /** Сколько символов введённого текста повторять в сообщении об ошибке. */
    private static final int ECHO_LIMIT = 20;

    private static final Set<String> QUIT_WORDS = Set.of("q", "quit", "exit", "выход", "й");
    private static final Set<String> HELP_WORDS = Set.of("h", "help", "?", "помощь", "р");

    /** Только ASCII-цифры: «５» или «٣» — не число для нас. */
    private static final Pattern DIGITS = Pattern.compile("[0-9]+");
    /** Похоже на число, но не целое положительное: со знаком или с дробной частью. */
    private static final Pattern NUMBER_LIKE = Pattern.compile("(?=.*[0-9])[+-]?[0-9]*([.,][0-9]*)?");

    private CommandParser() {
    }

    public static Command parse(String line) {
        if (line == null) {
            return new Command.Quit(); // конец ввода (Ctrl+D или закончился файл)
        }
        // BOM появляется в начале файла, сохранённого в Блокноте Windows
        String text = line.replace("\uFEFF", "").strip();
        String word = text.toLowerCase(Locale.ROOT);

        if (word.isEmpty()) {
            return new Command.Step(1);
        }
        if (QUIT_WORDS.contains(word)) {
            return new Command.Quit();
        }
        if (HELP_WORDS.contains(word)) {
            return new Command.Help();
        }
        if (DIGITS.matcher(word).matches()) {
            return steps(word);
        }
        if (NUMBER_LIKE.matcher(word).matches()) {
            return new Command.Invalid("число шагов должно быть целое, от 1 до " + MAX_STEPS
                    + ", а введено \"" + echo(text) + "\"");
        }
        return new Command.Invalid("неизвестная команда \"" + echo(text)
                + "\". Enter - шаг, число - несколько шагов, h - справка, q - выход");
    }

    private static Command steps(String digits) {
        String significant = digits.replaceFirst("^0+(?=.)", "");
        // Больше шести цифр — заведомо больше максимума; так огромное число не переполнит int
        if (significant.length() > 6) {
            return outOfRange(digits);
        }
        int count = Integer.parseInt(significant);
        if (count == 0 || count > MAX_STEPS) {
            return outOfRange(digits);
        }
        return new Command.Step(count);
    }

    private static Command outOfRange(String digits) {
        return new Command.Invalid("число шагов должно быть от 1 до " + MAX_STEPS + ", а введено \"" + echo(digits) + "\"");
    }

    private static String echo(String text) {
        return SafeText.preview(text, ECHO_LIMIT);
    }
}
