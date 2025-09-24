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

import metaheuristic.java_version_migration.Globals;
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

    public static Content process(Migration.MigrationConfig cfg, Globals globals, String content) {
        String excludePackages = MetaUtils.getValue(globals.metas, "junitTagsExcludePackages");
        List<String> excludes = excludePackages==null || excludePackages.isBlank() ? List.of() : cache.computeIfAbsent(excludePackages, (k)-> Arrays.stream(StringUtils.split(k, ", ")).toList());
        Path base  = Path.of(MetaUtils.getValue(globals.metas, "junitSrcBase"));
        boolean b = PathUtils.isEnd(cfg.path(), excludes, base);
        if (b) {
            return new Content(content, false);
        }
        String tag = MetaUtils.getValue(globals.metas, "junitTagsName");
        if (tag==null || tag.isBlank()) {
            throw new IllegalStateException();
        }

        String strategy = MetaUtils.getValue(globals.metas, "junitTagsStrategy");
        ExistTagStrategy existTagStrategy = (strategy==null || strategy.isBlank()) ? ExistTagStrategy.SKIP : to(strategy);

        String newContent = modify(content, tag, existTagStrategy);

        Content result = new Content(newContent, !newContent.equals(content));
        return result;
    }

    public static ExistTagStrategy to(String s) {
        for (ExistTagStrategy value : ExistTagStrategy.values()) {
            if (value.name().equalsIgnoreCase(s)) {
                return value;
            }
        }
        throw new IllegalStateException("unknown strategy: " + s);
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
        * return modified content
        
        new requirement:
        * if file isn't class (declaration of class not present) then import must not be added
        
        for testing use such content
        
        ```
        @ParametersAreNonnullByDefault
        package cons411.trafaret3;
        
        import javax.annotation.ParametersAreNonnullByDefault;
        ```
        
        change in execution logic:
        * add import if it not already added to file, import must be added in import section java class
        handle a case when import was done with *, like
        
        ```
        import org.junit.jupiter.api.*;
        ```
        
        
        import for adding:
        
        ```
        import org.junit.jupiter.api.Tag;
        ```
        
        """;

    // Strategy interface
    public interface TagStrategy {
        String handleExistingTag(String content, String tag, String existingTag);
    }

    // Enum that wraps strategy method references
    public enum ExistTagStrategy {
        ADD(AddTagStrategy::handleExistingTag),
        SKIP(SkipTagStrategy::handleExistingTag),
        REPLACE(ReplaceTagStrategy::handleExistingTag),
        CHECK(CheckTagStrategy::handleExistingTag);  // New CHECK strategy for dangling Javadoc fixes

        private final TagStrategy strategy;

        ExistTagStrategy(TagStrategy strategy) {
            this.strategy = strategy;
        }

        public TagStrategy getStrategy() {
            return strategy;
        }
    }

    // Skip strategy - don't modify if @Tag already exists
    static class SkipTagStrategy {
        public static String handleExistingTag(String content, String tag, String existingTag) {
            return content; // Return unchanged
        }
    }

    // Replace strategy - replace existing @Tag with new one
    static class ReplaceTagStrategy {
        public static String handleExistingTag(String content, String tag, String existingTag) {
            String newTag = "@Tag(\"" + tag + "\")";
            return content.replace(existingTag, newTag);
        }
    }

    // Add strategy - add new @Tag alongside existing one
    static class AddTagStrategy {
        public static String handleExistingTag(String content, String tag, String existingTag) {
            // Find the position after the existing @Tag annotation
            Pattern existingTagPattern = Pattern.compile("@Tag\\s*\\([^)]*\\)", Pattern.MULTILINE);
            Matcher matcher = existingTagPattern.matcher(content);

            if (matcher.find()) {
                int insertPos = matcher.end();
                // Skip any whitespace after the existing annotation
                while (insertPos < content.length() && Character.isWhitespace(content.charAt(insertPos))) {
                    insertPos++;
                }
                String additionalTag = "@Tag(\"" + tag + "\")\n";
                return content.substring(0, insertPos) + additionalTag + content.substring(insertPos);
            }

            return content;
        }
    }

    // CHECK strategy - fixes dangling Javadoc and import placement issues
    static class CheckTagStrategy {
        public static String handleExistingTag(String content, String tag, String existingTag) {
            String result = content;
            
            // Check and fix dangling Javadoc issues caused by @Tag imports
            result = checkAndFixTagImportPlacement(result);
            
            return result;
        }
    }

    public static String modify(String content, String tag, ExistTagStrategy existTagStrategy) {
        // Special handling for CHECK strategy - it needs to run first regardless of existing annotations
        if (existTagStrategy == ExistTagStrategy.CHECK) {
            // Always apply CHECK strategy to fix placement issues
            String checkedContent = CheckTagStrategy.handleExistingTag(content, tag, null);
            return checkedContent; // Don't add new tags, just fix placement
        }
        
        return modify(content, tag, existTagStrategy.getStrategy());
    }

    public static String modify(String content, String tag, TagStrategy strategy) {
        String modifiedContent = content;

        // Check if file contains class declaration
        boolean hasClassDeclaration = hasClassDeclaration(content);

        // If using CHECK strategy, apply it first to fix dangling Javadoc issues
        if (strategy instanceof CheckTagStrategy) {
            modifiedContent = CheckTagStrategy.handleExistingTag(modifiedContent, tag, null);
        }

        // Only add import if file has class declaration and import doesn't already exist
        String importStatement = "import org.junit.jupiter.api.Tag;";
        if (hasClassDeclaration && !isTagImportPresent(content)) {
            modifiedContent = addImport(modifiedContent, importStatement);
        }

        // Handle @Tag annotation based on strategy (only for classes)
        if (hasClassDeclaration) {
            // Skip adding tag annotation for CHECK strategy unless no @Tag exists
            if (!(strategy instanceof CheckTagStrategy) || !content.contains("@Tag(")) {
                modifiedContent = addTagAnnotation(modifiedContent, tag, strategy);
            }
        }

        return modifiedContent;
    }

    // Backward compatibility - defaults to SKIP strategy
    public static String modify(String content, String tag) {
        return modify(content, tag, ExistTagStrategy.SKIP);
    }

    private static boolean isTagImportPresent(String content) {
        // Check for specific Tag import
        Pattern specificImportPattern = Pattern.compile("import\\s+org\\.junit\\.jupiter\\.api\\.Tag\\s*;", Pattern.MULTILINE);
        if (specificImportPattern.matcher(content).find()) {
            return true;
        }

        // Check for wildcard import that covers Tag
        Pattern wildcardImportPattern = Pattern.compile("import\\s+org\\.junit\\.jupiter\\.api\\.\\*\\s*;", Pattern.MULTILINE);
        if (wildcardImportPattern.matcher(content).find()) {
            return true;
        }

        return false;
    }

    private static boolean hasClassDeclaration(String content) {
        // Pattern to find class, interface, or enum declaration
        // Matches: [annotations] [modifiers] class/interface/enum ClassName
        Pattern classPattern = Pattern.compile(
            "(?:@\\w+(?:\\([^)]*\\))?\\s*)*" +  // Optional annotations
            "(?:(?:public|private|protected|abstract|final|static)\\s+)*" +  // Optional modifiers (zero or more)
            "(?:class|interface|enum)\\s+\\w+",  // class/interface/enum declaration
            Pattern.MULTILINE
        );

        return classPattern.matcher(content).find();
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

    private static String addTagAnnotation(String content, String tag, TagStrategy strategy) {
        // Check if @Tag annotation already exists
        Pattern existingTagPattern = Pattern.compile("@Tag\\s*\\([^)]*\\)", Pattern.MULTILINE);
        Matcher existingTagMatcher = existingTagPattern.matcher(content);

        if (existingTagMatcher.find()) {
            // @Tag annotation already exists, delegate to strategy
            String existingTag = existingTagMatcher.group();
            return strategy.handleExistingTag(content, tag, existingTag);
        }

        // No existing @Tag annotation, add new one
        // Pattern to find class declaration
        // Matches: [annotations] [modifiers] class ClassName
        Pattern classPattern = Pattern.compile(
            "((?:@\\w+(?:\\([^)]*\\))?\\s*)*)" +  // Capture existing annotations
            "((?:(?:public|private|protected|abstract|final|static)\\s+)*)" +  // Modifiers
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

    /**
     * Information about class-level Javadoc location
     */
    private static class ClassJavadocInfo {
        private final int startPosition;
        private final int endPosition;
        private final String javadocContent;
        private final int classDeclarationStart;
        
        public ClassJavadocInfo(int startPosition, int endPosition, String javadocContent, int classDeclarationStart) {
            this.startPosition = startPosition;
            this.endPosition = endPosition;
            this.javadocContent = javadocContent;
            this.classDeclarationStart = classDeclarationStart;
        }
        
        public int getStartPosition() { return startPosition; }
        public int getEndPosition() { return endPosition; }
        public String getJavadocContent() { return javadocContent; }
        public int getClassDeclarationStart() { return classDeclarationStart; }
    }
    
    /**
     * Check and fix @Tag import placement to prevent dangling Javadoc comments
     * Ensures @Tag imports are placed before class-level Javadoc
     */
    private static String checkAndFixTagImportPlacement(String content) {
        // Find @Tag import
        String tagImportPattern = "import\\s+org\\.junit\\.jupiter\\.api\\.Tag\\s*;";
        Pattern tagPattern = Pattern.compile(tagImportPattern);
        Matcher tagMatcher = tagPattern.matcher(content);
        
        if (!tagMatcher.find()) {
            return content; // No @Tag import found
        }
        
        String tagImportStatement = tagMatcher.group().trim();
        
        // Find class-level Javadoc
        ClassJavadocInfo javadocInfo = findClassLevelJavadoc(content);
        if (javadocInfo == null) {
            return content; // No class-level Javadoc found
        }
        
        // Check if @Tag import is after Javadoc start (which creates dangling Javadoc)
        int tagImportPosition = tagMatcher.start();
        int javadocPosition = javadocInfo.getStartPosition();
        
        if (tagImportPosition > javadocPosition) {
            // @Tag import is after Javadoc start - this creates dangling Javadoc
            // Remove the existing @Tag import from its current position
            String contentWithoutTagImport = content.substring(0, tagMatcher.start()) + 
                                             content.substring(tagMatcher.end());
            
            // Find appropriate place to insert @Tag import before Javadoc
            String beforeJavadoc = contentWithoutTagImport.substring(0, javadocInfo.getStartPosition());
            String afterJavadoc = contentWithoutTagImport.substring(javadocInfo.getStartPosition());
            
            // Find the best insertion point for the import
            String insertionPoint = findTagImportInsertionPoint(beforeJavadoc);
            String remainingBefore = beforeJavadoc.substring(insertionPoint.length());
            
            // Reconstruct the content with proper import placement
            StringBuilder result = new StringBuilder();
            result.append(insertionPoint);
            
            if (!insertionPoint.isEmpty() && !insertionPoint.endsWith("\n")) {
                result.append("\n");
            }
            result.append(tagImportStatement).append("\n");
            
            if (!remainingBefore.trim().isEmpty()) {
                if (!remainingBefore.startsWith("\n")) {
                    result.append("\n");
                }
                result.append(remainingBefore);
            }
            
            result.append(afterJavadoc);
            
            return result.toString();
        }
        
        return content; // Import placement is already correct
    }
    
    /**
     * Find class-level Javadoc comment
     */
    private static ClassJavadocInfo findClassLevelJavadoc(String content) {
        // First find all Javadoc comments so we can exclude class matches inside them
        java.util.List<int[]> javadocRanges = new java.util.ArrayList<>();
        Pattern javadocPattern = Pattern.compile("/\\*\\*([\\s\\S]*?)\\*/", Pattern.MULTILINE | Pattern.DOTALL);
        Matcher javadocMatcher = javadocPattern.matcher(content);
        
        while (javadocMatcher.find()) {
            javadocRanges.add(new int[]{javadocMatcher.start(), javadocMatcher.end()});
        }
        
        // Then find class declaration that's NOT inside any Javadoc comment
        Pattern classPattern = Pattern.compile(
            "((?:(?:@\\w+(?:\\([^)]*\\))?\\s*)*)?(?:public|private|protected)?\\s*(?:abstract|final)?\\s*class\\s+\\w+)",
            Pattern.MULTILINE
        );
        Matcher classMatcher = classPattern.matcher(content);
        
        int classStart = -1;
        while (classMatcher.find()) {
            int candidateClassStart = classMatcher.start();
            
            // Check if this class declaration is inside any Javadoc
            boolean insideJavadoc = false;
            for (int[] range : javadocRanges) {
                if (candidateClassStart >= range[0] && candidateClassStart <= range[1]) {
                    insideJavadoc = true;
                    break;
                }
            }
            
            if (!insideJavadoc) {
                classStart = candidateClassStart;
                break; // Take the first valid class declaration
            }
        }
        
        if (classStart == -1) {
            return null; // No class declaration found
        }
        
        // Now find the Javadoc comment that comes before this class
        ClassJavadocInfo bestMatch = null;
        javadocMatcher.reset(); // Reset for reuse
        
        while (javadocMatcher.find()) {
            int javadocStart = javadocMatcher.start();
            int javadocEnd = javadocMatcher.end();
            
            if (javadocEnd < classStart) {
                // This Javadoc comes before the class, check if it's likely the class-level Javadoc
                String betweenJavadocAndClass = content.substring(javadocEnd, classStart);
                
                if (javadocEnd < classStart) {
                    // It's class-level Javadoc if the content between contains only:
                    // - whitespace, imports, and annotations
                    boolean isClassJavadoc = betweenJavadocAndClass.matches("(?s)\\s*(?:import\\s+[^;]+;\\s*|@\\w+(?:\\([^)]*\\))?\\s*)*");
                    
                    if (isClassJavadoc) {
                        String javadocContent = javadocMatcher.group(1);
                        bestMatch = new ClassJavadocInfo(javadocStart, javadocEnd, javadocContent, classStart);
                        // Keep looking for a later Javadoc that's closer to the class
                    }
                }
            }
        }
        
        return bestMatch;
    }
    
    /**
     * Fix dangling Javadoc caused by @Tag import placement
     */
    private static String fixDanglingJavadocFromTagImport(String content, String tagImportStatement, ClassJavadocInfo javadocInfo) {
        StringBuilder result = new StringBuilder();
        
        // Get content before Javadoc
        String beforeJavadoc = content.substring(0, javadocInfo.getStartPosition());
        
        // Get content after class declaration
        String afterClassDeclaration = content.substring(javadocInfo.getClassDeclarationStart());
        
        // Get the Javadoc and class declaration (without the misplaced @Tag import)
        String javadocAndClass = content.substring(javadocInfo.getStartPosition(), javadocInfo.getClassDeclarationStart());
        
        // Remove @Tag import from the middle section
        javadocAndClass = javadocAndClass.replaceAll("\\s*import\\s+org\\.junit\\.jupiter\\.api\\.Tag\\s*;\\s*", "");
        
        // Find appropriate place to insert @Tag import before Javadoc
        String insertionPoint = findTagImportInsertionPoint(beforeJavadoc);
        
        // Reconstruct the content
        result.append(insertionPoint);
        if (!insertionPoint.endsWith("\n")) {
            result.append("\n");
        }
        result.append(tagImportStatement);
        result.append("\n");
        
        // Add remaining before-javadoc content (after the insertion point)
        String remainingBeforeJavadoc = beforeJavadoc.substring(insertionPoint.length());
        if (!remainingBeforeJavadoc.trim().isEmpty()) {
            result.append(remainingBeforeJavadoc);
            if (!remainingBeforeJavadoc.endsWith("\n")) {
                result.append("\n");
            }
        }
        
        // Add Javadoc and class declaration
        result.append(javadocAndClass);
        
        // Add the rest of the content
        result.append(afterClassDeclaration);
        
        return result.toString();
    }
    
    /**
     * Find the appropriate insertion point for @Tag imports
     */
    private static String findTagImportInsertionPoint(String beforeJavadocContent) {
        // Find the last import statement
        Pattern lastImportPattern = Pattern.compile("(.*import\\s+[^;]+;)", Pattern.DOTALL);
        Matcher matcher = lastImportPattern.matcher(beforeJavadocContent);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // If no imports found, find package declaration
        Pattern packagePattern = Pattern.compile("(package\\s+[^;]+;)", Pattern.DOTALL);
        matcher = packagePattern.matcher(beforeJavadocContent);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // If no package declaration, return beginning of content
        return "";
    }
    
    /**
     * Check for dangling Javadoc issues in the content
     */
    public static java.util.List<String> detectDanglingJavadocIssues(String content) {
        java.util.List<String> issues = new java.util.ArrayList<>();
        
        // Find all Javadoc comments
        Pattern javadocPattern = Pattern.compile("/\\*\\*[\\s\\S]*?\\*/");
        Matcher javadocMatcher = javadocPattern.matcher(content);
        
        while (javadocMatcher.find()) {
            int javadocEnd = javadocMatcher.end();
            
            // Check what follows the Javadoc comment
            String afterJavadoc = content.substring(javadocEnd).trim();
            
            // Check if it's followed by an import (which would make it dangling)
            if (afterJavadoc.startsWith("import ")) {
                issues.add("Dangling Javadoc at position " + javadocMatcher.start() + 
                          " - followed by import statement instead of declaration");
            }
        }
        
        return issues;
    }


}
