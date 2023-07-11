package metaheuristic.java_version_migration;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * @author Sergio Lissner
 * Date: 7/10/2023
 * Time: 8:33 PM
 */
@SpringBootApplication
@Slf4j
@AllArgsConstructor
public class MetaheuristicJavaVersionMigration implements CommandLineRunner {

    private final Globals globals;
    private final ApplicationContext appCtx;

    public static void main(String[] args) {
        SpringApplication.run(MetaheuristicJavaVersionMigration.class, args);
    }

    @SuppressWarnings("finally")
    @Override
    public void run(String... args) {
        System.out.println("Started at " + LocalDateTime.now());

        try {
            MigrationProcessor.migrationProcessor(globals);
            System.out.println("Finished at " + LocalDateTime.now());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Throwable th) {
            log.error("Error", th);
        } finally {
            System.exit(SpringApplication.exit(appCtx, () -> 0));
        }
    }
}
