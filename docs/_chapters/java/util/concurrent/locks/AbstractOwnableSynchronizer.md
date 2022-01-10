---
layout: post
title: AbstractOwnableSynchronizer
permalink: java.util.concurrent.locks.AbstractOwnableSynchronizer.html
order: 1
pk: java.util.concurrent.locks
last_modified_at: 2022-01-10
---

> AbstractOwnableSynchronizer一个抽象类，所有同步器的基类。

## 域及方法
```java
public abstract class AbstractOwnableSynchronizer
    implements java.io.Serializable {
    private static final long serialVersionUID = 3737899427754241961L;
    // 受保护的构造方法 子类实例化
    protected AbstractOwnableSynchronizer() { }
    // 持有独占锁的线程
    private transient Thread exclusiveOwnerThread;
    // 设置持独占锁线程
    protected final void setExclusiveOwnerThread(Thread thread) {
        exclusiveOwnerThread = thread;
    }
    // 获得持独占锁线程
    protected final Thread getExclusiveOwnerThread() {
        return exclusiveOwnerThread;
    }
}
```