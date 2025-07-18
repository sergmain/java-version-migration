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

import metaheuristic.java_version_migration.Migration;
import metaheuristic.java_version_migration.data.Content;

/**
 * @author Sergio Lissner
 * Date: 11/9/2023
 * Time: 8:03 PM
 */
public class RemoveDoubleLF {

    public static Content process(Migration.MigrationConfig cfg, String content) {
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

        return new Content(sb.toString(), changed);
    }

}
