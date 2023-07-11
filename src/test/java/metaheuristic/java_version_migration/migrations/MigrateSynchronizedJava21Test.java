package metaheuristic.java_version_migration.migrations;

import org.junit.jupiter.api.Test;

import java.util.List;

import static metaheuristic.java_version_migration.migrations.MigrateSynchronizedJava21.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Sergio Lissner
 * Date: 7/10/2023
 * Time: 10:27 PM
 */
public class MigrateSynchronizedJava21Test {

    @Test
    public void test_positions_1() {

        String code = """
                public synchronized boolean yes() {
                    return true;
                }

                public boolean no() {
                synchronized(this)  {
                    return false;
                }

                public Boolean maybe() {
                synchronized
                (this)  {
                    return null;
                }
                }
                """;

        List<Position> positions = positions(code);
        assertEquals(3, positions.size());
        assertEquals(new Position(6, 20, Type.method), positions.get(0));
        assertEquals(new Position(77, 91, Type.object), positions.get(1));
        assertEquals(new Position(145, 159, Type.object), positions.get(2));

        int i=0;
    }

    @Test
    public void test_positions_2() {

        String code = """
                public Boolean maybe() {
                synchronized
                (this)  {
                    return null;
                }
                }
                """;

        List<Position> positions = positions(code);
        assertEquals(1, positions.size());
        assertEquals(new Position(24, 38, Type.object), positions.get(0));

        int i=0;
    }
}
