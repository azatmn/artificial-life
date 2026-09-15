package life.view.gui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import life.model.Agent;
import life.model.Herbivore;
import life.model.Plant;
import life.model.Predator;
import life.model.ReadOnlyWorld;

/** Поле цветными клетками. Растягивается под размер окна, клетки остаются квадратными. */
public class WorldCanvas extends Pane {

    static final Color BACKGROUND = Color.web("#eceff1");
    static final Color EMPTY = Color.web("#263238");
    static final Color PLANT = Color.web("#43a047");
    static final Color HERBIVORE = Color.web("#fdd835");
    static final Color PREDATOR = Color.web("#e53935");

    private final Canvas canvas = new Canvas();
    private ReadOnlyWorld world;

    public WorldCanvas() {
        getChildren().add(canvas);
    }

    /** Нарисовать поле (запоминается — при изменении размера окна перерисуется само). */
    public void draw(ReadOnlyWorld world) {
        this.world = world;
        redraw();
    }

    static Color colorOf(Agent agent) {
        return switch (agent) {
            case Plant plant -> PLANT;
            case Herbivore herbivore -> HERBIVORE;
            case Predator predator -> PREDATOR;
            default -> Color.GRAY;
        };
    }

    @Override
    protected void layoutChildren() {
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
        redraw();
    }

    // Размер холста не должен влиять на желаемый размер панели — иначе окно нельзя уменьшить.
    @Override
    protected double computePrefWidth(double height) {
        return 800;
    }

    @Override
    protected double computePrefHeight(double width) {
        return 480;
    }

    @Override
    protected double computeMinWidth(double height) {
        return 100;
    }

    @Override
    protected double computeMinHeight(double width) {
        return 60;
    }

    private void redraw() {
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(BACKGROUND);
        g.fillRect(0, 0, width, height);
        if (world == null || width <= 0 || height <= 0) {
            return;
        }

        double cell = Math.min(width / world.getWidth(), height / world.getHeight());
        double left = (width - cell * world.getWidth()) / 2;
        double top = (height - cell * world.getHeight()) / 2;
        // Тонкий зазор между клетками, если они достаточно крупные
        double size = cell >= 5 ? cell - 1 : cell;

        g.setFill(EMPTY);
        g.fillRect(left, top, cell * world.getWidth(), cell * world.getHeight());
        for (int y = 0; y < world.getHeight(); y++) {
            for (int x = 0; x < world.getWidth(); x++) {
                Agent agent = world.getAgent(x, y);
                if (agent != null) {
                    g.setFill(colorOf(agent));
                    g.fillRect(left + x * cell, top + y * cell, size, size);
                }
            }
        }
    }
}
