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
public class AngularMapGetMigrationTest {

    String sonnet = """
        ================================================
        DO NO MAKE ANY CHANGES IN OTHER FILES!!!
        
        ==============================================
        
        
        BEFORE STARTING, READ PROJECT'S INSTRUCTIONS CAREFULLY.
        by project's instructions I mean instruction in Claude, not in readme.md or any other file in repo
        
        -----------
        
        **Problem:** After migrating to Angular 19, I've got a warning:
         Global built-in functions are deprecated and will be removed in Dart Sass 3.0.0.
           Use map.get instead.
        \s
        **Before:**
        ```
        $accent: map-get($theme, accent);
        ```
        
        **After:**
        ```
        @use "sass:map";
        $accent: map.get($theme, accent);
        ```
        
        =================
        
        start to implement a method metaheuristic.java_version_migration.migrations.AngularMapGetMigration.migrateMapGetAngular
        and creating unit-tests in metaheuristic.java_version_migration.migrations.AngularMapGetMigration.migrateMapGetAngularTest
        
        ================
        variable 'String content', which method migrateMapGetAngularTest() is receiving, will be always not null, no need in additional check.
        ================
        
        usage of @use "sass:map"; must be added at the top of file
        
        =============
        if variable semicolon==true, then put semicolon at the end of @use, otherwise there shouldn't be semicolon at the end of line
        
        =============
        """;

    @Test
    public void migrateMapGetAngularTest() {
        String content = """
            $accent: map-get($theme, accent);
            $primary: map-get($colors, primary);
            """;
        
        String result = AngularMapGetMigration.migrateMapGetAngular(content, true);
        
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
        
        String result = AngularMapGetMigration.migrateMapGetAngular(content, false);
        
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
        
        String result = AngularMapGetMigration.migrateMapGetAngular(content, true);
        
        assertEquals(content, result);
    }

    @Test
    public void migrateMapGetAngularTest_alreadyMigrated() {
        String content = """
            @use "sass:map";
            $accent: map.get($theme, accent);
            """;
        
        String result = AngularMapGetMigration.migrateMapGetAngular(content, true);
        
        assertEquals(content, result);
    }
}
