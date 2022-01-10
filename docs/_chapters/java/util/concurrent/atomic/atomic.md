---
layout: post
title: atomic
permalink: java.util.concurrent.atomic.html
order: 1
k: java.util.concurrent.atomic
pk: java.util.concurrent
last_modified_at: 2022-01-10
---

> atomic包是专门为线程安全设计的Java包，包含多个原子操作类。
> 在atomic中频繁的用到了[Unsafe](../../../../sun/misc/Unsafe.md)，如果不明白可以先看看Unsafe的API。

Atomic类型主要分为以下几类:  
- 基本类:AtomicInteger、AtomicLong、AtomicBoolean  
- 引用类型:AtomicReference、AtomicStampedReference、AtomicMarkableReference  
- 数组类型:AtomicIntegerArray、AtomicLongArray、AtomicReferenceArray  
- 属性原子修改器:AtomicIntegerFieldUpdater、AtomicLongFieldUpdater、AtomicReferenceFieldUpdater  
- 累加器:DoubleAccumulator、DoubleAdder、LongAccumulator、LongAdder  


## 自旋锁
与互斥锁相似，基本作用是用于线程（进程）之间的同步，该锁作用于共享资源。
与互斥锁不同的是该锁不会让没获得锁的线程阻塞，没获得锁的线程不会放弃CPU时间片，
而是在原地忙等，直到锁的持有者释放锁，可见，自旋锁是非阻塞锁。  
可能引起的问题:  
- 过多的占据CPU时间  
- 死锁  
java中可以通过Unsafe中的CAS实现自旋锁，compareAndSwapInt、compareAndSwapLong、compareAndSwapObject。

## CAS
CAS有3个操作数，内存值V，旧的预期值A，要修改的新值B。当且仅当预期值A和内存值V相同时，将内存值V修改为B，否则什么都不做。
CAS是非阻塞的，一个线程的失败或者挂起不应该影响其他线程的失败或挂起的算法。

## ABA问题
CAS看起来很爽，但是会导致`ABA问题`。  
ABA：如果另一个线程修改V值假设原来是A，先修改成B，再修改回成A。当前线程的CAS操作无法分辨当前V值是否发生过变化。  
> 关于ABA问题的一个例子：在你非常渴的情况下你发现一个盛满水的杯子，你一饮而尽，
> 之后再给杯子里重新倒满水。然后你离开，当杯子的真正主人回来时看到杯子还是盛满水，
> 他当然不知道是否被人喝完重新倒满。解决这个问题的方案的一个策略是每一次倒水假设有一个自动记录仪记录下，
> 这样主人回来就可以分辨在她离开后是否发生过重新倒满的情况。这也是解决ABA问题目前采用的策略。
> ------  自[知乎](https://www.zhihu.com/question/23281499/answer/24112589)

