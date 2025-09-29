/*
 * Copyright (c) 2023. Sergio Lissner
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package metaheuristic.java_version_migration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;

import java.util.List;

import static metaheuristic.java_version_migration.utils.MigrationUtils.*;
import static metaheuristic.java_version_migration.migrations.MigrateSynchronizedJava21.*;
import static metaheuristic.java_version_migration.migrations.MigrateSynchronizedJava21.positions;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

/**
 * @author Sergio Lissner
 * Date: 7/11/2023
 * Time: 2:26 AM
 */
@Execution(CONCURRENT)
class MigrationUtilsTest {

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
