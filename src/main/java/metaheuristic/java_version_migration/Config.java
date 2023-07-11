package metaheuristic.java_version_migration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Sergio Lissner
 * Date: 7/10/2023
 * Time: 8:39 PM
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
@EnableConfigurationProperties(Globals.class)
public class Config {
}
