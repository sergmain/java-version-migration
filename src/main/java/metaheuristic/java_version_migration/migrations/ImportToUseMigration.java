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
        String newContent = migrateImportToUseMigration(content);
        Content result = new Content(newContent, !newContent.equals(content));
        return result;
    }

    public static String migrateImportToUseMigration(String content) {
        // Transform @import 'path.scss'; to @use 'path' as *;
        // Handle quoted and unquoted import statements
        return content
            .replaceAll("@import\\s*'([^'\\n]+)\\.scss'\\s*;", "@use '$1' as *;")
            .replaceAll("@import\\s*\"([^\"\\n]+)\\.scss\"\\s*;", "@use \"$1\" as *;")
            .replaceAll("@import\\s+([^\\s;'\"\\n]+)\\.scss\\s*;", "@use '$1' as *;");
    }
}
