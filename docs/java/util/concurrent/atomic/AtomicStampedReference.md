# AtomicStampedReference

可以用原子方式更新的对象引用及一个int版本号

## 构造方法
- AtomicStampedReference(V initialRef, int initialMark)  

## 静态内部类
```java
private static class Pair<T> {
    final T reference; // 引用
    final int stamp; // 版本号
    private Pair(T reference, int stamp) {
        this.reference = reference;
        this.stamp = stamp;
    }
    // builder构造方法
    static <T> Pair<T> of(T reference, int stamp) {
        return new Pair<T>(reference, stamp);
    }
}
```

## 方法
### V getReference()
```java
// 获得当前引用
public V getReference() {
    return pair.reference;
}
```

### int getStamp()
```java
// 获得当前版本号
public int getStamp() {
    return pair.stamp;
}
```

### V get(int[] stampHolder)
```java
// 获得当前引用及版本号
// stampHolder[0]为当前版本号
public V get(int[] stampHolder) {
    Pair<V> pair = this.pair;
    stampHolder[0] = pair.stamp;
    return pair.reference;
}
```

### void set(V newReference, int newStamp)
```java
// 设置引用和版本号为给定值 非原子操作
public void set(V newReference, int newStamp) {
    Pair<V> current = pair;
    // 引用和版本号其中一个不一致 则设置
    if (newReference != current.reference || newStamp != current.stamp)
        this.pair = Pair.of(newReference, newStamp);
}
```

### boolean compareAndSet(V expectedReference, V newReference, int expectedStamp, int newStamp)
```java
public boolean compareAndSet(V   expectedReference,
                             V   newReference,
                             int expectedStamp,
                             int newStamp) {
    Pair<V> current = pair;
    return
        expectedReference == current.reference && // 引用一致
        expectedStamp == current.stamp && // 版本号一直
        ((newReference == current.reference && 
          newStamp == current.stamp) || // 新引用和新版本号未更改
         // 否则 cas更新引用版本号对
         casPair(current, Pair.of(newReference, newStamp)));
}
// cas更新引用版本号对
private boolean casPair(Pair<V> cmp, Pair<V> val) {
    return UNSAFE.compareAndSwapObject(this, pairOffset, cmp, val);
}
```

###  public boolean weakCompareAndSet(V expectedReference, V newReference, int expectedStamp, int newStamp)
```java
// 同compareAndSet方法
public boolean weakCompareAndSet(V   expectedReference,
                                 V   newReference,
                                 int expectedStamp,
                                 int newStamp) {
    return compareAndSet(expectedReference, newReference,
                         expectedStamp, newStamp);
}
```

### boolean attemptStamp(V expectedReference, int newStamp)
```java
// 尝试标记
// 标记成功返回true 否则返回false
public boolean attemptStamp(V expectedReference, int newStamp) {
    Pair<V> current = pair;
    return
        expectedReference == current.reference &&
        (newStamp == current.stamp ||
         casPair(current, Pair.of(expectedReference, newStamp)));
}
```
