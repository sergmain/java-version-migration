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
public class MetaheuristicMigrationApplication implements CommandLineRunner {

    private final Globals globals;
    private final ApplicationContext appCtx;

    public static void main(String[] args) {
        SpringApplication.run(MetaheuristicMigrationApplication.class, args);
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
