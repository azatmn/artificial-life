package life.view.gui;

import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import life.model.Statistics;

import java.util.ArrayList;
import java.util.List;

/** График численности трёх видов по шагам. */
public class PopulationChart extends LineChart<Number, Number> {

    /** Больше точек на линию не рисуем: график остаётся быстрым при любой длине истории. */
    private static final int MAX_POINTS = 300;

    private final XYChart.Series<Number, Number> plants = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> herbivores = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> predators = new XYChart.Series<>();

    public PopulationChart() {
        super(new NumberAxis(), new NumberAxis());
        setAnimated(false);
        setCreateSymbols(false);
        getXAxis().setLabel("Шаг");
        getYAxis().setLabel("Численность");
        ((NumberAxis) getXAxis()).setForceZeroInRange(false);

        plants.setName("Растения");
        herbivores.setName("Травоядные");
        predators.setName("Хищники");
        getData().addAll(List.of(plants, herbivores, predators));
    }

    public void update(List<Statistics> history) {
        List<XYChart.Data<Number, Number>> p = new ArrayList<>();
        List<XYChart.Data<Number, Number>> h = new ArrayList<>();
        List<XYChart.Data<Number, Number>> x = new ArrayList<>();
        int stride = Math.max(1, (int) Math.ceil(history.size() / (double) MAX_POINTS));
        for (int i = 0; i < history.size(); i++) {
            // берём каждую stride-ю точку, но последнюю — всегда
            if (i % stride != 0 && i != history.size() - 1) {
                continue;
            }
            Statistics s = history.get(i);
            p.add(new XYChart.Data<>(s.step(), s.plants()));
            h.add(new XYChart.Data<>(s.step(), s.herbivores()));
            x.add(new XYChart.Data<>(s.step(), s.predators()));
        }
        plants.getData().setAll(p);
        herbivores.getData().setAll(h);
        predators.getData().setAll(x);
    }
}
