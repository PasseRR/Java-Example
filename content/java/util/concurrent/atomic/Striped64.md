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