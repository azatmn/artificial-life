package life.model;

/** Снимок численности на определённом шаге. */
public record Statistics(long step, int plants, int herbivores, int predators) {

    public int total() {
        return plants + herbivores + predators;
    }

    /** На поле никого не осталось — жизнь прекратилась. */
    public boolean allDead() {
        return total() == 0;
    }

    /** Хотя бы один вид вымер полностью. */
    public boolean anySpeciesExtinct() {
        return plants == 0 || herbivores == 0 || predators == 0;
    }
}
