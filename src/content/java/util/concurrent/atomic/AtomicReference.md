# AtomicReference
> 可以用原子方式更新的对象引用。

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
- AtomicReference() 初始值为null  
- AtomicLong(V initialValue) 初始值为initialValue  

## 方法
### V get()
```java
// 返回当前值
public final V get() {
    return value;
}
```

### void set()
```java
// 无条件设置value为新值
// 此方法非原子的
public final void set(V newValue) {
    value = newValue;
}
```

### boolean compareAndSet(V expect, V update)
```java
// 将当前value值跟expect预期值比较 若相同将value更新为update
// 更新成功返回true 否则返回false
public final boolean compareAndSet(V expect, V update) {
    return unsafe.compareAndSwapObject(this, valueOffset, expect, update);
}
```

### boolean weakCompareAndSet(V expect, V update)
```java
// 作用同compareAndSet 
// 但是作者注释"可能意外失败并且不提供排序保证" 但是改功能没有实现 
// 区别compareAndSet方法有final修饰 不能重写
// weakCompareAndSet可以重新 可以通过继承AtomicReference重写此方法
public boolean weakCompareAndSet(V expect, V update) {
    return unsafe.compareAndSwapObject(this, valueOffset, expect, update);
}
```

### void lazySet(V newValue)
```java
// 该方法不能保证newValue修改立刻被其他线程看到
// putOrderedObject是putObjectVolatile的延迟实现
public final void lazySet(V newValue) {
    unsafe.putOrderedObject(this, valueOffset, newValue);
}
```

### V getAndSet(V newValue)
```java
// 原子方式更新value 并返回旧值
public final V getAndSet(V newValue) {
    return unsafe.getAndSetObject(this, valueOffset, newValue);
}
```

### V getAndUpdate(UnaryOperator<V> updateFunction)
```java
// 对旧值进行一元操作 返回旧值
public final V getAndUpdate(UnaryOperator<V> updateFunction) {
    V prev, next;
    do {
        prev = get();
        next = updateFunction.apply(prev);
    } while (!compareAndSet(prev, next));
    return prev;
}
```

### V updateAndGet(UnaryOperator<V> updateFunction)
```java
// 对旧值进行一元操作 返回新值
public final V updateAndGet(UnaryOperator<V> updateFunction) {
    V prev, next;
    do {
        prev = get();
        next = updateFunction.apply(prev);
    } while (!compareAndSet(prev, next));
    return next;
}
```

### V getAndAccumulate(V x, BinaryOperator<V> accumulatorFunction)
```java
// 对旧值进行二元操作 返回旧值
public final V getAndAccumulate(V x,
                                BinaryOperator<V> accumulatorFunction) {
    V prev, next;
    do {
        prev = get();
        next = accumulatorFunction.apply(prev, x);
    } while (!compareAndSet(prev, next));
    return prev;
}
```

### V accumulateAndGet(V x, BinaryOperator<V> accumulatorFunction)
```java
// 对旧值进行二元操作 返回新值
public final V accumulateAndGet(V x,
                                BinaryOperator<V> accumulatorFunction) {
    V prev, next;
    do {
        prev = get();
        next = accumulatorFunction.apply(prev, x);
    } while (!compareAndSet(prev, next));
    return next;
}
```
