package metaheuristic.java_version_migration;

import org.junit.jupiter.api.Test;

import java.util.List;

import static metaheuristic.java_version_migration.MigrationUtils.*;
import static metaheuristic.java_version_migration.migrations.MigrateSynchronizedJava21.*;
import static metaheuristic.java_version_migration.migrations.MigrateSynchronizedJava21.positions;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Sergio Lissner
 * Date: 7/11/2023
 * Time: 2:26 AM
 */
public class MigrationUtilsTest {

    @Test
    public void test_isCommentLine_1() {
        String code = """
                
                // comment
                
                
                """;
        assertTrue(isInCommentLine(code, 6));
        assertFalse(isInCommentLine(code, 0));
        assertFalse(isInCommentLine(code, code.length() - 1));
    }

    @Test
    public void test_isInCommentBlock_1() {
        String code = """
                
                /* comment /*
                
                /* xxxxx */ int i=0;
                """;
        assertTrue(isInCommentBlock(code, 6));
        assertFalse(isInCommentBlock(code, code.length()-1));
    }

    @Test
    public void test_isInCommentBlock_2() {
        String code = """
                
                /**
                 * The stack of byte values. This class is not synchronized and should not be
                 * used by multiple threads concurrently.
                 */

                """;
        List<Position> positions = positions(code, true);
        assertEquals(1, positions.size());

        assertTrue(isInCommentBlock(code, positions.get(0).start()));
    }

}
