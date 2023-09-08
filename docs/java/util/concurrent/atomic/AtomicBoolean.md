# AtomicBoolean

可以用原子方式更新的boolean值。AtomicBoolean可用在应用程序中（如以原子方式更新的标志），但不能用于替换Boolean。  

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
- AtomicBoolean() 初始值为boolean基本类型默认值false  
- AtomicBoolean(boolean initialValue) 初始值为initialValue  

## 方法
### boolean get()
```java
// 返回当前值
public final boolean get() {
    return value != 0;
}
```

### void set()
```java
// 无条件设置value为新值
public final void set(boolean newValue) {
    value = newValue ? 1 : 0;
}
```

### boolean compareAndSet(boolean expect, boolean update)
```java
// 将当前value值跟expect预期值比较 若相同将value更新为update
// 更新成功返回true 否则返回false
public final boolean compareAndSet(boolean expect, boolean update) {
    int e = expect ? 1 : 0;
    int u = update ? 1 : 0;
    return unsafe.compareAndSwapInt(this, valueOffset, e, u);
}
```

### boolean weakCompareAndSet(boolean expect, boolean update)
```java
// 作用同compareAndSet 
// 但是作者注释"可能意外失败并且不提供排序保证" 但是改功能没有实现 
// 区别compareAndSet方法有final修饰 不能重写
// weakCompareAndSet可以重新 可以通过继承AtomicBoolean重写此方法
public boolean weakCompareAndSet(boolean expect, boolean update) {
    int e = expect ? 1 : 0;
    int u = update ? 1 : 0;
    return unsafe.compareAndSwapInt(this, valueOffset, e, u);
}
```

### void lazySet(boolean newValue)
```java
// 该方法不能保证newValue修改立刻被其他线程看到
// putOrderedInt是putIntVolatile的延迟实现
public final void lazySet(boolean newValue) {
    int v = newValue ? 1 : 0;
    unsafe.putOrderedInt(this, valueOffset, v);
}
```

### boolean getAndSet(boolean newValue)
```java
// 原子方式更新value 并返回旧值
// 自旋锁方式实现
public final boolean getAndSet(boolean newValue) {
    boolean prev;
    do {
        prev = get();
    } while (!compareAndSet(prev, newValue)); // 保证拿到的是最新值
    return prev;
}
```
