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
import metaheuristic.java_version_migration.data.Content;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static metaheuristic.java_version_migration.MigrationUtils.execStat;

/**
 * @author Sergio Lissner
 * Date: 7/10/2023
 * Time: 8:40 PM
 */
@Slf4j
public class MigrationProcessor {

    public static final AtomicLong totalSize = new AtomicLong();

    public static void migrationProcessor(final Globals globals) throws IOException, InterruptedException {
//        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(globals.threads);
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        long mills = System.currentTimeMillis();
        for (Path path : globals.startingPath) {
            final IOFileFilter filter = FileFileFilter.INSTANCE.and(new SuffixFileFilter(new String[]{".java"}));
            try (Stream<Path> stream = PathUtils.walk(path, filter, Integer.MAX_VALUE, false, FileVisitOption.FOLLOW_LINKS)) {
                stream.filter(p-> filterPath(p, globals.excludePath)).forEach(p -> executor.submit(()->process(p, globals)));
            }
        }

        MigrationUtils.waitTaskCompleted(executor, 100);
        long endMills = execStat(mills, executor);

        System.out.println("Total size of files: " + totalSize.get());

        int i=0;
    }

    private static boolean filterPath(Path p, List<Path> excludePath) {
        return true;
    }

    public static void process(Path path, Globals globals) {
        try {
            totalSize.addAndGet(Files.size(path));
            Migration.functions.stream()
                    .filter(f-> globals.startJavaVersion < f.version() && f.version() <= globals.targetJavaVersion)
                    .flatMap(f->f.functions().stream())
                    .forEach(f-> changeContent(f, new Migration.MigrationConfig(path, globals)));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void changeContent(BiFunction<Migration.MigrationConfig, String, Content> func, Migration.MigrationConfig cfg) {
        try {
            long mills = System.currentTimeMillis();
            String content = Files.readString(cfg.path(), cfg.globals().getCharset());
            Content newContent = func.apply(cfg, content);
            if (newContent.changed()) {
                Files.writeString(cfg.path(), newContent.content(), cfg.globals().getCharset(), StandardOpenOption.SYNC, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                System.out.println("\t\tprocessed for "+(System.currentTimeMillis() - mills));
            }
        } catch (Throwable th) {
            log.error("Error with path " + cfg.path(), th);
        }
    }
}
