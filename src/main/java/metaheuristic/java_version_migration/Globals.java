package metaheuristic.java_version_migration;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Sergio Lissner
 * Date: 7/10/2023
 * Time: 8:37 PM
 */
@ConfigurationProperties("migration")
@Getter
@Setter
@Slf4j
@RequiredArgsConstructor
public class Globals {

    public int startJavaVersion;
    public int targetJavaVersion;
    public int threads = 4;
    private Charset charset;

    // number of spaces as offset in code
    public int offset = 4;

    public final List<Path> startingPath = new ArrayList<>();
    public final List<Path> excludePath = new ArrayList<>();

    public Charset getCharset() {
        return charset==null ? StandardCharsets.UTF_8 : charset;
    }

    @PostConstruct
    public void init() {
        if (startingPath.isEmpty()) {
            startingPath.add(Path.of("src"));
        }
    }
}
