package metaheuristic.java_version_migration;

import lombok.AllArgsConstructor;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static metaheuristic.java_version_migration.MigrationUtils.execStat;

/**
 * @author Sergio Lissner
 * Date: 7/10/2023
 * Time: 8:40 PM
 */
public class MigrationProcessor {

    private static ThreadPoolExecutor executor;

    public static final AtomicLong totalSize = new AtomicLong();

    public static void migrationProcessor(final Globals globals) throws IOException, InterruptedException {
//        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(globals.threads);
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        long mills = System.currentTimeMillis();
        for (Path path : globals.startingPath) {
            final IOFileFilter filter = FileFileFilter.INSTANCE.and(new SuffixFileFilter(new String[]{".java"}));
            try (Stream<Path> stream = PathUtils.walk(path, filter, Integer.MAX_VALUE, false, FileVisitOption.FOLLOW_LINKS)) {
                stream.filter(p-> filterPath(p, globals.excludePath)).forEach(p -> process(p, globals));
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
                    .forEach(f-> f.accept(new Migration.MigrationConfig(path, globals.getCharset())));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
