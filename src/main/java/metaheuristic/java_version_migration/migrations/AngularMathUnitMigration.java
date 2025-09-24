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
 * Date: 9/6/2025
 * Time: 1:05 PM
 */
public class AngularMathUnitMigration {

    public static Content process(Migration.MigrationConfig cfg, String content) {
        boolean semicolon = cfg.path().getFileName().toString().toLowerCase().endsWith(".scss");
        String newContent = migrateMathUnitAngular(content, semicolon);
        Content result = new Content(newContent, !newContent.equals(content));
        return result;
    }

    public static String migrateMathUnitAngular(String content, boolean semicolon) {
        String result = content;
        
        // Replace unit( with math.unit( only when not already prefixed with math.
        result = result.replaceAll("(?<!math\\.)\\bunit\\s*\\(", "math.unit(");
        
        // Add '@use sass:math'; at the top if not already present and unit functions were found
        if (!content.equals(result) && !result.contains("@use 'sass:math'")) {
            String useStatement = semicolon ? "@use 'sass:math';\n" : "@use 'sass:math'\n";
            result = useStatement + result;
        }
        
        return result;
    }
}
