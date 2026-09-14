package life.model;

import life.model.TestAgents.Stone;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Общие умения животных, проверенные напрямую. */
class AnimalTest {

    private static Herbivore herbivoreAt(World world, int x, int y) {
        Herbivore h = new Herbivore(Settings.defaults());
        world.setAgent(x, y, h);
        return h;
    }

    @Test
    void visionRadiusIsTwoWhichMeansFiveByFive() {
        assertEquals(2, Animal.VISION_RADIUS);
    }

    @Test
    void lookReturnsEveryoneInFiveByFiveSquare() {
        World world = new World(7, 7);
        Herbivore h = herbivoreAt(world, 3, 3);
        Stone corner = new Stone();
        Stone edge = new Stone();
        world.setAgent(5, 5, corner);
        world.setAgent(1, 3, edge);
        world.setAgent(6, 3, new Stone()); // расстояние 3

        assertEquals(Set.of(corner, edge), new HashSet<>(h.look(world)));
    }

    @Test
    void nearestPicksClosestMatchingAgent() {
        World world = new World(5, 5);
        Herbivore h = herbivoreAt(world, 2, 2);
        Plant far = new Plant(Settings.defaults());
        Plant near = new Plant(Settings.defaults());
        world.setAgent(4, 4, far);
        world.setAgent(2, 3, near);
        world.setAgent(2, 1, new Stone()); // так же близко, но не подходит под условие

        Agent found = h.nearest(h.look(world), a -> a instanceof Plant, new Random(1));

        assertSame(near, found);
    }

    @Test
    void nearestReturnsNullWhenNothingMatches() {
        World world = new World(5, 5);
        Herbivore h = herbivoreAt(world, 2, 2);
        world.setAgent(2, 3, new Stone());

        assertNull(h.nearest(h.look(world), a -> a instanceof Plant, new Random(1)));
        assertNull(h.nearest(List.of(), a -> true, new Random(1)));
    }

    @Test
    void nearestBreaksTiesRandomly() {
        World world = new World(5, 5);
        Herbivore h = herbivoreAt(world, 2, 2);
        Plant left = new Plant(Settings.defaults());
        Plant right = new Plant(Settings.defaults());
        world.setAgent(1, 2, left);
        world.setAgent(3, 2, right);

        Random random = new Random(5);
        Set<Agent> picked = new HashSet<>();
        for (int i = 0; i < 30; i++) {
            picked.add(h.nearest(h.look(world), a -> a instanceof Plant, random));
        }
        assertEquals(Set.of(left, right), picked, "при равном расстоянии выбираются оба");
    }

    @Test
    void nearestDoesNotPreferFartherAgentListedFirst() {
        World world = new World(5, 5);
        Herbivore h = herbivoreAt(world, 2, 2);
        Plant far = new Plant(Settings.defaults());
        Plant near = new Plant(Settings.defaults());
        world.setAgent(0, 0, far);
        world.setAgent(3, 3, near);

        Random random = new Random(5);
        for (int i = 0; i < 20; i++) {
            assertSame(near, h.nearest(List.of(far, near), a -> true, random));
            assertSame(near, h.nearest(List.of(near, far), a -> true, random));
        }
    }
}
