---
layout: post
title: DoubleAdder
permalink: java.util.concurrent.atomic.DoubleAdder.html
order: 14
pk: java.util.concurrent.atomic
last_modified_at: 2022-01-10
---

> double累加器，跟[LongAdder](LongAdder.md)类似，操作的是double。
> 但是[Striped64](Striped64.md)的base是long型，需要Double.doubleToRawLongBits
> 及Double.longBitsToDouble来进行转换。

## 构造方法
### DoubleAdder()
```java
// 初始值为0
public DoubleAdder() {
}
```

## 方法
### void add(double x)
```java
// 累加double值x
// 跟LongAdder.add()类似
public void add(double x) {
    Cell[] as; long b, v; int m; Cell a;
    if ((as = cells) != null || // hash表不为null
        // 尝试在base上累加失败
        !casBase(b = base,
                 Double.doubleToRawLongBits
                 (Double.longBitsToDouble(b) + x))) {
        // 设置为非竞争
        boolean uncontended = true;
        if (as == null || (m = as.length - 1) < 0 || // hash表未初始化
            // hash表根据hash值计算的索引位置的Cell为null
            (a = as[getProbe() & m]) == null ||
            // 在Cell a上直接cas累加x失败 设置为竞争
            !(uncontended = a.cas(v = a.value,
                                  Double.doubleToRawLongBits
                                  (Double.longBitsToDouble(v) + x))))
            // 调用父类的doubleAccumulate
            // DoubleBinaryOperator为null是默认为累加
            doubleAccumulate(x, null, uncontended);
    }
}
```

### double sum()
```java
// 获得当前实例的double值
// 用于Number类的方法longValue、intValue、doubleValue等
// 类似LongAdder.sum()
public double sum() {
    Cell[] as = cells; Cell a;
    // 获得base的double值
    double sum = Double.longBitsToDouble(base);
    if (as != null) {
        // 累加hash表所有非null的单元
        for (int i = 0; i < as.length; ++i) {
            if ((a = as[i]) != null)
                sum += Double.longBitsToDouble(a.value);
        }
    }
    return sum;
}
```

### void reset()
```java
// 重置当前实例的double值为0
public void reset() {
    Cell[] as = cells; Cell a;
    // 重置base值为0
    // Double.longBitsToDouble(0L)值为0
    base = 0L; // relies on fact that double 0 must have same rep as long
    if (as != null) {
        // 重置hash表所有非null的单元为0
        for (int i = 0; i < as.length; ++i) {
            if ((a = as[i]) != null)
                a.value = 0L;
        }
    }
}
```

### double sumThenReset()
```java
// 获得当前实例的double值并重置
public double sumThenReset() {
    Cell[] as = cells; Cell a;
    // 获得base的值并重置为0
    double sum = Double.longBitsToDouble(base);
    base = 0L;
    if (as != null) {
        // 获得hash表所有非空单元的值并重置为0
        for (int i = 0; i < as.length; ++i) {
            if ((a = as[i]) != null) {
                long v = a.value;
                a.value = 0L;
                sum += Double.longBitsToDouble(v);
            }
        }
    }
    return sum;
}
```