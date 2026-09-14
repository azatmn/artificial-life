package life.model;

import java.util.Random;

/** Простейшие жители для тестов поля: ничего не делают за ход. */
final class TestAgents {

    private TestAgents() {
    }

    static class Stone extends Agent {
        Stone() {
            super(1);
        }

        @Override
        public void act(World world, Random random) {
        }

        @Override
        public char symbol() {
            return 'S';
        }
    }

    static class Tree extends Agent {
        Tree() {
            super(1);
        }

        @Override
        public void act(World world, Random random) {
        }

        @Override
        public char symbol() {
            return 'W';
        }
    }
}
