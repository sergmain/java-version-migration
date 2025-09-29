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

package metaheuristic.java_version_migration.migrations;

import lombok.extern.slf4j.Slf4j;
import metaheuristic.java_version_migration.Globals;
import metaheuristic.java_version_migration.Migration;
import metaheuristic.java_version_migration.utils.MigrationUtils;
import metaheuristic.java_version_migration.data.Content;
import metaheuristic.java_version_migration.utils.MetaUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static metaheuristic.java_version_migration.utils.MigrationUtils.isInComment;

/**
 * @author Sergio Lissner
 * Date: 7/10/2023
 * Time: 10:11 PM
 */
@Slf4j
public class MigrateSynchronizedJava21 {

    interface LockerTypeCode {
        String appendDeclarationLockVariables(int idx, int offsetInt);
        String getImport();

        String getCloseTry(int idx, String offset, String doubleOffset);
        String getOpenTry(int idx, String doubleOffset);
    }

    public static final ReentrantReadWriteLockCode REENTRANT_READ_WRITE_LOCK_CODE_INSTANCE = new ReentrantReadWriteLockCode();
    public static final StampedLockCode STAMPED_LOCK_CODE_INSTANCE = new StampedLockCode();

    public static final Pattern SYNC_PATTERN = Pattern.compile("\\s+synchronized(\\s+|\\()");
    public static final Pattern PACKAGE_PATTERN = Pattern.compile("package\\s+");

    public static final String MIGRATE_SYNCHRONIZED_LOCKER = "migrateSynchronizedLocker";

    public enum Type {method(false), object(false), comment(true), variable(true);
        public final boolean skip;
        Type(boolean skip) {
            this.skip = skip;
        }
    };
    public enum LockerType { StampedLock(STAMPED_LOCK_CODE_INSTANCE), ReentrantReadWriteLock(REENTRANT_READ_WRITE_LOCK_CODE_INSTANCE);

        public final LockerTypeCode lockerTypeCode;

        LockerType(LockerTypeCode lockerTypeCode) {
            this.lockerTypeCode = lockerTypeCode;
        }
    }

    public record Position(int start, int end, Type type) {};;

    public static LockerType getLockerType(Globals globals) {
        final String type = MetaUtils.getValue(globals.metas, MIGRATE_SYNCHRONIZED_LOCKER);
        return type==null ? LockerType.ReentrantReadWriteLock : LockerType.valueOf(type);
    }

    public static Content process(Migration.MigrationConfig cfg, Globals globals, String content) {

        Path path = cfg.path();
        List<Position> positions = positions(content, true);
        if (positions.isEmpty()) {
            return new Content(content, false);
        }
        LockerType lockerType = getLockerType(globals);
        System.out.println(path.toString());
        positions.forEach(p->System.out.printf("\t%d %d %s\n", p.start, p.end, p.type));

        String code = content;
        int idx = 0;
        boolean changed = false;
        int startOffset = -1;
        while (!(positions = positions(startOffset, code, true)).isEmpty()) {
            Position position = positions.get(0);
            if (position.type.skip) {
                startOffset = position.start+1;
                continue;
            }
            switch (position.type) {
                case method -> {
                    code = processAsMethod(lockerType.lockerTypeCode, code, position, idx++, globals.offset);
                    changed = true;
                }
                case object -> {
                    code= processAsObject(code, position, idx++, globals.offset);
                    startOffset = position.start+1;
                }
            }
        }
        if (changed) {
            code = insertImport(lockerType.lockerTypeCode, code);
        }
        return new Content(code, changed);
    }

    public static String insertImport(LockerTypeCode lockerTypeCode, String content) {
        Matcher m = PACKAGE_PATTERN.matcher(content);
        String code;
        if (m.find()) {
            int packageIdx = m.start();
            int idx = content.indexOf(";", packageIdx);
            if (idx==-1) {
                throw new IllegalStateException("(idx==-1)");
            }

            code = content.substring(0, idx+1) + "\n\n"+ lockerTypeCode.getImport() + content.substring(idx+1);
        }
        else {
            code = lockerTypeCode.getImport()+"\n\n" + content;
        }
        return code;
    }

    private static String processAsObject(String content, Position position, int idx, int offset) {

        return content;
    }

    public static String processAsMethod(LockerTypeCode lockerTypeCode, String content, Position position, int idx, int offset) {
        String code = appendSecondPart(lockerTypeCode, content, position, idx, offset);
        code = insertFirstPart(lockerTypeCode, code, position, idx, offset);

        return code;
    }

    private static String appendSecondPart(LockerTypeCode lockerTypeCode, String content, Position position, int idx, int offset) {
        int openBracket = findOpenBracket(content, position);
        int closeBracket = findCloseBracket(content, position);

        String code = insertTry(lockerTypeCode, content, openBracket, closeBracket, idx, offset);

        return code;
    }

    public static String insertTry(LockerTypeCode lockerTypeCode, String content, int openBracket, int closeBracket, int idx, int offsetInt) {
        String offset = " ".repeat(offsetInt);
        String doubleOffset = " ".repeat(offsetInt*2);
        String open = lockerTypeCode.getOpenTry(idx, doubleOffset);
        String close = lockerTypeCode.getCloseTry(idx, offset, doubleOffset);

        String method = content.substring(openBracket+1, closeBracket);
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

    public static String insertFirstPart(LockerTypeCode lockerTypeCode, String content, Position position, int idx, int offset) {
        int pos = calcPos(content, position);

        StringBuilder sb = new StringBuilder();
        sb.append(content, 0, pos);
        String lock = lockerTypeCode.appendDeclarationLockVariables(idx, offset);
        sb.append(lock).append(content, pos, position.start);
        if (Character.isWhitespace(content.charAt(position.start))) {
            sb.append(' ');
        }
        sb.append(content, position.end, content.length());

        return sb.toString();
    }

    public static int calcPos(String content, Position position) {
        int posPrevDelimiter = searchPrevDelimiter(content, position.start);
        int posPrevAnnotation = searchPrevAnnotation(content, posPrevDelimiter, position.start);
        int posStartLine = MigrationUtils.searchStartLine(content, position.start);

        int pos = Math.max(posPrevDelimiter, Math.min(posPrevAnnotation, posStartLine));

        boolean atStart = (pos==0 || pos==posPrevAnnotation) ;

        int offset = 0;
        if (!atStart) {
            offset++;
        }
        return pos + offset;
    }

    public static int searchPrevDelimiter(String content, int start) {
        for (int i = start-1; i >=0; i--) {
            char ch = content.charAt(i);
            if (ch==';' || ch=='}' || ch=='{') {
                if (isInComment(content, i)) {
                    continue;
                }
                return i;
            }
        }
        return 0;
    }

    public static int searchPrevAnnotation(String content, int start, int end) {

        for (int i = start+1; i <=end; i++) {
            char ch = content.charAt(i);
            if (ch=='@') {
                if (isInComment(content, i)) {
                    continue;
                }
                return MigrationUtils.searchStartLine(content, i);
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
            int end = m.end();
            if (start<=startOffset) {
                continue;
            }
            Type type = null;
            if (isInComment(content, start)) {
                type = Type.comment;;
            }
            else if (MigrationUtils.isInVariable(content, start)) {
                type = Type.variable;;
            }
            else {
                String s = content.substring(start, end);
                if (s.endsWith("(")) {
                    type = Type.object;
                }
                else {
                    for (int i = end; i < content.length(); i++) {
                        char ch = content.charAt(i);
                        if (Character.isWhitespace(ch)) {
                            continue;
                        }
                        type = ch=='(' ? Type.object : Type.method;
                        break;
                    }
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
