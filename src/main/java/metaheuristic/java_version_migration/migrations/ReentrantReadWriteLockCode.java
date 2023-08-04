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

/**
 * @author Sergio Lissner
 * Date: 8/3/2023
 * Time: 11:46 PM
 */
public class ReentrantReadWriteLockCode implements MigrateSynchronizedJava21.LockerTypeCode {
    public String appendDeclarationLockVariables(int idx, int offsetInt) {
        String offset = " ".repeat(offsetInt);
        String lock = String.format(
                """
                                            
                                            
                        %sprivate static final ReentrantReadWriteLock lock%d = new ReentrantReadWriteLock();
                        %sprivate static final ReentrantReadWriteLock.ReadLock readLock%d = lock%d.readLock();
                        %sprivate static final ReentrantReadWriteLock.WriteLock writeLock%d = lock%d.writeLock();
                        """, offset, idx, offset, idx, idx, offset, idx, idx);

        return lock;
    }

    @Override
    public String getImport() {
        return "import java.util.concurrent.locks.ReentrantReadWriteLock;";
    }

    @Override
    public String getCloseTry(int idx, String offset, String doubleOffset) {
        String close = String.format(
                """
                        %s} finally {
                        %s    writeLock%d.unlock();
                        %s}
                        %s""", offset, doubleOffset, idx, doubleOffset, offset);
        return close;
    }

    @Override
    public String getOpenTry(int idx, String doubleOffset) {
        String open = String.format(
                """
                                            
                        %swriteLock%d.lock();
                        %stry {""", doubleOffset, idx, doubleOffset);
        return open;
    }
}
