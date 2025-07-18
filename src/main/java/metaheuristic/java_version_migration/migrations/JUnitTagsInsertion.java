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

import metaheuristic.java_version_migration.Migration;
import metaheuristic.java_version_migration.data.Content;
import metaheuristic.java_version_migration.utils.MetaUtils;
import metaheuristic.java_version_migration.utils.PathUtils;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Sergio Lissner
 * Date: 7/18/2025
 * Time: 2:32 PM
 */
public class JUnitTagsInsertion {

    private static Map<String, List<String>> cache = new HashMap<>();

    public static Content process(Migration.MigrationConfig cfg, String content) {
        String excludePackages = MetaUtils.getValue(cfg.globals().metas, "junitTagsExcludePackages");
        List<String> excludes = excludePackages==null || excludePackages.isBlank() ? List.of() : cache.computeIfAbsent(excludePackages, (k)-> Arrays.stream(StringUtils.split(k)).toList());
        boolean b = PathUtils.isEnd(cfg.path(), excludes);
        if (b) {
            return new Content(content, false);
        }
        String tag = MetaUtils.getValue(cfg.globals().metas, "junitTagsName");
        if (tag==null || tag.isBlank()) {
            throw new IllegalStateException();
        }

        String newContent = modify(content, tag);

        Content result = new Content(newContent, !newContent.equals(content));
        return result;
    }

    private static String claude_modify = """
        java
        write method
        public static String modify(String content, String tag)
        
        string content is a java class
        requirements:\s
        * add import if it not already added to file, import must be added in import section java class
        
        ```
        import org.junit.jupiter.api.Tag;
        ```
        
        * find declaration of class and check a presence of annotation @Tag
        * if @Tag is absent add new annotation @Tag(tag) there tag is variable
        * return modified content""";

    public static String modify(String content, String tag) {
        String modifiedContent = content;

        // Check if import already exists
        String importStatement = "import org.junit.jupiter.api.Tag;";
        if (!content.contains(importStatement)) {
            modifiedContent = addImport(modifiedContent, importStatement);
        }

        // Check if @Tag annotation already exists and add if missing
        modifiedContent = addTagAnnotation(modifiedContent, tag);

        return modifiedContent;
    }

    private static String addImport(String content, String importStatement) {
        // Pattern to find the location where imports should be added
        // Look for package declaration first, then existing imports, then class declaration

        // Find package declaration
        Pattern packagePattern = Pattern.compile("(package\\s+[^;]+;\\s*)", Pattern.MULTILINE);
        Matcher packageMatcher = packagePattern.matcher(content);

        // Find existing imports
        Pattern importPattern = Pattern.compile("((?:import\\s+[^;]+;\\s*)*)", Pattern.MULTILINE);

        if (packageMatcher.find()) {
            // If package exists, add import after package and existing imports
            String afterPackage = content.substring(packageMatcher.end());
            Matcher importMatcher = importPattern.matcher(afterPackage);

            if (importMatcher.find() && importMatcher.start() == 0) {
                // There are existing imports, add after them
                int insertPos = packageMatcher.end() + importMatcher.end();
                return content.substring(0, insertPos) + importStatement + "\n" + content.substring(insertPos);
            } else {
                // No existing imports, add after package
                int insertPos = packageMatcher.end();
                return content.substring(0, insertPos) + "\n" + importStatement + "\n" + content.substring(insertPos);
            }
        } else {
            // No package declaration, add import at the beginning
            Matcher importMatcher = importPattern.matcher(content);
            if (importMatcher.find() && importMatcher.start() == 0) {
                // There are existing imports at the start, add after them
                int insertPos = importMatcher.end();
                return content.substring(0, insertPos) + importStatement + "\n" + content.substring(insertPos);
            } else {
                // No imports at start, add at the very beginning
                return importStatement + "\n\n" + content;
            }
        }
    }

    private static String addTagAnnotation(String content, String tag) {
        // Check if @Tag annotation already exists
        Pattern existingTagPattern = Pattern.compile("@Tag\\s*\\([^)]*\\)", Pattern.MULTILINE);
        if (existingTagPattern.matcher(content).find()) {
            // @Tag annotation already exists, return content unchanged
            return content;
        }

        // Pattern to find class declaration
        // Matches: [annotations] [modifiers] class ClassName
        Pattern classPattern = Pattern.compile(
            "((?:@\\w+(?:\\([^)]*\\))?\\s*)*)" +  // Capture existing annotations
            "((?:public|private|protected|abstract|final|static)\\s+)*" +  // Modifiers
            "(class|interface|enum)\\s+\\w+",  // class/interface/enum declaration
            Pattern.MULTILINE
        );

        Matcher classMatcher = classPattern.matcher(content);

        if (classMatcher.find()) {
            String existingAnnotations = classMatcher.group(1);
            int classStart = classMatcher.start();

            // Add @Tag annotation before the class declaration
            String tagAnnotation = "@Tag(\"" + tag + "\")\n";

            if (existingAnnotations != null && !existingAnnotations.trim().isEmpty()) {
                // There are existing annotations, add @Tag after them
                return content.substring(0, classStart) +
                       existingAnnotations +
                       tagAnnotation +
                       content.substring(classStart + existingAnnotations.length());
            } else {
                // No existing annotations, add @Tag before class declaration
                return content.substring(0, classStart) +
                       tagAnnotation +
                       content.substring(classStart);
            }
        }

        // If no class declaration found, return original content
        return content;
    }
}
