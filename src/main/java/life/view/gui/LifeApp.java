package life.view.gui;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import life.controller.SimulationController;
import life.controller.SpeedScale;
import life.model.Settings;
import life.model.Statistics;

/**
 * Главное окно: поле в центре, управление справа, график снизу.
 * Игровой цикл — таймер JavaFX (AnimationTimer): на каждом кадре контроллер решает,
 * сколько шагов сделать, а окно перерисовывает то, что изменилось.
 */
public class LifeApp extends Application {

    /** График перерисовываем не чаще 4 раз в секунду — он дороже поля. */
    private static final long CHART_INTERVAL_NANOS = 250_000_000L;

    private SimulationController controller;
    private AnimationTimer timer;

    private final WorldCanvas canvas = new WorldCanvas();
    private final PopulationChart chart = new PopulationChart();
    private final Button startPause = new Button();
    private final Button step = new Button("Шаг");
    private final Button reset = new Button("Сброс");
    private final Button parameters = new Button("Параметры…");
    private final Slider speed = new Slider(SpeedScale.SLIDER_MIN, SpeedScale.SLIDER_MAX, 0);
    private final Label speedLabel = new Label();
    private final Label stepLabel = new Label();
    private final Label plantsLabel = new Label();
    private final Label herbivoresLabel = new Label();
    private final Label predatorsLabel = new Label();
    private final Label statusLabel = new Label();

    private long lastDrawnStep = -1;
    private long lastChartUpdate;

    static String stylesheet() {
        return LifeApp.class.getResource("app.css").toExternalForm();
    }

    @Override
    public void start(Stage stage) {
        controller = new SimulationController(Settings.defaults());

        BorderPane root = new BorderPane();
        root.setCenter(canvas);
        root.setRight(buildControls());
        chart.setPrefHeight(230);
        root.setBottom(chart);
        BorderPane.setMargin(canvas, new Insets(8));

        Scene scene = new Scene(root, 1200, 820);
        scene.getStylesheets().add(stylesheet());
        stage.setTitle("Искусственная жизнь");
        stage.setMinWidth(820);
        stage.setMinHeight(620);
        stage.setScene(scene);

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                controller.tick(now);
                refresh(false, now);
            }
        };
        refresh(true, System.nanoTime());
        stage.show();
        timer.start();
    }

    @Override
    public void stop() {
        if (timer != null) {
            timer.stop();
        }
    }

    private VBox buildControls() {
        Label title = new Label("Управление");
        title.getStyleClass().add("title");

        startPause.setId("startPause");
        step.setId("step");
        reset.setId("reset");
        parameters.setId("parameters");
        speed.setId("speed");
        for (Button button : new Button[]{startPause, step, reset, parameters}) {
            button.setMaxWidth(Double.MAX_VALUE);
        }

        startPause.setOnAction(e -> command(controller::toggleRunning));
        step.setOnAction(e -> command(controller::stepOnce));
        reset.setOnAction(e -> command(controller::reset));
        parameters.setOnAction(e -> openParameters());

        speed.setValue(SpeedScale.toSlider(controller.getSpeed()));
        speed.valueProperty().addListener((observable, oldValue, newValue) -> {
            controller.setSpeed(SpeedScale.toSpeed(newValue.doubleValue()));
            updateSpeedLabel();
        });
        updateSpeedLabel();

        statusLabel.getStyleClass().add("status");
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(220);

        VBox counters = new VBox(6,
                stepLabel,
                counter(WorldCanvas.PLANT, plantsLabel),
                counter(WorldCanvas.HERBIVORE, herbivoresLabel),
                counter(WorldCanvas.PREDATOR, predatorsLabel));

        VBox box = new VBox(10,
                title, startPause, step, reset,
                new Separator(), speedLabel, speed,
                new Separator(), counters, statusLabel,
                new Separator(), parameters);
        box.getStyleClass().add("panel");
        box.setPrefWidth(240);
        return box;
    }

    private static HBox counter(Color color, Label label) {
        Rectangle square = new Rectangle(14, 14, color);
        square.setStroke(Color.web("#455a64"));
        HBox row = new HBox(8, square, label);
        row.setAlignment(Pos.CENTER_LEFT);
        label.getStyleClass().add("counter");
        return row;
    }

    /** Нажатие кнопки: выполнить действие и сразу показать результат. */
    private void command(Runnable action) {
        action.run();
        refresh(true, System.nanoTime());
    }

    private void openParameters() {
        controller.pause();
        new SettingsDialog(controller.getSettings()).showAndWait().ifPresent(controller::applySettings);
        refresh(true, System.nanoTime());
    }

    private void updateSpeedLabel() {
        speedLabel.setText("Скорость: " + controller.getSpeed() + " шаг./с");
    }

    private void refresh(boolean force, long now) {
        Statistics stats = controller.getStatistics();
        boolean changed = stats.step() != lastDrawnStep;
        if (force || changed) {
            canvas.draw(controller.getWorld());
            stepLabel.setText("Шаг: " + stats.step());
            plantsLabel.setText("Растения: " + stats.plants());
            herbivoresLabel.setText("Травоядные: " + stats.herbivores());
            predatorsLabel.setText("Хищники: " + stats.predators());
            statusLabel.setText(controller.getStatusMessage());
            lastDrawnStep = stats.step();
        }
        if (force || (changed && now - lastChartUpdate >= CHART_INTERVAL_NANOS)) {
            chart.update(controller.getHistory());
            lastChartUpdate = now;
        }
        boolean dead = stats.allDead();
        startPause.setText(controller.isRunning() ? "Пауза" : "Старт");
        startPause.setDisable(dead);
        step.setDisable(dead);
    }
}
