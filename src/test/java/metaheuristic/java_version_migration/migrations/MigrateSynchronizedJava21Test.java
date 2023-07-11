package metaheuristic.java_version_migration.migrations;

import org.junit.jupiter.api.Test;

import java.util.List;

import static metaheuristic.java_version_migration.migrations.MigrateSynchronizedJava21.*;
import static metaheuristic.java_version_migration.migrations.MigrateSynchronizedJava21.Type.comment;
import static metaheuristic.java_version_migration.migrations.MigrateSynchronizedJava21.Type.variable;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Sergio Lissner
 * Date: 7/10/2023
 * Time: 10:27 PM
 */
public class MigrateSynchronizedJava21Test {

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
        String r = processAsMethod(code, positions.get(0), 1, 4);

        assertEquals(
                """
                class Text {

                    private static final ReentrantReadWriteLock lock1 = new ReentrantReadWriteLock();
                    public static final ReentrantReadWriteLock.ReadLock readLock1 = lock1.readLock();
                    public static final ReentrantReadWriteLock.WriteLock writeLock1 = lock1.writeLock();

                    public boolean yes() {
                        writeLock1.lock();
                        try {
                        return true;
                        } finally {
                            writeLock1.unlock();
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
        String r = processAsMethod(code, positions.get(0), 1, 4);

        assertEquals(
                """
                class Text {
                    public void some() {
                        int i=0;
                    }
                
                    private static final ReentrantReadWriteLock lock1 = new ReentrantReadWriteLock();
                    public static final ReentrantReadWriteLock.ReadLock readLock1 = lock1.readLock();
                    public static final ReentrantReadWriteLock.WriteLock writeLock1 = lock1.writeLock();


                    public boolean yes() {
                        writeLock1.lock();
                        try {
                        return true;
                        } finally {
                            writeLock1.unlock();
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

        String r = insertFirstPart(code, positions.get(0), 1, 4);

        assertEquals("""


            private static final ReentrantReadWriteLock lock1 = new ReentrantReadWriteLock();
            public static final ReentrantReadWriteLock.ReadLock readLock1 = lock1.readLock();
            public static final ReentrantReadWriteLock.WriteLock writeLock1 = lock1.writeLock();
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
        String r = processAsMethod(code, positions.get(1), 1, 4);

        assertEquals(
            """
                class Text {
                    public void some() {
                        int i=0;
                    }
                
                    private static final ReentrantReadWriteLock lock1 = new ReentrantReadWriteLock();
                    public static final ReentrantReadWriteLock.ReadLock readLock1 = lock1.readLock();
                    public static final ReentrantReadWriteLock.WriteLock writeLock1 = lock1.writeLock();


                /**
                 * The stack of byte values. This class is not synchronized and should not be
                 * used by multiple threads concurrently.
                 */
                                 
                    public boolean yes() {
                        writeLock1.lock();
                        try {
                        return true;
                        } finally {
                            writeLock1.unlock();
                        }
                    }
                }
                """,
                r);

    }

    @Test
    public void test_positions_1() {

        String code = """
                public synchronized boolean yes() {
                    return true;
                }

                public boolean no() {
                synchronized(this)  {
                    return false;
                }

                public Boolean maybe() {
                synchronized
                (this)  {
                    return null;
                }
                }
                """;

        List<Position> positions = positions(code, false);
        assertEquals(3, positions.size());
        assertEquals(new Position(6, 20, Type.method), positions.get(0));
        assertEquals(new Position(77, 91, Type.object), positions.get(1));
        assertEquals(new Position(145, 159, Type.object), positions.get(2));

        int i=0;
    }

    @Test
    public void test_positions_2() {

        String code = """
                public Boolean maybe() {
                synchronized
                (this)  {
                    return null;
                }
                }
                """;

        List<Position> positions = positions(code, true);
        assertEquals(1, positions.size());
        assertEquals(new Position(24, 38, Type.object), positions.get(0));

        int i=0;
    }

    @Test
    public void test_positions_3() {

        String code = """
                public void some() {
                    int i=0;
                }
                
                private final Set<SessionLocal> userSessions = Collections.synchronizedSet(new HashSet<>());
                                 
                public boolean yes() {
                    return true;
                }
                """;

        List<Position> positions = positions(code, false);
        assertEquals(0, positions.size());

    }

    @Test
    public void test_positions_4() {

        String code = """
                public void some() {
                    int i=0;
                }
                
                /**
                 * The stack of byte values. This class is not synchronized and should not be
                 * used by multiple threads concurrently.
                 */
                                 
                public boolean yes() {
                    return true;
                }
                """;

        List<Position> positions = positions(code, true);
        assertEquals(comment, positions.get(0).type());
    }

    @Test
    public void test_positions_5() {

        String code = """
                public void some() {
                    int i=0;
                }
                
                
                // The stack of byte values. This class is not synchronized and should not be
                                 
                public boolean yes() {
                    return true;
                }
                """;

        List<Position> positions = positions(code, true);
        assertEquals(comment, positions.get(0).type());
    }

    @Test
    public void test_positions_6() {

        String code = """
                /*
                 * First comment
                 */
                package test;
                
                /**
                 * stack is synchronized
                 */
                                  
                public class Test {
                    public boolean yes() {
                        return true;
                    }
                }
                """;

        List<Position> positions = positions(code, true);
        assertEquals(comment, positions.get(0).type());
    }

    @Test
    public void test_positions_7() {

        String code = """
                public class Test {
                    public boolean yes() {
                        String message = " synchronized after ";
                        return true;
                    }
                }
                """;

        List<Position> positions = positions(code, true);
        assertEquals(variable, positions.get(0).type());
    }

    @Test
    public void test_findCloseBracket_1() {
        String code = """
                public synchronized boolean yes() {
                      return true  ;
                }
                """;
        List<Position> positions = positions(code, true);
        assertEquals(1, positions.size());
        assertEquals(new Position(6, 20, Type.method), positions.get(0));

        int idx = findCloseBracket(code, positions.get(0));
        assertEquals(57, idx);
    }

    @Test
    public void test_findCloseBracket_2() {
        String code = """
                public synchronized boolean yes() {
                    {{return true}};
                }
                """;
        List<Position> positions = positions(code, true);
        assertEquals(1, positions.size());
        assertEquals(new Position(6, 20, Type.method), positions.get(0));

        int idx = findCloseBracket(code, positions.get(0));
        assertEquals(57, idx);
    }

    @Test
    public void test_findOpenBracket_1() {
        String code = """
                public synchronized boolean yes() {
                    return true;
                }
                """;
        List<Position> positions = positions(code, true);
        assertEquals(1, positions.size());

        int idx = findOpenBracket(code, positions.get(0));
        assertEquals(34, idx);

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

        String newCode = insertTry(code, openIdx, closeIdx, 1, 4);
        assertEquals(
                """
                class Text {
                    public synchronized boolean yes() {
                        writeLock1.lock();
                        try {
                        return true;
                        } finally {
                            writeLock1.unlock();
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
        String newCode = insertImport(code);
        assertEquals(
                """
                    import java.util.concurrent.locks.ReentrantReadWriteLock;
                                            
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
        String newCode = insertImport(code);
        assertEquals(
                """
                    package metaheuristic;
                    
                    import java.util.concurrent.locks.ReentrantReadWriteLock;
                                            
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

        String r = insertFirstPart(code, positions.get(0), 1, 4);

        assertEquals("""
            class Text {


                private static final ReentrantReadWriteLock lock1 = new ReentrantReadWriteLock();
                public static final ReentrantReadWriteLock.ReadLock readLock1 = lock1.readLock();
                public static final ReentrantReadWriteLock.WriteLock writeLock1 = lock1.writeLock();
    
                @Override
                public boolean yes() {
                    return true;
                }
            }
            """,
                r);
    }

    @Test
    public void test_searchPrevDelimiter_1() {
        String code = """
            {
            /*
             * {
             * }
             */

                @Override
                public synchronized boolean yes() {
                    return true;
                }
            }
            """;

        List<Position> positions = positions(code, true);
        assertEquals(1, positions.size());

        int idx = searchPrevDelimiter(code, positions.get(0).start());
        assertEquals(0, idx);
    }

    @Test
    public void test_searchPrevAnnotation_1() {
        String code = """
            {

                @Override
                public synchronized boolean yes() {
                    return true;
                }
            }
            """;

        List<Position> positions = positions(code, true);
        assertEquals(1, positions.size());

        int prevDelimiter = searchPrevDelimiter(code, positions.get(0).start());

        int idx = searchPrevAnnotation(code, prevDelimiter, positions.get(0).start());
        assertEquals(2, idx);
    }



}
