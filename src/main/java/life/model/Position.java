package life.model;

/** Координаты клетки на поле: x — столбец (слева направо), y — строка (сверху вниз). */
public record Position(int x, int y) {

    /** Соседняя клетка, сдвинутая на (dx, dy). */
    public Position translate(int dx, int dy) {
        return new Position(x + dx, y + dy);
    }
}
