---
layout: post
title: Striped64
permalink: java.util.concurrent.atomic.Striped64.html
order: 17
pk: java.util.concurrent.atomic
last_modified_at: 2022-01-10
---
> Striped64是一个抽象类  
> Striped64是jdk1.8提供的用于支持[DoubleAdder](DoubleAdder.md)、[LongAdder](LongAdder.md)、[DoubleAccumulator](DoubleAccumulator.md)、[LongAccumulator](LongAccumulator.md)这样机制的基础类。
> Striped64的设计核心思路就是通过内部的分散计算来避免竞争(比如多线程CAS操作时的竞争)。  
> Striped64内部包含一个基础值和一个单元哈希表。没有竞争的情况下，要累加的数会累加到这个基础值上；
> 如果有竞争的话，会将要累加的数累加到单元哈希表中的某个单元里面。所以整个Striped64的值包括基础值和单元哈希表中所有单元的值的总和。

## 域及静态块
```java
// CPU数量
static final int NCPU = Runtime.getRuntime().availableProcessors();
// 存放Cell的hash表，大小为2的幂。 
transient volatile Cell[] cells;
// 基础值，没有竞争时会使用(更新)这个值，同时做为初始化竞争失败的回退方案。 
transient volatile long base;
// 自旋锁，通过CAS操作加锁，用于保护创建或者扩展Cell表。 
transient volatile int cellsBusy;
// Unsafe实例
private static final sun.misc.Unsafe UNSAFE;
// 基础值偏移量
private static final long BASE;
// 自旋锁偏移量
private static final long CELLSBUSY;
// Thread中threadLocalRandomProbe的偏移量
private static final long PROBE;
static {
    try {
        UNSAFE = sun.misc.Unsafe.getUnsafe();
        Class<?> sk = Striped64.class;
        BASE = UNSAFE.objectFieldOffset
            (sk.getDeclaredField("base"));
        CELLSBUSY = UNSAFE.objectFieldOffset
            (sk.getDeclaredField("cellsBusy"));
        Class<?> tk = Thread.class;
        PROBE = UNSAFE.objectFieldOffset
            (tk.getDeclaredField("threadLocalRandomProbe"));
    } catch (Exception e) {
        throw new Error(e);
    }
}
```

## 静态内部类
```java
@sun.misc.Contended
static final class Cell {
    volatile long value;
    Cell(long x) { value = x; }
    // value的原子cas操作
    final boolean cas(long cmp, long val) {
        return UNSAFE.compareAndSwapLong(this, valueOffset, cmp, val);
    }

    // Unsafe mechanics
    private static final sun.misc.Unsafe UNSAFE;
    // value的偏移量
    private static final long valueOffset;
    
    // 静态块初始化
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> ak = Cell.class;
            valueOffset = UNSAFE.objectFieldOffset
                (ak.getDeclaredField("value"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
```

## 方法

### boolean casBase(long cmp, long val) 
```java
// cas基础值
final boolean casBase(long cmp, long val) {
    return UNSAFE.compareAndSwapLong(this, BASE, cmp, val);
}
```

### boolean casCellsBusy()
```java
// cas自旋锁
// cellsBusy从0->1获得锁
// 从1->0释放锁
final boolean casCellsBusy() {
    return UNSAFE.compareAndSwapInt(this, CELLSBUSY, 0, 1);
}
```

### int getProbe()
```java
// 获得当前线程threadLocalRandomProbe的值
static final int getProbe() {
    return UNSAFE.getInt(Thread.currentThread(), PROBE);
}
```

### int advanceProbe(int probe)
```java
// threadLocalRandomProbe根据xorshift算法赋值
static final int advanceProbe(int probe) {
    probe ^= probe << 13;   // xorshift
    probe ^= probe >>> 17;
    probe ^= probe << 5;
    UNSAFE.putInt(Thread.currentThread(), PROBE, probe);
    return probe;
}
```

### void longAccumulate(long x, LongBinaryOperator fn, boolean wasUncontended)
```java
// long累加核心方法
final void longAccumulate(long x, LongBinaryOperator fn,
                          boolean wasUncontended) {
    int h;
    // 获取当前线程的probe值作为hash值 
    if ((h = getProbe()) == 0) { 
        // 如果hash值为0强制初始化当前线程的hash值
        ThreadLocalRandom.current(); // force initialization
        // 获得计算的hash值
        h = getProbe();
        // 设置为竞非争标识为true
        wasUncontended = true;
    }
    // True if last slot nonempty
    // 索引位置的Cell是否hash碰撞
    boolean collide = false;                
    for (;;) {
        // 当前hash表的快照
        Cell[] as; 
        // 存(n-1) & h索引的Cell
        Cell a; 
        // hash表长度
        int n; 
        // 当前base快照
        long v;
        // hash表不为null且长度大于0
        if ((as = cells) != null && (n = as.length) > 0) {
            // 根据hash值取得一个Cell
            if ((a = as[(n - 1) & h]) == null) {
                // 双重判断hash表是否在使用
                if (cellsBusy == 0) {       // Try to attach new Cell
                    Cell r = new Cell(x);   // Optimistically create
                    // 再次判断并获得锁
                    if (cellsBusy == 0 && casCellsBusy()) {
                        // 是否计算成功
                        boolean created = false;
                        try {               // Recheck under lock
                            Cell[] rs; int m, j;
                            if ((rs = cells) != null && // hash表非空
                                // hash表长度大于0
                                (m = rs.length) > 0 && 
                                // hash表索引位置为null
                                rs[j = (m - 1) & h] == null) { 
                                rs[j] = r; // 设置新建的Cell到该位置
                                created = true; // 创建成功
                            }
                        } finally {
                            cellsBusy = 0; // 释放锁
                        }
                        if (created)
                            // 创建成功 方法结束
                            break;
                        // 没创建成功 说明该位置已经被其他线程创建的Cell占用
                        // 继续尝试    
                        continue;           // Slot is now non-empty
                    }
                }
                // 没获取到cellsBusy锁
                collide = false;
            } 
            /**
             * 以下条件说明通过hash表索引(n - 1) & h位置已经存在一个Cell且非空
             * a = as[(n - 1) & h] 即a
             */
            // 说明cas失败 为竞争状态
            // 如果非竞争为false 设置为true
            else if (!wasUncontended)       // CAS already known to fail
                wasUncontended = true;      // Continue after rehash
            // 尝试将x的值计算到cell的value上 成功则方法结束
            else if (a.cas(v = a.value, ((fn == null) ? v + x :
                                         fn.applyAsLong(v, x))))
                break;
            // 如果hash表长度已到最大即CPU数量
            // 或者hash表已经发生变化(as是hash表的一个快照)
            // 设置冲突为false
            else if (n >= NCPU || cells != as)
                collide = false;            // At max size or stale
            // 如果为非冲突 设置冲突标识
            else if (!collide)
                collide = true;
            // 尝试获取cellsBusy锁
            else if (cellsBusy == 0 && casCellsBusy()) {
                try {
                    // 如果hash表快照未过时
                    if (cells == as) {      // Expand table unless stale
                        // 将hash表扩容1倍
                        Cell[] rs = new Cell[n << 1];
                        for (int i = 0; i < n; ++i)
                            rs[i] = as[i];
                        cells = rs;
                    }
                } finally {
                    // 释放锁
                    cellsBusy = 0;
                }
                // 设置冲突标识为false并重试
                collide = false;
                continue;                   // Retry with expanded table
            }
            // rehash
            h = advanceProbe(h);
        }
        /**
         * 以下条件说明hash表未初始化 
         */
        // 尝试获得cellsBusy锁
        else if (cellsBusy == 0 && cells == as && casCellsBusy()) {
            // 初始化成功标识
            boolean init = false;
            try {                           // Initialize table
                if (cells == as) {
                    // 初始化hash表长度为2
                    Cell[] rs = new Cell[2];
                    // 并将h & 1位置设置的Cell
                    rs[h & 1] = new Cell(x);
                    cells = rs;
                    init = true;
                }
            } finally {
                // 释放锁
                cellsBusy = 0;
            }
            // 如果初始化成功 方法结束
            if (init) 
                break;
        }
        // 如果尝试初始化hash表失败
        // 尝试将x计算到base上
        else if (casBase(v = base, ((fn == null) ? v + x :
                                    fn.applyAsLong(v, x))))
            // 计算成功 方法结束
            break;                          // Fall back on using base
    }
}
```

### void doubleAccumulate(double x, DoubleBinaryOperator fn, boolean wasUncontended)
```java
// double累加核心方法
final void doubleAccumulate(double x, DoubleBinaryOperator fn,
                            boolean wasUncontended) {
    int h;
    // 获取当前线程的probe值作为hash值 
    if ((h = getProbe()) == 0) { 
        // 如果hash值为0强制初始化当前线程的hash值
        ThreadLocalRandom.current(); // force initialization
        // 获得计算的hash值
        h = getProbe();
        // 设置为竞非争标识为true
        wasUncontended = true;
    }
    // True if last slot nonempty
    // 索引位置的Cell是否hash碰撞
    boolean collide = false;                 // True if last slot nonempty
    for (;;) {
        Cell[] as; Cell a; int n; long v;
        // hash表已经初始化
        if ((as = cells) != null && (n = as.length) > 0) {
            // hash表(n - 1) & h位置的Cell为null
            if ((a = as[(n - 1) & h]) == null) {
                // 锁可以获取
                if (cellsBusy == 0) {       // Try to attach new Cell
                    // 以x初始化一个Cell
                    Cell r = new Cell(Double.doubleToRawLongBits(x));
                    // 尝试获得锁
                    if (cellsBusy == 0 && casCellsBusy()) {
                        // 是否创建成功
                        boolean created = false;
                        try {               // Recheck under lock
                            Cell[] rs; int m, j;
                            if ((rs = cells) != null && // hash表最新快照非空
                                // hash表最新快照长度大于0
                                (m = rs.length) > 0 && 
                                // hash表快照(m - 1) & h索引Cell为null
                                rs[j = (m - 1) & h] == null) {
                                rs[j] = r; // 设置该索引Cell为r
                                created = true; // 设置创建成功标识
                            }
                        } finally {
                            // 释放锁
                            cellsBusy = 0;
                        }
                        if (created) // 创建成功 方法结束
                            break;
                        // 没创建成功 说明该位置已经被其他线程创建的Cell占用
                        // 继续尝试  
                        continue;           // Slot is now non-empty
                    }
                }
                // 设置碰撞标识为false
                collide = false;
            }
            /**
             * 以下条件表示hash表(n - 1) & h位置的Cell为非null
             */
            // 说明cas失败 为竞争状态
            // 如果非竞争为false 设置为true
            else if (!wasUncontended)       // CAS already known to fail
                wasUncontended = true;      // Continue after rehash
            // 尝试将x计算到a的value上
            else if (a.cas(v = a.value,
                           ((fn == null) ?
                            Double.doubleToRawLongBits
                            (Double.longBitsToDouble(v) + x) :
                            Double.doubleToRawLongBits
                            (fn.applyAsDouble
                             (Double.longBitsToDouble(v), x)))))
                // 计算成功 方法结束
                break;
            // 如果hash表满了或者hash表快照已过时
            else if (n >= NCPU || cells != as)
                // 设置hash碰撞标识
                collide = false;            // At max size or stale
            // 如果为非冲突 设置冲突标识
            else if (!collide)
                collide = true;
            // 尝试获得锁
            else if (cellsBusy == 0 && casCellsBusy()) {
                try {
                    // 如果快照为过时 对hash表进行扩容
                    // 容量增加1倍
                    if (cells == as) {      // Expand table unless stale
                        Cell[] rs = new Cell[n << 1];
                        for (int i = 0; i < n; ++i)
                            rs[i] = as[i];
                        cells = rs;
                    }
                } finally {
                    // 释放锁
                    cellsBusy = 0;
                }
                // 设置碰撞标识为false
                collide = false;
                // 继续尝试计算
                continue;                   // Retry with expanded table
            }
            // rehash
            h = advanceProbe(h);
        }
        // 尝试获得锁
        else if (cellsBusy == 0 && cells == as && casCellsBusy()) {
            boolean init = false;
            try {                           // Initialize table
                if (cells == as) { // 如果快照未过期
                    // 初始化hash表
                    Cell[] rs = new Cell[2];
                    rs[h & 1] = new Cell(Double.doubleToRawLongBits(x));
                    cells = rs;
                    init = true;
                }
            } finally {
                // 释放锁
                cellsBusy = 0;
            }
            if (init) // 初始化成功 方法结束
                break;
        }
        // 初始化hash表失败 尝试将x计算到base上
        else if (casBase(v = base,
                         ((fn == null) ?
                          Double.doubleToRawLongBits
                          (Double.longBitsToDouble(v) + x) :
                          Double.doubleToRawLongBits
                          (fn.applyAsDouble
                           (Double.longBitsToDouble(v), x)))))
            // 计算成功 方法结束
            break;                          // Fall back on using base
    }
}
```