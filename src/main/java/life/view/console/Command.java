package life.view.console;

/** Команда, которую пользователь ввёл в консоли. */
public sealed interface Command {

    /** Сделать count шагов подряд. */
    record Step(int count) implements Command {
    }

    /** Показать справку. */
    record Help() implements Command {
    }

    /** Выйти из программы. */
    record Quit() implements Command {
    }

    /** Ввод не распознан; message — что сказать пользователю. */
    record Invalid(String message) implements Command {
    }
}
