package metaheuristic.java_version_migration;

import lombok.extern.slf4j.Slf4j;
import metaheuristic.java_version_migration.migrations.MigrateSynchronizedJava21;

import java.nio.charset.Charset;
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

    public record MigrationConfig(Path path, Charset charset) {}
    public record MigrationFunctions(int version, List<Consumer<MigrationConfig>> functions) {}

    public static final List<MigrationFunctions> functions = Stream.of(
            new MigrationFunctions(21, List.of(MigrateSynchronizedJava21::migrateSynchronized))
    ).sorted(Comparator.comparingInt(MigrationFunctions::version)).toList();

}
