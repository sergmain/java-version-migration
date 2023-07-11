package metaheuristic.java_version_migration.migrations;

import lombok.extern.slf4j.Slf4j;
import metaheuristic.java_version_migration.Migration;
import metaheuristic.java_version_migration.MigrationUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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

    public static final Pattern SYNC_PATTERN = Pattern.compile("\\s+synchronized(\\s+|\\()");
    public static final Pattern PACKAGE_PATTERN = Pattern.compile("package\\s+");

    public enum Type {method, object};

    public record Position(int start, int end, Type type) {};
    public record Content(String content, boolean changed) {};

    public static void migrateSynchronized(Migration.MigrationConfig cfg) {
        try {
            long mills = System.currentTimeMillis();
            String content = Files.readString(cfg.path(), cfg.globals().getCharset());
            Content newContent = process(cfg, content);
            if (newContent.changed) {
//                Files.writeString(cfg.path(), newContent.content, cfg.globals().getCharset(), StandardOpenOption.SYNC, StandardOpenOption.TRUNCATE_EXISTING);
                Files.writeString(cfg.path(), newContent.content, cfg.globals().getCharset(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                System.out.println("\t\tprocessed for "+(System.currentTimeMillis() - mills));
            }
        } catch (Throwable th) {
            log.error("Error with path " + cfg.path(), th);
        }
    }

    private static Content process(Migration.MigrationConfig cfg, String content) {
        Path path = cfg.path();
        List<Position> positions = positions(content, true);
        if (positions.isEmpty()) {
            return new Content(content, false);
        }
        System.out.println(path.toString());
        positions.forEach(p->System.out.printf("\t%d %d %s\n", p.start, p.end, p.type));

        String code = content;
        int idx = 0;
        boolean found = false;
        int startOffset = -1;
        while (!(positions = positions(startOffset, code, true)).isEmpty()) {
            found = true;
            Position position = positions.get(0);
            if (MigrationUtils.isInCommentBlock(code, position.start)) {
                startOffset = position.start+1;
                continue;
            }
            switch (position.type) {
                case method -> code = processAsMethod(code, position, idx, cfg.globals().offset);
                case object -> code= processAsObject(code, position, idx, cfg.globals().offset);
            }
        }
        if (found) {
            code = insertImport(code);
        }
        return new Content(code, found);
    }

    public static String insertImport(String content) {
        Matcher m = PACKAGE_PATTERN.matcher(content);
        String code;
        if (m.find()) {
            int packageIdx = m.start();
            int idx = content.indexOf(";", packageIdx);
            if (idx==-1) {
                throw new IllegalStateException("(idx==-1)");
            }

            code = content.substring(0, idx+1) + "\n\nimport java.util.concurrent.locks.ReentrantReadWriteLock;" + content.substring(idx+1);
        }
        else {
            code = "import java.util.concurrent.locks.ReentrantReadWriteLock;\n\n" + content;
        }
        return code;
    }

    private static String processAsObject(String content, Position position, int idx, int offset) {

        return "";
    }

    public static String processAsMethod(String content, Position position, int idx, int offset) {
        String code = appendSecondPart(content, position, idx, offset);
        code = insertFirstPart(code, position, idx, offset);

        return code;
    }

    private static String appendSecondPart(String content, Position position, int idx, int offset) {
        int openBracket = findOpenBracket(content, position);
        int closeBracket = findCloseBracket(content, position);

        String code = insertTry(content, openBracket, closeBracket, idx, offset);

        return code;
    }

    public static String insertTry(String content, int openBracket, int closeBracket, int idx, int offsetInt) {
        String method = content.substring(openBracket+1, closeBracket);
        String offset = " ".repeat(offsetInt);
        String open = String.format(
                """
                
                %swriteLock%d.lock();
                %stry {""", offset, idx, offset);
        String close = String.format(
                """
                %s} finally {
                %s    writeLock%d.unlock();
                %s}
                """, offset, offset, idx, offset);

        return content.substring(0, openBracket+1) +
               open +
               method +
               close +
               content.substring(closeBracket);
    }

    public static int findOpenBracket(String content, Position position) {
        for (int i = position.end; i < content.length(); i++) {
            if (content.charAt(i)=='{') {
                return i;
            }
        }
        throw new IllegalStateException("not found");
    }

    public static int findCloseBracket(String content, Position position) {
        int countOpen = 0;
        for (int i = position.end; i < content.length(); i++) {
            final char ch = content.charAt(i);
            if (ch=='{') {
                ++countOpen;
                continue;
            }
            if (ch=='}') {
                --countOpen;
                if (countOpen==0) {
                    return i;
                }
            }
        }
        throw new IllegalStateException("not found");
    }

    public static String insertFirstPart(String content, Position position, int idx, int offset) {
        int posPrevDelimiter = searchPrevDelimiter(content, position.start);
        int posStartLine = searchStartLine(content, position.start);

        int pos = Math.max(posPrevDelimiter, posStartLine);

        boolean atStart = (pos==0);
        StringBuilder sb = new StringBuilder();
        sb.append(content, 0, pos+(atStart ? 0 : 1 ));
        String lock = appendReadWriteLock(idx, offset, atStart);
        sb.append(lock).append(content, pos+(atStart ? 0 : 1 ), position.start);
        if (Character.isWhitespace(content.charAt(position.start))) {
            sb.append(' ');
        }
        sb.append(content, position.end, content.length());

        return sb.toString();
    }

    private static int searchStartLine(String content, int start) {
        for (int i = start-1; i >=0; i--) {
            char ch = content.charAt(i);
            if (ch=='\n' || ch=='\r') {
                return i;
            }
        }
        return 0;
    }

    private static String appendReadWriteLock(int idx, int offsetInt, boolean atStart) {
        String offset = " ".repeat(offsetInt);
        String lock = String.format(
                """
                %sprivate static final ReentrantReadWriteLock lock%d = new ReentrantReadWriteLock();
                %spublic static final ReentrantReadWriteLock.ReadLock readLock%d = lock%d.readLock();
                %spublic static final ReentrantReadWriteLock.WriteLock writeLock%d = lock%d.writeLock();
                
                """, offset, idx, offset, idx, idx, offset, idx, idx);

        return lock;
    }

    private static int searchPrevDelimiter(String content, int start) {
        for (int i = start-1; i >=0; i--) {
            char ch = content.charAt(i);
            if (ch==';' || ch=='}' || ch=='{') {
                return i;
            }
        }
        return 0;
    }

    public static List<Position> positions(String content, boolean onlyFirst) {
        return positions(-1,  content, onlyFirst);
    }

    public static List<Position> positions(int startOffset, String content, boolean onlyFirst) {
        Matcher m = SYNC_PATTERN.matcher(content);
        List<Position> positions = new ArrayList<>();
        while (m.find()) {
            int start = m.start();
            if (start<=startOffset) {
                continue;
            }
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
            if (onlyFirst) {
                break;
            }
        }
        return positions;
    }

}
