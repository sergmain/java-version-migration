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
    void testModify_NoChanges_WhenFileIsNotClass() {
        // Given - package-info.java content with no class declaration
        String originalContent = """
            @ParametersAreNonnullByDefault
            package cons411.trafaret3;
            
            import javax.annotation.ParametersAreNonnullByDefault;
            """;

        String tag = "integration";

        // When
        String result = modify(originalContent, tag);

        // Then
        assertEquals(originalContent, result,
            "Content should remain unchanged when file contains no class declaration");

        // Verify no Tag import was added
        assertFalse(result.contains("import org.junit.jupiter.api.Tag;"),
            "Should not add Tag import when file has no class declaration");

        // Verify no @Tag annotation was added
        assertFalse(result.contains("@Tag(\"integration\")"),
            "Should not add @Tag annotation when file has no class declaration");

        // Verify original content is preserved
        assertTrue(result.contains("@ParametersAreNonnullByDefault"),
            "Should preserve existing annotations");
        assertTrue(result.contains("package cons411.trafaret3;"),
            "Should preserve package declaration");
        assertTrue(result.contains("import javax.annotation.ParametersAreNonnullByDefault;"),
            "Should preserve existing imports");
    }

    @Test
    void testModify_AddsOnlyTagAnnotation_WhenWildcardImportExists() {
        // Given
        String originalContent = """
            package com.example.test;
            
            import java.util.List;
            import org.junit.jupiter.api.*;
            
            public class WildcardImportTest {
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

        // Verify that specific Tag import was NOT added (since wildcard covers it)
        assertFalse(result.contains("import org.junit.jupiter.api.Tag;"),
            "Should not add specific Tag import when wildcard import exists");

        // Verify wildcard import is preserved
        assertTrue(result.contains("import org.junit.jupiter.api.*;"),
            "Should preserve wildcard import");

        // Verify annotation is before class declaration
        int annotationIndex = result.indexOf("@Tag(\"unit\")");
        int classIndex = result.indexOf("public class WildcardImportTest");
        assertTrue(annotationIndex < classIndex,
            "@Tag annotation should be before class declaration");

        // Verify existing imports are preserved
        assertTrue(result.contains("import java.util.List;"),
            "Should preserve existing imports");
    }

    @Test
    @DisplayName("Should return true when file is in excluded package directory")
    void test_exclude_as_root_package() {
        // Given
        Path srcPath = Paths.get("src/main/java/com/example");
        Path path = Paths.get("src/main/java/com/example/excluded/a/b/c/file.txt");
        List<String> excludePackages = Arrays.asList("excluded.a");

        // When
        boolean result = PathUtils.isEnd(path, excludePackages, srcPath);

        // Then
        assertTrue(result, "Should return true when file is in 'excluded.a' package directory");
    }

    @Test
    @DisplayName("Should handle multiple exclude packages with dot notation")
    void shouldHandleMultipleExcludePackagesWithDotNotation() {
        // Given
        Path srcPath = Paths.get("src/main/java/com/example");
        List<String> excludePackages = Arrays.asList("excluded.a", "test.utils", "temp.files");

        // Test file in excluded.a package
        Path path1 = Paths.get("src/main/java/com/example/excluded/a/SomeClass.java");
        boolean result1 = PathUtils.isEnd(path1, excludePackages, srcPath);
        assertTrue(result1, "Should return true for file in 'excluded.a' package");

        // Test file in test.utils package
        Path path2 = Paths.get("src/main/java/com/example/test/utils/TestUtil.java");
        boolean result2 = PathUtils.isEnd(path2, excludePackages, srcPath);
        assertTrue(result2, "Should return true for file in 'test.utils' package");

        // Test file in allowed package
        Path path3 = Paths.get("src/main/java/com/example/allowed/MyClass.java");
        boolean result3 = PathUtils.isEnd(path3, excludePackages, srcPath);
        assertFalse(result3, "Should return false for file not in any excluded package");
    }

    @Test
    @DisplayName("Should handle edge cases with new srcPath parameter")
    void shouldHandleEdgeCasesWithSrcPath() {
        // Test null srcPath
        Path path = Paths.get("src/main/java/excluded/MyClass.java");
        List<String> excludePackages = Arrays.asList("excluded");
        boolean result1 = PathUtils.isEnd(path, excludePackages, null);
        assertFalse(result1, "Should return false when srcPath is null");

        // Test file not under srcPath
        Path srcPath = Paths.get("src/main/java/com/example");
        Path pathOutsideSrc = Paths.get("different/path/excluded/MyClass.java");
        boolean result2 = PathUtils.isEnd(pathOutsideSrc, excludePackages, srcPath);
        assertFalse(result2, "Should return false when file is not under srcPath");

        // Test exact match (file directly in excluded directory)
        Path exactPath = Paths.get("src/main/java/com/example/excluded/DirectFile.java");
        List<String> simpleExclude = Arrays.asList("excluded");
        boolean result3 = PathUtils.isEnd(exactPath, simpleExclude, srcPath);
        assertTrue(result3, "Should return true for file directly in excluded directory");
    }
}
