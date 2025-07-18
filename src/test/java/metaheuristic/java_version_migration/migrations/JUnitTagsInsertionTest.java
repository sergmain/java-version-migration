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

import metaheuristic.java_version_migration.utils.PathUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static metaheuristic.java_version_migration.migrations.JUnitTagsInsertion.modify;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

/**
 * @author Sergio Lissner
 * Date: 7/18/2025
 * Time: 3:08 PM
 */
@Execution(CONCURRENT)
class JUnitTagsInsertionTest {

    @Test
    @DisplayName("Should return true when path ends with excluded package")
    void shouldReturnTrueWhenPathEndsWithExcludedPackage() {
        // Given
        Path path = Paths.get("src/main/java/com/example/excluded/file.txt");
        List<String> excludePackages = Arrays.asList("excluded", "test", "temp");

        // When
        boolean result = PathUtils.isEnd(path, excludePackages);

        // Then
        assertTrue(result, "Should return true when path ends with 'excluded' package");

        // Test with different excluded package
        Path path2 = Paths.get("project/modules/test/file.txt");
        boolean result2 = PathUtils.isEnd(path2, excludePackages);
        assertTrue(result2, "Should return true when path ends with 'test' package");

        // Test with path separators (Windows style)
        Path path3 = Paths.get("src\\main\\java\\com\\example\\temp\\file.txt");
        boolean result3 = PathUtils.isEnd(path3, excludePackages);
        assertTrue(result3, "Should return true regardless of path separator style");
    }

    @Test
    @DisplayName("Should return false when path does not end with any excluded package")
    void shouldReturnFalseWhenPathDoesNotEndWithExcludedPackage() {
        // Given
        Path path = Paths.get("src/main/java/com/example/allowed");
        List<String> excludePackages = Arrays.asList("excluded", "test", "temp");

        // When
        boolean result = PathUtils.isEnd(path, excludePackages);

        // Then
        assertFalse(result, "Should return false when path doesn't end with any excluded package");

        // Test partial match (should not match)
        Path path2 = Paths.get("src/main/java/com/example/excludedPackage");
        boolean result2 = PathUtils.isEnd(path2, excludePackages);
        assertFalse(result2, "Should return false for partial matches that don't end exactly");

        // Test when path contains excluded package but doesn't end with it
        Path path3 = Paths.get("src/excluded/main/java/allowed");
        boolean result3 = PathUtils.isEnd(path3, excludePackages);
        assertFalse(result3, "Should return false when excluded package is in middle of path");
    }

    @Test
    @DisplayName("Should handle edge cases with null and empty inputs")
    void shouldHandleEdgeCasesWithNullAndEmptyInputs() {
        // Test null path
        List<String> excludePackages = Arrays.asList("excluded", "test");
        boolean result1 = PathUtils.isEnd(null, excludePackages);
        assertFalse(result1, "Should return false when path is null");

        // Test null excludePackages
        Path path = Paths.get("src/main/java/excluded/file.txt");
        boolean result2 = PathUtils.isEnd(path, null);
        assertFalse(result2, "Should return false when excludePackages is null");

        // Test empty excludePackages list
        boolean result3 = PathUtils.isEnd(path, Collections.emptyList());
        assertFalse(result3, "Should return false when excludePackages is empty");

        // Test excludePackages with null and empty strings
        List<String> mixedPackages = Arrays.asList("excluded", null, "", "test");
        Path validPath = Paths.get("src/main/java/excluded/file.txt");
        boolean result4 = PathUtils.isEnd(validPath, mixedPackages);
        assertTrue(result4, "Should handle null and empty strings in excludePackages and still find valid matches");

        // Test both null
        boolean result5 = PathUtils.isEnd(null, null);
        assertFalse(result5, "Should return false when both parameters are null");
    }

    @Test
    void testModify_AddsImportAndTagAnnotation_WhenBothMissing() {
        // Given
        String originalContent = """
            package com.example.test;
            
            import java.util.List;
            
            public class TestClass {
                public void testMethod() {
                    // test implementation
                }
            }
            """;

        String tag = "integration";

        // When
        String result = modify(originalContent, tag);

        // Then
        assertTrue(result.contains("import org.junit.jupiter.api.Tag;"),
            "Should add Tag import");
        assertTrue(result.contains("@Tag(\"integration\")"),
            "Should add @Tag annotation with correct tag value");
        assertTrue(result.contains("import java.util.List;"),
            "Should preserve existing imports");
        assertTrue(result.contains("public class TestClass"),
            "Should preserve class declaration");

        // Verify import is in correct location (after existing imports)
        int tagImportIndex = result.indexOf("import org.junit.jupiter.api.Tag;");
        int listImportIndex = result.indexOf("import java.util.List;");
        assertTrue(tagImportIndex > listImportIndex,
            "Tag import should be after existing imports");

        // Verify annotation is before class declaration
        int annotationIndex = result.indexOf("@Tag(\"integration\")");
        int classIndex = result.indexOf("public class TestClass");
        assertTrue(annotationIndex < classIndex,
            "@Tag annotation should be before class declaration");
    }

    @Test
    void testModify_AddsOnlyTagAnnotation_WhenImportExists() {
        // Given
        String originalContent = """
            package com.example.test;
            
            import java.util.List;
            import org.junit.jupiter.api.Tag;
            import org.junit.jupiter.api.Test;
            
            public class ExistingImportTest {
                @Test
                public void someTest() {
                    // test implementation
                }
            }
            """;

        String tag = "unit";

        // When
        String result = modify(originalContent, tag);

        // Then
        assertTrue(result.contains("@Tag(\"unit\")"),
            "Should add @Tag annotation with correct tag value");

        // Verify that Tag import appears only once
        int firstImportIndex = result.indexOf("import org.junit.jupiter.api.Tag;");
        int lastImportIndex = result.lastIndexOf("import org.junit.jupiter.api.Tag;");
        assertEquals(firstImportIndex, lastImportIndex,
            "Tag import should appear only once");

        // Verify annotation is before class declaration
        int annotationIndex = result.indexOf("@Tag(\"unit\")");
        int classIndex = result.indexOf("public class ExistingImportTest");
        assertTrue(annotationIndex < classIndex,
            "@Tag annotation should be before class declaration");

        // Verify existing imports are preserved
        assertTrue(result.contains("import java.util.List;"),
            "Should preserve existing imports");
        assertTrue(result.contains("import org.junit.jupiter.api.Test;"),
            "Should preserve existing imports");
    }

    @Test
    void testModify_NoChanges_WhenImportAndTagAnnotationExist() {
        // Given
        String originalContent = """
            package com.example.test;
            
            import java.util.List;
            import org.junit.jupiter.api.Tag;
            import org.junit.jupiter.api.Test;
            
            @Tag("existing")
            public class AlreadyTaggedTest {
                @Test
                public void someTest() {
                    // test implementation
                }
            }
            """;

        String tag = "newTag";

        // When
        String result = modify(originalContent, tag);

        // Then
        assertEquals(originalContent, result,
            "Content should remain unchanged when import and @Tag annotation already exist");

        // Verify existing tag is preserved and new tag is not added
        assertTrue(result.contains("@Tag(\"existing\")"),
            "Should preserve existing @Tag annotation");
        assertFalse(result.contains("@Tag(\"newTag\")"),
            "Should not add new @Tag annotation when one already exists");

        // Verify that Tag import appears only once
        int firstImportIndex = result.indexOf("import org.junit.jupiter.api.Tag;");
        int lastImportIndex = result.lastIndexOf("import org.junit.jupiter.api.Tag;");
        assertEquals(firstImportIndex, lastImportIndex,
            "Tag import should appear only once");
    }

    @Test
    @DisplayName("Should handle Windows drive letters and backslash separators")
    void shouldHandleWindowsDriveLettersAndBackslashSeparators() {
        // Given
        List<String> excludePackages = Arrays.asList("excluded", "test", "temp");

        // Test with C: drive and backslashes
        Path windowsFilePath1 = Paths.get("C:\\Users\\Developer\\Projects\\src\\main\\java\\excluded\\MyClass.java");
        boolean result1 = PathUtils.isEnd(windowsFilePath1, excludePackages);
        assertTrue(result1, "Should return true for Windows file with C: drive in 'excluded' directory");

        // Test with D: drive and backslashes
        Path windowsFilePath2 = Paths.get("D:\\workspace\\project\\modules\\test\\TestClass.java");
        boolean result2 = PathUtils.isEnd(windowsFilePath2, excludePackages);
        assertTrue(result2, "Should return true for Windows file with D: drive in 'test' directory");

        // Test mixed separators (backslash and forward slash)
        Path mixedFilePath = Paths.get("C:\\Projects/src\\main/java\\temp\\TempFile.java");
        boolean result3 = PathUtils.isEnd(mixedFilePath, excludePackages);
        assertTrue(result3, "Should handle mixed path separators on Windows");

        // Test file that is not in excluded package directory
        Path nonMatchingFilePath = Paths.get("C:\\Users\\Developer\\Projects\\src\\main\\java\\allowed\\MyClass.java");
        boolean result4 = PathUtils.isEnd(nonMatchingFilePath, excludePackages);
        assertFalse(result4, "Should return false when Windows file is not in excluded package directory");
    }

    @Test
    @DisplayName("Should handle UNC paths and network shares")
    void shouldHandleUNCPathsAndNetworkShares() {
        // Given
        List<String> excludePackages = Arrays.asList("excluded", "test", "shared");

        // Test UNC path with server and share
        Path uncFilePath1 = Paths.get("\\\\server\\share\\projects\\src\\excluded\\MyClass.java");
        boolean result1 = PathUtils.isEnd(uncFilePath1, excludePackages);
        assertTrue(result1, "Should return true for UNC file in 'excluded' directory");

        // Test UNC path with nested folders
        Path uncFilePath2 = Paths.get("\\\\fileserver\\development\\workspace\\modules\\test\\TestFile.java");
        boolean result2 = PathUtils.isEnd(uncFilePath2, excludePackages);
        assertTrue(result2, "Should return true for UNC file in 'test' directory");

        // Test when UNC path contains excluded package in share name but file is not in excluded directory
        Path uncFilePath3 = Paths.get("\\\\server\\shared\\projects\\src\\allowed\\MyClass.java");
        boolean result3 = PathUtils.isEnd(uncFilePath3, excludePackages);
        assertFalse(result3, "Should return false when UNC file has excluded package in share name but is not in excluded directory");

        // Test UNC path where file is in directory that ends with excluded package matching share name
        Path uncFilePath4 = Paths.get("\\\\server\\documents\\shared\\MyFile.txt");
        boolean result4 = PathUtils.isEnd(uncFilePath4, excludePackages);
        assertTrue(result4, "Should return true when UNC file is in directory ending with excluded package name");
    }

    @Test
    @DisplayName("Should handle Windows case sensitivity and long paths")
    void shouldHandleWindowsCaseSensitivityAndLongPaths() {
        // Given - test case insensitivity (Windows file system is case-insensitive)
        List<String> excludePackages = Arrays.asList("EXCLUDED", "Test", "temp");

        // Test lowercase file with uppercase excluded package
        Path lowerCaseFilePath = Paths.get("C:\\projects\\src\\main\\java\\excluded\\MyClass.java");
        boolean result1 = PathUtils.isEnd(lowerCaseFilePath, excludePackages);
        // Note: This depends on how the method handles case sensitivity
        // The current implementation is case-sensitive, but we test the behavior
        assertFalse(result1, "Current implementation is case-sensitive - 'excluded' != 'EXCLUDED'");

        // Test mixed case file with mixed case excluded package
        Path mixedCaseFilePath = Paths.get("C:\\Projects\\Src\\Main\\Java\\Test\\TestClass.java");
        boolean result2 = PathUtils.isEnd(mixedCaseFilePath, excludePackages);
        assertTrue(result2, "Should return true for exact case match");

        // Test very long Windows file path
        String longPathSegment = "VeryLongDirectoryNameThatExceedsTypicalLengthLimitsForTestingPurposes";
        Path longFilePath = Paths.get("C:\\Users\\Developer\\Projects\\" + longPathSegment +
                                      "\\src\\main\\java\\com\\example\\very\\deep\\nested\\structure\\temp\\VeryLongFileName.java");
        boolean result3 = PathUtils.isEnd(longFilePath, excludePackages);
        assertTrue(result3, "Should handle very long Windows file paths correctly");

        // Test absolute vs relative file path behavior
        Path relativeFilePath = Paths.get("..\\..\\src\\excluded\\MyClass.java");
        boolean result4 = PathUtils.isEnd(relativeFilePath, Arrays.asList("excluded"));
        assertTrue(result4, "Should handle relative Windows file paths with parent directory references");

        // Test file path with spaces (common in Windows)
        Path filePathWithSpaces = Paths.get("C:\\Program Files\\My Application\\src\\main\\excluded\\MyClass.java");
        boolean result5 = PathUtils.isEnd(filePathWithSpaces, Arrays.asList("excluded"));
        assertTrue(result5, "Should handle Windows file paths with spaces correctly");
    }
}
