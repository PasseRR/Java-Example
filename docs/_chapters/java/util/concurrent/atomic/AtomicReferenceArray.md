---
layout: post
title: AtomicReferenceArray
permalink: java.util.concurrent.atomic.AtomicReferenceArray.html
order: 10
pk: java.util.concurrent.atomic
last_modified_at: 2022-01-10
---
可以原子更新的Object[]，修改指定索引的值和[AtomicReference](AtomicReference.md)的方法类似

## 静态域及块
```java
// Unsafe实例
private static final Unsafe unsafe = Unsafe.getUnsafe();
// Object数组第一个元素的偏移量
private static final int base;
// 用于定位数组元素内存位置的位移
private static final int shift;
// 数组相对于对象内存地址的偏移量
private static final long arrayFieldOffset;
// Object[]
private final Object[] array; // must have exact type Object[]

static {
    try {
        // Unsafe实例
        unsafe = Unsafe.getUnsafe();
        // 通过AtomicReferenceArray字段获得数组相对的偏移量
        arrayFieldOffset = unsafe.objectFieldOffset
            (AtomicReferenceArray.class.getDeclaredField("array"));
        // 数组第一个元素的偏移量 可以用Unsafe.ARRAY_OBJECT_BASE_OFFSET代替
        base = unsafe.arrayBaseOffset(Object[].class);
        // 数组元素字节大小 可以用Unsafe.ARRAY_OBJECT_INDEX_SCALE代替
        int scale = unsafe.arrayIndexScale(Object[].class);
        if ((scale & (scale - 1)) != 0)
            throw new Error("data type scale not a power of two");
        // 获得每个元素之间的位移
        shift = 31 - Integer.numberOfLeadingZeros(scale);
    } catch (Exception e) {
        throw new Error(e);
    }
}
```

## 构造方法
- AtomicReferenceArray(int length) 根据指定length初始化数组  
- AtomicReferenceArray(E[] array) 根据给定array初始化数组  

## 方法
### int length() 
```java
// 返回数组元素个数
public final int length() {
    return array.length;
}
```

### E get(int i)
```java
// 获得数组给定索引i的值
public final long get(int i) {
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
// 获得数组给定偏移量offset的值
private E getRaw(long offset) {
    return (E) unsafe.getObjectVolatile(array, offset);
}
```

### void set(int i, E newValue)
```java
// 更新数组索引i的值为newValue
public final void set(int i, E newValue) {
    unsafe.putObjectVolatile(array, checkedByteOffset(i), newValue);
}
```

### void lazySet(int i, E newValue)
```java
// 更新数组索引i的值为newValue 不能保证被其他线程实时看到最新值
public final void lazySet(int i, E newValue) {
    unsafe.putOrderedObject(array, checkedByteOffset(i), newValue);
}
```

### E getAndSet(int i, E newValue)
```java
// 设置数组索引i的值为newValue 并返回该位置的旧值
public final E getAndSet(int i, E newValue) {
    return (E)unsafe.getAndSetObject(array, checkedByteOffset(i), newValue);
}
```

### boolean compareAndSet(int i, E expect, E update)
```java
// 比较数组索引i的值是否为expect 若是更新为update
// 更新成功返回true 否则返回false
public final boolean compareAndSet(int i, E expect, E update) {
    return compareAndSetRaw(checkedByteOffset(i), expect, update);
}
// 比较数组偏移量offset的值是否为expect 若是更新为update
// 更新成功返回true 否则返回false
private boolean compareAndSetRaw(long offset, E expect, E update) {
    return unsafe.compareAndSwapObject(array, offset, expect, update);
}
```

### boolean weakCompareAndSet(int i, E expect, E update)
```java
// 跟compareAndSet一致
public final boolean weakCompareAndSet(int i, E expect, E update) {
    return compareAndSet(i, expect, update);
}
```

### E getAndUpdate(int i, UnaryOperator&lt;E&gt; updateFunction)
```java
// 数组索引i的值进行一元操作 返回操作前的值
public final E getAndUpdate(int i, UnaryOperator<E> updateFunction) {
    long offset = checkedByteOffset(i);
    E prev, next;
    do {
        prev = getRaw(offset);
        next = updateFunction.apply(prev);
    } while (!compareAndSetRaw(offset, prev, next));
    return prev;
}
```

### E updateAndGet(int i, UnaryOperator&lt;E&gt; updateFunction)
```java
// 数组索引i的值进行一元操作 返回操作后的值
public final E updateAndGet(int i, UnaryOperator<E> updateFunction) {
    long offset = checkedByteOffset(i);
    E prev, next;
    do {
        prev = getRaw(offset);
        next = updateFunction.apply(prev);
    } while (!compareAndSetRaw(offset, prev, next));
    return next;
}
```

### E getAndAccumulate(int i, E x,  BinaryOperator&lt;E&gt; accumulatorFunction)
```java
// 数组索引i的值进行二元操作 返回操作前的值
 public final E getAndAccumulate(int i, E x,
                                BinaryOperator<E> accumulatorFunction) {
    long offset = checkedByteOffset(i);
    E prev, next;
    do {
        prev = getRaw(offset);
        next = accumulatorFunction.apply(prev, x);
    } while (!compareAndSetRaw(offset, prev, next));
    return prev;
}
```

### E accumulateAndGet(int i, E x, BinaryOperator&lt;E&gt; accumulatorFunction)
```java
// 数组索引i的值进行二元操作 返回操作后的值
public final E accumulateAndGet(int i, E x,
                                BinaryOperator<E> accumulatorFunction) {
    long offset = checkedByteOffset(i);
    E prev, next;
    do {
        prev = getRaw(offset);
        next = accumulatorFunction.apply(prev, x);
    } while (!compareAndSetRaw(offset, prev, next));
    return next;
}
```