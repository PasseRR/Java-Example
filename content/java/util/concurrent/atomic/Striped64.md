---
layout: page
title: Striped64
permalink: java.util.concurrent.atomic.Striped64.html
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
    if ((h = getProbe()) == 0) {
        ThreadLocalRandom.current(); // force initialization
        h = getProbe();
        wasUncontended = true;
    }
    // True if last slot nonempty
    boolean collide = false;                
    for (;;) {
        Cell[] as; Cell a; int n; long v;
        if ((as = cells) != null && (n = as.length) > 0) {
            if ((a = as[(n - 1) & h]) == null) {
                if (cellsBusy == 0) {       // Try to attach new Cell
                    Cell r = new Cell(x);   // Optimistically create
                    if (cellsBusy == 0 && casCellsBusy()) {
                        boolean created = false;
                        try {               // Recheck under lock
                            Cell[] rs; int m, j;
                            if ((rs = cells) != null &&
                                (m = rs.length) > 0 &&
                                rs[j = (m - 1) & h] == null) {
                                rs[j] = r;
                                created = true;
                            }
                        } finally {
                            cellsBusy = 0;
                        }
                        if (created)
                            break;
                        continue;           // Slot is now non-empty
                    }
                }
                collide = false;
            }
            else if (!wasUncontended)       // CAS already known to fail
                wasUncontended = true;      // Continue after rehash
            else if (a.cas(v = a.value, ((fn == null) ? v + x :
                                         fn.applyAsLong(v, x))))
                break;
            else if (n >= NCPU || cells != as)
                collide = false;            // At max size or stale
            else if (!collide)
                collide = true;
            else if (cellsBusy == 0 && casCellsBusy()) {
                try {
                    if (cells == as) {      // Expand table unless stale
                        Cell[] rs = new Cell[n << 1];
                        for (int i = 0; i < n; ++i)
                            rs[i] = as[i];
                        cells = rs;
                    }
                } finally {
                    cellsBusy = 0;
                }
                collide = false;
                continue;                   // Retry with expanded table
            }
            h = advanceProbe(h);
        }
        else if (cellsBusy == 0 && cells == as && casCellsBusy()) {
            boolean init = false;
            try {                           // Initialize table
                if (cells == as) {
                    Cell[] rs = new Cell[2];
                    rs[h & 1] = new Cell(x);
                    cells = rs;
                    init = true;
                }
            } finally {
                cellsBusy = 0;
            }
            if (init)
                break;
        }
        else if (casBase(v = base, ((fn == null) ? v + x :
                                    fn.applyAsLong(v, x))))
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
    if ((h = getProbe()) == 0) {
        ThreadLocalRandom.current(); // force initialization
        h = getProbe();
        wasUncontended = true;
    }
    boolean collide = false;                // True if last slot nonempty
    for (;;) {
        Cell[] as; Cell a; int n; long v;
        if ((as = cells) != null && (n = as.length) > 0) {
            if ((a = as[(n - 1) & h]) == null) {
                if (cellsBusy == 0) {       // Try to attach new Cell
                    Cell r = new Cell(Double.doubleToRawLongBits(x));
                    if (cellsBusy == 0 && casCellsBusy()) {
                        boolean created = false;
                        try {               // Recheck under lock
                            Cell[] rs; int m, j;
                            if ((rs = cells) != null &&
                                (m = rs.length) > 0 &&
                                rs[j = (m - 1) & h] == null) {
                                rs[j] = r;
                                created = true;
                            }
                        } finally {
                            cellsBusy = 0;
                        }
                        if (created)
                            break;
                        continue;           // Slot is now non-empty
                    }
                }
                collide = false;
            }
            else if (!wasUncontended)       // CAS already known to fail
                wasUncontended = true;      // Continue after rehash
            else if (a.cas(v = a.value,
                           ((fn == null) ?
                            Double.doubleToRawLongBits
                            (Double.longBitsToDouble(v) + x) :
                            Double.doubleToRawLongBits
                            (fn.applyAsDouble
                             (Double.longBitsToDouble(v), x)))))
                break;
            else if (n >= NCPU || cells != as)
                collide = false;            // At max size or stale
            else if (!collide)
                collide = true;
            else if (cellsBusy == 0 && casCellsBusy()) {
                try {
                    if (cells == as) {      // Expand table unless stale
                        Cell[] rs = new Cell[n << 1];
                        for (int i = 0; i < n; ++i)
                            rs[i] = as[i];
                        cells = rs;
                    }
                } finally {
                    cellsBusy = 0;
                }
                collide = false;
                continue;                   // Retry with expanded table
            }
            h = advanceProbe(h);
        }
        else if (cellsBusy == 0 && cells == as && casCellsBusy()) {
            boolean init = false;
            try {                           // Initialize table
                if (cells == as) {
                    Cell[] rs = new Cell[2];
                    rs[h & 1] = new Cell(Double.doubleToRawLongBits(x));
                    cells = rs;
                    init = true;
                }
            } finally {
                cellsBusy = 0;
            }
            if (init)
                break;
        }
        else if (casBase(v = base,
                         ((fn == null) ?
                          Double.doubleToRawLongBits
                          (Double.longBitsToDouble(v) + x) :
                          Double.doubleToRawLongBits
                          (fn.applyAsDouble
                           (Double.longBitsToDouble(v), x)))))
            break;                          // Fall back on using base
    }
}
```