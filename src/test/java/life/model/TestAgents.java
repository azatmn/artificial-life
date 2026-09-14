package life.model;

import java.util.Random;

/** Простейшие жители для тестов: ничего не делают за ход. */
final class TestAgents {

    private TestAgents() {
    }

    /** Камень: не размножается никогда. */
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

        @Override
        protected int reproductionThreshold() {
            return Integer.MAX_VALUE;
        }

        @Override
        protected Agent createChild(int energy) {
            return new Stone();
        }
    }

    /** Другой вид, не родственный камню, — чтобы проверять подсчёт по типам. */
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

        @Override
        protected int reproductionThreshold() {
            return Integer.MAX_VALUE;
        }

        @Override
        protected Agent createChild(int energy) {
            return new Tree();
        }
    }

    /** Капля: размножается делением при энергии выше заданного порога. */
    static class Blob extends Agent {
        private final int threshold;

        Blob(int energy, int threshold) {
            super(energy);
            this.threshold = threshold;
        }

        @Override
        public void act(World world, Random random) {
        }

        @Override
        public char symbol() {
            return 'B';
        }

        @Override
        protected int reproductionThreshold() {
            return threshold;
        }

        @Override
        protected Agent createChild(int energy) {
            return new Blob(energy, threshold);
        }
    }
}
