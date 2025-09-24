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

import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * @author Sergio Lissner
 * Date: 9/24/2025
 * Time: 11:28 AM
 */
public class AngularToSignalMigration {

    String sonnet = """
        ================================================
        DO NO MAKE ANY CHANGES IN OTHER FILES!!!
        ==============================================
        BEFORE STARTING, READ PROJECT'S INSTRUCTIONS CAREFULLY.
        by project's instructions I mean instruction in Claude, not in readme.md or any other file in repo
        -----------
        **Problem:** I need to create static method witch will migrate content of .ts files in my angular ver 20 application to usage of signals
        
        =================
        start to implement a method metaheuristic.java_version_migration.migrations.AngularToSignalMigration.migrateAngularToSignalMigration
        and creating unit-tests in metaheuristic.java_version_migration.migrations.AngularToSignalMigration.migrateAngularToSignalMigrationTest
        ================
        variable 'String content', which method migrateAngularToSignalMigration() will receive, will be always not null, no need in additional check.
        ================
        as i understand, for better decision, this migration method must have access to other files in dir, specifically, .html, to understand what to migrate to signal.
        use Map<Path, String> files  in metaheuristic.java_version_migration.Migration.MigrationConfig for accessing needed files
        =============
        """;

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
        
        // Get corresponding HTML file content for analysis
        String htmlContent = getHtmlContent(cfg);
        
        // Pattern 1: Convert simple properties used in templates to signals
        // Example: private _data: any[] = []; -> private _data = signal<any[]>([]);
        result = convertPropertiesToSignals(result, htmlContent);
        if (!result.equals(content)) {
            needsSignalImport = true;
        }
        
        // Pattern 2: Convert getter methods to computed signals
        // Example: get items() { return this._items.filter(...); } -> items = computed(() => this._items().filter(...));
        String beforeComputed = result;
        result = convertGettersToComputed(result, htmlContent);
        if (!result.equals(beforeComputed)) {
            needsComputedImport = true;
        }
        
        // Pattern 3: Update method calls that modify properties to use signal updates
        // Example: this.items = [...]; -> this.items.set([...]);
        result = convertPropertyAssignments(result);
        
        // Pattern 4: Update template usage patterns in TypeScript code
        // Example: this.property -> this.property()
        result = updatePropertyUsage(result);
        
        // Add necessary imports at the top
        result = addSignalImports(result, needsSignalImport, needsComputedImport);
        
        return result;
    }
    
    private static String getHtmlContent(Migration.MigrationConfig cfg) {
        // Look for corresponding .html file
        String tsFileName = cfg.path().getFileName().toString();
        if (tsFileName.endsWith(".ts")) {
            String baseName = tsFileName.substring(0, tsFileName.length() - 3);
            Path htmlPath = cfg.path().getParent().resolve(baseName + ".html");
            return cfg.files().getOrDefault(htmlPath, "");
        }
        return "";
    }
    
    private static String convertPropertiesToSignals(String content, String htmlContent) {
        String result = content;
        
        // Pattern: private/public property = value; -> property = signal(value);
        // Only convert properties that are used in HTML templates
        Pattern pattern = Pattern.compile("(?m)^(\\s*)((?:private|public|protected)\\s+)(\\w+)(:?\\s*:\\s*[^=]+)?\\s*=\\s*([^;]+);");
        Matcher matcher = pattern.matcher(result);
        StringBuilder sb = new StringBuilder();
        
        while (matcher.find()) {
            String indent = matcher.group(1);
            String visibility = matcher.group(2);
            String propertyName = matcher.group(3);
            String typeAnnotation = matcher.group(4) != null ? matcher.group(4) : "";
            String value = matcher.group(5);
            
            // Check if this property is used in the HTML template
            if (isPropertyUsedInTemplate(propertyName, htmlContent)) {
                // Extract type if present
                String signalType = "";
                if (typeAnnotation != null && !typeAnnotation.trim().isEmpty()) {
                    String type = typeAnnotation.substring(1).trim(); // Remove ':'
                    signalType = "<" + type + ">";
                }
                String replacement = indent + visibility + propertyName + " = signal" + signalType + "(" + value + ");";
                matcher.appendReplacement(sb, replacement);
            } else {
                matcher.appendReplacement(sb, matcher.group(0)); // No change
            }
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
    
    private static String convertGettersToComputed(String content, String htmlContent) {
        String result = content;
        
        // Pattern: get propertyName() { return ...; } -> propertyName = computed(() => { return ...; });
        Pattern pattern = Pattern.compile("(?s)get\\s+(\\w+)\\(\\)\\s*\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(result);
        StringBuilder sb = new StringBuilder();
        
        while (matcher.find()) {
            String propertyName = matcher.group(1);
            String body = matcher.group(2).trim();
            
            // Check if this getter is used in the HTML template
            if (isPropertyUsedInTemplate(propertyName, htmlContent)) {
                String replacement = propertyName + " = computed(() => {" + body + "});";
                matcher.appendReplacement(sb, replacement);
            } else {
                matcher.appendReplacement(sb, matcher.group(0)); // No change
            }
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
    
    private static String convertPropertyAssignments(String content) {
        String result = content;
        
        // Pattern: this.propertyName = value; -> this.propertyName.set(value);
        // Only convert if the property looks like a signal (has parentheses when accessed)
        Pattern pattern = Pattern.compile("(?m)this\\.(\\w+)\\s*=\\s*([^;]+);");
        Matcher matcher = pattern.matcher(result);
        StringBuilder sb = new StringBuilder();
        
        while (matcher.find()) {
            String propertyName = matcher.group(1);
            String value = matcher.group(2);
            
            // Check if this property is used as a signal elsewhere (has () calls)
            if (content.contains("this." + propertyName + "()")) {
                String replacement = "this." + propertyName + ".set(" + value + ");";
                matcher.appendReplacement(sb, replacement);
            } else {
                matcher.appendReplacement(sb, matcher.group(0)); // No change
            }
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
    
    private static String updatePropertyUsage(String content) {
        String result = content;
        
        // Pattern: this.property in contexts where it should be this.property()
        // This is complex and needs careful analysis to avoid breaking non-signal properties
        
        return result;
    }
    
    private static boolean isPropertyUsedInTemplate(String propertyName, String htmlContent) {
        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            return false; // Conservative approach: if no HTML, don't convert
        }
        
        // Check common Angular template patterns
        return htmlContent.contains("{{ " + propertyName) ||
               htmlContent.contains("{{" + propertyName) ||
               htmlContent.contains("[" + propertyName + "]") ||
               htmlContent.contains("*ngFor") && htmlContent.contains(propertyName) ||
               htmlContent.contains("*ngIf") && htmlContent.contains(propertyName) ||
               htmlContent.contains("(click)") && htmlContent.contains(propertyName) ||
               htmlContent.contains("(change)") && htmlContent.contains(propertyName);
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
            String existingImports = matcher.group(1).trim();
            StringBuilder newImports = new StringBuilder(existingImports);
            
            if (needsSignal && !existingImports.contains("signal")) {
                if (!existingImports.isEmpty() && !existingImports.endsWith(",")) {
                    newImports.append(", ");
                }
                newImports.append("signal");
            }
            
            if (needsComputed && !existingImports.contains("computed")) {
                if (!newImports.toString().isEmpty() && !newImports.toString().endsWith(",")) {
                    newImports.append(", ");
                }
                newImports.append("computed");
            }
            
            return content.replace(matcher.group(1), newImports.toString());
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
