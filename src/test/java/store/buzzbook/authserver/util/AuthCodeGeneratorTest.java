package store.buzzbook.authserver.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuthCodeGeneratorTest {

    private static final String CHAR_LIST = "qpwoeirutyalskdjfhgmznxbcv1029384756QPWOEIRUTYALSKDJFHGZMXNCBV";

    @Test
    void testGenerate() {
        // Generate a code
        String code = AuthCodeGenerator.generate();

        // Check the length of the generated code
        assertEquals(5, code.length(), "The generated code should have a length of 5");

        // Check that each character in the generated code is in the CHAR_LIST
        for (char ch : code.toCharArray()) {
            assertTrue(CHAR_LIST.indexOf(ch) >= 0, "The generated code contains invalid characters");
        }
    }

    @Test
    void testMultipleGenerations() {
        // Generate multiple codes to ensure randomness
        String code1 = AuthCodeGenerator.generate();
        String code2 = AuthCodeGenerator.generate();
        String code3 = AuthCodeGenerator.generate();

        // Ensure that multiple generations do not produce the same code
        assertNotEquals(code1, code2, "Two consecutive codes should not be the same");
        assertNotEquals(code1, code3, "Two consecutive codes should not be the same");
        assertNotEquals(code2, code3, "Two consecutive codes should not be the same");
    }
}
