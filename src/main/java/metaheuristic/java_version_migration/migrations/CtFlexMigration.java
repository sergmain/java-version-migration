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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Migration tool to convert ct-flex elements to div elements with CSS classes
 * 
 * @author Sergio Lissner
 * Date: 9/4/2025
 * Time: 4:57 PM
 */
public class CtFlexMigration {

    public static Content process(Migration.MigrationConfig cfg, String content) {
        if (content == null) {
            return new Content(null, false);
        }
        String newContent = processContent(content);
        boolean changed = newContent != null && !newContent.equals(content);
        return new Content(newContent != null ? newContent : content, changed);
    }

    public static String processContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }
        
        String result = content;
        boolean hasChanges = true;
        
        // Keep processing until no more changes are made
        while (hasChanges) {
            String previousResult = result;
            result = processCtFlexElements(result);
            hasChanges = !result.equals(previousResult);
        }
        
        return result;
    }
    
    /**
     * Process ct-flex elements and convert them to div elements with CSS classes
     */
    private static String processCtFlexElements(String content) {
        // Pattern to match ct-flex elements
        Pattern pattern = Pattern.compile(
            "<ct-flex([^>]*?)>([\\s\\S]*?)</ct-flex>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher matcher = pattern.matcher(content);
        
        while (matcher.find()) {
            String attributes = matcher.group(1);
            String innerContent = matcher.group(2);
            String fullMatch = matcher.group(0);
            
            // Convert attributes to CSS classes
            String cssClasses = convertAttributesToCssClasses(attributes);
            
            // Process inner ct-flex-item elements
            String processedInnerContent = processCtFlexItems(innerContent);
            
            // Create the replacement div
            String replacement = "<div class=\"" + cssClasses + "\">" + processedInnerContent + "</div>";
            
            // Replace the first match only
            return content.substring(0, matcher.start()) + 
                   replacement + 
                   content.substring(matcher.end());
        }
        
        return content; // No ct-flex elements found
    }
    
    /**
     * Convert ct-flex attributes to CSS classes
     */
    private static String convertAttributesToCssClasses(String attributes) {
        List<String> cssClasses = new ArrayList<>();
        cssClasses.add("flex"); // Base flex class
        
        if (attributes == null || attributes.trim().isEmpty()) {
            return "flex";
        }
        
        // Pattern to match attributes
        Pattern attrPattern = Pattern.compile(
            "([\\w-]+)\\s*=\\s*[\"']([^\"']*)[\"']",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = attrPattern.matcher(attributes);
        
        while (matcher.find()) {
            String attrName = matcher.group(1);
            String attrValue = matcher.group(2);
            
            // Convert known flex attributes to CSS classes
            String cssClass = convertAttributeToCssClass(attrName, attrValue);
            if (cssClass != null && !cssClass.isEmpty()) {
                cssClasses.add(cssClass);
            }
        }
        
        return String.join(" ", cssClasses);
    }
    
    /**
     * Convert individual attribute to CSS class
     */
    private static String convertAttributeToCssClass(String attrName, String attrValue) {
        // Handle known flex attributes
        switch (attrName.toLowerCase()) {
            case "justify-content":
                return "justify-content-" + attrValue;
            case "align-items":
                return "align-items-" + attrValue;
            case "align-content":
                return "align-content-" + attrValue;
            case "flex-direction":
                return "flex-direction-" + attrValue;
            case "flex-wrap":
                return "flex-wrap-" + attrValue;
            case "gap":
                return "gap-" + attrValue;
            default:
                // For other attributes, create a generic CSS class
                return attrName + "-" + attrValue;
        }
    }
    
    /**
     * Process ct-flex-item elements and unwrap them while preserving flex attributes
     */
    private static String processCtFlexItems(String content) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }
        
        // Pattern to match ct-flex-item elements
        Pattern pattern = Pattern.compile(
            "<ct-flex-item([^>]*?)>([\\s\\S]*?)</ct-flex-item>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        StringBuilder result = new StringBuilder();
        int lastEnd = 0;
        
        Matcher matcher = pattern.matcher(content);
        
        while (matcher.find()) {
            // Add content before this match
            result.append(content.substring(lastEnd, matcher.start()));
            
            String attributes = matcher.group(1);
            String innerContent = matcher.group(2);
            
            // Process the ct-flex-item
            String processedContent = processCtFlexItem(attributes, innerContent);
            result.append(processedContent);
            
            lastEnd = matcher.end();
        }
        
        // Add remaining content
        result.append(content.substring(lastEnd));
        
        return result.toString();
    }
    
    /**
     * Process individual ct-flex-item element
     */
    private static String processCtFlexItem(String attributes, String innerContent) {
        if (innerContent == null || innerContent.trim().isEmpty()) {
            return innerContent != null ? innerContent : "";
        }
        
        // Extract flex-related attributes and Angular directives
        FlexItemInfo flexInfo = extractFlexItemInfo(attributes);
        
        // Find the first child element to add classes to
        String processedContent = addClassesToFirstChild(innerContent, flexInfo);
        
        return processedContent;
    }
    
    /**
     * Extract flex attributes and Angular directives from ct-flex-item
     */
    private static FlexItemInfo extractFlexItemInfo(String attributes) {
        FlexItemInfo info = new FlexItemInfo();
        
        if (attributes == null || attributes.trim().isEmpty()) {
            return info;
        }
        
        // Pattern to match all attributes (both regular and Angular directives)
        Pattern attrPattern = Pattern.compile(
            "(\\*?[\\w.-]+)\\s*=\\s*[\"']([^\"']*(?:[\\r\\n][^\"']*)*)[\"']|" +
            "(\\*?[\\w.-]+)(?=\\s|$)", // Attributes without values
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher matcher = attrPattern.matcher(attributes);
        
        while (matcher.find()) {
            String attrName = matcher.group(1) != null ? matcher.group(1) : matcher.group(3);
            String attrValue = matcher.group(2);
            
            if (attrName.startsWith("*") || attrName.startsWith("[") || attrName.startsWith("(") || 
                attrName.equals("routerLink") || attrName.equals("routerLinkActive")) {
                // Angular directive or binding
                if (attrValue != null) {
                    String cleanValue = attrValue.replaceAll("\\s+", " ").trim();
                    info.angularDirectives.add(attrName + "=\"" + cleanValue + "\"");
                } else {
                    info.angularDirectives.add(attrName);
                }
            } else if (attrName.startsWith("flex")) {
                // Flex-related attribute
                String cssClass = convertFlexAttributeToCssClass(attrName, attrValue);
                if (cssClass != null) {
                    info.flexClasses.add(cssClass);
                }
            }
        }
        
        return info;
    }
    
    /**
     * Convert flex attributes to CSS classes
     */
    private static String convertFlexAttributeToCssClass(String attrName, String attrValue) {
        if (attrValue == null || attrValue.trim().isEmpty()) {
            return null;
        }
        
        switch (attrName.toLowerCase()) {
            case "flex":
                return "flex-" + attrValue;
            case "flex-grow":
                return "flex-grow-" + attrValue;
            case "flex-shrink":
                return "flex-shrink-" + attrValue;
            case "flex-basis":
                return "flex-basis-" + attrValue;
            default:
                return null;
        }
    }
    
    /**
     * Add classes and directives to the first child element
     */
    private static String addClassesToFirstChild(String innerContent, FlexItemInfo flexInfo) {
        if (flexInfo.flexClasses.isEmpty() && flexInfo.angularDirectives.isEmpty()) {
            return innerContent;
        }
        
        // Pattern to find the first HTML element
        Pattern elementPattern = Pattern.compile(
            "<([^\\s/>]+)([^>]*?)(/?)>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher matcher = elementPattern.matcher(innerContent.trim());
        
        if (matcher.find()) {
            String tagName = matcher.group(1);
            String existingAttrs = matcher.group(2);
            String closingSlash = matcher.group(3);
            
            // Build new attributes
            StringBuilder newAttrs = new StringBuilder(existingAttrs);
            
            // Add flex classes to existing class attribute or create new one
            if (!flexInfo.flexClasses.isEmpty()) {
                String flexClassesStr = String.join(" ", flexInfo.flexClasses);
                newAttrs = addOrUpdateClassAttribute(newAttrs, flexClassesStr);
            }
            
            // Add Angular directives
            for (String directive : flexInfo.angularDirectives) {
                newAttrs.append(" ").append(directive);
            }
            
            // Replace the opening tag
            String newOpeningTag = "<" + tagName + newAttrs + closingSlash + ">";
            
            return innerContent.substring(0, matcher.start()) + 
                   newOpeningTag + 
                   innerContent.substring(matcher.end());
        } else {
            // If no HTML element found, just return the content as is
            return innerContent;
        }
    }
    
    /**
     * Add or update the class attribute
     */
    private static StringBuilder addOrUpdateClassAttribute(StringBuilder attrs, String newClasses) {
        String attrsStr = attrs.toString();
        
        // Pattern to find existing class attribute
        Pattern classPattern = Pattern.compile(
            "\\bclass\\s*=\\s*[\"']([^\"']*)[\"']",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = classPattern.matcher(attrsStr);
        
        if (matcher.find()) {
            // Update existing class attribute
            String existingClasses = matcher.group(1);
            String updatedClasses = existingClasses.trim() + " " + newClasses;
            String replacement = "class=\"" + updatedClasses + "\"";
            
            return new StringBuilder(attrsStr.substring(0, matcher.start()) + 
                                   replacement + 
                                   attrsStr.substring(matcher.end()));
        } else {
            // Add new class attribute
            return attrs.append(" class=\"").append(newClasses).append("\"");
        }
    }
    
    /**
     * Helper class to store flex item information
     */
    private static class FlexItemInfo {
        List<String> flexClasses = new ArrayList<>();
        List<String> angularDirectives = new ArrayList<>();
    }
}
