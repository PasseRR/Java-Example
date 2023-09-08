# locks包

从Java5开始，java.util.concurrent.locks包中包含了一些锁的实现。
ReentrantLock、ReentrantReadWriteLock其实现都依赖于 [AbstractQueuedSynchronizer](AbstractQueuedSynchronizer.md)类。
同时，Lock提供了[Condition](Condition.md)接口，类似与Object的wait/notify，但比后者要方便、灵活得多。