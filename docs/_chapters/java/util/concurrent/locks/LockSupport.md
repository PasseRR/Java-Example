---
layout: post
title: LockSupport
permalink: java.util.concurrent.locks.LockSupport.html
order: 5
pk: java.util.concurrent.locks
last_modified_at: 2022-01-10
---

> 对线程阻塞/唤醒可以用Object的wait/notify，LockSupport是另一种对线程
> 阻塞/唤醒的原语，由park/unpark对应且基于[Unsafe](../../../../sun/misc/Unsafe.md)实现。
> [Condition](Condition.md)和[Lock](Lock.md)均基于此类实现。

LockSupport是一个工具类，所有方法均为static，构造方法为私有的。  

## 域及静态块
```java
// Unsafe实例
private static final sun.misc.Unsafe UNSAFE;
// Thread的parkBlocker字段偏移量
// 用于记录线程被谁阻塞
private static final long parkBlockerOffset;
// Thread的threadLocalRandomSeed字段偏移量
// 随机数生成种子
private static final long SEED;
// Thread的threadLocalRandomProbe字段偏移量
// 随机数
private static final long PROBE;
// Thread的threadLocalRandomSecondarySeed字段偏移量
// 随机数生成第二种子
private static final long SECONDARY;
static {
    try {
        UNSAFE = sun.misc.Unsafe.getUnsafe();
        Class<?> tk = Thread.class;
        parkBlockerOffset = UNSAFE.objectFieldOffset
            (tk.getDeclaredField("parkBlocker"));
        SEED = UNSAFE.objectFieldOffset
            (tk.getDeclaredField("threadLocalRandomSeed"));
        PROBE = UNSAFE.objectFieldOffset
            (tk.getDeclaredField("threadLocalRandomProbe"));
        SECONDARY = UNSAFE.objectFieldOffset
            (tk.getDeclaredField("threadLocalRandomSecondarySeed"));
    } catch (Exception ex) { throw new Error(ex); }
}
```

## 方法
### void setBlocker(Thread t, Object arg)
```java
// 设置导致线程阻塞的对象
private static void setBlocker(Thread t, Object arg) {
    // Thread的parkBlocker字段为volatile
    // 所以不需要使用内存屏障
    UNSAFE.putObject(t, parkBlockerOffset, arg);
}
```

### void unpark(Thread thread)
```java
// 当给定线程许可不可用时，使得给定线程的许可可用
// 若当前线程阻塞(通过park方法)，则唤醒该线程
// 若线程没有start，则此方法没有任何效果
public static void unpark(Thread thread) {
    if (thread != null)
        UNSAFE.unpark(thread);
}
```

### void park(Object blocker)
```java
// 阻塞当前线程除非当前线程许可可用
// 许可不可用时会阻塞直到：
// 1. unpark方法调用
// 2. 当前线程被中断
// 3. 调用没有原因返回
public static void park(Object blocker) {
    Thread t = Thread.currentThread();
    // 设置导致线程阻塞对象
    setBlocker(t, blocker);
    // 阻塞直到许可可用
    UNSAFE.park(false, 0L);
    // 设置导致线程阻塞对象为null
    setBlocker(t, null);
}
```

### void parkNanos(Object blocker, long nanos)
```java
// 阻塞给定纳秒
public static void parkNanos(Object blocker, long nanos) {
    if (nanos > 0) {
        Thread t = Thread.currentThread();
        setBlocker(t, blocker);
        UNSAFE.park(false, nanos);
        setBlocker(t, null);
    }
}
```

### void parkUntil(Object blocker, long deadline)
```java
// 阻塞到给定日期
public static void parkUntil(Object blocker, long deadline) {
    Thread t = Thread.currentThread();
    setBlocker(t, blocker);
    // 当isAbsolute为true时，timeout是相对于新纪元之后的毫秒
    // 具体见Unsafe
    UNSAFE.park(true, deadline);
    setBlocker(t, null);
}
```

### void park()
```java
// 阻塞当前线程 不保存导致线程阻塞的对象
public static void park() {
    UNSAFE.park(false, 0L);
}
```

### void parkNanos(long nanos)
```java
// 阻塞当前线程给定纳秒 不保存导致线程阻塞的对象
public static void parkNanos(long nanos) {
    if (nanos > 0)
        UNSAFE.park(false, nanos);
}
```

### void parkUntil(long deadline)
```java
// 阻塞到给定日期 不保存导致线程阻塞的对象
public static void parkUntil(long deadline) {
    UNSAFE.park(true, deadline);
}
```

### Object getBlocker(Thread t)
```java
// 获得最近导致线程阻塞的对象且该线程没被唤醒
public static Object getBlocker(Thread t) {
    if (t == null)
        throw new NullPointerException();
    return UNSAFE.getObjectVolatile(t, parkBlockerOffset);
}
```

### int nextSecondarySeed()
```java
// xorshift算法获得下一个第二种子
// 用于StampedLock
static final int nextSecondarySeed() {
    int r;
    Thread t = Thread.currentThread();
    if ((r = UNSAFE.getInt(t, SECONDARY)) != 0) {
        r ^= r << 13;   // xorshift
        r ^= r >>> 17;
        r ^= r << 5;
    }
    else if ((r = java.util.concurrent.ThreadLocalRandom.current().nextInt()) == 0)
        r = 1; // avoid zero
    UNSAFE.putInt(t, SECONDARY, r);
    return r;
}
```