package life.model;

import java.util.List;
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

    /** Записывает каждый свой ход в общий журнал. Не размножается. */
    static class Recorder extends Agent {
        final List<Agent> log;
        int acts;

        Recorder(List<Agent> log, int energy) {
            super(energy);
            this.log = log;
        }

        @Override
        public void act(World world, Random random) {
            acts++;
            log.add(this);
        }

        @Override
        public char symbol() {
            return 'R';
        }

        @Override
        protected int reproductionThreshold() {
            return Integer.MAX_VALUE;
        }

        @Override
        protected Agent createChild(int energy) {
            return new Recorder(log, energy);
        }
    }

    /** Каждый свой ход делится, если есть место. */
    static class Breeder extends Recorder {
        Breeder(List<Agent> log, int energy) {
            super(log, energy);
        }

        @Override
        public void act(World world, Random random) {
            super.act(world, random);
            tryReproduce(world, random);
        }

        @Override
        protected int reproductionThreshold() {
            return 1;
        }

        @Override
        protected Agent createChild(int energy) {
            return new Breeder(log, energy);
        }
    }

    /** За свой ход уничтожает всех, кто стоит вплотную. */
    static class Eater extends Recorder {
        Eater(List<Agent> log) {
            super(log, 1);
        }

        @Override
        public void act(World world, Random random) {
            super.act(world, random);
            Position p = getPosition();
            for (Agent neighbor : world.getNeighbors(p.x(), p.y(), 1)) {
                neighbor.die(world);
            }
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
