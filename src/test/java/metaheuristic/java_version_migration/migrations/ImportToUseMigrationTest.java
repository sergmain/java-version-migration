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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;

import static metaheuristic.java_version_migration.migrations.ImportToUseMigration.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

/**
 * Unit tests for ImportToUseMigration functionality
 * Tests migration from SCSS @import to @use statements
 * 
 * @author Sergio Lissner
 * Date: 9/6/2025
 * Time: 12:00 PM
 */
@Execution(CONCURRENT)
class ImportToUseMigrationTest {

    @Test
    void test_migrateImportToUseMigration_1() {
        String input = "@import ../../../variables.scss";
        String expected = "@use '../../../variables' as *";

        // When
        String result = migrateImportToUseMigration(input, false);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void test_migrateImportToUseMigration_2() {
        String input = """
            @import ../../../variables.scss;
            
            :host {
                .app-view {
                    height: 100%;
                    display: flex;
                    flex-direction: column;
                }
            }""";
        String expected = """
            @use '../../../variables' as *;
            
            :host {
                .app-view {
                    height: 100%;
                    display: flex;
                    flex-direction: column;
                }
            }""";

        // When
        String result = migrateImportToUseMigration(input, true);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void test_migrateImportToUseMigration_3() {
        String input = """
            @import ../ai
            
            mat-sidenav-content\s
                 overflow: hidden
            """;
        String expected = """
            @use '../ai' as *;
            
            mat-sidenav-content\s
                 overflow: hidden
            """;

        // When
        String result = migrateImportToUseMigration(input, true);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void test_migrateImportToUseMigration_4() {
        String input = """
            @import "../ct";
            
            :host {
                display: block;
                padding: unit(1.5) unit(1.75) unit(1.25);
                line-height: 1.2;
                border-radius: unit(0.5);
            }
            """;
        String expected = """
            @use "../ct" as *;
            
            :host {
                display: block;
                padding: unit(1.5) unit(1.75) unit(1.25);
                line-height: 1.2;
                border-radius: unit(0.5);
            }
            """;

        // When
        String result = migrateImportToUseMigration(input, true);

        // Then
        assertEquals(expected, result);
    }

    @Test
    void test_migrateImportToUseMigration_5() {
        String input = """
            @import "../company.sass";
            
            mat-checkbox {
                position: relative
            }
            """;
        String expected = """
            @use "../company" as *;
            
            mat-checkbox {
                position: relative
            }
            """;

        // When
        String result = migrateImportToUseMigration(input, true);

        // Then
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Should convert basic SCSS import with single quotes")
    void testBasicImportWithSingleQuotes() {
        // Given
        String input = "@import '../../../variables.scss';";
        String expected = "@use '../../../variables' as *;";

        // When
        String result = migrateImportToUseMigration(input, true);

        // Then
        assertEquals(expected, result,
            "Should convert @import with single quotes to @use with 'as *'");
    }

    @Test
    @DisplayName("Should convert basic SCSS import with double quotes")
    void testBasicImportWithDoubleQuotes() {
        // Given
        String input = "@import \"../../../variables.scss\";";
        String expected = "@use \"../../../variables\" as *;";

        // When
        String result = migrateImportToUseMigration(input, true);

        // Then
        assertEquals(expected, result,
            "Should convert @import with double quotes to @use with 'as *'");
    }

    @Test
    @DisplayName("Should handle unquoted import statements")
    void testUnquotedImports() {
        // Given
        String input = """
            @import ../../../variables.scss;
            @import ./mixins.scss;
            @import ../../base/reset.scss;
            """;

        String expected = """
            @use '../../../variables' as *;
            @use './mixins' as *;
            @use '../../base/reset' as *;
            """;

        // When
        String result = migrateImportToUseMigration(input, true);

        // Then
        assertEquals(expected, result,
            "Should convert unquoted @import statements to @use with single quotes");
    }

    @Test
    @DisplayName("Should handle multiple imports in same content")
    void testMultipleImports() {
        // Given
        String input = """
            @import '../variables.scss';
            @import "./mixins.scss";
            @import '../../base/reset.scss';
            
            .my-class {
              color: $primary-color;
            }
            """;

        String expected = """
            @use '../variables' as *;
            @use "./mixins" as *;
            @use '../../base/reset' as *;
            
            .my-class {
              color: $primary-color;
            }
            """;

        // When
        String result = migrateImportToUseMigration(input, true);

        // Then
        assertEquals(expected, result,
            "Should convert multiple @import statements to @use");
    }

    @Test
    @DisplayName("Should handle imports with different path depths")
    void testImportsWithDifferentPaths() {
        // Given
        String input = """
            @import 'variables.scss';
            @import '../variables.scss';
            @import '../../shared/variables.scss';
            @import '../../../global/variables.scss';
            """;

        String expected = """
            @use 'variables' as *;
            @use '../variables' as *;
            @use '../../shared/variables' as *;
            @use '../../../global/variables' as *;
            """;

        // When
        String result = migrateImportToUseMigration(input, true);

        // Then
        assertEquals(expected, result,
            "Should handle imports with various path depths correctly");
    }

    @Test
    @DisplayName("Should handle imports with whitespace")
    void testImportsWithWhitespace() {
        // Given
        String input = """
            @import  '../variables.scss'  ;
            @import   "../../base.scss"   ;
            """;

        String expected = """
            @use '../variables' as *;
            @use "../../base" as *;
            """;

        // When
        String result = migrateImportToUseMigration(input, true);

        // Then
        assertEquals(expected, result,
            "Should handle whitespace patterns around @import statements");
    }

    @Test
    @DisplayName("Should handle imports with complex file names")
    void testImportsWithComplexFileNames() {
        // Given
        String input = """
            @import './component-styles.scss';
            @import '../mixins/button-mixins.scss';
            @import '../../themes/dark-theme.scss';
            @import '../../../shared/utilities/text-utils.scss';
            """;

        String expected = """
            @use './component-styles' as *;
            @use '../mixins/button-mixins' as *;
            @use '../../themes/dark-theme' as *;
            @use '../../../shared/utilities/text-utils' as *;
            """;

        // When
        String result = migrateImportToUseMigration(input, true);

        // Then
        assertEquals(expected, result,
            "Should handle complex file names with hyphens and underscores");
    }

    @Test
    @DisplayName("Should preserve other CSS content unchanged")
    void testPreserveOtherContent() {
        // Given
        String input = """
            // Component styles
            @import '../variables.scss';
            
            .component {
              display: flex;
              padding: 1rem;
              
              &__title {
                font-size: 1.5rem;
                color: $primary-color;
              }
              
              &--modifier {
                background-color: $secondary-color;
              }
            }
            
            @media (max-width: 768px) {
              .component {
                padding: 0.5rem;
              }
            }
            """;

        String expected = """
            // Component styles
            @use '../variables' as *;
            
            .component {
              display: flex;
              padding: 1rem;
              
              &__title {
                font-size: 1.5rem;
                color: $primary-color;
              }
              
              &--modifier {
                background-color: $secondary-color;
              }
            }
            
            @media (max-width: 768px) {
              .component {
                padding: 0.5rem;
              }
            }
            """;

        // When
        String result = migrateImportToUseMigration(input, true);

        // Then
        assertEquals(expected, result,
            "Should preserve all other CSS content while only migrating @import statements");
    }

    @Test
    @DisplayName("Should not modify content without SCSS imports")
    void testContentWithoutImports() {
        // Given
        String input = """
            .my-component {
              display: grid;
              grid-template-columns: 1fr 2fr;
              gap: 1rem;
              
              .header {
                grid-column: span 2;
                background: #f0f0f0;
              }
            }
            """;

        // When
        String result = migrateImportToUseMigration(input, true);

        // Then
        assertEquals(input, result,
            "Should not modify content that doesn't contain @import statements");
    }

    @Test
    @DisplayName("Should handle mixed quotes in same content")
    void testMixedQuotes() {
        // Given
        String input = """
            @import '../variables.scss';
            @import "./mixins.scss";
            @import '../../base.scss';
            """;

        String expected = """
            @use '../variables' as *;
            @use "./mixins" as *;
            @use '../../base' as *;
            """;

        // When
        String result = migrateImportToUseMigration(input, true);

        // Then
        assertEquals(expected, result,
            "Should handle files with mixed single and double quotes");
    }

    @Test
    @DisplayName("Should handle imports in nested structures")
    void testImportsInNestedStructures() {
        // Given
        String input = """
            @if $theme == 'dark' {
              @import '../themes/dark-variables.scss';
            } @else {
              @import '../themes/light-variables.scss';
            }
            
            @mixin button-theme {
              @import '../button-variables.scss';
              
              .button {
                background: $button-bg;
              }
            }
            """;

        String expected = """
            @if $theme == 'dark' {
              @use '../themes/dark-variables' as *;
            } @else {
              @use '../themes/light-variables' as *;
            }
            
            @mixin button-theme {
              @use '../button-variables' as *;
              
              .button {
                background: $button-bg;
              }
            }
            """;

        // When
        String result = migrateImportToUseMigration(input, true);

        // Then
        assertEquals(expected, result,
            "Should handle @import statements within SCSS control structures and mixins");
    }

    @Test
    @DisplayName("Should handle imports with absolute paths")
    void testImportsWithAbsolutePaths() {
        // Given
        String input = """
            @import '/src/styles/variables.scss';
            @import '/assets/scss/mixins.scss';
            """;

        String expected = """
            @use '/src/styles/variables' as *;
            @use '/assets/scss/mixins' as *;
            """;

        // When
        String result = migrateImportToUseMigration(input, true);

        // Then
        assertEquals(expected, result,
            "Should handle absolute path imports correctly");
    }

    @Test
    @DisplayName("Should handle imports with node_modules paths")
    void testImportsWithNodeModules() {
        // Given
        String input = """
            @import '~bootstrap/scss/variables.scss';
            @import '~@angular/material/theming.scss';
            """;

        String expected = """
            @use '~bootstrap/scss/variables' as *;
            @use '~@angular/material/theming' as *;
            """;

        // When
        String result = migrateImportToUseMigration(input, true);

        // Then
        assertEquals(expected, result,
            "Should handle node_modules imports with ~ prefix correctly");
    }

    @Test
    @DisplayName("Should handle imports on same line as other content")
    void testImportsOnSameLine() {
        // Given
        String input = """
            /* Styles */ @import '../variables.scss'; /* Variables */
            .component { @import './component-vars.scss'; color: red; }
            """;

        String expected = """
            /* Styles */ @use '../variables' as *; /* Variables */
            .component { @use './component-vars' as *; color: red; }
            """;

        // When
        String result = migrateImportToUseMigration(input, true);

        // Then
        assertEquals(expected, result,
            "Should handle @import statements on same line as other content");
    }

    @Test
    @DisplayName("Should handle empty content")
    void testEmptyContent() {
        // Given
        String input = "";

        // When
        String result = migrateImportToUseMigration(input, true);

        // Then
        assertEquals("", result,
            "Should handle empty content without errors");
    }

    @Test
    @DisplayName("Should handle content with only whitespace")
    void testWhitespaceOnlyContent() {
        // Given
        String input = "   \n\t  \r\n  ";

        // When
        String result = migrateImportToUseMigration(input, true);

        // Then
        assertEquals(input, result,
            "Should handle whitespace-only content unchanged");
    }

    @Test
    @DisplayName("Should handle import statement without semicolon")
    void testImportWithoutSemicolon() {
        // Given
        String input = "@import '../variables.scss'";

        // When
        String result = migrateImportToUseMigration(input, true);

        // Then
        assertEquals(input, result,
            "Should not modify @import statements without semicolons (invalid SCSS)");
    }

    @Test
    @DisplayName("Should handle well-formed imports correctly")
    void testWellFormedImports() {
        // Given
        String input = """
            @import '../variables';
            @import 'variables.scss';
            @import "variables.scss";
            """;

        String expected = """
            @use '../variables' as *;
            @use 'variables' as *;
            @use "variables" as *;
            """;

        // When
        String result = migrateImportToUseMigration(input, true);

        // Then
        assertEquals(expected, result,
            "Should only process well-formed @import statements");
    }
}
