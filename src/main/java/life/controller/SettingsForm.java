package life.controller;

import life.model.InvalidSettingsException;
import life.model.InvalidSettingsException.Problem;
import life.model.Settings;
import life.util.SafeText;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Форма настроек без JavaFX: тексты из полей ввода превращаются в {@link Settings}
 * или в список понятных проблем. Окно параметров только показывает поля и результат.
 */
public final class SettingsForm {

    /** Поле формы: ключ (имя параметра в Settings) и подпись для человека. */
    public record Field(String key, String label) {
    }

    /** Результат разбора формы. */
    public sealed interface Result {
        record Valid(Settings settings) implements Result {
        }

        record Invalid(List<Problem> problems) implements Result {
        }
    }

    /** Поля в том же порядке, что и параметры в {@link Settings}. */
    public static final List<Field> FIELDS = List.of(
            new Field("width", "Ширина поля"),
            new Field("height", "Высота поля"),
            new Field("initialPlants", "Растений в начале"),
            new Field("initialHerbivores", "Травоядных в начале"),
            new Field("initialPredators", "Хищников в начале"),
            new Field("plantStartEnergy", "Растение: начальная энергия"),
            new Field("plantGrowth", "Растение: прирост энергии за шаг"),
            new Field("plantMaxEnergy", "Растение: максимум энергии"),
            new Field("plantReproductionThreshold", "Растение: порог размножения"),
            new Field("herbivoreStartEnergy", "Травоядное: начальная энергия"),
            new Field("herbivoreStepCost", "Травоядное: расход энергии за шаг"),
            new Field("herbivoreFoodEnergy", "Травоядное: энергия от растения"),
            new Field("herbivoreReproductionThreshold", "Травоядное: порог размножения"),
            new Field("predatorStartEnergy", "Хищник: начальная энергия"),
            new Field("predatorStepCost", "Хищник: расход энергии за шаг"),
            new Field("predatorFoodEnergy", "Хищник: энергия от травоядного"),
            new Field("predatorReproductionThreshold", "Хищник: порог размножения"),
            new Field("seed", "Зерно случайности (seed)"));

    /** Только ASCII-цифры, необязательный знак: «５» или «٣» числом не считаются. */
    private static final Pattern INTEGER = Pattern.compile("[+-]?[0-9]+");
    private static final int ECHO_LIMIT = 20;

    private SettingsForm() {
    }

    /** Значения настроек как тексты для полей формы, в порядке {@link #FIELDS}. */
    public static Map<String, String> toTexts(Settings s) {
        long[] values = {s.width(), s.height(),
                s.initialPlants(), s.initialHerbivores(), s.initialPredators(),
                s.plantStartEnergy(), s.plantGrowth(), s.plantMaxEnergy(), s.plantReproductionThreshold(),
                s.herbivoreStartEnergy(), s.herbivoreStepCost(), s.herbivoreFoodEnergy(), s.herbivoreReproductionThreshold(),
                s.predatorStartEnergy(), s.predatorStepCost(), s.predatorFoodEnergy(), s.predatorReproductionThreshold(),
                s.seed()};
        Map<String, String> texts = new LinkedHashMap<>();
        for (int i = 0; i < FIELDS.size(); i++) {
            texts.put(FIELDS.get(i).key(), Long.toString(values[i]));
        }
        return texts;
    }

    /**
     * Сначала каждое поле читается как целое число; если хоть одно не прочиталось — возвращаются эти ошибки.
     * Если все прочитались — проверяются правила самих настроек (размеры, пороги, влезут ли жители).
     */
    public static Result parse(Map<String, String> texts) {
        List<Problem> problems = new ArrayList<>();
        long[] values = new long[FIELDS.size()];
        for (int i = 0; i < FIELDS.size(); i++) {
            Field field = FIELDS.get(i);
            String raw = texts.get(field.key());
            String text = raw == null ? "" : raw.strip();
            String error = null;
            if (text.isEmpty()) {
                error = "не заполнено";
            } else if (!INTEGER.matcher(text).matches()) {
                error = "нужно целое число, а указано \"" + SafeText.preview(text, ECHO_LIMIT) + "\"";
            } else {
                try {
                    values[i] = field.key().equals("seed") ? Long.parseLong(text) : Integer.parseInt(text);
                } catch (NumberFormatException e) {
                    error = "слишком большое число \"" + SafeText.preview(text, ECHO_LIMIT) + "\"";
                }
            }
            if (error != null) {
                problems.add(new Problem(field.key(), field.label() + ": " + error));
            }
        }
        if (!problems.isEmpty()) {
            return new Result.Invalid(List.copyOf(problems));
        }
        try {
            return new Result.Valid(Settings.builder()
                    .width((int) values[0]).height((int) values[1])
                    .initialPlants((int) values[2]).initialHerbivores((int) values[3]).initialPredators((int) values[4])
                    .plantStartEnergy((int) values[5]).plantGrowth((int) values[6])
                    .plantMaxEnergy((int) values[7]).plantReproductionThreshold((int) values[8])
                    .herbivoreStartEnergy((int) values[9]).herbivoreStepCost((int) values[10])
                    .herbivoreFoodEnergy((int) values[11]).herbivoreReproductionThreshold((int) values[12])
                    .predatorStartEnergy((int) values[13]).predatorStepCost((int) values[14])
                    .predatorFoodEnergy((int) values[15]).predatorReproductionThreshold((int) values[16])
                    .seed(values[17])
                    .build());
        } catch (InvalidSettingsException e) {
            return new Result.Invalid(e.problems());
        }
    }
}
