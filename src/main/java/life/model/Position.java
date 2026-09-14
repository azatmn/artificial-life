package life.model;

/** Координаты клетки на поле: x — столбец (слева направо), y — строка (сверху вниз). */
public record Position(int x, int y) {

    /** Соседняя клетка, сдвинутая на (dx, dy). */
    public Position translate(int dx, int dy) {
        return new Position(x + dx, y + dy);
    }

    /** Квадрат расстояния до другой клетки. Корень не нужен: сравнивать «ближе/дальше» можно и так. */
    public long distanceSquaredTo(Position other) {
        long dx = (long) x - other.x;
        long dy = (long) y - other.y;
        return dx * dx + dy * dy;
    }
}
