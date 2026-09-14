package life.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Поле — двумерная сетка клеток. В клетке либо пусто, либо ровно один житель.
 * Только поле двигает жителей, поэтому координаты в поле и в самом жителе всегда совпадают.
 */
public class World implements ReadOnlyWorld {

    private final int width;
    private final int height;
    /** cells[y][x]: сначала строка, потом столбец. */
    private final Agent[][] cells;

    public World(int width, int height) {
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("Размер поля должен быть положительным, указано " + width + "×" + height);
        }
        this.width = width;
        this.height = height;
        this.cells = new Agent[height][width];
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isInside(Position p) {
        return p.x() >= 0 && p.x() < width && p.y() >= 0 && p.y() < height;
    }

    public Agent getAgent(int x, int y) {
        return getAgent(new Position(x, y));
    }

    /** Житель в клетке или null, если клетка пустая. */
    public Agent getAgent(Position p) {
        requireInside(p);
        return cells[p.y()][p.x()];
    }

    /** Клетка на поле и в ней никого нет. Клетка за краем поля пустой не считается. */
    public boolean isEmpty(Position p) {
        return isInside(p) && cells[p.y()][p.x()] == null;
    }

    public void setAgent(int x, int y, Agent agent) {
        setAgent(new Position(x, y), agent);
    }

    /**
     * Размещает жителя в пустой клетке. Если agent == null — очищает клетку.
     *
     * @throws IllegalArgumentException клетка за краем поля
     * @throws IllegalStateException    клетка занята или житель уже стоит на каком-то поле
     */
    public void setAgent(Position p, Agent agent) {
        requireInside(p);
        Agent current = cells[p.y()][p.x()];
        if (agent == null) {
            if (current != null) {
                current.setPosition(null);
                cells[p.y()][p.x()] = null;
            }
            return;
        }
        if (agent.getPosition() != null) {
            throw new IllegalStateException("Житель уже стоит на поле в клетке " + agent.getPosition());
        }
        if (current != null) {
            throw new IllegalStateException("Клетка " + p + " уже занята");
        }
        cells[p.y()][p.x()] = agent;
        agent.setPosition(p);
    }

    public void removeAgent(Agent agent) {
        requireOnThisWorld(agent);
        setAgent(agent.getPosition(), null);
    }

    /** Переносит жителя в пустую клетку. */
    public void moveAgent(Agent agent, Position to) {
        requireOnThisWorld(agent);
        requireInside(to);
        if (cells[to.y()][to.x()] != null) {
            throw new IllegalStateException("Клетка " + to + " уже занята");
        }
        Position from = agent.getPosition();
        cells[from.y()][from.x()] = null;
        cells[to.y()][to.x()] = agent;
        agent.setPosition(to);
    }

    /**
     * Все жители в квадрате со стороной 2·radius+1 вокруг клетки (x, y), кроме того, кто в самой клетке.
     * Радиус 2 — это зрение 5×5. Часть квадрата за краем поля просто не рассматривается.
     */
    public List<Agent> getNeighbors(int x, int y, int radius) {
        if (radius < 0) {
            throw new IllegalArgumentException("Радиус не может быть отрицательным: " + radius);
        }
        requireInside(new Position(x, y));
        // long, чтобы x + огромный радиус не переполнился
        int top = (int) Math.max(0, (long) y - radius);
        int bottom = (int) Math.min(height - 1, (long) y + radius);
        int left = (int) Math.max(0, (long) x - radius);
        int right = (int) Math.min(width - 1, (long) x + radius);

        List<Agent> result = new ArrayList<>();
        for (int row = top; row <= bottom; row++) {
            for (int col = left; col <= right; col++) {
                Agent agent = cells[row][col];
                if (agent != null && (row != y || col != x)) {
                    result.add(agent);
                }
            }
        }
        return List.copyOf(result);
    }

    /** Свободные клетки среди восьми соседних (по сторонам и по диагонали). */
    public List<Position> getEmptyCellsAround(Position center) {
        requireInside(center);
        List<Position> result = new ArrayList<>();
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                Position p = center.translate(dx, dy);
                if ((dx != 0 || dy != 0) && isEmpty(p)) {
                    result.add(p);
                }
            }
        }
        return List.copyOf(result);
    }

    /** Все жители поля, построчно сверху вниз. Список — копия: менять поле через него нельзя. */
    public List<Agent> getAgents() {
        List<Agent> result = new ArrayList<>();
        for (Agent[] row : cells) {
            for (Agent agent : row) {
                if (agent != null) {
                    result.add(agent);
                }
            }
        }
        return List.copyOf(result);
    }

    /** Сколько на поле жителей данного типа (включая наследников). */
    public int count(Class<? extends Agent> type) {
        int count = 0;
        for (Agent[] row : cells) {
            for (Agent agent : row) {
                if (type.isInstance(agent)) {
                    count++;
                }
            }
        }
        return count;
    }

    private void requireInside(Position p) {
        if (!isInside(p)) {
            throw new IllegalArgumentException("Клетка " + p + " за краем поля " + width + "×" + height);
        }
    }

    private void requireOnThisWorld(Agent agent) {
        Position p = agent.getPosition();
        if (p == null || !isInside(p) || cells[p.y()][p.x()] != agent) {
            throw new IllegalStateException("Этого жителя нет на поле");
        }
    }
}
