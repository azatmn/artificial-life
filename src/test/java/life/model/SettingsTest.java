package life.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SettingsTest {

    // ---------- вспомогательное ----------

    /** Какие поля отвергла проверка (пустой список — настройки приняты). */
    private static List<String> problemFields(Settings.Builder builder) {
        try {
            builder.build();
            return List.of();
        } catch (InvalidSettingsException e) {
            return e.problems().stream().map(InvalidSettingsException.Problem::field).toList();
        }
    }

    /**
     * Основа для проверок границ: максимум энергии растения поднят до предела,
     * чтобы проверка одного поля не задевала связанные с ним правила.
     */
    private static Settings.Builder base() {
        return Settings.builder().plantMaxEnergy(Settings.MAX_ENERGY);
    }

    private static Arguments field(String name,
                                   BiFunction<Settings.Builder, Integer, Settings.Builder> setter,
                                   int min, Integer max) {
        return Arguments.of(name, setter, min, max);
    }

    /** Имя поля, сеттер, минимум и максимум (null — верхнюю границу задаёт другое правило). */
    static Stream<Arguments> numericFields() {
        return Stream.of(
                field("width", Settings.Builder::width, 1, Settings.MAX_SIZE),
                field("height", Settings.Builder::height, 1, Settings.MAX_SIZE),
                field("initialPlants", Settings.Builder::initialPlants, 0, null),
                field("initialHerbivores", Settings.Builder::initialHerbivores, 0, null),
                field("initialPredators", Settings.Builder::initialPredators, 0, null),
                field("plantStartEnergy", Settings.Builder::plantStartEnergy, 1, Settings.MAX_ENERGY),
                field("plantGrowth", Settings.Builder::plantGrowth, 1, Settings.MAX_ENERGY),
                field("plantMaxEnergy", Settings.Builder::plantMaxEnergy, 1, Settings.MAX_ENERGY),
                field("plantReproductionThreshold", Settings.Builder::plantReproductionThreshold, 1, Settings.MAX_ENERGY - 1),
                field("herbivoreStartEnergy", Settings.Builder::herbivoreStartEnergy, 1, Settings.MAX_ENERGY),
                field("herbivoreStepCost", Settings.Builder::herbivoreStepCost, 1, Settings.MAX_ENERGY),
                field("herbivoreFoodEnergy", Settings.Builder::herbivoreFoodEnergy, 1, Settings.MAX_ENERGY),
                field("herbivoreReproductionThreshold", Settings.Builder::herbivoreReproductionThreshold, 1, Settings.MAX_ENERGY),
                field("predatorStartEnergy", Settings.Builder::predatorStartEnergy, 1, Settings.MAX_ENERGY),
                field("predatorStepCost", Settings.Builder::predatorStepCost, 1, Settings.MAX_ENERGY),
                field("predatorFoodEnergy", Settings.Builder::predatorFoodEnergy, 1, Settings.MAX_ENERGY),
                field("predatorReproductionThreshold", Settings.Builder::predatorReproductionThreshold, 1, Settings.MAX_ENERGY)
        );
    }

    // ---------- настройки по умолчанию ----------

    @Test
    void defaultSettingsAreValid() {
        assertDoesNotThrow(Settings::defaults);
    }

    // ---------- границы каждого числового поля ----------

    @ParameterizedTest(name = "{0}: {2} — можно, на единицу меньше — нельзя")
    @MethodSource("numericFields")
    void lowerBound(String name, BiFunction<Settings.Builder, Integer, Settings.Builder> setter, int min, Integer max) {
        assertFalse(problemFields(setter.apply(base(), min)).contains(name), name + " = " + min + " должно приниматься");
        assertTrue(problemFields(setter.apply(base(), min - 1)).contains(name), name + " = " + (min - 1) + " должно отвергаться");
    }

    @ParameterizedTest(name = "{0}: верхняя граница")
    @MethodSource("numericFields")
    void upperBound(String name, BiFunction<Settings.Builder, Integer, Settings.Builder> setter, int min, Integer max) {
        if (max == null) {
            return; // для количества жителей верхнюю границу проверяют отдельные тесты ниже
        }
        assertFalse(problemFields(setter.apply(base(), max)).contains(name), name + " = " + max + " должно приниматься");
        assertTrue(problemFields(setter.apply(base(), max + 1)).contains(name), name + " = " + (max + 1) + " должно отвергаться");
    }

    @ParameterizedTest(name = "{0}: Integer.MIN_VALUE отвергается")
    @MethodSource("numericFields")
    void hugeNegativeValueIsRejected(String name, BiFunction<Settings.Builder, Integer, Settings.Builder> setter) {
        assertTrue(problemFields(setter.apply(base(), Integer.MIN_VALUE)).contains(name));
    }

    @ParameterizedTest(name = "{0}: Integer.MAX_VALUE отвергается")
    @MethodSource("numericFields")
    void hugePositiveValueIsRejected(String name, BiFunction<Settings.Builder, Integer, Settings.Builder> setter, int min, Integer max) {
        List<String> problems = problemFields(setter.apply(base(), Integer.MAX_VALUE));
        String expectedField = max == null ? "initialCount" : name;
        assertTrue(problems.contains(expectedField), "ожидали проблему в " + expectedField + ", получили " + problems);
    }

    // ---------- жители должны помещаться на поле ----------

    @Test
    void moreAgentsThanCellsIsRejected() {
        Settings.Builder b = Settings.builder().width(10).height(10)
                .initialPlants(50).initialHerbivores(30).initialPredators(21);
        assertTrue(problemFields(b).contains("initialCount"));
    }

    @Test
    void completelyFilledFieldIsAllowed() {
        Settings.Builder b = Settings.builder().width(10).height(10)
                .initialPlants(50).initialHerbivores(30).initialPredators(20);
        assertEquals(List.of(), problemFields(b));
    }

    @Test
    void cellCountUsesBothWidthAndHeight() {
        // Поле 5×20 = 100 клеток. Квадратные поля не отличили бы ширину от высоты.
        Settings.Builder full = Settings.builder().width(5).height(20)
                .initialPlants(60).initialHerbivores(30).initialPredators(10);
        Settings.Builder overfull = Settings.builder().width(5).height(20)
                .initialPlants(60).initialHerbivores(30).initialPredators(11);
        assertEquals(List.of(), problemFields(full));
        assertTrue(problemFields(overfull).contains("initialCount"));
        assertEquals(List.of(), problemFields(Settings.builder().width(20).height(5)
                .initialPlants(60).initialHerbivores(30).initialPredators(10)));
    }

    @Test
    void everyWrongAgentCountIsReported() {
        List<String> problems = problemFields(Settings.builder()
                .initialPlants(-1).initialHerbivores(-1).initialPredators(-1));
        assertEquals(List.of("initialPlants", "initialHerbivores", "initialPredators"), problems);
    }

    @Test
    void agentCountSumDoesNotOverflow() {
        // В int: MAX + MAX + 2 == 0, и «0 жителей» влезло бы на любое поле.
        Settings.Builder b = Settings.builder().width(10).height(10)
                .initialPlants(Integer.MAX_VALUE).initialHerbivores(Integer.MAX_VALUE).initialPredators(2);
        assertTrue(problemFields(b).contains("initialCount"));
    }

    @Test
    void emptyFieldWithoutAgentsIsAllowed() {
        Settings.Builder b = Settings.builder().width(1).height(1)
                .initialPlants(0).initialHerbivores(0).initialPredators(0);
        assertEquals(List.of(), problemFields(b));
    }

    @Test
    void agentCountIsNotComparedWhenSizeIsInvalid() {
        // Поле 0×0 уже ошибка; дополнительно ругаться на «жители не влезли» бессмысленно.
        List<String> problems = problemFields(Settings.builder().width(0).height(0));
        assertTrue(problems.contains("width"));
        assertTrue(problems.contains("height"));
        assertFalse(problems.contains("initialCount"));
    }

    // ---------- связи между параметрами растения ----------

    @Test
    void plantCannotStartAboveItsMaximum() {
        assertTrue(problemFields(Settings.builder().plantMaxEnergy(20).plantStartEnergy(21)).contains("plantStartEnergy"));
        assertFalse(problemFields(Settings.builder().plantMaxEnergy(20).plantStartEnergy(20)).contains("plantStartEnergy"));
    }

    @Test
    void plantThresholdMustBeBelowMaximumOtherwisePlantsNeverReproduce() {
        Settings.Builder atMax = Settings.builder().plantMaxEnergy(20).plantStartEnergy(1).plantReproductionThreshold(20);
        Settings.Builder belowMax = Settings.builder().plantMaxEnergy(20).plantStartEnergy(1).plantReproductionThreshold(19);
        assertTrue(problemFields(atMax).contains("plantReproductionThreshold"));
        assertEquals(List.of(), problemFields(belowMax));
    }

    // ---------- сообщения об ошибках ----------

    @Test
    void allProblemsAreReportedAtOnce() {
        List<String> problems = problemFields(Settings.builder().width(0).predatorStepCost(-3));
        assertEquals(List.of("width", "predatorStepCost"), problems);
    }

    @Test
    void messageNamesTheWrongValue() {
        InvalidSettingsException e = assertThrows(InvalidSettingsException.class,
                () -> Settings.builder().herbivoreFoodEnergy(-7).build());
        assertEquals(1, e.problems().size());
        String message = e.problems().getFirst().message();
        assertTrue(message.contains("-7"), message);
        assertTrue(e.getMessage().contains(message), "общее сообщение должно включать каждую проблему");
    }

    @Test
    void problemListCannotBeModifiedFromOutside() {
        InvalidSettingsException e = assertThrows(InvalidSettingsException.class,
                () -> Settings.builder().width(0).build());
        assertThrows(UnsupportedOperationException.class, () -> e.problems().clear());
    }

    @Test
    void validSettingsDoNotThrow() {
        assertDoesNotThrow(() -> Settings.builder().width(5).height(5)
                .initialPlants(10).initialHerbivores(5).initialPredators(2).build());
    }

    // ---------- копирование ----------

    @Test
    void toBuilderCopiesEveryField() {
        Settings original = Settings.builder()
                .width(11).height(12)
                .initialPlants(13).initialHerbivores(14).initialPredators(15)
                .plantStartEnergy(16).plantGrowth(17).plantMaxEnergy(40).plantReproductionThreshold(19)
                .herbivoreStartEnergy(20).herbivoreStepCost(21).herbivoreFoodEnergy(22).herbivoreReproductionThreshold(23)
                .predatorStartEnergy(24).predatorStepCost(25).predatorFoodEnergy(26).predatorReproductionThreshold(27)
                .seed(-28L)
                .build();
        assertEquals(original, original.toBuilder().build());
        assertNotEquals(Settings.defaults(), original, "значения выше должны отличаться от умолчаний");
    }

    @Test
    void anySeedIsAllowed() {
        assertDoesNotThrow(() -> Settings.builder().seed(Long.MIN_VALUE).build());
        assertDoesNotThrow(() -> Settings.builder().seed(Long.MAX_VALUE).build());
    }
}
