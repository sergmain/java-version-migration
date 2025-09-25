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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Sergio Lissner
 * Date: 9/24/2025
 * Time: 11:28 AM
 */
public class AngularToSignalMigration {

    public static Content process(Migration.MigrationConfig cfg, Globals globals, String content) {
        String newContent = migrateAngularToSignalMigration(cfg, content);
        Content result = new Content(newContent, !newContent.equals(content));
        return result;
    }

    public static String migrateAngularToSignalMigration(Migration.MigrationConfig cfg, String content) {
        if (content.trim().isEmpty()) {
            return content;
        }
        
        String result = content;
        boolean needsSignalImport = false;
        boolean needsComputedImport = false;
        java.util.Set<String> signalProperties = new java.util.HashSet<>();
        
        // Get corresponding HTML file content for analysis
        String htmlContent = getHtmlContent(cfg);
        
        // Pattern 1: Convert simple properties used in templates to signals
        String beforeSignals = result;
        result = convertPropertiesToSignals(result, htmlContent, signalProperties);
        if (!result.equals(beforeSignals)) {
            needsSignalImport = true;
        }
        
        // Pattern 2: Convert getter methods to computed signals  
        String beforeComputed = result;
        result = convertGettersToComputed(result, htmlContent);
        if (!result.equals(beforeComputed)) {
            needsComputedImport = true;
        }
        
        // Pattern 3: Update method calls that modify properties to use signal updates and handle property usage
        result = convertPropertyAssignments(result, signalProperties);
        
        // Add necessary imports at the top
        result = addSignalImports(result, needsSignalImport, needsComputedImport);
        
        return result;
    }
    
    private static String getHtmlContent(Migration.MigrationConfig cfg) {
        // Look for corresponding .html file
        String tsFileName = cfg.path().getFileName().toString();
        if (tsFileName.endsWith(".ts")) {
            String baseName = tsFileName.substring(0, tsFileName.length() - 3);
            String htmlName = baseName+".html";
            return cfg.files().getOrDefault(htmlName, "");
        }
        return "";
    }
    
    private static String convertPropertiesToSignals(String content, String htmlContent, java.util.Set<String> signalProperties) {
        String result = content;
        
        // First, find all getters used in the template
        Pattern getterPattern = Pattern.compile("(?s)get\\s+(\\w+)\\(\\)\\s*\\{([^}]+)\\}");
        Matcher getterMatcher = getterPattern.matcher(content);
        java.util.Set<String> gettersUsedInTemplate = new java.util.HashSet<>();
        java.util.Set<String> propertiesUsedInGetters = new java.util.HashSet<>();
        
        while (getterMatcher.find()) {
            String getterName = getterMatcher.group(1);
            String getterBody = getterMatcher.group(2);
            
            if (isPropertyUsedInTemplate(getterName, htmlContent)) {
                gettersUsedInTemplate.add(getterName);
                // Extract properties used in this getter
                Pattern propPattern = Pattern.compile("this\\.(\\w+)(?!\\()");
                Matcher propMatcher = propPattern.matcher(getterBody);
                while (propMatcher.find()) {
                    propertiesUsedInGetters.add(propMatcher.group(1));
                }
            }
        }
        
        // Find event handler methods and properties modified in them
        java.util.Set<String> propertiesModifiedInEventHandlers = findPropertiesModifiedInEventHandlers(content, htmlContent);
        
        // Pattern: private/public property = value; -> property = signal(value);
        // Convert properties that are used in HTML templates OR used in getters OR modified in event handlers
        Pattern pattern = Pattern.compile("(?m)^(\\s*)((?:private|public|protected)\\s+)(\\w+)(:?\\s*:\\s*[^=]+)?\\s*=\\s*([^;]+);");
        Matcher matcher = pattern.matcher(result);
        StringBuilder sb = new StringBuilder();
        
        while (matcher.find()) {
            String indent = matcher.group(1);
            String visibility = matcher.group(2);
            String propertyName = matcher.group(3);
            String typeAnnotation = matcher.group(4) != null ? matcher.group(4) : "";
            String value = matcher.group(5);
            
            // Check if this property should be converted to signal
            boolean shouldConvert = isPropertyUsedInTemplate(propertyName, htmlContent) || 
                                  propertiesUsedInGetters.contains(propertyName) ||
                                  propertiesModifiedInEventHandlers.contains(propertyName);
            
            if (shouldConvert) {
                signalProperties.add(propertyName); // Track this as a signal property
                // Extract type if present
                String signalType = "";
                if (typeAnnotation != null && !typeAnnotation.trim().isEmpty()) {
                    String type = typeAnnotation.substring(1).trim(); // Remove ':'
                    signalType = "<" + type + ">";
                }
                String replacement = indent + visibility + propertyName + " = signal" + signalType + "(" + value + ");";
                matcher.appendReplacement(sb, replacement);
            } else {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0))); // No change
            }
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
    
    private static String convertGettersToComputed(String content, String htmlContent) {
        String result = content;
        
        // Pattern: get propertyName() { body } -> propertyName = computed(() => { body });
        Pattern pattern = Pattern.compile("(?s)get\\s+(\\w+)\\(\\)\\s*\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(result);
        StringBuilder sb = new StringBuilder();
        
        while (matcher.find()) {
            String propertyName = matcher.group(1);
            String body = matcher.group(2);
            
            // Check if this getter is used in the HTML template
            if (isPropertyUsedInTemplate(propertyName, htmlContent)) {
                // Extract and preserve the original body structure
                // Looking for the pattern where body starts with whitespace + return
                String trimmedBody = body.trim();
                
                // Check if it's a simple single return statement
                if (trimmedBody.startsWith("return ")) {
                    // Format as multiline computed
                    String returnStatement = trimmedBody.substring(7); // Remove "return "
                    if (returnStatement.endsWith(";")) {
                        returnStatement = returnStatement.substring(0, returnStatement.length() - 1); // Remove trailing semicolon
                    }
                    
                    String replacement = propertyName + " = computed(() => {\n        return " + returnStatement + ";\n    });";
                    matcher.appendReplacement(sb, replacement);
                } else {
                    // Keep original body structure  
                    String replacement = propertyName + " = computed(() => {" + body + "});";
                    matcher.appendReplacement(sb, replacement);
                }
            } else {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0))); // No change
            }
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
    
    private static String convertPropertyAssignments(String content, java.util.Set<String> signalProperties) {
        String result = content;
        
        // First, detect additional signal properties by looking for signal call patterns in the code
        java.util.Set<String> detectedSignalProperties = new java.util.HashSet<>(signalProperties);
        
        // Look for patterns like this.propertyName() which indicate the property is a signal
        Pattern signalCallPattern = Pattern.compile("\\bthis\\.(\\w+)\\(\\)");
        Matcher signalCallMatcher = signalCallPattern.matcher(content);
        while (signalCallMatcher.find()) {
            detectedSignalProperties.add(signalCallMatcher.group(1));
        }
        
        // Pattern: this.propertyName = value; -> this.propertyName.set(value);
        // Only convert if the property was converted to a signal or detected as signal usage
        Pattern pattern = Pattern.compile("(?m)this\\.(\\w+)\\s*=\\s*([^;]+);");
        Matcher matcher = pattern.matcher(result);
        StringBuilder sb = new StringBuilder();
        
        while (matcher.find()) {
            String propertyName = matcher.group(1);
            String value = matcher.group(2);
            
            // Check if this property was converted to a signal or detected as signal usage
            if (detectedSignalProperties.contains(propertyName)) {
                String replacement = "this." + propertyName + ".set(" + value + ");";
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            } else {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0))); // No change
            }
        }
        matcher.appendTail(sb);
        
        // Now handle property usage (reading, not assignment)
        result = sb.toString();
        
        // Convert property usage to function calls for all detected signal properties
        for (String signalProp : detectedSignalProperties) {
            Pattern propUsagePattern = Pattern.compile("\\bthis\\." + signalProp + "\\b(?!\\(\\)|\\s*=|\\s*\\.set\\()");
            Matcher propMatcher = propUsagePattern.matcher(result);
            StringBuilder propSb = new StringBuilder();
            while (propMatcher.find()) {
                propMatcher.appendReplacement(propSb, Matcher.quoteReplacement("this." + signalProp + "()"));
            }
            propMatcher.appendTail(propSb);
            result = propSb.toString();
        }
        
        return result;
    }
    
    private static java.util.Set<String> findPropertiesModifiedInEventHandlers(String content, String htmlContent) {
        java.util.Set<String> propertiesModifiedInEventHandlers = new java.util.HashSet<>();
        
        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            return propertiesModifiedInEventHandlers;
        }
        
        // Find event handler method names from HTML
        java.util.Set<String> eventHandlerMethods = new java.util.HashSet<>();
        Pattern eventPattern = Pattern.compile("\\([a-zA-Z]+\\)=\"([a-zA-Z]\\w*)\\([^)]*\\)\"");
        Matcher eventMatcher = eventPattern.matcher(htmlContent);
        while (eventMatcher.find()) {
            eventHandlerMethods.add(eventMatcher.group(1));
        }
        
        // For each event handler method, find properties that are assigned in it
        for (String methodName : eventHandlerMethods) {
            Pattern methodPattern = Pattern.compile("(?s)" + methodName + "\\([^)]*\\)\\s*\\{([^}]+)\\}");
            Matcher methodMatcher = methodPattern.matcher(content);
            if (methodMatcher.find()) {
                String methodBody = methodMatcher.group(1);
                // Find property assignments in this method
                Pattern assignmentPattern = Pattern.compile("\\bthis\\.(\\w+)\\s*=");
                Matcher assignmentMatcher = assignmentPattern.matcher(methodBody);
                while (assignmentMatcher.find()) {
                    propertiesModifiedInEventHandlers.add(assignmentMatcher.group(1));
                }
            }
        }
        
        return propertiesModifiedInEventHandlers;
    }
    
    private static boolean isPropertyUsedInTemplate(String propertyName, String htmlContent) {
        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            return false; // Conservative approach: if no HTML, don't convert
        }
        
        // Check common Angular template patterns
        return htmlContent.contains("{{ " + propertyName) ||
               htmlContent.contains("{{" + propertyName) ||
               htmlContent.contains("[" + propertyName + "]") ||
               htmlContent.contains("[(ngModel)]=\"" + propertyName + "\"") ||
               htmlContent.contains("*ngFor") && htmlContent.contains(propertyName) ||
               htmlContent.contains("*ngIf") && htmlContent.contains(propertyName) ||
               htmlContent.contains("(click)") && htmlContent.contains(propertyName) ||
               htmlContent.contains("(change)") && htmlContent.contains(propertyName) ||
               htmlContent.contains("[disabled]=\"" + propertyName) ||
               htmlContent.contains("[class.") && htmlContent.contains(propertyName) ||
               htmlContent.contains("[style.") && htmlContent.contains(propertyName) ||
               htmlContent.contains("[attr.") && htmlContent.contains(propertyName) ||
               htmlContent.contains("\" + propertyName + \"") ||
               htmlContent.matches(".*\\b" + propertyName + "\\b.*");
    }
    
    private static String addSignalImports(String content, boolean needsSignal, boolean needsComputed) {
        if (!needsSignal && !needsComputed) {
            return content;
        }
        
        // Check if @angular/core import already exists
        String importPattern = "import\\s*\\{([^}]+)\\}\\s*from\\s*['\"]@angular/core['\"];";
        Pattern pattern = Pattern.compile(importPattern);
        Matcher matcher = pattern.matcher(content);
        
        if (matcher.find()) {
            // Angular core import exists, add signal/computed to it
            String fullMatch = matcher.group(0);
            String existingImports = matcher.group(1);
            
            // Parse existing imports to preserve formatting
            String[] imports = existingImports.split(",");
            boolean hasSpaces = existingImports.contains(" ");
            String separator = hasSpaces ? ", " : ", ";
            
            StringBuilder newImports = new StringBuilder();
            for (String imp : imports) {
                if (newImports.length() > 0) {
                    newImports.append(separator);
                }
                newImports.append(imp.trim());
            }
            
            if (needsSignal && !existingImports.contains("signal")) {
                if (newImports.length() > 0) {
                    newImports.append(separator);
                }
                newImports.append("signal");
            }
            
            if (needsComputed && !existingImports.contains("computed")) {
                if (newImports.length() > 0) {
                    newImports.append(separator);
                }
                newImports.append("computed");
            }
            
            // Preserve the original spacing style
            String replacement;
            if (hasSpaces) {
                replacement = fullMatch.replace(existingImports, " " + newImports + " ");
            } else {
                replacement = fullMatch.replace(existingImports, newImports.toString());
            }
            
            return content.replace(fullMatch, replacement);
        } else {
            // No @angular/core import, add it at the top
            StringBuilder imports = new StringBuilder("import { ");
            if (needsSignal) {
                imports.append("signal");
            }
            if (needsComputed) {
                if (needsSignal) {
                    imports.append(", ");
                }
                imports.append("computed");
            }
            imports.append(" } from '@angular/core';\n");
            
            return imports + content;
        }
    }
}
