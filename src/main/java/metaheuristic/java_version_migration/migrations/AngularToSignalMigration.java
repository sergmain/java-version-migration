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
        start to implement a method metaheuristic.java_version_migration.migrations.AngularToSignalMigration.migrateMapGetAngular
        and creating unit-tests in metaheuristic.java_version_migration.migrations.AngularToSignalMigration.migrateMapGetAngularTest
        ================
        variable 'String content', which method migrateMapGetAngularTest() is receiving, will be always not null, no need in additional check.
        ================
        as i understand, for better decision, this migration method must have access to other files in dir, specifically, .html, to understand what to migrate to signal.
        use Map<Path, String> files  in metaheuristic.java_version_migration.Migration.MigrationConfig for accessing needed files
        =============
        """;

    public static Content process(Migration.MigrationConfig cfg, String content) {
        String newContent = migrateMapGetAngular(cfg, content);
        Content result = new Content(newContent, !newContent.equals(content));
        return result;
    }

    public static String migrateMapGetAngular(Migration.MigrationConfig cfg, String content) {

    }
}
