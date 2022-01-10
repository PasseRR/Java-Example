---
layout: post
title: locks
permalink: java.util.concurrent.locks.html
order: 2
k: java.util.concurrent.locks
pk: java.util.concurrent
last_modified_at: 2022-01-10
---

> 从Java5开始，java.util.concurrent.locks包中包含了一些锁的实现。
> [ReentrantLock](ReentrantLock.md)、[ReentrantReadWriteLock](ReentrantReadWriteLock.md)其实现都依赖于
> [AbstractQueuedSynchronizer](AbstractQueuedSynchronizer.md)类。
> 同时，[Lock](Lock.md)提供了[Condition](Condition.md)接口，类似与Object的wait/notify，但比后者要方便、灵活得多。