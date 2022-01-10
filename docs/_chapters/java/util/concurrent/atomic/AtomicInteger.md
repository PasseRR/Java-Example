---
layout: post
title: AtomicInteger
permalink: java.util.concurrent.atomic.AtomicInteger.html
order: 2
pk: java.util.concurrent.atomic
last_modified_at: 2022-01-10
---

> 可以用原子方式更新的int值。AtomicInteger可用在应用程序中（如以原子方式更新的标志），但不能用于替换Integer。
> 此类确实扩展了 Number，允许那些处理基于数字类的工具和实用工具进行统一访问。
> 在java并发编程中，会出现i++，i--等操作，但是这些不是原子性操作，这在线程安全上面就会出现相应的问题。因此java提供了相应类的原子性操作类。

## 静态块
```java
static {
    try {
        // 获得volatile变量value的内存地址偏移量
        // 修改value值时根据该偏移量进行
        valueOffset = unsafe.objectFieldOffset
            (AtomicBoolean.class.getDeclaredField("value"));
    } catch (Exception ex) { throw new Error(ex); }
}
```

## 构造方法
- AtomicInteger() 初始值为int基本类型默认值0  
- AtomicInteger(int initialValue) 初始值为initialValue  

## 方法
### int get()
```java
// 返回当前值
public final int get() {
    return value;
}
```

### void set()
```java
// 无条件设置value为新值
public final void set(int newValue) {
    value = newValue;
}
```

### boolean compareAndSet(int expect, int update)
```java
// 将当前value值跟expect预期值比较 若相同将value更新为update
// 更新成功返回true 否则返回false
public final boolean compareAndSet(int expect, int update) {
    return unsafe.compareAndSwapInt(this, valueOffset, expect, update);
}
```

### boolean weakCompareAndSet(boolean expect, boolean update)
```java
// 作用同compareAndSet 
// 但是作者注释"可能意外失败并且不提供排序保证" 但是改功能没有实现 
// 区别compareAndSet方法有final修饰 不能重写
// weakCompareAndSet可以重新 可以通过继承AtomicInteger重写此方法
public boolean weakCompareAndSet(int expect, int update) {
    return unsafe.compareAndSwapInt(this, valueOffset, expect, update);
}
```

### void lazySet(int newValue)
```java
// 该方法不能保证newValue修改立刻被其他线程看到
// putOrderedInt是putIntVolatile的延迟实现
public final void lazySet(int newValue) {
    unsafe.putOrderedInt(this, valueOffset, newValue);
}
```

### int getAndSet(int newValue)
```java
// 原子方式更新value 并返回旧值
public final int getAndSet(int newValue) {
    return unsafe.getAndSetInt(this, valueOffset, newValue);
}
```

### int getAndIncrement()
```java
// 原子的i++操作 返回i
public final int getAndIncrement() {
    return unsafe.getAndAddInt(this, valueOffset, 1);
}
```

### int incrementAndGet()
```java
// 原子的++i操作 返回i+1
public final int incrementAndGet() {
    return unsafe.getAndAddInt(this, valueOffset, 1) + 1;
}
```

### int getAndDecrement()
```java
// 原子的i--操作 返回i
public final int getAndDecrement() {
    return unsafe.getAndAddInt(this, valueOffset, -1);
}
```

### int decrementAndGet()
```java
// 原子--i操作 返回i-1
public final int decrementAndGet() {
    return unsafe.getAndAddInt(this, valueOffset, -1) - 1;
}
```

### int getAndAdd(int delta)
```java
// 原子的(i+delta)操作 返回i
public final int getAndAdd(int delta) {
    return unsafe.getAndAddInt(this, valueOffset, delta);
}
```

### int addAndGet(int delta)
```java
// 原子的(i+delta)操作 返回i+delta
public final int addAndGet(int delta) {
    return unsafe.getAndAddInt(this, valueOffset, delta) + delta;
}
```

### int getAndUpdate(IntUnaryOperator updateFunction)
```java
// 对旧值进行一元操作 返回旧值
public final int getAndUpdate(IntUnaryOperator updateFunction) {
    int prev, next;
    do {
        prev = get();
        next = updateFunction.applyAsInt(prev);
    } while (!compareAndSet(prev, next));
    return prev;
}
```

### int updateAndGet(IntUnaryOperator updateFunction)
```java
// 对旧值进行一元操作 返回新值
public final int updateAndGet(IntUnaryOperator updateFunction) {
    int prev, next;
    do {
        prev = get();
        next = updateFunction.applyAsInt(prev);
    } while (!compareAndSet(prev, next));
    return next;
}
```

### int getAndAccumulate(int x, IntBinaryOperator accumulatorFunction)
```java
// 对旧值进行二元操作 返回旧值
public final int getAndAccumulate(int x,
                                  IntBinaryOperator accumulatorFunction) {
    int prev, next;
    do {
        prev = get();
        next = accumulatorFunction.applyAsInt(prev, x);
    } while (!compareAndSet(prev, next));
    return prev;
}
```

### int accumulateAndGet(int x, IntBinaryOperator accumulatorFunction)
```java
// 对旧值进行二元操作 返回新值
public final int accumulateAndGet(int x,
                                  IntBinaryOperator accumulatorFunction) {
    int prev, next;
    do {
        prev = get();
        next = accumulatorFunction.applyAsInt(prev, x);
    } while (!compareAndSet(prev, next));
    return next;
}
```
