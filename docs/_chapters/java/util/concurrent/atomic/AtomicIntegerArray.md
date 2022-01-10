---
layout: post
title: AtomicIntegerArray
permalink: java.util.concurrent.atomic.AtomicIntegerArray.html
order: 3
pk: java.util.concurrent.atomic
last_modified_at: 2022-01-10
---

> 可以原子更新的int[]，修改指定索引的值和[AtomicInteger](AtomicInteger.md)的方法类似

## 静态域及块
```java
// Unsafe实例
private static final Unsafe unsafe = Unsafe.getUnsafe();
// int数组的第一个元素的偏移量
private static final int base = unsafe.arrayBaseOffset(int[].class);
// 用于定位数组元素内存位置的位移
private static final int shift;
// int[]
private final int[] array;

static {
    // 获得数组第一个元素的字节大小
    int scale = unsafe.arrayIndexScale(int[].class);
    if ((scale & (scale - 1)) != 0)
        throw new Error("data type scale not a power of two");
    // Integer.numberOfLeadingZeros(scale)返回高位0的个数
    // 获得每个元素之间的位移
    shift = 31 - Integer.numberOfLeadingZeros(scale);
}
```

## 构造方法
- AtomicIntegerArray(int length) 根据指定length初始化数组  
- AtomicIntegerArray(int[] array) 根据给定array初始化数组  

## 方法
### int length() 
```java
// 返回数组元素个数
public final int length() {
    return array.length;
}
```

### int get(int i)
```java
// 获得数组给定索引i的值
public final int get(int i) {
    return getRaw(checkedByteOffset(i));
}
// check索引
private long checkedByteOffset(int i) {
    if (i < 0 || i >= array.length)
        throw new IndexOutOfBoundsException("index " + i);

    return byteOffset(i);
}
// 计算给定索引i的偏移量
private static long byteOffset(int i) {
    return ((long) i << shift) + base;
}
// 获得数组给定偏移量offset的int值
private int getRaw(long offset) {
    return unsafe.getIntVolatile(array, offset);
}
```

### void set(int i, int newValue)
```java
// 更新数组索引i的值为newValue
public final void set(int i, int newValue) {
    unsafe.putIntVolatile(array, checkedByteOffset(i), newValue);
}
```

### void lazySet(int i, int newValue)
```java
// 更新数组索引i的值为newValue 不能保证被其他线程实时看到最新值
public final void lazySet(int i, int newValue) {
    unsafe.putOrderedInt(array, checkedByteOffset(i), newValue);
}
```

### int getAndSet(int i, int newValue)
```java
// 设置数组索引i的值为newValue 并返回该位置的旧值
public final int getAndSet(int i, int newValue) {
    return unsafe.getAndSetInt(array, checkedByteOffset(i), newValue);
}
```

### boolean compareAndSet(int i, int expect, int update)
```java
// 比较数组索引i的值是否为expect 若是更新为update
// 更新成功返回true 否则返回false
public final boolean compareAndSet(int i, int expect, int update) {
    return compareAndSetRaw(checkedByteOffset(i), expect, update);
}
// 比较数组偏移量offset的值是否为expect 若是更新为update
// 更新成功返回true 否则返回false
private boolean compareAndSetRaw(long offset, int expect, int update) {
    return unsafe.compareAndSwapInt(array, offset, expect, update);
}
```

### boolean weakCompareAndSet(int i, int expect, int update)
```java
// 跟compareAndSet一致
public final boolean weakCompareAndSet(int i, int expect, int update) {
    return compareAndSet(i, expect, update);
}
```

### int getAndAdd(int i, int delta)
```java
// 将数组索引i的值设置为i+delta 并返回旧值
public final int getAndAdd(int i, int delta) {
    return unsafe.getAndAddInt(array, checkedByteOffset(i), delta);
}
```

### int addAndGet(int i, int delta)
```java
// 将数组索引i的值设置为i+delta 并返回新值
public final int addAndGet(int i, int delta) {
    // 见getAndAdd()方法
    return getAndAdd(i, delta) + delta;
}
```

### int getAndIncrement(int i)
```java
// 数组索引i的值进行i++操作 返回递增前的值
public final int getAndIncrement(int i) {
    return getAndAdd(i, 1);
}
```

### int incrementAndGet(int i)
```java
// 数组索引i的值进行++i操作 返回递增后的值
public final int incrementAndGet(int i) {
    return getAndAdd(i, 1) + 1;
}
```

### int getAndDecrement(int i)
```java
// 数组索引i的值进行i--操作 返回递减前的值
public final int getAndDecrement(int i) {
    return getAndAdd(i, -1);
}
```

### int decrementAndGet(int i)
```java
// 数组索引i的值进行--i操作 返回递减前后值
public final int decrementAndGet(int i) {
    return getAndAdd(i, -1) - 1;
}
```
### int getAndUpdate(int i, IntUnaryOperator updateFunction)
```java
// 数组索引i的值进行一元操作 返回操作前的值
public final int getAndUpdate(int i, IntUnaryOperator updateFunction) {
    long offset = checkedByteOffset(i);
    int prev, next;
    do {
        prev = getRaw(offset);
        // 一元操作方法
        next = updateFunction.applyAsInt(prev);
    } while (!compareAndSetRaw(offset, prev, next));
    return prev;
}
```

### int updateAndGet(int i, IntUnaryOperator updateFunction)
```java
// 数组索引i的值进行一元操作 返回操作后的值
public final int updateAndGet(int i, IntUnaryOperator updateFunction) {
    long offset = checkedByteOffset(i);
    int prev, next;
    do {
        prev = getRaw(offset);
        next = updateFunction.applyAsInt(prev);
    } while (!compareAndSetRaw(offset, prev, next));
    return next;
}
```

### int getAndAccumulate(int i, int x, IntBinaryOperator accumulatorFunction)
```java
// 数组索引i的值进行二元操作 返回操作前的值
public final int getAndAccumulate(int i, int x,
                                  IntBinaryOperator accumulatorFunction) {
    long offset = checkedByteOffset(i);
    int prev, next;
    do {
        prev = getRaw(offset);
        next = accumulatorFunction.applyAsInt(prev, x);
    } while (!compareAndSetRaw(offset, prev, next));
    return prev;
}
```

### int accumulateAndGet(int i, int x, IntBinaryOperator accumulatorFunction)
```java
// 数组索引i的值进行二元操作 返回操作后的值
public final int accumulateAndGet(int i, int x,
                                  IntBinaryOperator accumulatorFunction) {
    long offset = checkedByteOffset(i);
    int prev, next;
    do {
        prev = getRaw(offset);
        next = accumulatorFunction.applyAsInt(prev, x);
    } while (!compareAndSetRaw(offset, prev, next));
    return next;
}
```