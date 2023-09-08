# LinkedBlockingQueue

LinkedBlockingQueue是一个基于单链表的无界阻塞队列，它跟ArrayBlockingQueue一样都是通过使用ReentrantLock来保证线程安全的。
但是LinkedBlockingQueue有两把锁，即put重入锁和take重入锁。ArrayBlockingQueue中put和take只能有一个被执行，不允许并行执行。
LinkedBlockingQueue允许take和put并行执行，当然只能有1个线程各自运行。
LinkedBlockingQueue不允许null值，也不强制我们指定队列得初始容量，默认的容量为Integer.MAX_VALUE。

## 源码分析  
初始化若指定容量以指定为准，否则容量为Integer.MAX_VALUE，也可以使用一个现有集合来初始化  
* LinkedBlockingQueue()  
* LinkedBlockingQueue(int capacity)  
* LinkedBlockingQueue(Collection<? extends E> c)  

### 属性

```java
// 链表容量
private final int capacity;
// 当前队列元素数量
// 由于有take、put两把锁 需要使用AtomicInteger来保证队列元素数量线程安全
private final AtomicInteger count = new AtomicInteger();
// 链表头
transient Node<E> head;
// 链表尾
private transient Node<E> last;
// take重入锁
private final ReentrantLock takeLock = new ReentrantLock();
// 非空条件 由take重入锁创建
private final Condition notEmpty = takeLock.newCondition();
// put重入锁
private final ReentrantLock putLock = new ReentrantLock();
// 非满条件 由put重入锁创建
private final Condition notFull = putLock.newCondition();

// 链表节点
static class Node<E> {
    E item;
    /**
     * One of:
     * - the real successor Node
     * - this Node, meaning the successor is head.next
     * - null, meaning there is no successor (this is the last node)
     */
    Node<E> next;
    Node(E x) { item = x; }
}
```
### 方法
#### 添加元素  
* boolean offer(E e)  
```java
public boolean offer(E e) {
    // LinkedBlockingQueue不允许null
    if (e == null) throw new NullPointerException();
    final AtomicInteger count = this.count;
    // 如果队列满了返回false
    if (count.get() == capacity)
        return false;
    int c = -1;
    Node<E> node = new Node<E>(e);
    final ReentrantLock putLock = this.putLock;
    putLock.lock();
    try {
        // 双重校验队列是否满
        if (count.get() < capacity) {
            enqueue(node); // 将元素添加到链表尾部 此方法为入队核心方法
            c = count.getAndIncrement();
            if (c + 1 < capacity) // 如果队列未满
                notFull.signal(); // 唤醒等待的put线程
        }
    } finally {
        putLock.unlock();
    }
    if (c == 0) // 如果队列在插入元素前为空 此时c为0 但此时队列中有一个元素
        signalNotEmpty(); // 唤醒等待的take线程
    return c >= 0;
}
private void enqueue(Node<E> node) {
    // 入队的时候只在last尾节点添加元素
    // Java是自右向左逐一赋值的，比如：A=B=C=0，首先给C赋值0，即C=0，然后B=C;最后A=B
    // 故此处为先将last.next地址指向node 即将head链表增加一个节点
    // 再将last地址指向last.next 即last指向最后一个节点
    last = last.next = node;
}
private void signalNotEmpty() {
    final ReentrantLock takeLock = this.takeLock;
    takeLock.lock();
    try {
        notEmpty.signal();
    } finally {
        takeLock.unlock();
    }
}
```
* boolean offer(E e, long timeout, TimeUnit unit)  
```java
public boolean offer(E e, long timeout, TimeUnit unit)
    throws InterruptedException {
    if (e == null) throw new NullPointerException();
    long nanos = unit.toNanos(timeout);
    int c = -1;
    final ReentrantLock putLock = this.putLock;
    final AtomicInteger count = this.count;
    putLock.lockInterruptibly();
    try {
        // 跟offer(E e)方法的主要区别
        // 等待给定timeout
        // 超时则返回false 否则进行入队操作
        while (count.get() == capacity) {
            if (nanos <= 0)
                return false;
            nanos = notFull.awaitNanos(nanos);
        }
        enqueue(new Node<E>(e));
        c = count.getAndIncrement();
        if (c + 1 < capacity)
            notFull.signal();
    } finally {
        putLock.unlock();
    }
    if (c == 0)
        signalNotEmpty();
    return true;
}
```
* boolean add(E e)  
```java
public boolean add(E e) {
    if (offer(e)) // 见上offer(E e)方法
        return true;
    else
        throw new IllegalStateException("Queue full");
}
```
* void put(E e)  
```java
public void put(E e) throws InterruptedException {
    if (e == null) throw new NullPointerException();
    // Note: convention in all put/take/etc is to preset local var
    // holding count negative to indicate failure unless set.
    int c = -1;
    Node<E> node = new Node<E>(e);
    final ReentrantLock putLock = this.putLock;
    final AtomicInteger count = this.count;
    putLock.lockInterruptibly();
    try {
        /*
         * Note that count is used in wait guard even though it is
         * not protected by lock. This works because count can
         * only decrease at this point (all other puts are shut
         * out by lock), and we (or some other waiting put) are
         * signalled if it ever changes from capacity. Similarly
         * for all other uses of count in other wait guards.
         */
        // 如果队列是满的 阻塞到队列非满
        while (count.get() == capacity) {
            notFull.await();
        }
        enqueue(node);
        c = count.getAndIncrement();
        if (c + 1 < capacity)
            notFull.signal();
    } finally {
        putLock.unlock();
    }
    if (c == 0)
        signalNotEmpty();
}
```

#### 移除元素  
* E poll()  
```java
public E poll() {
    final AtomicInteger count = this.count;
    if (count.get() == 0) // 如果队列为空 直接返回null
        return null;
    E x = null;
    int c = -1;
    final ReentrantLock takeLock = this.takeLock;
    takeLock.lock();
    try {
        if (count.get() > 0) {
            x = dequeue(); // 出队核心方法
            c = count.getAndDecrement();
            if (c > 1)
                notEmpty.signal();
        }
    } finally {
        takeLock.unlock();
    }
    // 和入队类似 如果在删除元素前队列是满的
    // 此时c等于capacity 但实际元素个数只有capacity-1个
    // 故唤醒put线程 可以进行入队操作
    if (c == capacity)
        signalNotFull();
    return x;
}
private E dequeue() {
    Node<E> h = head; // 将h指向当前头结点
    Node<E> first = h.next; // 将first指向第二个节点
    h.next = h; // help GC // h指向h后一个节点
    head = first; // 将头结点指向第二个节点
    E x = first.item; // 获得队首的元素
    first.item = null; // 删除队首元素
    return x; 
}
```
* E poll(long timeout, TimeUnit unit)  
```java
public E poll(long timeout, TimeUnit unit) throws InterruptedException {
    E x = null;
    int c = -1;
    long nanos = unit.toNanos(timeout);
    final AtomicInteger count = this.count;
    final ReentrantLock takeLock = this.takeLock;
    takeLock.lockInterruptibly();
    try {
        // 等待timeout 超时返回null 否则删除元素
        while (count.get() == 0) {
            if (nanos <= 0)
                return null;
            nanos = notEmpty.awaitNanos(nanos);
        }
        x = dequeue();
        c = count.getAndDecrement();
        if (c > 1)
            notEmpty.signal();
    } finally {
        takeLock.unlock();
    }
    if (c == capacity)
        signalNotFull();
    return x;
}
```
* E remove()  
```java
public E remove() {
    E x = poll(); // 见上poll()方法
    if (x != null)
        return x;
    else // 队列为空抛出异常
        throw new NoSuchElementException();
}
```
* boolean remove(Object o)  
```java
// 删除指定元素
public boolean remove(Object o) {
    if (o == null) return false;
    // 跟其他删除不同 这里加的是全锁即take、put锁
    // 因为指定元素可能不在队首
    fullyLock();
    try {
        // 从head开始遍历 直到next不为null
        for (Node<E> trail = head, p = trail.next;
             p != null;
             trail = p, p = p.next) {
            if (o.equals(p.item)) {
                // p为要删除节点
                // trail为p的前一个节点
                unlink(p, trail);
                return true;
            }
        }
        return false;
    } finally {
        fullyUnlock();
    }
}
void unlink(Node<E> p, Node<E> trail) {
    // assert isFullyLocked();
    // p.next is not changed, to allow iterators that are
    // traversing p to maintain their weak-consistency guarantee.
    p.item = null; // 移除节点元素
    trail.next = p.next; // 连接移除节点前后节点
    if (last == p) // 如果删除节点为队尾
        last = trail; // 将last节点指向trail
    if (count.getAndDecrement() == capacity) // 删除元素前队列是满的 唤醒put线程
        notFull.signal();
}
```
* E take()  
```java
public E take() throws InterruptedException {
    E x;
    int c = -1;
    final AtomicInteger count = this.count;
    final ReentrantLock takeLock = this.takeLock;
    takeLock.lockInterruptibly();
    try {
        // 如果队列为空 阻塞到队列为非空
        while (count.get() == 0) {
            notEmpty.await();
        }
        x = dequeue();
        c = count.getAndDecrement();
        if (c > 1)
            notEmpty.signal();
    } finally {
        takeLock.unlock();
    }
    if (c == capacity)
        signalNotFull();
    return x;
}
```
## 总结
LinkedBlockingQueue底层是一个单链表，使用put锁、take锁和两锁生成的条件对象进行并发控制。
其他方法都会加全锁(即put、take锁)比如contains、toArray、drainTo、clear、toString。