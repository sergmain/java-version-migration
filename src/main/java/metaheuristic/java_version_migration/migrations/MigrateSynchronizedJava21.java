package metaheuristic.java_version_migration.migrations;

import lombok.extern.slf4j.Slf4j;
import metaheuristic.java_version_migration.Migration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Sergio Lissner
 * Date: 7/10/2023
 * Time: 10:11 PM
 */
@Slf4j
public class MigrateSynchronizedJava21 {

    public static final Pattern p = Pattern.compile("\\s+synchronized(\\s+|\\()");

    public enum Type {method, object};

    public record Position(int start, int end, Type type) {};

    public static void migrateSynchronized(Migration.MigrationConfig cfg) {
        try {
            String content = Files.readString(cfg.path(), cfg.charset());
            process(cfg.path(), content);
            //Files.writeString(cfg.path(), content, cfg.charset(), StandardOpenOption.SYNC, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Throwable th) {
            log.error("Error with path " + cfg.path(), th);
        }
    }

    private static void process(Path path, String content) {
        List<Position> positions = positions(content);
        if (positions.isEmpty()) {
            return;
        }
        System.out.println(path.toString());
        positions.forEach(p->System.out.printf("\t%d %d %s\n", p.start, p.end, p.type));

    }

    public static List<Position> positions(String content) {
        Matcher m = p.matcher(content);
        List<Position> positions = new ArrayList<>();
        while (m.find()) {
            int start = m.start();
            int end = m.end();
            String s = content.substring(start, end);
            Type type = null;
            if (s.endsWith("(")) {
                type = Type.object;
            }
            else {
                for (int i = end; i < content.length(); i++) {
                    char ch=content.charAt(i);
                    if (Character.isWhitespace(ch)) {
                        continue;
                    }
                    type = ch=='(' ? Type.object : Type.method;
                    break;
                }
            }
            if (type==null) {
                throw new IllegalStateException("(type==null)");
            }
            positions.add(new Position(start, end, type));
        }
        return positions;
    }

}
