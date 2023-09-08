# LongAdder

LongAdder是jdk1.8提供的累加器，[Striped64](Striped64.md)的子类。它常用于状态采集、统计等场景。
[AtomicLong](AtomicLong.md)也可以用于这种场景，但在线程竞争激烈的情况下，LongAdder要比AtomicLong
拥有更高的吞吐量，但会耗费更多的内存空间。

## 构造方法
- LongAdder() 无参构造方法

## 方法
### void add(long x)
```java
// 在base上累加x
public void add(long x) {
    Cell[] as; long b, v; int m; Cell a;
    // 若hahs表不为空或通过cas累加x到base失败
    if ((as = cells) != null || !casBase(b = base, b + x)) {
        boolean uncontended = true; // 设置为非竞争
        if (as == null // hash表为空
            || (m = as.length - 1) < 0  // hash表长度为0
            // hash表根据hash值计算的索引位置的Cell为null
            || (a = as[getProbe() & m]) == null 
            // 在a(hash索引所在的Cell)上直接通过cas累加x失败
            || !(uncontended = a.cas(v = a.value, v + x)))
            // 调用父类的longAccumulate计算
            // LongBinaryOperator为null时 默认为累加
            longAccumulate(x, null, uncontended);
    }
}
```

### void increment()
```java
// 跟add(1L)一样
public void increment() {
    add(1L);
}
```

### void decrement()
```java
// 跟add(-1L)一样
public void decrement() {
    add(-1L);
}
```

### long sum()
```java
// 累加hash表所有非null单元的值
// 返回当前实例的值
// 用于Number类的方法longValue、intValue、doubleValue等
public long sum() {
    Cell[] as = cells; Cell a;
    long sum = base;
    if (as != null) {
        for (int i = 0; i < as.length; ++i) {
            if ((a = as[i]) != null)
                sum += a.value;
        }
    }
    return sum;
}
```

### void reset()
```java
// 重置当前LongAdder的值
public void reset() {
    Cell[] as = cells; Cell a;
    base = 0L; // 设置base为0
    if (as != null) {
        // 设置hash表所有单元值为0
        for (int i = 0; i < as.length; ++i) {
            if ((a = as[i]) != null)
                a.value = 0L;
        }
    }
}
```

### long sumThenReset()
```java
// 获得当前实例的值并重置为0
public long sumThenReset() {
    Cell[] as = cells; Cell a;
    // 累加base的值并重置base为0
    long sum = base;
    base = 0L;
    if (as != null) {
        // 累加hash表所有单元的值并重置为0
        for (int i = 0; i < as.length; ++i) {
            if ((a = as[i]) != null) {
                sum += a.value;
                a.value = 0L;
            }
        }
    }
    return sum;
}
```