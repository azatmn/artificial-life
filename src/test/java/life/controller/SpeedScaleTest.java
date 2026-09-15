package life.controller;

import org.junit.jupiter.api.Test;

import static life.controller.SimulationController.MAX_SPEED;
import static life.controller.SimulationController.MIN_SPEED;
import static org.junit.jupiter.api.Assertions.*;

class SpeedScaleTest {

    @Test
    void endsOfSliderAreSlowestAndFastestSpeed() {
        assertEquals(MIN_SPEED, SpeedScale.toSpeed(SpeedScale.SLIDER_MIN));
        assertEquals(MAX_SPEED, SpeedScale.toSpeed(SpeedScale.SLIDER_MAX));
    }

    @Test
    void valuesOutsideSliderAreClamped() {
        assertEquals(MIN_SPEED, SpeedScale.toSpeed(-50));
        assertEquals(MAX_SPEED, SpeedScale.toSpeed(1000));
        assertEquals(MIN_SPEED, SpeedScale.toSpeed(Double.NEGATIVE_INFINITY));
        assertEquals(MAX_SPEED, SpeedScale.toSpeed(Double.POSITIVE_INFINITY));
        assertEquals(MIN_SPEED, SpeedScale.toSpeed(Double.NaN));
    }

    @Test
    void movingSliderRightNeverSlowsDown() {
        int previous = SpeedScale.toSpeed(SpeedScale.SLIDER_MIN);
        for (double s = SpeedScale.SLIDER_MIN; s <= SpeedScale.SLIDER_MAX; s += 0.25) {
            int speed = SpeedScale.toSpeed(s);
            assertTrue(speed >= previous, "на " + s + " скорость " + speed + " меньше прежней " + previous);
            assertTrue(speed >= MIN_SPEED && speed <= MAX_SPEED);
            previous = speed;
        }
    }

    @Test
    void scaleIsLogarithmicSoSlowSpeedsGetHalfTheSlider() {
        // На линейной шкале середина была бы ~250 шагов/с. На логарифмической — около √500 ≈ 22.
        int middle = SpeedScale.toSpeed((SpeedScale.SLIDER_MIN + SpeedScale.SLIDER_MAX) / 2);
        assertTrue(middle >= 15 && middle <= 30, "середина слайдера даёт " + middle);
    }

    @Test
    void everySpeedCanBeShownOnSliderAndReadBack() {
        for (int speed = MIN_SPEED; speed <= MAX_SPEED; speed++) {
            double slider = SpeedScale.toSlider(speed);
            assertTrue(slider >= SpeedScale.SLIDER_MIN && slider <= SpeedScale.SLIDER_MAX, "скорость " + speed + " → " + slider);
            assertEquals(speed, SpeedScale.toSpeed(slider), "скорость " + speed + " не пережила путь туда-обратно");
        }
    }

    @Test
    void sliderPositionForOutOfRangeSpeedIsClamped() {
        assertEquals(SpeedScale.SLIDER_MIN, SpeedScale.toSlider(0));
        assertEquals(SpeedScale.SLIDER_MIN, SpeedScale.toSlider(Integer.MIN_VALUE));
        assertEquals(SpeedScale.SLIDER_MAX, SpeedScale.toSlider(Integer.MAX_VALUE));
    }
}
