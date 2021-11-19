---
layout: page
title: ArrayBlockingQueue
permalink: java.util.concurrent.ArrayBlockingQueue.html
---
> ArrayBlockingQueue是一个有界的阻塞队列，其内部实现是将对象放到一个数组里。
> 有界也就意味着，它不能够存储无限多数量的元素。它有一个同一时间能够存储元素数量的上限。
> 你可以在对其初始化的时候设定这个上限，但之后就无法对这个上限进行修改了
> (因为它是基于数组实现的，也就具有数组的特性：一旦初始化，大小就无法修改)。

## 源码分析  
初始化必须指定容量 可选是否为公平锁或者通过一个现有集合(Collection)初始化
* ArrayBlockingQueue(int capacity)  
* ArrayBlockingQueue(int capacity, boolean fair)  
* ArrayBlockingQueue(int capacity, boolean fair, Collection<? extends E> c)   

### 属性

```java
// 存储队列元素的数组，是个循环数组
final Object[] items;

// 拿数据的索引，用于take，poll，peek，remove方法
int takeIndex;

// 放数据的索引，用于put，offer，add方法
int putIndex;

// 元素个数
int count;

// 可重入锁
final ReentrantLock lock;
// notEmpty条件对象，由lock创建
private final Condition notEmpty;
// notFull条件对象，由lock创建
private final Condition notFull;
```

### 方法
#### 添加元素  
* boolean offer(E e)  
```java
public boolean offer(E e) {
    checkNotNull(e); // null元素抛出NullPointerException
    final ReentrantLock lock = this.lock;
    lock.lock(); // 加锁
    try {
        if (count == items.length)
            return false; // 队列满了返回false
        else {
            enqueue(e); // 入队
            return true;
        }
    } finally {
        lock.unlock(); // 释放锁
    }
}
private void enqueue(E x) {
    // assert lock.getHoldCount() == 1;
    // assert items[putIndex] == null;
    final Object[] items = this.items;
    items[putIndex] = x;
    if (++putIndex == items.length) // 如果put索引满了 移动到0
        putIndex = 0;
    count++; // 队列元素个数增加
    // 使用条件对象notEmpty通知，比如使用take方法的时候队列里没有数据，被阻塞。
    // 这个时候队列insert了一条数据，需要调用signal进行通知
    notEmpty.signal(); 
}
```
* boolean offer(E e, long timeout, TimeUnit unit)  
```java
public boolean offer(E e, long timeout, TimeUnit unit)
    throws InterruptedException {
    checkNotNull(e);
    long nanos = unit.toNanos(timeout); // 将TimeUnit转为纳秒
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();
    try {
        while (count == items.length) { // 若果队列满了
            if (nanos <= 0) // 等待时间已经完成直接返回false
                return false;
            // notFull.awaitNanos(nanos)返回未等待纳秒时间   
            // cpu时间片可能没有timeout这么长
            nanos = notFull.awaitNanos(nanos);
        }
        enqueue(e);
        return true;
    } finally {
        lock.unlock();
    }
}
```
* boolean add(E e)  
```java
public boolean add(E e) {
    if (offer(e)) // 队列未满 直接添加元素 见上offer方法
        return true;
    else // 否则抛出队列满的异常
        throw new IllegalStateException("Queue full");
}
```
* void put(E e)  
```java
public void put(E e) throws InterruptedException {
    checkNotNull(e);
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();
    try {
        while (count == items.length)
            notFull.await(); // 如果队列满了阻塞挂起 释放锁
        enqueue(e);
    } finally {
        lock.unlock();
    }
}
```

#### 移除元素  
* E poll()  
```java
public E poll() {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        // 队列空返回null 否者进行出队操作
        return (count == 0) ? null : dequeue();
    } finally {
        lock.unlock();
    }
}
private E dequeue() {
    // assert lock.getHoldCount() == 1;
    // assert items[takeIndex] != null;
    final Object[] items = this.items;
    // 删除take索引位置的元素
    E x = (E) items[takeIndex];
    items[takeIndex] = null;
    // take索引后移 并判断是否需要移动到起始位置
    if (++takeIndex == items.length)
        takeIndex = 0;
    count--;
    // 重置迭代器如果take索引回到起始位置 
    // 如果队列为空 将迭代器置为null
    if (itrs != null) 
        itrs.elementDequeued();
    // 使用条件对象notFull通知，比如使用put方法放数据的时候队列已满，被阻塞。
    // 这个时候消费了一条数据，队列没满了，就需要调用signal进行通知
    notFull.signal();
    return x;
}
```
* E poll(long timeout, TimeUnit unit)  
```java
public E poll(long timeout, TimeUnit unit) throws InterruptedException {
    long nanos = unit.toNanos(timeout);
    final ReentrantLock lock = this.lock;
    // 如果当前线程被中断 抛出InterruptedException
    lock.lockInterruptibly();
    try {
        while (count == 0) {
            if (nanos <= 0)
                return null;
            // notEmpty.awaitNanos(nanos)返回未等待纳秒时间   
            // cpu时间片可能没有timeout这么长
            nanos = notEmpty.awaitNanos(nanos);
        }
        return dequeue();
    } finally {
        lock.unlock();
    }
}
```
* E remove()  
```java
 public E remove() {
    E x = poll(); // 见poll方法
    if (x != null)
        return x;
    else
        throw new NoSuchElementException();
}
```
* boolean remove(Object o)  
```java
// 删除一个指定元素
public boolean remove(Object o) {
    if (o == null) return false;
    final Object[] items = this.items;
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        if (count > 0) {
            // put索引
            final int putIndex = this.putIndex;
            // take索引
            int i = takeIndex;
            // 从take索引遍历到put索引 如果找到删除指定索引位置元素
            // 否则 返回false
            do {
                if (o.equals(items[i])) {
                    removeAt(i);
                    return true;
                }
                if (++i == items.length)
                    i = 0;
            } while (i != putIndex);
        }
        return false;
    } finally {
        lock.unlock();
    }
}
void removeAt(final int removeIndex) {
    // assert lock.getHoldCount() == 1;
    // assert items[removeIndex] != null;
    // assert removeIndex >= 0 && removeIndex < items.length;
    final Object[] items = this.items;
    // 若果删除索引跟take索引相同 则直接删除元素
    // 并更新take索引
    if (removeIndex == takeIndex) {
        // removing front item; just advance
        items[takeIndex] = null;
        if (++takeIndex == items.length)
            takeIndex = 0;
        count--;
        if (itrs != null)
            itrs.elementDequeued();
    } else { // 否则从删除索引开始遍历 找到并删除元素
        // an "interior" remove
        // slide over all others up through putIndex.
        final int putIndex = this.putIndex;
        // 将删除索引后的元素向前移动1移除最后一个元素更新put索引
        for (int i = removeIndex;;) {
            int next = i + 1;
            if (next == items.length) // 循环数组
                next = 0;
            if (next != putIndex) { // 索引递增并向前移动元素
                items[i] = items[next];
                i = next; 
            } else { // 删除当前put索引的元素 并更新put索引-1
                items[i] = null;
                this.putIndex = i;
                break;
            }
        }
        count--;
        if (itrs != null)
            itrs.removedAt(removeIndex);
    }
    notFull.signal();
}
```
* E take()  
```java
public E take() throws InterruptedException {
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();
    try {
        while (count == 0)
            notEmpty.await(); // 如果队列为空 阻塞到队列非空
        return dequeue(); // 出队操作 见E poll()
    } finally {
        lock.unlock();
    }
}
```
## 总结
ArrayBlockingQueue底层是一个循环的数组，使用一个重入锁和这个锁生成的两个条件对象进行并发控制。
其他方法都会加锁比如contains、toArray、drainTo、clear、toString。