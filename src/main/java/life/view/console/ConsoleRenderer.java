package life.view.console;

import life.model.Agent;
import life.model.ReadOnlyWorld;
import life.model.Statistics;

import java.util.ArrayList;
import java.util.List;

/** Рисует поле и счётчики текстом для консоли. Только ASCII-символы рамки — их покажет любая консоль. */
public final class ConsoleRenderer {

    /** Больше этого поле в консоль не выводится целиком — только левый верхний угол. */
    public static final int MAX_COLUMNS = 120;
    public static final int MAX_ROWS = 60;

    private ConsoleRenderer() {
    }

    public static String render(ReadOnlyWorld world, Statistics stats) {
        int columns = Math.min(world.getWidth(), MAX_COLUMNS);
        int rows = Math.min(world.getHeight(), MAX_ROWS);
        StringBuilder sb = new StringBuilder();

        sb.append("Шаг ").append(stats.step())
                .append(" | растения P: ").append(stats.plants())
                .append(" | травоядные T: ").append(stats.herbivores())
                .append(" | хищники X: ").append(stats.predators())
                .append('\n');

        String border = "+" + "-".repeat(columns) + "+\n";
        sb.append(border);
        for (int y = 0; y < rows; y++) {
            sb.append('|');
            for (int x = 0; x < columns; x++) {
                Agent agent = world.getAgent(x, y);
                sb.append(agent == null ? '.' : agent.symbol());
            }
            sb.append("|\n");
        }
        sb.append(border);

        if (columns < world.getWidth() || rows < world.getHeight()) {
            sb.append("(показан фрагмент ").append(columns).append('x').append(rows)
                    .append(" из ").append(world.getWidth()).append('x').append(world.getHeight()).append(")\n");
        }

        List<String> extinct = new ArrayList<>();
        if (stats.plants() == 0) {
            extinct.add("растения");
        }
        if (stats.herbivores() == 0) {
            extinct.add("травоядные");
        }
        if (stats.predators() == 0) {
            extinct.add("хищники");
        }
        if (!extinct.isEmpty()) {
            sb.append("Вымерли: ").append(String.join(", ", extinct)).append('\n');
        }
        return sb.toString();
    }
}
