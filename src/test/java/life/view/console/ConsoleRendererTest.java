package life.view.console;

import life.model.Herbivore;
import life.model.Plant;
import life.model.Predator;
import life.model.Settings;
import life.model.Statistics;
import life.model.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConsoleRendererTest {

    private static long countRows(String text) {
        return text.lines().filter(line -> line.startsWith("|")).count();
    }

    @Test
    void smallWorldIsDrawnExactly() {
        Settings settings = Settings.defaults();
        World world = new World(3, 2);
        world.setAgent(0, 0, new Plant(settings));
        world.setAgent(2, 0, new Herbivore(settings));
        world.setAgent(2, 1, new Predator(settings));

        String text = ConsoleRenderer.render(world, new Statistics(7, 1, 1, 1));

        assertEquals("""
                Шаг 7 | растения P: 1 | травоядные T: 1 | хищники X: 1
                +---+
                |P.T|
                |..X|
                +---+
                """, text);
    }

    @Test
    void extinctSpeciesAreListed() {
        String text = ConsoleRenderer.render(new World(2, 2), new Statistics(3, 5, 0, 0));
        assertTrue(text.contains("Вымерли: травоядные, хищники"), text);
    }

    @Test
    void nothingIsListedWhileEveryoneLives() {
        String text = ConsoleRenderer.render(new World(2, 2), new Statistics(3, 5, 1, 1));
        assertFalse(text.contains("Вымерли"), text);
    }

    @Test
    void fieldOfMaximumSizeIsShownWhole() {
        String text = ConsoleRenderer.render(new World(120, 60), new Statistics(0, 0, 0, 0));
        assertEquals(60, countRows(text));
        assertFalse(text.contains("фрагмент"));
    }

    @Test
    void tooWideFieldIsCropped() {
        String text = ConsoleRenderer.render(new World(121, 10), new Statistics(0, 0, 0, 0));
        assertEquals(10, countRows(text));
        assertTrue(text.lines().filter(l -> l.startsWith("|")).allMatch(l -> l.length() == 122), "120 клеток + 2 рамки");
        assertTrue(text.contains("показан фрагмент 120x10 из 121x10"), text);
    }

    @Test
    void tooTallFieldIsCropped() {
        String text = ConsoleRenderer.render(new World(200, 100), new Statistics(0, 0, 0, 0));
        assertEquals(60, countRows(text));
        assertTrue(text.contains("показан фрагмент 120x60 из 200x100"), text);
    }
}
