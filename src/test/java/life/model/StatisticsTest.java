package life.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatisticsTest {

    @Test
    void totalAddsAllSpecies() {
        assertEquals(12, new Statistics(5, 3, 4, 5).total());
        assertEquals(0, new Statistics(5, 0, 0, 0).total());
    }

    @Test
    void allDeadOnlyWhenNobodyIsLeft() {
        assertTrue(new Statistics(1, 0, 0, 0).allDead());
        assertFalse(new Statistics(1, 1, 0, 0).allDead());
        assertFalse(new Statistics(1, 0, 1, 0).allDead());
        assertFalse(new Statistics(1, 0, 0, 1).allDead());
    }

    @Test
    void speciesExtinctWhenAnyCountIsZero() {
        assertTrue(new Statistics(1, 0, 4, 5).anySpeciesExtinct());
        assertTrue(new Statistics(1, 3, 0, 5).anySpeciesExtinct());
        assertTrue(new Statistics(1, 3, 4, 0).anySpeciesExtinct());
        assertFalse(new Statistics(1, 1, 1, 1).anySpeciesExtinct());
    }
}
