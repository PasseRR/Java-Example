---
layout: page
title: DelayQueue
permalink: java.util.concurrent.DelayQueue.html
---
> DelayQueue对元素进行持有直到一个特定的延迟到期，不允许null元素且注入其中的元素必须实现java.util.concurrent.Delayed接口。
> PriorityQueue是一个根据队列里元素某些属性排列先后的顺序队列，DelayQueue其实就是在每次往优先级队列中添加元素，
> 然后以元素的delay过期值作为排序的因素，以此来达到先过期的元素会排在队首，每次从队列里取出来都是最先过期的元素。  

## 源码分析 
DelayQueue是容量无界的最大为Integer.MAX_VALUE，默认容量为11，若容量不够以当前容量50%递增，
可以使用一个现有集合对象初始化。  
* DelayQueue()  
* DelayQueue(Collection<? extends E> c)  
元素需要实现的接口**Delayed**  

```java
public interface Delayed extends Comparable<Delayed> {
    /**
     * Returns the remaining delay associated with this object, in the
     * given time unit.
     *
     * @param unit the time unit
     * @return the remaining delay; zero or negative values indicate
     * that the delay has already elapsed
     */
    long getDelay(TimeUnit unit);
}
```

### 属性

```java
// 重入锁
private final transient ReentrantLock lock = new ReentrantLock();
// 锁条件对象
private final Condition available = lock.newCondition();
// 根据delay时间排序的优先队列
private final PriorityQueue<E> q = new PriorityQueue<E>();
/**
 * Thread designated to wait for the element at the head of
 * the queue.  This variant of the Leader-Follower pattern
 * (http://www.cs.wustl.edu/~schmidt/POSA/POSA2/) serves to
 * minimize unnecessary timed waiting.  When a thread becomes
 * the leader, it waits only for the next delay to elapse, but
 * other threads await indefinitely.  The leader thread must
 * signal some other thread before returning from take() or
 * poll(...), unless some other thread becomes leader in the
 * interim.  Whenever the head of the queue is replaced with
 * an element with an earlier expiration time, the leader
 * field is invalidated by being reset to null, and some
 * waiting thread, but not necessarily the current leader, is
 * signalled.  So waiting threads must be prepared to acquire
 * and lose leadership while waiting.
 */
// 用于优化阻塞通知的线程元素leader 用leader来减少不必要的等待时间
// 
private Thread leader = null;
```

leader的作用?  
> leader的主要作用用于减少不必要的阻塞时间，例如有多个消费者线程用take方法去取，
> 内部先加锁，然后每个线程都去peek第一个节点。如果leader不为空说明已经有线程在取了，设置当前线程阻塞。
> 如果为空说明没有其他线程去取这个节点，设置leader并等待delay延时到期，直到poll后结束循环。

### 方法
#### 添加元素
* boolean offer(E e)  
```java
public boolean offer(E e) {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        q.offer(e); // 优先队列入队
        if (q.peek() == e) { // 查看元素是否是优先队列队首
            leader = null; // 设置leader为空 唤醒take线程
            available.signal();
        }
        return true;
    } finally {
        lock.unlock();
    }
}
```
* boolean offer(E e, long timeout, TimeUnit unit)  
同offer(E e)方法 因为队列没有容量限制故没有超时的offer方法  
* boolean add(E e)  
同offer(E e)方法 因为队列没有容量限制故没有抛出异常的add方法  
* void put(E e)  
同offer(E e)方法 因为队列没有容量限制故没有阻塞的put方法  
#### 移除元素
* E poll()  
```java
public E poll() {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        E first = q.peek(); // 查看优先队队首
        // 如果优先队列为空或队首delay时间为达到返回null
        if (first == null || first.getDelay(NANOSECONDS) > 0)
            return null;
        else
            return q.poll(); // 优先队列出队
    } finally {
        lock.unlock();
    }
}
```
* E poll(long timeout, TimeUnit unit)  
```java
 public E poll(long timeout, TimeUnit unit) throws InterruptedException {
    long nanos = unit.toNanos(timeout);
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();
    try {
        for (;;) {
            E first = q.peek();
            if (first == null) { // 当优先队列为空时
                if (nanos <= 0) // 超过dequeue操作的timeout时间 返回null
                    return null;
                else // dequeue操作阻塞
                    nanos = available.awaitNanos(nanos);
            } else {
                long delay = first.getDelay(NANOSECONDS);
                if (delay <= 0)
                    return q.poll();
                if (nanos <= 0)
                    return null;
                // 如果优先队列队首未达到可用时间 释放队首节点避免内存溢出
                first = null; // don't retain ref while waiting
                // 如果队首delay时间大于poll等待时间
                // 或者leader不为null(已有其他线程在阻塞队首delay时间)
                // 继续进行poll等待
                if (nanos < delay || leader != null)
                    nanos = available.awaitNanos(nanos);
                else {
                    // 如果队首delay时间小于poll阻塞时间
                    // 且leader为null(没有线程在阻塞队首delay时间)
                    // 将当前线程设置为leader
                    Thread thisThread = Thread.currentThread();
                    leader = thisThread;
                    try {
                        // 阻塞delay时间
                        long timeLeft = available.awaitNanos(delay);
                        // 设置poll需要阻塞的时间
                        nanos -= delay - timeLeft;
                    } finally {
                        // 释放leader
                        if (leader == thisThread)
                            leader = null;
                    }
                }
            }
        }
    } finally {
        // 如果没有线程阻塞delay且优先队列队首不为null
        if (leader == null && q.peek() != null)
            available.signal(); // 唤醒等待消费的线程
        lock.unlock();
    }
}
```
出队方法中为什么要释放first元素?  
> 假如有线程A和B都来获取队首，如果线程A阻塞完毕，获取对象成功，出队完成。
> 这个对象理应被GC回收，但是他还被线程B持有着，GC链可达，所以不能回收这个first。

* E remove()  
```java
public E remove() {
    E x = poll(); // 见上poll()方法
    if (x != null)
        return x;
    else
        throw new NoSuchElementException();
}
```
* boolean remove(Object o)  
```java
public boolean remove(Object o) {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        // 调用优先队列remove(Object o)方法
        return q.remove(o);
    } finally {
        lock.unlock();
    }
}
```
* E take()  
```java
public E take() throws InterruptedException {
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();
    try {
        for (;;) {
            E first = q.peek();
            if (first == null) // 如果队首为null 阻塞当前线程
                available.await();
            else {
                long delay = first.getDelay(NANOSECONDS);
                if (delay <= 0) // 队首可用 队列弹出队首元素
                    return q.poll();
                // 防止内存溢出
                first = null; // don't retain ref while waiting
                if (leader != null) // 如果有其他线程在阻塞队首delay时间 阻塞当前线程
                    available.await();
                else {
                    Thread thisThread = Thread.currentThread();
                    leader = thisThread;
                    try {
                        // 阻塞队首delay时间
                        available.awaitNanos(delay);
                    } finally {
                        if (leader == thisThread)
                            leader = null;
                    }
                }
            }
        }
    } finally {
        if (leader == null && q.peek() != null)
            available.signal();
        lock.unlock();
    }
}
```

## 总结
DelayQueue底层是一个优先队列(java.util.PriorityQueue)，使用一个重入锁和锁生成的条件对象进行并发控制。
toArray、drainTo、clear都加锁但contains、toString未加锁。  