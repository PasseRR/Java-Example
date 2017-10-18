---
layout: page
title: AtomicMarkableReference
permalink: java.util.concurrent.atomic.AtomicMarkableReference.html
---
> 可以用原子方式更新的对象引用及一个boolean标记

## 构造方法
- AtomicMarkableReference(V initialRef, boolean initialMark)  

## 静态内部类
```java
private static class Pair<T> {
    final T reference; // 引用
    final boolean mark; // 更新标记
    private Pair(T reference, boolean mark) {
        this.reference = reference;
        this.mark = mark;
    }
    // builder构建方法
    static <T> Pair<T> of(T reference, boolean mark) {
        return new Pair<T>(reference, mark);
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

### boolean isMarked()
```java
// 获得引用标记
public boolean isMarked() {
    return pair.mark;
}
```

### V get(boolean[] markHolder)
```java
// 获得当前引用及标记
// markHolder[0]为当前标记
public V get(boolean[] markHolder) {
    Pair<V> pair = this.pair;
    markHolder[0] = pair.mark;
    return pair.reference;
}
```

### void set(V newReference, boolean newMark)
```java
// 设置引用和标记为给定值 非原子操作
public void set(V newReference, boolean newMark) {
    Pair<V> current = pair;
    // 引用和标记其中一个不一致 则设置
    if (newReference != current.reference || newMark != current.mark)
        this.pair = Pair.of(newReference, newMark);
}
```

### boolean compareAndSet(V expectedReference, V newReference, boolean expectedMark, boolean newMark)
```java
public boolean compareAndSet(V       expectedReference,
                             V       newReference,
                             boolean expectedMark,
                             boolean newMark) {
    Pair<V> current = pair;
    return
        expectedReference == current.reference && // 引用一致
        expectedMark == current.mark && // 标记一致
        ((newReference == current.reference && 
          newMark == current.mark) || // 新引用和新标记未更改
         // 否则cas更新引用和标记
         casPair(current, Pair.of(newReference, newMark)));
}
// cas更新引用标记对
private boolean casPair(Pair<V> cmp, Pair<V> val) {
    return UNSAFE.compareAndSwapObject(this, pairOffset, cmp, val);
}
```

###  public boolean weakCompareAndSet(V expectedReference, V newReference, boolean expectedMark, boolean newMark)
```java
// 同compareAndSet方法
public boolean weakCompareAndSet(V       expectedReference,
                                 V       newReference,
                                 boolean expectedMark,
                                 boolean newMark) {
    return compareAndSet(expectedReference, newReference,
                         expectedMark, newMark);
}
```

### boolean attemptMark(V expectedReference, boolean newMark)
```java
// 尝试标记
// 标记成功返回true 否则返回false
public boolean attemptMark(V expectedReference, boolean newMark) {
    Pair<V> current = pair;
    return
        expectedReference == current.reference &&
        (newMark == current.mark ||
         casPair(current, Pair.of(expectedReference, newMark)));
}
```
