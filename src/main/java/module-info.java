/**
 * Модуль программы «Искусственная жизнь»: какие пакеты есть и что им нужно.
 * JavaFX требует, чтобы приложение было модулем, иначе при запуске выводится предупреждение.
 */
module life {
    requires javafx.controls;

    exports life;
    exports life.model;
    exports life.controller;
    exports life.util;
    exports life.view.console;
    // JavaFX сам создаёт объект окна LifeApp и читает стили из этого пакета
    exports life.view.gui to javafx.graphics;
    opens life.view.gui to javafx.graphics;
}
