package life.controller;

import life.model.InvalidSettingsException;
import life.model.Settings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SettingsFormTest {

    /** Тексты формы с умолчаниями, где одно поле заменено. */
    private static Map<String, String> defaultsWith(String key, String text) {
        Map<String, String> texts = new HashMap<>(SettingsForm.toTexts(Settings.defaults()));
        texts.put(key, text);
        return texts;
    }

    private static List<InvalidSettingsException.Problem> problems(Map<String, String> texts) {
        SettingsForm.Result result = SettingsForm.parse(texts);
        return assertInstanceOf(SettingsForm.Result.Invalid.class, result, "форма должна быть отвергнута").problems();
    }

    private static Settings valid(Map<String, String> texts) {
        SettingsForm.Result result = SettingsForm.parse(texts);
        return assertInstanceOf(SettingsForm.Result.Valid.class, result, () -> "форма должна приниматься: " + result).settings();
    }

    private static String onlyProblem(Map<String, String> texts, String expectedField) {
        List<InvalidSettingsException.Problem> list = problems(texts);
        assertEquals(1, list.size(), list.toString());
        assertEquals(expectedField, list.getFirst().field());
        return list.getFirst().message();
    }

    // ---------- состав формы ----------

    @Test
    void formHasFieldForEverySettingInSameOrder() {
        List<String> components = Arrays.stream(Settings.class.getRecordComponents()).map(RecordComponent::getName).toList();
        List<String> keys = SettingsForm.FIELDS.stream().map(SettingsForm.Field::key).toList();
        assertEquals(components, keys, "добавили параметр в Settings — добавьте поле в форму");
    }

    @Test
    void everyFieldHasHumanLabel() {
        for (SettingsForm.Field field : SettingsForm.FIELDS) {
            assertFalse(field.label().isBlank(), field.key());
            assertNotEquals(field.key(), field.label(), "подпись должна быть по-русски, а не имя переменной");
        }
    }

    @Test
    void textsFollowFieldOrderAndShowValues() {
        Map<String, String> texts = SettingsForm.toTexts(Settings.builder().width(90).seed(-7).build());
        assertEquals(SettingsForm.FIELDS.stream().map(SettingsForm.Field::key).toList(), List.copyOf(texts.keySet()));
        assertEquals("90", texts.get("width"));
        assertEquals("-7", texts.get("seed"));
    }

    // ---------- корректный ввод ----------

    @Test
    void defaultsSurviveRoundTrip() {
        assertEquals(Settings.defaults(), valid(SettingsForm.toTexts(Settings.defaults())));
    }

    @Test
    void everyFieldSurvivesRoundTrip() {
        Settings original = Settings.builder()
                .width(11).height(12)
                .initialPlants(13).initialHerbivores(14).initialPredators(15)
                .plantStartEnergy(16).plantGrowth(17).plantMaxEnergy(40).plantReproductionThreshold(19)
                .herbivoreStartEnergy(20).herbivoreStepCost(21).herbivoreFoodEnergy(22).herbivoreReproductionThreshold(23)
                .predatorStartEnergy(24).predatorStepCost(25).predatorFoodEnergy(26).predatorReproductionThreshold(27)
                .seed(-28L)
                .build();
        assertEquals(original, valid(SettingsForm.toTexts(original)));
    }

    @Test
    void spacesAroundNumbersAreAllowed() {
        assertEquals(42, valid(defaultsWith("width", "  42 ")).width());
        assertEquals(57, valid(defaultsWith("height", "\t57\n")).height());
    }

    @Test
    void plusSignIsAllowed() {
        assertEquals(5, valid(defaultsWith("plantGrowth", "+5")).plantGrowth());
    }

    @Test
    void seedAcceptsWholeLongRange() {
        assertEquals(Long.MAX_VALUE, valid(defaultsWith("seed", "9223372036854775807")).seed());
        assertEquals(Long.MIN_VALUE, valid(defaultsWith("seed", "-9223372036854775808")).seed());
    }

    @Test
    void unknownKeysAreIgnored() {
        assertEquals(Settings.defaults(), valid(defaultsWith("что-то лишнее", "abc")));
    }

    // ---------- пустые поля ----------

    @ParameterizedTest(name = "«{0}» — поле не заполнено")
    @ValueSource(strings = {"", "   ", "\t"})
    void blankFieldIsReported(String text) {
        assertTrue(onlyProblem(defaultsWith("height", text), "height").contains("не заполнено"));
    }

    @Test
    void missingFieldIsReported() {
        Map<String, String> texts = new HashMap<>(SettingsForm.toTexts(Settings.defaults()));
        texts.remove("predatorFoodEnergy");
        assertTrue(onlyProblem(texts, "predatorFoodEnergy").contains("не заполнено"));
    }

    // ---------- не числа ----------

    @ParameterizedTest(name = "«{0}» — не целое число")
    @ValueSource(strings = {"abc", "2.5", "2,5", "1e3", "５", "٣", "12 34", "--5", "0x10", "+", "-", "пять"})
    void notAnIntegerIsReported(String text) {
        assertTrue(onlyProblem(defaultsWith("width", text), "width").contains("целое число"));
    }

    @Test
    void messageUsesRussianLabelAndQuotesInput() {
        String message = onlyProblem(defaultsWith("width", "abc"), "width");
        assertTrue(message.startsWith("Ширина поля"), message);
        assertTrue(message.contains("abc"), message);
    }

    @Test
    void controlCharactersAreNotEchoed() {
        String message = onlyProblem(defaultsWith("width", "\u001b[2J"), "width");
        assertTrue(message.chars().noneMatch(Character::isISOControl), message);
    }

    @Test
    void longInputIsNotEchoedInFull() {
        String message = onlyProblem(defaultsWith("width", "x".repeat(5000)), "width");
        assertTrue(message.length() < 200, "длина сообщения " + message.length());
    }

    // ---------- огромные числа ----------

    @ParameterizedTest(name = "«{0}» не влезает в int")
    @ValueSource(strings = {"2147483648", "-2147483649", "99999999999999999999", "000000000002147483648"})
    void numberTooBigForIntIsReported(String text) {
        assertTrue(onlyProblem(defaultsWith("width", text), "width").contains("слишком большое"));
    }

    @Test
    void intLimitsParseAndAreThenCheckedBySettings() {
        String max = onlyProblem(defaultsWith("width", "2147483647"), "width");
        assertTrue(max.contains("допустимо"), "число прочитано, а отвергла его проверка настроек: " + max);
        String min = onlyProblem(defaultsWith("width", "-2147483648"), "width");
        assertTrue(min.contains("допустимо"), min);
    }

    @Test
    void leadingZerosDoNotMakeSmallNumberTooBig() {
        // Поле по умолчанию высотой 60: ширина 50 вмещает 2300 жителей по умолчанию
        assertEquals(50, valid(defaultsWith("width", "0000000000000050")).width());
    }

    @Test
    void seedTooBigForLongIsReported() {
        assertTrue(onlyProblem(defaultsWith("seed", "9223372036854775808"), "seed").contains("слишком большое"));
        assertTrue(onlyProblem(defaultsWith("seed", "-9223372036854775809"), "seed").contains("слишком большое"));
    }

    // ---------- несколько ошибок и проверка настроек ----------

    @Test
    void allUnreadableFieldsAreReportedInFormOrder() {
        Map<String, String> texts = defaultsWith("seed", "x");
        texts.put("width", "abc");
        texts.put("plantGrowth", "");
        assertEquals(List.of("width", "plantGrowth", "seed"),
                problems(texts).stream().map(InvalidSettingsException.Problem::field).toList());
    }

    @Test
    void settingsRulesAreCheckedWhenEverythingIsReadable() {
        // Все поля — числа, но жителей больше, чем клеток: это правило Settings.
        Map<String, String> texts = defaultsWith("initialPlants", "100000");
        assertEquals("initialCount", onlyProblemField(texts));
    }

    @Test
    void unreadableFieldsAreReportedBeforeSettingsRules() {
        Map<String, String> texts = defaultsWith("width", "abc");
        texts.put("height", "0");
        assertEquals(List.of("width"), problems(texts).stream().map(InvalidSettingsException.Problem::field).toList());
    }

    private static String onlyProblemField(Map<String, String> texts) {
        List<InvalidSettingsException.Problem> list = problems(texts);
        assertEquals(1, list.size(), list.toString());
        return list.getFirst().field();
    }

    @Test
    void parserNeverThrows() {
        String[] nasty = {"\uD800", "🙂", "%s", "\\", "9".repeat(10_000), "-".repeat(50), "\u0000", null};
        for (String text : nasty) {
            Map<String, String> texts = defaultsWith("width", text);
            assertDoesNotThrow(() -> SettingsForm.parse(texts));
        }
    }
}
