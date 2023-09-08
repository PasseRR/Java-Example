# AtomicLong

可以用原子方式更新的long值。AtomicLong可用在应用程序中（如以原子方式更新的标志），但不能用于替换Long。
此类确实扩展了 Number，允许那些处理基于数字类的工具和实用工具进行统一访问。

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
- AtomicLong() 初始值为long基本类型默认值0  
- AtomicLong(long initialValue) 初始值为initialValue  

## 方法
### long get()
```java
// 返回当前值
public final long get() {
    return value;
}
```

### void set()
```java
// 无条件设置value为新值
public final void set(long newValue) {
    value = newValue;
}
```

### boolean compareAndSet(long expect, long update)
```java
// 将当前value值跟expect预期值比较 若相同将value更新为update
// 更新成功返回true 否则返回false
public final boolean compareAndSet(long expect, long update) {
    return unsafe.compareAndSwapLong(this, valueOffset, expect, update);
}
```

### boolean weakCompareAndSet(long expect, long update)
```java
// 作用同compareAndSet 
// 但是作者注释"可能意外失败并且不提供排序保证" 但是改功能没有实现 
// 区别compareAndSet方法有final修饰 不能重写
// weakCompareAndSet可以重新 可以通过继承AtomicLong重写此方法
public boolean weakCompareAndSet(long expect, long update) {
    return unsafe.compareAndSwapInt(this, valueOffset, expect, update);
}
```

### void lazySet(long newValue)
```java
// 该方法不能保证newValue修改立刻被其他线程看到
// putOrderedLong是putLongVolatile的延迟实现
public final void lazySet(long newValue) {
    unsafe.putOrderedLong(this, valueOffset, newValue);
}
```

### long getAndSet(long newValue)
```java
// 原子方式更新value 并返回旧值
public final long getAndSet(long newValue) {
    return unsafe.getAndSetLong(this, valueOffset, newValue);
}
```

### long getAndIncrement()
```java
// 原子的i++操作 返回i
public final long getAndIncrement() {
    return unsafe.getAndAddLong(this, valueOffset, 1);
}
```

### long incrementAndGet()
```java
// 原子的++i操作 返回i+1
public final long incrementAndGet() {
    return unsafe.getAndAddLong(this, valueOffset, 1) + 1;
}
```

### long getAndDecrement()
```java
// 原子的i--操作 返回i
public final long getAndDecrement() {
    return unsafe.getAndAddLong(this, valueOffset, -1);
}
```

### long decrementAndGet()
```java
// 原子--i操作 返回i-1
public final long decrementAndGet() {
    return unsafe.getAndAddLong(this, valueOffset, -1) - 1;
}
```

### long getAndAdd(long delta)
```java
// 原子的(i+delta)操作 返回旧值
public final long getAndAdd(long delta) {
    return unsafe.getAndAddLong(this, valueOffset, delta);
}
```

### long addAndGet(long delta)
```java
// 原子的(i+delta)操作 返回i+delta
public final long addAndGet(long delta) {
    return unsafe.getAndAddLong(this, valueOffset, delta) + delta;
}
```

### int getAndUpdate(LongUnaryOperator updateFunction)
```java
// 对旧值进行一元操作 返回旧值
public final long getAndUpdate(LongUnaryOperator updateFunction) {
    long prev, next;
    do {
        prev = get();
        next = updateFunction.applyAsLong(prev);
    } while (!compareAndSet(prev, next));
    return prev;
}
```

### long updateAndGet(LongUnaryOperator updateFunction)
```java
// 对旧值进行一元操作 返回新值
public final long updateAndGet(LongUnaryOperator updateFunction) {
    long prev, next;
    do {
        prev = get();
        next = updateFunction.applyAsLong(prev);
    } while (!compareAndSet(prev, next));
    return next;
}
```

### long getAndAccumulate(long x, LongBinaryOperator accumulatorFunction)
```java
// 对旧值进行二元操作 返回旧值
public final long getAndAccumulate(long x,
                                   LongBinaryOperator accumulatorFunction) {
    long prev, next;
    do {
        prev = get();
        next = accumulatorFunction.applyAsLong(prev, x);
    } while (!compareAndSet(prev, next));
    return prev;
}
```

### long accumulateAndGet(long x, LongBinaryOperator accumulatorFunction)
```java
// 对旧值进行二元操作 返回新值
public final long accumulateAndGet(long x,
                                   LongBinaryOperator accumulatorFunction) {
    long prev, next;
    do {
        prev = get();
        next = accumulatorFunction.applyAsLong(prev, x);
    } while (!compareAndSet(prev, next));
    return next;
}
```
