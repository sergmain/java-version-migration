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
}
