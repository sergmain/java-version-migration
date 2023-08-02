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

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Sergio Lissner
 * Date: 7/10/2023
 * Time: 9:17 PM
 */
@Slf4j
public class MigrationUtils {

    public static final long SECONDS_MILLIS = TimeUnit.SECONDS.toMillis(1);

    public static boolean isInVariable(String content, int start) {
        int lineStart = searchStartLine(content, start);
        int quoteIdx = content.indexOf("\"", lineStart);

        return quoteIdx!=-1 && quoteIdx<start;
    }

    public static boolean isInComment(String content, int start) {
        return isInCommentBlock(content, start) || isInCommentLine(content, start);
    }

    public static boolean isInCommentBlock(String content, int start) {
        int startCommentLeft = content.lastIndexOf("/*", start);
        int endCommentLeft = content.lastIndexOf("*/", start);

        return (startCommentLeft!=-1 && endCommentLeft==-1) || (startCommentLeft>endCommentLeft);
    }

    public static boolean isInCommentLine(String content, int start) {
        for (int i = start; i >= 0; i--) {
            char c = content.charAt(i);
            if (c=='\n' || c=='\r') {
                return false;
            }
            if (c=='/' && i>0 && content.charAt(i-1)=='/') {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("BusyWait")
    public static void waitTaskCompleted(ThreadPoolExecutor executor, int numberOfPeriods) throws InterruptedException {
        int i = 0;
        while ((executor.getTaskCount() - executor.getCompletedTaskCount()) > 0) {
            Thread.sleep(SECONDS_MILLIS);
            if (++i % numberOfPeriods == 0) {
                System.out.print("total: " + executor.getTaskCount() + ", completed: " + executor.getCompletedTaskCount());
                final Runtime rt = Runtime.getRuntime();
                System.out.println(", free: " + rt.freeMemory() + ", max: " + rt.maxMemory() + ", total: " + rt.totalMemory());
                i = 0;
            }
        }
    }

    public static long execStat(long mills, ThreadPoolExecutor executor) {
        final long curr = System.currentTimeMillis();
        if (log.isInfoEnabled()) {
            final int sec = (int) ((curr - mills) / 1000);
            String s = String.format("\nprocessed %d tasks for %d seconds", executor.getTaskCount(), sec);
            if (sec!=0) {
                s += (", " + (((int) executor.getTaskCount() / sec)) + " tasks/sec");
            }
            log.info(s);
        }
        return curr;
    }

    public static int searchStartLine(String content, int start) {
        for (int i = start-1; i >=0; i--) {
            char ch = content.charAt(i);
            if (ch=='\n' || ch=='\r') {
                return i;
            }
        }
        return 0;
    }
}
