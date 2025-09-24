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

/**
 * @author Sergio Lissner
 * Date: 9/6/2025
 * Time: 4:24 PM
 */
public class AngularMapGetMigration {

    public static Content process(Migration.MigrationConfig cfg, Globals globals, String content) {
        boolean semicolon = cfg.path().getFileName().toString().toLowerCase().endsWith(".scss");
        String newContent = migrateMapGetAngular(content, semicolon);
        Content result = new Content(newContent, !newContent.equals(content));
        return result;
    }

    public static String migrateMapGetAngular(String content, boolean semicolon) {
        String result = content;
        
        // Replace map-get with map.get
        result = result.replaceAll("map-get\\(", "map.get(");
        
        // Add @use "sass:map" at the top if map.get is used and not already present
        if (result.contains("map.get(") && !result.contains("@use \"sass:map\"")) {
            String useStatement = semicolon ? "@use \"sass:map\";" : "@use \"sass:map\"";
            result = useStatement + "\n" + result;
        }
        
        return result;
    }
}
