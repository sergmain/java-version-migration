/*
 * Copyright (c) 2023. Sergio Lissner
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

import lombok.extern.slf4j.Slf4j;
import metaheuristic.java_version_migration.Migration;

import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

/**
 * @author Sergio Lissner
 * Date: 11/9/2023
 * Time: 8:03 PM
 */
@Slf4j
public class RemoveDoubleLF {

    public static void migrateSynchronized(Migration.MigrationConfig cfg) {
        try {
            long mills = System.currentTimeMillis();
            String content = Files.readString(cfg.path(), cfg.globals().getCharset());
            MigrateSynchronizedJava21.Content newContent = process(cfg, content);
            if (newContent.changed()) {
                Files.writeString(cfg.path(), newContent.content(), cfg.globals().getCharset(), StandardOpenOption.SYNC, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                System.out.println("\t\tprocessed for "+(System.currentTimeMillis() - mills));
            }
        } catch (Throwable th) {
            log.error("Error with path " + cfg.path(), th);
        }
    }

    private static MigrateSynchronizedJava21.Content process(Migration.MigrationConfig cfg, String content) {
        StringBuilder sb = new StringBuilder(content);
        boolean changed=false;
        for (int i = 0; i < content.length() - 1; i++) {
            char[] chars = new char[2];
            sb.getChars(i, i+1, chars, 0);
            if (chars[0] ==7 && chars[0]==chars[1] ) {
                sb.deleteCharAt(i);
                changed = true;
                i--;
            }
        }

        return new MigrateSynchronizedJava21.Content(sb.toString(), changed);
    }

}
