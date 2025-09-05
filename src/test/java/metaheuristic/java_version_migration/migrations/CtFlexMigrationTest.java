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

import static metaheuristic.java_version_migration.migrations.CtFlexMigration.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

/**
 * Unit tests for CtFlexMigration functionality
 * 
 * @author Sergio Lissner
 * Date: 9/4/2025
 * Time: 5:15 PM
 */
@Execution(CONCURRENT)
class CtFlexMigrationTest {

    @Test
    @DisplayName("Should convert simple button group pattern")
    void testSimpleButtonGroupPattern() {
        // Given
        String input = """
            <ct-flex justify-content="flex-end" gap="8px">
              <ct-flex-item>
                <button mat-stroked-button>Cancel</button>
              </ct-flex-item>
              <ct-flex-item>
                <button mat-flat-button color="primary">Save</button>
              </ct-flex-item>
            </ct-flex>
            """;

        String expected = """
            <div class="flex justify-content-flex-end gap-8px">
              <button mat-stroked-button>Cancel</button>
              <button mat-flat-button color="primary">Save</button>
            </div>
            """;

        // When
        String result = processContent(input);

        // Then
        assertEquals(normalizeWhitespace(expected), normalizeWhitespace(result),
            "Should convert ct-flex to div with appropriate CSS classes and unwrap ct-flex-item elements");
    }

    @Test
    @DisplayName("Should convert space between layout pattern")
    void testSpaceBetweenLayoutPattern() {
        // Given
        String input = """
            <ct-flex justify-content="space-between">
              <ct-flex-item>
                <ct-heading>Title</ct-heading>
              </ct-flex-item>
              <ct-flex-item>
                <button mat-flat-button>Action</button>
              </ct-flex-item>
            </ct-flex>
            """;

        String expected = """
            <div class="flex justify-content-space-between">
              <ct-heading>Title</ct-heading>
              <button mat-flat-button>Action</button>
            </div>
            """;

        // When
        String result = processContent(input);

        // Then
        assertEquals(normalizeWhitespace(expected), normalizeWhitespace(result),
            "Should convert ct-flex with space-between to div with appropriate CSS class");
    }

    @Test
    @DisplayName("Should convert table action buttons pattern")
    void testTableActionButtonsPattern() {
        // Given
        String input = """
            <ct-flex justify-content="flex-end" gap="9px">
              <ct-flex-item>
                <button mat-flat-button>Edit</button>
              </ct-flex-item>
              <ct-flex-item>
                <button mat-flat-button color="warn">Delete</button>
              </ct-flex-item>
            </ct-flex>
            """;

        String expected = """
            <div class="flex justify-content-flex-end gap-9px">
              <button mat-flat-button>Edit</button>
              <button mat-flat-button color="warn">Delete</button>
            </div>
            """;

        // When
        String result = processContent(input);

        // Then
        assertEquals(normalizeWhitespace(expected), normalizeWhitespace(result),
            "Should convert table action buttons pattern correctly");
    }

    @Test
    @DisplayName("Should convert pattern with align-items")
    void testAlignItemsPattern() {
        // Given
        String input = """
            <ct-flex justify-content="space-between" align-items="center" gap="8px">
              <ct-flex-item>
                <span>Content</span>
              </ct-flex-item>
            </ct-flex>
            """;

        String expected = """
            <div class="flex justify-content-space-between align-items-center gap-8px">
              <span>Content</span>
            </div>
            """;

        // When
        String result = processContent(input);

        // Then
        assertEquals(normalizeWhitespace(expected), normalizeWhitespace(result),
            "Should convert ct-flex with align-items to div with appropriate CSS classes");
    }

    @Test
    @DisplayName("Should handle complex nested layouts")
    void testComplexNestedLayouts() {
        // Given
        String input = """
            <ct-flex justify-content="space-between" align-items="center" gap="16px">
              <ct-flex-item>Content</ct-flex-item>
              <ct-flex-item>
                <ct-flex justify-content="flex-end" gap="8px">
                  <ct-flex-item><button>Edit</button></ct-flex-item>
                  <ct-flex-item><button>Delete</button></ct-flex-item>
                </ct-flex>
              </ct-flex-item>
            </ct-flex>
            """;

        String expected = """
            <div class="flex justify-content-space-between align-items-center gap-16px">
              Content
              <div class="flex justify-content-flex-end gap-8px">
                <button>Edit</button>
                <button>Delete</button>
              </div>
            </div>
            """;

        // When
        String result = processContent(input);

        // Then
        assertEquals(normalizeWhitespace(expected), normalizeWhitespace(result),
            "Should handle complex nested ct-flex layouts correctly");
    }

    @Test
    @DisplayName("Should handle ct-flex without attributes")
    void testCtFlexWithoutAttributes() {
        // Given
        String input = """
            <ct-flex>
              <ct-flex-item>
                <button>Simple Button</button>
              </ct-flex-item>
            </ct-flex>
            """;

        String expected = """
            <div class="flex">
              <button>Simple Button</button>
            </div>
            """;

        // When
        String result = processContent(input);

        // Then
        assertEquals(normalizeWhitespace(expected), normalizeWhitespace(result),
            "Should convert ct-flex without attributes to div with base flex class");
    }

    @Test
    @DisplayName("Should handle multiple ct-flex elements in same content")
    void testMultipleCtFlexElements() {
        // Given
        String input = """
            <div>
              <ct-flex justify-content="flex-start" gap="4px">
                <ct-flex-item><span>First</span></ct-flex-item>
                <ct-flex-item><span>Second</span></ct-flex-item>
              </ct-flex>
              <p>Some content</p>
              <ct-flex justify-content="flex-end" gap="12px">
                <ct-flex-item><button>Action</button></ct-flex-item>
              </ct-flex>
            </div>
            """;

        String expected = """
            <div>
              <div class="flex justify-content-flex-start gap-4px">
                <span>First</span>
                <span>Second</span>
              </div>
              <p>Some content</p>
              <div class="flex justify-content-flex-end gap-12px">
                <button>Action</button>
              </div>
            </div>
            """;

        // When
        String result = processContent(input);

        // Then
        assertEquals(normalizeWhitespace(expected), normalizeWhitespace(result),
            "Should handle multiple ct-flex elements in the same content correctly");
    }

    @Test
    @DisplayName("Should handle ct-flex-item with complex nested content")
    void testCtFlexItemWithComplexContent() {
        // Given
        String input = """
            <ct-flex justify-content="space-between">
              <ct-flex-item>
                <div class="complex-content">
                  <h3>Title</h3>
                  <p>Description</p>
                  <span class="info">Additional info</span>
                </div>
              </ct-flex-item>
              <ct-flex-item>
                <button mat-flat-button>Action</button>
              </ct-flex-item>
            </ct-flex>
            """;

        String expected = """
            <div class="flex justify-content-space-between">
              <div class="complex-content">
                <h3>Title</h3>
                <p>Description</p>
                <span class="info">Additional info</span>
              </div>
              <button mat-flat-button>Action</button>
            </div>
            """;

        // When
        String result = processContent(input);

        // Then
        assertEquals(normalizeWhitespace(expected), normalizeWhitespace(result),
            "Should handle ct-flex-item with complex nested content correctly");
    }

    @Test
    @DisplayName("Should handle empty ct-flex-item elements")
    void testEmptyCtFlexItems() {
        // Given
        String input = """
            <ct-flex justify-content="flex-end" gap="8px">
              <ct-flex-item></ct-flex-item>
              <ct-flex-item>
                <button>Save</button>
              </ct-flex-item>
            </ct-flex>
            """;

        String expected = """
            <div class="flex justify-content-flex-end gap-8px">
              
              <button>Save</button>
            </div>
            """;

        // When
        String result = processContent(input);

        // Then
        assertEquals(normalizeWhitespace(expected), normalizeWhitespace(result),
            "Should handle empty ct-flex-item elements correctly");
    }

    @Test
    @DisplayName("Should preserve indentation correctly")
    void testIndentationPreservation() {
        // Given
        String input = """
            <div class="container">
              <ct-flex justify-content="center" gap="16px">
                <ct-flex-item>
                  <button class="btn primary">
                    Primary Action
                  </button>
                </ct-flex-item>
                <ct-flex-item>
                  <button class="btn secondary">
                    Secondary Action
                  </button>
                </ct-flex-item>
              </ct-flex>
            </div>
            """;

        // When
        String result = processContent(input);

        // Then
        assertTrue(result.contains("<div class=\"flex justify-content-center gap-16px\">"),
            "Should convert ct-flex to div with correct classes");
        assertTrue(result.contains("<button class=\"btn primary\">"),
            "Should preserve button structure and classes");
        assertTrue(result.contains("Primary Action"),
            "Should preserve button content");
        assertFalse(result.contains("ct-flex"),
            "Should not contain any ct-flex elements after migration");
        assertFalse(result.contains("ct-flex-item"),
            "Should not contain any ct-flex-item elements after migration");
    }

    @Test
    @DisplayName("Should handle flex-direction attribute")
    void testFlexDirectionAttribute() {
        // Given
        String input = """
            <ct-flex flex-direction="column" justify-content="center" gap="10px">
              <ct-flex-item><div>Item 1</div></ct-flex-item>
              <ct-flex-item><div>Item 2</div></ct-flex-item>
            </ct-flex>
            """;

        String expected = """
            <div class="flex flex-direction-column justify-content-center gap-10px">
              <div>Item 1</div>
              <div>Item 2</div>
            </div>
            """;

        // When
        String result = processContent(input);

        // Then
        assertEquals(normalizeWhitespace(expected), normalizeWhitespace(result),
            "Should handle flex-direction attribute correctly");
    }

    @Test
    @DisplayName("Should handle flex-wrap and align-content attributes")
    void testFlexWrapAndAlignContentAttributes() {
        // Given
        String input = """
            <ct-flex flex-wrap="wrap" align-content="space-around" gap="5px">
              <ct-flex-item><span>A</span></ct-flex-item>
              <ct-flex-item><span>B</span></ct-flex-item>
            </ct-flex>
            """;

        String expected = """
            <div class="flex flex-wrap-wrap align-content-space-around gap-5px">
              <span>A</span>
              <span>B</span>
            </div>
            """;

        // When
        String result = processContent(input);

        // Then
        assertEquals(normalizeWhitespace(expected), normalizeWhitespace(result),
            "Should handle flex-wrap and align-content attributes correctly");
    }

    @Test
    @DisplayName("Should handle custom attributes")
    void testCustomAttributes() {
        // Given
        String input = """
            <ct-flex custom-attr="custom-value" data-test="test-value">
              <ct-flex-item><span>Content</span></ct-flex-item>
            </ct-flex>
            """;

        String expected = """
            <div class="flex custom-attr-custom-value data-test-test-value">
              <span>Content</span>
            </div>
            """;

        // When
        String result = processContent(input);

        // Then
        assertEquals(normalizeWhitespace(expected), normalizeWhitespace(result),
            "Should handle custom attributes by converting them to CSS classes");
    }

    @Test
    @DisplayName("Should handle ct-flex-item with flex attribute")
    void testCtFlexItemWithFlexAttribute() {
        // Given
        String input = """
            <ct-flex justify-content="space-between">
              <ct-flex-item flex="1">
                <span>Left content</span>
              </ct-flex-item>
              <ct-flex-item flex="2">
                <ct-section>
                  <ct-section-body>
                    <ct-section-body-row>
                      <div [innerHtml]="htmlContent"></div>
                    </ct-section-body-row>
                  </ct-section-body>
                </ct-section>
              </ct-flex-item>
            </ct-flex>
            """;

        String expected = """
            <div class="flex justify-content-space-between">
              <span class="flex-1">Left content</span>
              <ct-section class="flex-2">
                <ct-section-body>
                  <ct-section-body-row>
                    <div [innerHtml]="htmlContent"></div>
                  </ct-section-body-row>
                </ct-section-body>
              </ct-section>
            </div>
            """;

        // When
        String result = processContent(input);

        // Then
        assertEquals(normalizeWhitespace(expected), normalizeWhitespace(result),
            "Should convert ct-flex-item with flex attributes to CSS classes on child elements");
    }

    @Test
    @DisplayName("Should handle ct-flex-item with multiple flex attributes")
    void testCtFlexItemWithMultipleFlexAttributes() {
        // Given
        String input = """
            <ct-flex justify-content="flex-start" gap="8px">
              <ct-flex-item flex="1" flex-shrink="0">
                <button mat-flat-button>Button 1</button>
              </ct-flex-item>
              <ct-flex-item flex="2" flex-grow="1">
                <input type="text" placeholder="Search...">
              </ct-flex-item>
            </ct-flex>
            """;

        String expected = """
            <div class="flex justify-content-flex-start gap-8px">
              <button mat-flat-button class="flex-1 flex-shrink-0">Button 1</button>
              <input type="text" placeholder="Search..." class="flex-2 flex-grow-1">
            </div>
            """;

        // When
        String result = processContent(input);

        // Then
        assertEquals(normalizeWhitespace(expected), normalizeWhitespace(result),
            "Should handle multiple flex attributes on ct-flex-item elements");
    }

    @Test
    @DisplayName("Should handle ct-flex-item with existing class attribute")
    void testCtFlexItemWithExistingClassAttribute() {
        // Given
        String input = """
            <ct-flex justify-content="center">
              <ct-flex-item flex="2">
                <div class="existing-class another-class">
                  <p>Content with existing classes</p>
                </div>
              </ct-flex-item>
            </ct-flex>
            """;

        String expected = """
            <div class="flex justify-content-center">
              <div class="existing-class another-class flex-2">
                <p>Content with existing classes</p>
              </div>
            </div>
            """;

        // When
        String result = processContent(input);

        // Then
        assertEquals(normalizeWhitespace(expected), normalizeWhitespace(result),
            "Should combine flex classes with existing class attribute");
    }

    @Test
    @DisplayName("Should handle ct-flex-item with flex-basis attribute")
    void testCtFlexItemWithFlexBasisAttribute() {
        // Given
        String input = """
            <ct-flex justify-content="space-between">
              <ct-flex-item flex-basis="200px">
                <div class="sidebar">Sidebar Content</div>
              </ct-flex-item>
              <ct-flex-item flex="1">
                <main class="main-content">Main Content</main>
              </ct-flex-item>
            </ct-flex>
            """;

        String expected = """
            <div class="flex justify-content-space-between">
              <div class="sidebar flex-basis-200px">Sidebar Content</div>
              <main class="main-content flex-1">Main Content</main>
            </div>
            """;

        // When
        String result = processContent(input);

        // Then
        assertEquals(normalizeWhitespace(expected), normalizeWhitespace(result),
            "Should handle flex-basis attribute correctly");
    }

    @Test
    @DisplayName("Should handle ct-flex-item without content elements")
    void testCtFlexItemWithTextContentOnly() {
        // Given
        String input = """
            <ct-flex justify-content="center" gap="8px">
              <ct-flex-item flex="1">
                Simple text content
              </ct-flex-item>
              <ct-flex-item flex="2">
                <span>Wrapped content</span>
              </ct-flex-item>
            </ct-flex>
            """;

        String expected = """
            <div class="flex justify-content-center gap-8px">
              Simple text content
              <span class="flex-2">Wrapped content</span>
            </div>
            """;

        // When
        String result = processContent(input);

        // Then
        assertEquals(normalizeWhitespace(expected), normalizeWhitespace(result),
            "Should handle ct-flex-item with text content and preserve flex classes on wrapped elements");
    }

    @Test
    @DisplayName("Should not modify content without ct-flex elements")
    void testContentWithoutCtFlex() {
        // Given
        String input = """
            <div class="regular-div">
              <span>Regular content</span>
              <button>Regular button</button>
            </div>
            """;

        // When
        String result = processContent(input);

        // Then
        assertEquals(input, result,
            "Should not modify content that doesn't contain ct-flex elements");
    }

    @Test
    @DisplayName("Should handle mixed content with ct-flex and regular elements")
    void testMixedContent() {
        // Given
        String input = """
            <div class="page">
              <header>
                <h1>Page Title</h1>
              </header>
              <main>
                <ct-flex justify-content="space-between" align-items="center">
                  <ct-flex-item>
                    <h2>Section Title</h2>
                  </ct-flex-item>
                  <ct-flex-item>
                    <button>Action</button>
                  </ct-flex-item>
                </ct-flex>
                <p>Regular paragraph content</p>
              </main>
              <footer>
                <ct-flex justify-content="center" gap="20px">
                  <ct-flex-item><a href="#">Link 1</a></ct-flex-item>
                  <ct-flex-item><a href="#">Link 2</a></ct-flex-item>
                </ct-flex>
              </footer>
            </div>
            """;

        // When
        String result = processContent(input);

        // Then
        assertTrue(result.contains("<header>"),
            "Should preserve regular HTML elements");
        assertTrue(result.contains("<h1>Page Title</h1>"),
            "Should preserve regular content");
        assertTrue(result.contains("<div class=\"flex justify-content-space-between align-items-center\">"),
            "Should convert first ct-flex correctly");
        assertTrue(result.contains("<div class=\"flex justify-content-center gap-20px\">"),
            "Should convert second ct-flex correctly");
        assertFalse(result.contains("ct-flex"),
            "Should not contain any ct-flex elements after migration");
        assertTrue(result.contains("<p>Regular paragraph content</p>"),
            "Should preserve regular paragraph content");
    }

    /**
     * Utility method to normalize whitespace for easier comparison in tests
     */
    private static String normalizeWhitespace(String content) {
        return content.replaceAll("\\s+", " ").trim();
    }
}
