package life;

import javafx.application.Application;
import life.view.gui.LifeApp;

/**
 * Точка входа оконной версии.
 * Отдельный класс, а не сам LifeApp: если главный класс наследует Application,
 * запуск без модулей JavaFX падает с ошибкой «JavaFX runtime components are missing».
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Application.launch(LifeApp.class, args);
    }
}
