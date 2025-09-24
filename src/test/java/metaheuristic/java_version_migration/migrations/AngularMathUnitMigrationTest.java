/*
 * Copyright (c) 2025. Sergio Lissner
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

package metaheuristic.java_version_migration.migrations;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

/**
 * @author Sergio Lissner
 * Date: 9/6/2025
 * Time: 1:11 PM
 */
@Execution(CONCURRENT)
class AngularMathUnitMigrationTest {

    @Test
    void testMigrateSingleUnitCallWithSemicolon() {
        String input = """
                .element {
                  top: unit(6);
                }
                """;
        
        String expected = """
                @use 'sass:math';
                .element {
                  top: math.unit(6);
                }
                """;
        
        String result = AngularMathUnitMigration.migrateMathUnitAngular(input, true);
        assertEquals(expected, result);
    }

    @Test
    void testMigrateSingleUnitCallWithoutSemicolon() {
        String input = """
                .element {
                  top: unit(6);
                }
                """;
        
        String expected = """
                @use 'sass:math'
                .element {
                  top: math.unit(6);
                }
                """;
        
        String result = AngularMathUnitMigration.migrateMathUnitAngular(input, false);
        assertEquals(expected, result);
    }

    @Test
    void testMigrateMultipleUnitCallsWithSemicolon() {
        String input = """
                .element {
                  top: unit(6);
                  left: unit(10);
                  margin: unit(5) unit(3);
                }
                """;
        
        String expected = """
                @use 'sass:math';
                .element {
                  top: math.unit(6);
                  left: math.unit(10);
                  margin: math.unit(5) math.unit(3);
                }
                """;
        
        String result = AngularMathUnitMigration.migrateMathUnitAngular(input, true);
        assertEquals(expected, result);
    }

    @Test
    void testMigrateMultipleUnitCallsWithoutSemicolon() {
        String input = """
                .element {
                  top: unit(6);
                  left: unit(10);
                  margin: unit(5) unit(3);
                }
                """;
        
        String expected = """
                @use 'sass:math'
                .element {
                  top: math.unit(6);
                  left: math.unit(10);
                  margin: math.unit(5) math.unit(3);
                }
                """;
        
        String result = AngularMathUnitMigration.migrateMathUnitAngular(input, false);
        assertEquals(expected, result);
    }

    @Test
    void testMigrateUnitWithSpaces() {
        String input = """
                .element {
                  top: unit (6);
                  left: unit  (10);
                }
                """;
        
        String expected = """
                @use 'sass:math';
                .element {
                  top: math.unit(6);
                  left: math.unit(10);
                }
                """;
        
        String result = AngularMathUnitMigration.migrateMathUnitAngular(input, true);
        assertEquals(expected, result);
    }

    @Test
    void testNoChangeWhenAlreadyHasSassMath() {
        String input = """
                @use 'sass:math';
                .element {
                  top: unit(6);
                }
                """;
        
        String expected = """
                @use 'sass:math';
                .element {
                  top: math.unit(6);
                }
                """;
        
        String result = AngularMathUnitMigration.migrateMathUnitAngular(input, true);
        assertEquals(expected, result);
    }

    @Test
    void testNoChangeWhenNoUnitCalls() {
        String input = """
                .element {
                  top: 10px;
                  left: 20rem;
                  margin: 5em;
                }
                """;
        
        String result = AngularMathUnitMigration.migrateMathUnitAngular(input, true);
        assertEquals(input, result);
        
        result = AngularMathUnitMigration.migrateMathUnitAngular(input, false);
        assertEquals(input, result);
    }

    @Test
    void testNoChangeWhenAlreadyMathUnit() {
        String input = """
                @use 'sass:math';
                .element {
                  top: math.unit(6);
                  left: math.unit(10);
                }
                """;
        
        String result = AngularMathUnitMigration.migrateMathUnitAngular(input, true);
        assertEquals(input, result);
        
        result = AngularMathUnitMigration.migrateMathUnitAngular(input, false);
        assertEquals(input, result);
    }

    @Test
    void testEmptyContent() {
        String input = "";
        String result = AngularMathUnitMigration.migrateMathUnitAngular(input, true);
        assertEquals(input, result);
        
        result = AngularMathUnitMigration.migrateMathUnitAngular(input, false);
        assertEquals(input, result);
    }

    @Test
    void testComplexSassWithMixinsWithSemicolon() {
        String input = """
                @mixin button-style {
                  padding: unit(8) unit(16);
                  border-radius: unit(4);
                }
                
                .button {
                  @include button-style;
                  margin: unit(10);
                }
                """;
        
        String expected = """
                @use 'sass:math';
                @mixin button-style {
                  padding: math.unit(8) math.unit(16);
                  border-radius: math.unit(4);
                }
                
                .button {
                  @include button-style;
                  margin: math.unit(10);
                }
                """;
        
        String result = AngularMathUnitMigration.migrateMathUnitAngular(input, true);
        assertEquals(expected, result);
    }

    @Test
    void testComplexSassWithMixinsWithoutSemicolon() {
        String input = """
                @mixin button-style {
                  padding: unit(8) unit(16);
                  border-radius: unit(4);
                }
                
                .button {
                  @include button-style;
                  margin: unit(10);
                }
                """;
        
        String expected = """
                @use 'sass:math'
                @mixin button-style {
                  padding: math.unit(8) math.unit(16);
                  border-radius: math.unit(4);
                }
                
                .button {
                  @include button-style;
                  margin: math.unit(10);
                }
                """;
        
        String result = AngularMathUnitMigration.migrateMathUnitAngular(input, false);
        assertEquals(expected, result);
    }
}
