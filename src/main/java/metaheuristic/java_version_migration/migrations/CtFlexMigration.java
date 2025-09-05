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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Migration tool to convert Angular ct-flex components to div elements with CSS classes
 * 
 * @author Sergio Lissner
 * Date: 9/4/2025
 * Time: 4:57 PM
 */
public class CtFlexMigration {

    public static Content process(Migration.MigrationConfig cfg, String content) {
        String newContent = migrateCtFlexToDiv(content);
        Content result = new Content(newContent, !newContent.equals(content));
        return result;
    }

    /**
     * Main migration method that converts ct-flex components to div elements
     * Uses a simpler approach that handles nesting reliably
     */
    public static String migrateCtFlexToDiv(String content) {
        String result = content;
        
        // Keep processing until no more changes are made
        boolean hasChanges = true;
        while (hasChanges) {
            String previousResult = result;
            
            // First unwrap all ct-flex-item elements
            result = processCtFlexItems(result);
            
            // Then convert one level of ct-flex elements to div
            result = processOneCtFlexLevel(result);
            
            hasChanges = !result.equals(previousResult);
        }
        
        return result;
    }

    /**
     * Process a single ct-flex element (the first one found)
     */
    private static String processOneCtFlexLevel(String content) {
        // Simple pattern to match any ct-flex element
        Pattern ctFlexPattern = Pattern.compile(
            "<ct-flex([^>]*?)>(.*?)</ct-flex>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher matcher = ctFlexPattern.matcher(content);
        
        if (matcher.find()) {
            String attributes = matcher.group(1);
            String innerContent = matcher.group(2);
            String divElement = convertCtFlexToDiv(attributes, innerContent);
            
            // Replace only the first match
            return content.substring(0, matcher.start()) + 
                   divElement + 
                   content.substring(matcher.end());
        }
        
        return content; // No ct-flex elements found
    }
    /**
     * Process and unwrap ct-flex-item elements within a given content string
     * Also handles ct-flex-item attributes by converting them to CSS classes on child elements
     */
    private static String processCtFlexItems(String content) {
        // Pattern to match ct-flex-item elements with their attributes and content
        Pattern ctFlexItemPattern = Pattern.compile(
            "<ct-flex-item([^>]*?)>([\\s\\S]*?)</ct-flex-item>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher matcher = ctFlexItemPattern.matcher(content);
        StringBuilder result = new StringBuilder();
        int lastEnd = 0;
        
        while (matcher.find()) {
            // Append content before this match
            result.append(content, lastEnd, matcher.start());
            
            // Extract attributes and inner content
            String attributes = matcher.group(1);
            String innerContent = matcher.group(2);
            
            // Process the ct-flex-item
            String processedContent = processCtFlexItem(attributes, innerContent);
            result.append(processedContent);
            
            lastEnd = matcher.end();
        }
        
        // Append remaining content
        result.append(content.substring(lastEnd));
        
        return result.toString();
    }

    /**
     * Process a single ct-flex-item element with its attributes and content
     */
    private static String processCtFlexItem(String attributes, String innerContent) {
        // Parse ct-flex-item attributes to extract CSS classes
        List<String> flexCssClasses = parseCtFlexItemAttributes(attributes);
        
        // Unwrap the content while preserving proper indentation
        String unwrappedContent = unwrapFlexItemContent(innerContent);
        
        // If there are flex CSS classes to apply, add them to the first child element
        if (!flexCssClasses.isEmpty()) {
            unwrappedContent = addCssClassesToFirstElement(unwrappedContent, flexCssClasses);
        }
        
        return unwrappedContent;
    }

    /**
     * Parse ct-flex-item attributes and convert them to CSS class names
     */
    private static List<String> parseCtFlexItemAttributes(String attributes) {
        List<String> cssClasses = new ArrayList<>();
        
        if (attributes == null || attributes.trim().isEmpty()) {
            return cssClasses;
        }
        
        // Pattern to match attribute="value" pairs
        Pattern attributePattern = Pattern.compile(
            "(\\w[\\w-]*?)\\s*=\\s*[\"']([^\"']*)[\"']",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = attributePattern.matcher(attributes);
        
        while (matcher.find()) {
            String attrName = matcher.group(1);
            String attrValue = matcher.group(2);
            
            String cssClass = convertCtFlexItemAttributeToCssClass(attrName, attrValue);
            if (!cssClass.isEmpty()) {
                cssClasses.add(cssClass);
            }
        }
        
        return cssClasses;
    }

    /**
     * Convert ct-flex-item attribute to CSS class name
     */
    private static String convertCtFlexItemAttributeToCssClass(String attributeName, String attributeValue) {
        if (attributeName == null || attributeValue == null) {
            return "";
        }
        
        String normalizedAttrName = attributeName.toLowerCase().trim();
        String normalizedAttrValue = attributeValue.trim();
        
        return switch (normalizedAttrName) {
            case "flex" -> "flex-" + normalizedAttrValue;
            case "flex-shrink" -> "flex-shrink-" + normalizedAttrValue;
            case "flex-grow" -> "flex-grow-" + normalizedAttrValue;
            case "flex-basis" -> "flex-basis-" + normalizedAttrValue;
            default -> ""; // Ignore other attributes
        };
    }

    /**
     * Add CSS classes to the first HTML element found in the content
     */
    private static String addCssClassesToFirstElement(String content, List<String> cssClasses) {
        if (cssClasses.isEmpty() || content == null || content.trim().isEmpty()) {
            return content;
        }
        
        // Pattern to match the first HTML element (opening tag)
        Pattern firstElementPattern = Pattern.compile(
            "(<[a-zA-Z][^>]*?)(\\s+class\\s*=\\s*[\"']([^\"']*)[\"'])([^>]*?>)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = firstElementPattern.matcher(content);
        
        if (matcher.find()) {
            // Element already has a class attribute
            String beforeClass = matcher.group(1);
            String existingClasses = matcher.group(3);
            String afterClass = matcher.group(4);
            
            // Combine existing classes with new flex classes
            String combinedClasses = combineClasses(existingClasses, cssClasses);
            
            return content.substring(0, matcher.start()) + 
                   beforeClass + " class=\"" + combinedClasses + "\"" + afterClass +
                   content.substring(matcher.end());
        } else {
            // Look for first element without class attribute
            Pattern noClassElementPattern = Pattern.compile(
                "(<[a-zA-Z][^>]*?)(/?>)",
                Pattern.CASE_INSENSITIVE
            );
            
            Matcher noClassMatcher = noClassElementPattern.matcher(content);
            
            if (noClassMatcher.find()) {
                String beforeClosing = noClassMatcher.group(1);
                String closing = noClassMatcher.group(2);
                
                String newClasses = String.join(" ", cssClasses);
                
                return content.substring(0, noClassMatcher.start()) + 
                       beforeClosing + " class=\"" + newClasses + "\"" + closing +
                       content.substring(noClassMatcher.end());
            }
        }
        
        // If no HTML element found, return original content
        return content;
    }

    /**
     * Combine existing CSS classes with new flex classes
     */
    private static String combineClasses(String existingClasses, List<String> newClasses) {
        List<String> allClasses = new ArrayList<>();
        
        // Add existing classes
        if (existingClasses != null && !existingClasses.trim().isEmpty()) {
            String[] existing = existingClasses.trim().split("\\s+");
            allClasses.addAll(Arrays.asList(existing));
        }
        
        // Add new classes (avoid duplicates)
        for (String newClass : newClasses) {
            if (!allClasses.contains(newClass)) {
                allClasses.add(newClass);
            }
        }
        
        return String.join(" ", allClasses);
    }

    /**
     * Unwrap ct-flex-item content while preserving proper indentation
     */
    private static String unwrapFlexItemContent(String innerContent) {
        if (innerContent.trim().isEmpty()) {
            return "";
        }
        
        // Split into lines to handle indentation properly
        String[] lines = innerContent.split("\n");
        StringBuilder result = new StringBuilder();
        
        // Skip leading empty lines
        int startIndex = 0;
        while (startIndex < lines.length && lines[startIndex].trim().isEmpty()) {
            startIndex++;
        }
        
        // Skip trailing empty lines
        int endIndex = lines.length - 1;
        while (endIndex >= startIndex && lines[endIndex].trim().isEmpty()) {
            endIndex--;
        }
        
        // Process remaining lines
        if (startIndex <= endIndex) {
            // Find minimum indentation (excluding empty lines)
            int minIndent = Integer.MAX_VALUE;
            for (int i = startIndex; i <= endIndex; i++) {
                String line = lines[i];
                if (!line.trim().isEmpty()) {
                    int indent = getIndentationLevel(line);
                    minIndent = Math.min(minIndent, indent);
                }
            }
            
            // Remove common indentation and build result
            for (int i = startIndex; i <= endIndex; i++) {
                String line = lines[i];
                if (line.trim().isEmpty()) {
                    result.append("\n");
                } else {
                    // Remove the minimum indentation
                    if (line.length() > minIndent) {
                        result.append(line.substring(minIndent));
                    } else {
                        result.append(line.trim());
                    }
                    if (i < endIndex) {
                        result.append("\n");
                    }
                }
            }
        }
        
        return result.toString();
    }

    /**
     * Get the indentation level of a line (number of leading spaces)
     */
    private static int getIndentationLevel(String line) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') {
                count++;
            } else if (c == '\t') {
                count += 2; // Treat tab as 2 spaces
            } else {
                break;
            }
        }
        return count;
    }



    /**
     * Convert a single ct-flex element to div with appropriate CSS classes
     */
    private static String convertCtFlexToDiv(String attributes, String innerContent) {
        List<String> cssClasses = new ArrayList<>();
        cssClasses.add("flex"); // Base flex class
        
        // Parse attributes and convert to CSS classes
        if (!attributes.trim().isEmpty()) {
            cssClasses.addAll(parseAttributesToCssClasses(attributes));
        }
        
        // Build the div element
        return "<div class=\"" + String.join(" ", cssClasses) + "\">" +
               innerContent +
               "</div>";
    }

    /**
     * Parse ct-flex attributes and convert them to CSS class names
     */
    private static List<String> parseAttributesToCssClasses(String attributes) {
        List<String> cssClasses = new ArrayList<>();
        
        // Pattern to match attribute="value" pairs
        Pattern attributePattern = Pattern.compile(
            "(\\w[\\w-]*?)\\s*=\\s*[\"']([^\"']*)[\"']",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = attributePattern.matcher(attributes);
        
        while (matcher.find()) {
            String attrName = matcher.group(1);
            String attrValue = matcher.group(2);
            
            String cssClass = convertAttributeToCssClass(attrName, attrValue);
            if (!cssClass.isEmpty()) {
                cssClasses.add(cssClass);
            }
        }
        
        return cssClasses;
    }

    /**
     * Convert individual attribute to CSS class name
     */
    private static String convertAttributeToCssClass(String attributeName, String attributeValue) {
        String normalizedAttrName = attributeName.toLowerCase().trim();
        String normalizedAttrValue = attributeValue.trim();
        
        return switch (normalizedAttrName) {
            case "justify-content" -> "justify-content-" + normalizedAttrValue;
            case "align-items" -> "align-items-" + normalizedAttrValue;
            case "gap" -> "gap-" + normalizedAttrValue;
            case "flex-direction" -> "flex-direction-" + normalizedAttrValue;
            case "flex-wrap" -> "flex-wrap-" + normalizedAttrValue;
            case "align-content" -> "align-content-" + normalizedAttrValue;
            default -> normalizedAttrName + "-" + normalizedAttrValue;
        };
    }

    /**
     * Utility method for testing - processes content with default configuration
     */
    public static String processContent(String content) {
        return migrateCtFlexToDiv(content);
    }
}
