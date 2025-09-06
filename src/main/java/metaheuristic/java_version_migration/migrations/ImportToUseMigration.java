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

/**
 * @author Sergio Lissner
 * Date: 9/5/2025
 * Time: 5:39 PM
 */
public class ImportToUseMigration {

    public static Content process(Migration.MigrationConfig cfg, String content) {
        boolean semicolon = cfg.path().getFileName().toString().toLowerCase().endsWith(".scss");
        String newContent = migrateImportToUseMigration(content, semicolon);
        Content result = new Content(newContent, !newContent.equals(content));
        return result;
    }

//    new requirements:
//    add semicolon variable to method migrateMathUnitAngular() - if it true, then put semicolon at the end, otherwise there shouldn't be semicolon at the end of line
//
//    fix code and tests

    public static String migrateImportToUseMigration(String content) {
        // Transform @import statements to @use statements while preserving semicolon presence
        return content
            // Handle quoted imports with .scss extension and semicolons
            .replaceAll("@import\\s*'([^'\\n]+)\\.scss'\\s*;", "@use '$1' as *;")
            .replaceAll("@import\\s*\"([^\"\\n]+)\\.scss\"\\s*;", "@use \"$1\" as *;")
            // Handle quoted imports with .sass extension and semicolons
            .replaceAll("@import\\s*'([^'\\n]+)\\.sass'\\s*;", "@use '$1' as *;")
            .replaceAll("@import\\s*\"([^\"\\n]+)\\.sass\"\\s*;", "@use \"$1\" as *;")
            // Handle quoted imports without extension and with semicolons only
            .replaceAll("@import\\s*'([^'\\n]*[^'\\n.]+)'\\s*;", "@use '$1' as *;")
            .replaceAll("@import\\s*\"([^\"\\n]*[^\"\\n.]+)\"\\s*;", "@use \"$1\" as *;")
            // Handle unquoted imports with .scss extension and semicolons  
            .replaceAll("@import\\s+([^\\s;'\"\\n]+)\\.scss\\s*;", "@use '$1' as *;")
            // Handle unquoted imports with .scss extension but without semicolons
            .replaceAll("@import\\s+([^\\s;'\"\\n]+)\\.scss(?![;])", "@use '$1' as *")
            // Handle unquoted imports without .scss extension and with semicolons
            .replaceAll("@import\\s+([^\\s;'\"\\n]+)\\s*;", "@use '$1' as *;")
            // Handle unquoted imports without .scss extension and without semicolons
            .replaceAll("@import\\s+([^\\s;'\"\\n]+)(?![;])", "@use '$1' as *;");
    }
}
