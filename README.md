# java-version-migration
Utility to help migrate java project to new java version. I.E. for java 21, it  will migrate synchronized to ReentrantReadWriteLock

I.E.
before
```java
public synchronized boolean yes() {
    return true;
}
```

after
```java
private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
private static final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
private static final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

public boolean yes() {
    writeLock.lock();
    try {
        return true;
    } finally {
        writeLock.unlock();
    }
}
```

default lock implementation which will be used is ReentrantReadWriteLock
```yaml
    - migrateSynchronizedLocker: ReentrantReadWriteLock
```

it can be changed to StampedLock with
```yaml
    - migrateSynchronizedLocker: StampedLock
```

see config/application.sample.yml 

### How to use

- copy file config/application.sample.yml to config/application.yml
- make value of field startingPath to directory with .java files, i.e. src
- run metaheuristic.java_version_migration.MetaheuristicJavaVersionMigration.main

### Roadmap

[ ] add a support of migration of ```synchronized (variable) {}```   
[ ] make field excludePath meaningful