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
