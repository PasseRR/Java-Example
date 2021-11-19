---
layout: page
title: LongAccumulator
permalink: java.util.concurrent.atomic.LongAccumulator.html
---

> LongAccumulator和[LongAdder](LongAdder.md)类似，也基于[Striped64](Striped64.md)实现。
> 但要比LongAdder更加灵活(要传入一个LongBinaryOperator二元操作方法)，
> LongAdder相当于是LongAccumulator的一种特例。

```java
// LongAdder相当于
new LongAccumulator((x, y) -> x + y, 0);
```
## 域
```java
// 二元操作方法
private final LongBinaryOperator function;
// 初始值
// reset的时候会以这个值设置
private final long identity;
```

## 构造方法
### LongAccumulator(LongBinaryOperator accumulatorFunction, long identity)
```java
public LongAccumulator(LongBinaryOperator accumulatorFunction,
                       long identity) {
    // 设置操作方法
    this.function = accumulatorFunction;
    // 设置初始值及base的值
    base = this.identity = identity;
}
```

## 方法
### void accumulate(long x)
```java
public void accumulate(long x) {
    // hash表快照
    Cell[] as; 
    // b为base
    // v为Cell a的value
    // r为base和x的二元操作结果
    long b, v, r; 
    // hash表的长度-1
    int m; 
    // 根据hash值计算索引位置的Cell
    Cell a;
    if ((as = cells) != null || // hash表不为空
        // r != b 且 cas修改base不成功
        (r = function.applyAsLong(b = base, x)) != b && !casBase(b, r)) {
        boolean uncontended = true; // 设置为非竞争
        if (as == null // hash表为null 
            || (m = as.length - 1) < 0 // hash表长度为0
            // hash表根据hash值计算的索引位置的Cell为null
            || (a = as[getProbe() & m]) == null 
            || !(uncontended = // cell计算前后的值一致或在cell做cas失败
              (r = function.applyAsLong(v = a.value, x)) == v 
              || a.cas(v, r)))
            // 调用父类的longAccumulate方法计算
            longAccumulate(x, function, uncontended);
    }
}
```

### long get()
```java
// 获得当前实例的值
// 类似LongAdder的sum()
// 用于Number类的方法longValue、intValue、doubleValue等
public long get() {
    Cell[] as = cells; Cell a;
    long result = base; // 初始化计算值为base
    if (as != null) {
        // 对hash表所有非空单元进行计算
        for (int i = 0; i < as.length; ++i) {
            if ((a = as[i]) != null)
                // 设置resut为计算结果
                result = function.applyAsLong(result, a.value);
        }
    }
    return result;
}
```

### void reset() 
```java
// 重置当前实例的值
// 类似LongAdder的reset()
public void reset() {
    Cell[] as = cells; Cell a;
    base = identity; // 重置base为identity
    if (as != null) {
        // 重置hash表所有非空单元为identity
        for (int i = 0; i < as.length; ++i) {
            if ((a = as[i]) != null)
                a.value = identity;
        }
    }
}
```

### long getThenReset()
```java
// 获得当前实例的值并重置
// 类似LongAdder的sumThenReset()
public long getThenReset() {
    Cell[] as = cells; Cell a;
    long result = base; // 初始化计算值为base
    base = identity; // 重置base为identity
    if (as != null) {
        // hash表的所有非空单元进行计算并初始化
        for (int i = 0; i < as.length; ++i) {
            if ((a = as[i]) != null) {
                long v = a.value; // 快照备份
                a.value = identity; // 初始化该单元
                // 设置result为计算结果
                result = function.applyAsLong(result, v);
            }
        }
    }
    return result;
}
```