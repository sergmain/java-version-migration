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
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Sergio Lissner
 * Date: 9/6/2025
 * Time: 4:24 PM
 */
public class MapGetAngularMigrationTest {

    @Test
    public void migrateMapGetAngularTest() {
        String content = """
            $accent: map-get($theme, accent);
            $primary: map-get($colors, primary);
            """;
        
        String result = MapGetAngularMigration.migrateMapGetAngular(content, true);
        
        String expected = """
            @use "sass:map";
            $accent: map.get($theme, accent);
            $primary: map.get($colors, primary);
            """;
        
        assertEquals(expected, result);
    }

    @Test
    public void migrateMapGetAngularTest_withoutSemicolon() {
        String content = """
            $accent: map-get($theme, accent);
            """;
        
        String result = MapGetAngularMigration.migrateMapGetAngular(content, false);
        
        String expected = """
            @use "sass:map"
            $accent: map.get($theme, accent);
            """;
        
        assertEquals(expected, result);
    }

    @Test
    public void migrateMapGetAngularTest_noMapGet() {
        String content = """
            $primary: #000000;
            $secondary: #ffffff;
            """;
        
        String result = MapGetAngularMigration.migrateMapGetAngular(content, true);
        
        assertEquals(content, result);
    }

    @Test
    public void migrateMapGetAngularTest_alreadyMigrated() {
        String content = """
            @use "sass:map";
            $accent: map.get($theme, accent);
            """;
        
        String result = MapGetAngularMigration.migrateMapGetAngular(content, true);
        
        assertEquals(content, result);
    }
}
