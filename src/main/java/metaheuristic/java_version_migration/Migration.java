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

package metaheuristic.java_version_migration;

import lombok.extern.slf4j.Slf4j;
import metaheuristic.java_version_migration.migrations.MigrateSynchronizedJava21;
import metaheuristic.java_version_migration.migrations.RemoveDoubleLF;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * @author Sergio Lissner
 * Date: 7/10/2023
 * Time: 9:57 PM
 */
@SuppressWarnings("RedundantStreamOptionalCall")
@Slf4j
public class Migration {

    public record MigrationConfig(Path path, Globals globals) {}
    public record MigrationFunctions(int version, List<Consumer<MigrationConfig>> functions) {}

    public static final List<MigrationFunctions> functions = Stream.of(
            new MigrationFunctions(21, List.of(
//                    MigrateSynchronizedJava21::migrateSynchronized
                    RemoveDoubleLF::migrateSynchronized
            )))
            .sorted(Comparator.comparingInt(MigrationFunctions::version)).toList();

}
