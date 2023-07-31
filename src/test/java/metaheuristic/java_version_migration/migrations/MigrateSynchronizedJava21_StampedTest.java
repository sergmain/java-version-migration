package metaheuristic.java_version_migration.migrations;

import org.junit.jupiter.api.Test;

import java.util.List;

import static metaheuristic.java_version_migration.migrations.MigrateSynchronizedJava21.*;
import static metaheuristic.java_version_migration.migrations.MigrateSynchronizedJava21.Type.comment;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Sergio Lissner
 * Date: 7/10/2023
 * Time: 10:27 PM
 */
public class MigrateSynchronizedJava21_StampedTest {


    @Test
    public void test_processAsMethod_1() {

        String code = """
                class Text {
                    public synchronized boolean yes() {
                        return true;
                    }
                }
                """;

        List<Position> positions = positions(code, true);
        assertEquals(1, positions.size());
        String r = processAsMethod(STAMPED_LOCK_CODE_INSTANCE, code, positions.get(0), 1, 4);

        assertEquals(
                """
                class Text {

                    private static final StampedLock lock1 = new StampedLock();

                    public boolean yes() {
                        lock1.lock();
                        try {
                        return true;
                        } finally {
                            lock1.unlock();
                        }
                    }
                }
                """,
                r);

    }

    @Test
    public void test_processAsMethod_2() {

        String code = """
                class Text {
                    public void some() {
                        int i=0;
                    }
                    
                    public synchronized boolean yes() {
                        return true;
                    }
                }
                """;

        List<Position> positions = positions(code, true);
        assertEquals(1, positions.size());
        String r = processAsMethod(STAMPED_LOCK_CODE_INSTANCE, code, positions.get(0), 1, 4);

        assertEquals(
                """
                class Text {
                    public void some() {
                        int i=0;
                    }
                
                    private static final StampedLock lock1 = new StampedLock();


                    public boolean yes() {
                        lock1.lock();
                        try {
                        return true;
                        } finally {
                            lock1.unlock();
                        }
                    }
                }
                """,
                r);

    }

    @Test
    public void test_processAsMethod_3() {
        String code = """
                public synchronized boolean yes() {
                    return true;
                }
                """;

        List<Position> positions = positions(code, true);
        assertEquals(1, positions.size());
        assertEquals(new Position(6, 20, Type.method), positions.get(0));

        String r = insertFirstPart(STAMPED_LOCK_CODE_INSTANCE, code, positions.get(0), 1, 4);

        assertEquals("""


            private static final StampedLock lock1 = new StampedLock();
        public boolean yes() {
            return true;
        }
        """,
                r);
    }

    @Test
    public void test_processAsMethod_4() {

        String code = """
                class Text {
                    public void some() {
                        int i=0;
                    }
                
                /**
                 * The stack of byte values. This class is not synchronized and should not be
                 * used by multiple threads concurrently.
                 */
                                 
                    public synchronized boolean yes() {
                        return true;
                    }
                }
                """;

        List<Position> positions = positions(code, false);
        assertEquals(comment, positions.get(0).type());
        assertEquals(2, positions.size());
        String r = processAsMethod(STAMPED_LOCK_CODE_INSTANCE, code, positions.get(1), 1, 4);

        assertEquals(
                """
                    class Text {
                        public void some() {
                            int i=0;
                        }
                    
                        private static final StampedLock lock1 = new StampedLock();
    
    
                    /**
                     * The stack of byte values. This class is not synchronized and should not be
                     * used by multiple threads concurrently.
                     */
                                     
                        public boolean yes() {
                            lock1.lock();
                            try {
                            return true;
                            } finally {
                                lock1.unlock();
                            }
                        }
                    }
                    """,
                r);

    }

    @Test
    public void test_insertTry_1() {
        String code = """
                class Text {
                    public synchronized boolean yes() {
                        return true;
                    }
                }
                """;
        List<Position> positions = positions(code, true);
        assertEquals(1, positions.size());

        int openIdx = findOpenBracket(code, positions.get(0));
        assertEquals(51, openIdx);


        int closeIdx = findCloseBracket(code, positions.get(0));
        assertEquals(78, closeIdx);

        String newCode = insertTry(STAMPED_LOCK_CODE_INSTANCE, code, openIdx, closeIdx, 1, 4);
        assertEquals(
                """
                class Text {
                    public synchronized boolean yes() {
                        lock1.lock();
                        try {
                        return true;
                        } finally {
                            lock1.unlock();
                        }
                    }
                }
                """,
                newCode);

    }

    @Test
    public void test_insertImport_1() {
        String code = """
                class Text {
                    public synchronized boolean yes() {
                        return true;
                    }
                }
                """;
        String newCode = insertImport(STAMPED_LOCK_CODE_INSTANCE, code);
        assertEquals(
                """
                    import java.util.concurrent.locks.StampedLock;
                                            
                    class Text {
                        public synchronized boolean yes() {
                            return true;
                        }
                    }
                    """,
                newCode);

    }

    @Test
    public void test_insertImport_2() {
        String code = """
                package metaheuristic;
                
                public synchronized boolean yes() {
                    return true;
                }
                """;
        String newCode = insertImport(STAMPED_LOCK_CODE_INSTANCE, code);
        assertEquals(
                """
                    package metaheuristic;
                    
                    import java.util.concurrent.locks.StampedLock;
                                            
                    public synchronized boolean yes() {
                        return true;
                    }
                    """,
                newCode);

    }

    @Test
    public void test_insertFirstPart_1() {
        String code = """
            class Text {
            
                @Override
                public synchronized boolean yes() {
                    return true;
                }
            }
            """;

        List<Position> positions = positions(code, true);
        assertEquals(1, positions.size());

        String r = insertFirstPart(STAMPED_LOCK_CODE_INSTANCE, code, positions.get(0), 1, 4);

        assertEquals("""
            class Text {


                private static final StampedLock lock1 = new StampedLock();
    
                @Override
                public boolean yes() {
                    return true;
                }
            }
            """,
                r);
    }

}
