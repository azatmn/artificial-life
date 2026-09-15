package life.view.gui;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import life.controller.SettingsForm;
import life.model.InvalidSettingsException.Problem;
import life.model.Settings;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Окно параметров. Проверка введённого — в {@link SettingsForm};
 * здесь только поля, подсветка ошибок и кнопки. С ошибками окно не закрывается.
 */
public class SettingsDialog extends Dialog<Settings> {

    private static final ButtonType APPLY = new ButtonType("Применить", ButtonBar.ButtonData.OK_DONE);
    private static final ButtonType CANCEL = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
    /** Проблема «жители не влезли» относится сразу к трём полям. */
    private static final Set<String> COUNT_FIELDS = Set.of("initialPlants", "initialHerbivores", "initialPredators");

    private final Map<String, TextField> inputs = new LinkedHashMap<>();
    private final Label errors = new Label();
    private Settings accepted;

    public SettingsDialog(Settings current) {
        setTitle("Параметры симуляции");
        setHeaderText("После применения поле создаётся заново.");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(6);
        grid.setPadding(new Insets(10));
        // Подписи никогда не обрезаются многоточием: столбец не уже самой длинной подписи
        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setMinWidth(Region.USE_PREF_SIZE);
        grid.getColumnConstraints().addAll(labelColumn, new ColumnConstraints(), new ColumnConstraints());
        Map<String, String> texts = SettingsForm.toTexts(current);
        int row = 0;
        for (SettingsForm.Field field : SettingsForm.FIELDS) {
            TextField input = new TextField(texts.get(field.key()));
            input.setId("field-" + field.key());
            input.setPrefColumnCount(12);
            inputs.put(field.key(), input);
            grid.add(new Label(field.label()), 0, row);
            grid.add(input, 1, row);
            row++;
        }
        Button randomSeed = new Button("Случайный seed");
        randomSeed.setOnAction(e -> inputs.get("seed").setText(Long.toString(new Random().nextLong())));
        grid.add(randomSeed, 2, row - 1);

        Button defaults = new Button("По умолчанию");
        defaults.setOnAction(e -> SettingsForm.toTexts(Settings.defaults())
                .forEach((key, text) -> inputs.get(key).setText(text)));

        errors.getStyleClass().add("error-text");
        errors.setWrapText(true);
        errors.setMaxWidth(560);
        // Блок ошибок растёт по высоте под все строки, а не обрезается
        errors.setMinHeight(Region.USE_PREF_SIZE);

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(610);
        VBox content = new VBox(10, scroll, new HBox(defaults), errors);
        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().addAll(APPLY, CANCEL);
        getDialogPane().getStylesheets().add(LifeApp.stylesheet());

        Button apply = (Button) getDialogPane().lookupButton(APPLY);
        apply.addEventFilter(ActionEvent.ACTION, event -> {
            if (!tryAccept()) {
                event.consume(); // есть ошибки — окно остаётся открытым
            }
        });
        setResultConverter(button -> button == APPLY ? accepted : null);
    }

    private boolean tryAccept() {
        Map<String, String> texts = new HashMap<>();
        inputs.forEach((key, input) -> texts.put(key, input.getText()));
        inputs.values().forEach(input -> input.getStyleClass().remove("invalid"));

        switch (SettingsForm.parse(texts)) {
            case SettingsForm.Result.Valid valid -> {
                accepted = valid.settings();
                errors.setText("");
                return true;
            }
            case SettingsForm.Result.Invalid invalid -> {
                showProblems(invalid.problems());
                return false;
            }
        }
    }

    private void showProblems(List<Problem> problems) {
        StringBuilder text = new StringBuilder();
        for (Problem problem : problems) {
            text.append("• ").append(problem.message()).append('\n');
            Set<String> fields = problem.field().equals("initialCount") ? COUNT_FIELDS : Set.of(problem.field());
            for (String key : fields) {
                TextField input = inputs.get(key);
                if (input != null && !input.getStyleClass().contains("invalid")) {
                    input.getStyleClass().add("invalid");
                }
            }
        }
        errors.setText(text.toString().strip());
    }
}
