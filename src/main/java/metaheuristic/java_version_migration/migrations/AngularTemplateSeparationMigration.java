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
 * Migration tool to separate multiple structural directives on Angular elements
 * 
 * @author Sergio Lissner
 * Date: 9/4/2025
 * Time: 4:57 PM
 */
public class AngularTemplateSeparationMigration {

    public static Content process(Migration.MigrationConfig cfg, String content) {
        String newContent = migrateAngularTemplateSeparationMigration(content);
        Content result = new Content(newContent, !newContent.equals(content));
        return result;
    }

    public static String migrateAngularTemplateSeparationMigration(String content) {
        String result = content;
        boolean hasChanges = true;
        
        while (hasChanges) {
            String previousResult = result;
            result = processOneNgContainer(result);
            hasChanges = !result.equals(previousResult);
        }
        
        return result;
    }
    
    /**
     * Process a single ng-container element that has multiple structural directives
     */
    private static String processOneNgContainer(String content) {
        // Pattern to match ng-container elements with their attributes and content
        Pattern ngContainerPattern = Pattern.compile(
            "<ng-container([^>]*?)>(.*?)</ng-container>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher matcher = ngContainerPattern.matcher(content);
        
        while (matcher.find()) {
            String attributes = matcher.group(1);
            String innerContent = matcher.group(2);
            
            // Check if this ng-container has both *ngIf and *ngTemplateOutlet
            if (hasBothStructuralDirectives(attributes)) {
                String separatedElement = separateStructuralDirectives(attributes, innerContent);
                
                // Replace this ng-container
                return content.substring(0, matcher.start()) +
                       separatedElement +
                       content.substring(matcher.end());
            }
        }
        
        return content; // No ng-container with multiple structural directives found
    }
    
    /**
     * Check if the attributes contain both *ngIf and *ngTemplateOutlet directives
     */
    private static boolean hasBothStructuralDirectives(String attributes) {
        boolean hasNgIf = false;
        boolean hasTemplateOutlet = false;
        
        // Pattern to match attribute names, including multi-line values
        Pattern attributePattern = Pattern.compile(
            "(\\*[\\w-]+)\\s*=\\s*[\"']([^\"']*(?:\\s*[\\r\\n]\\s*[^\"']*)*)[\"']",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher matcher = attributePattern.matcher(attributes);
        
        while (matcher.find()) {
            String attrName = matcher.group(1).toLowerCase().trim();
            
            if (attrName.equals("*ngif")) {
                hasNgIf = true;
            } else if (attrName.equals("*ngtemplateoutlet")) {
                hasTemplateOutlet = true;
            }
        }
        
        return hasNgIf && hasTemplateOutlet;
    }
    
    /**
     * Separate multiple structural directives into nested ng-container elements
     */
    private static String separateStructuralDirectives(String attributes, String innerContent) {
        DirectiveInfo directiveInfo = parseDirectives(attributes);
        
        // Build nested structure with *ngIf on outer container
        StringBuilder result = new StringBuilder();
        
        // Outer ng-container with *ngIf and other non-structural attributes
        result.append("<ng-container");
        if (directiveInfo.ngIfDirective != null) {
            result.append(" ").append(directiveInfo.ngIfDirective);
        }
        for (String attr : directiveInfo.otherAttributes) {
            result.append(" ").append(attr);
        }
        result.append(">");
        
        // Inner ng-container with *ngTemplateOutlet and other structural directives
        result.append("\n  <ng-container");
        if (directiveInfo.templateOutletDirective != null) {
            result.append(" ").append(directiveInfo.templateOutletDirective);
        }
        for (String attr : directiveInfo.otherStructuralDirectives) {
            result.append(" ").append(attr);
        }
        result.append(">");
        
        // Inner content
        result.append(innerContent);
        
        // Close inner ng-container
        result.append("</ng-container>");
        
        // Close outer ng-container
        result.append("\n</ng-container>");
        
        return result.toString();
    }
    
    /**
     * Data class to hold parsed directive information
     */
    private static class DirectiveInfo {
        String ngIfDirective;
        String templateOutletDirective;
        List<String> otherStructuralDirectives;
        List<String> otherAttributes;
        
        DirectiveInfo() {
            this.otherStructuralDirectives = new ArrayList<>();
            this.otherAttributes = new ArrayList<>();
        }
    }
    
    /**
     * Parse attributes and separate them into different directive types
     */
    private static DirectiveInfo parseDirectives(String attributes) {
        DirectiveInfo info = new DirectiveInfo();
        
        // Pattern to match all attributes, including multi-line values
        Pattern attributePattern = Pattern.compile(
            "([\\*\\[\\(]?[\\w-]+[\\]\\)]?)\\s*=\\s*[\"']([^\"']*(?:\\s*[\\r\\n]\\s*[^\"']*)*)[\"']",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher matcher = attributePattern.matcher(attributes);
        
        while (matcher.find()) {
            String attrName = matcher.group(1);
            String attrValue = matcher.group(2);
            String fullAttribute = attrName + "=\"" + attrValue + "\"";
            
            String normalizedName = attrName.toLowerCase().trim();
            
            if (normalizedName.equals("*ngif")) {
                info.ngIfDirective = fullAttribute;
            } else if (normalizedName.equals("*ngtemplateoutlet")) {
                info.templateOutletDirective = fullAttribute;
            } else if (normalizedName.startsWith("*")) {
                // Other structural directives
                info.otherStructuralDirectives.add(fullAttribute);
            } else {
                // Non-structural attributes
                info.otherAttributes.add(fullAttribute);
            }
        }
        
        return info;
    }
}
