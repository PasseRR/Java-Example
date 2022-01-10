---
layout: post
title: AtomicReferenceFieldUpdater
permalink: java.util.concurrent.atomic.AtomicReferenceFieldUpdater.html
order: 11
pk: java.util.concurrent.atomic
last_modified_at: 2022-01-10
---
> 原子更新对象的引用字段，字段必须为volatile修饰。AtomicReferenceFieldUpdater
> 是一个抽象类，通过一个工厂方法获得该抽象类子类的实例。

## 构造方法
- protected AtomicReferenceFieldUpdater() 受保护的构造方法 使用下面工厂方法代替  
```java
// tclass为对象字段持有Class vclass为字段对象Class fieldName为字段名
public static <U,W> AtomicReferenceFieldUpdater<U,W> newUpdater(Class<U> tclass,
                                                                Class<W> vclass,
                                                                String fieldName) {
    return new AtomicReferenceFieldUpdaterImpl<U,W>
        (tclass, vclass, fieldName, Reflection.getCallerClass());
}
```

## 静态内部类
```java
private static final class AtomicReferenceFieldUpdaterImpl<T,V>
    extends AtomicReferenceFieldUpdater<T,V> {
    // Unsafe实例
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    // 对象字段偏移量
    private final long offset;
    // 对象字段持有对象Class
    private final Class<T> tclass;
    // 对象字段Class
    private final Class<V> vclass;
    // caller Class
    private final Class<?> cclass;

    /*
     * Internal type checks within all update methods contain
     * internal inlined optimizations checking for the common
     * cases where the class is final (in which case a simple
     * getClass comparison suffices) or is of type Object (in
     * which case no check is needed because all objects are
     * instances of Object). The Object case is handled simply by
     * setting vclass to null in constructor.  The targetCheck and
     * updateCheck methods are invoked when these faster
     * screenings fail.
     */
    AtomicReferenceFieldUpdaterImpl(final Class<T> tclass,
                                    final Class<V> vclass,
                                    final String fieldName,
                                    final Class<?> caller) {
        final Field field;
        final Class<?> fieldClass;
        final int modifiers;
        try {
            // AccessController 根据当前有效的安全策略决定是否允许或拒绝对关键资源的访问
            // 根据字段名获得Field
            field = AccessController.doPrivileged(
                new PrivilegedExceptionAction<Field>() {
                    public Field run() throws NoSuchFieldException {
                        return tclass.getDeclaredField(fieldName);
                    }
                });
            // 获得字段修饰符
            modifiers = field.getModifiers();
            // 验证caller与tclass、tclass与字段之间的访问权限
            sun.reflect.misc.ReflectUtil.ensureMemberAccess(
                caller, tclass, null, modifiers);
            ClassLoader cl = tclass.getClassLoader();
            ClassLoader ccl = caller.getClassLoader();
            if ((ccl != null) && (ccl != cl) &&
                ((cl == null) || !isAncestor(cl, ccl))) {
              sun.reflect.misc.ReflectUtil.checkPackageAccess(tclass);
            }
            fieldClass = field.getType();
        } catch (PrivilegedActionException pae) {
            throw new RuntimeException(pae.getException());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        
        // 字段Class一致
        if (vclass != fieldClass)
            throw new ClassCastException();
        // 字段Class必须为引用类型
        if (vclass.isPrimitive())
            throw new IllegalArgumentException("Must be reference type");
        // 字段必须为volatile修饰
        if (!Modifier.isVolatile(modifiers))
            throw new IllegalArgumentException("Must be volatile type");

        this.cclass = (Modifier.isProtected(modifiers) &&
                       caller != tclass) ? caller : null;
        this.tclass = tclass;
        if (vclass == Object.class)
            this.vclass = null;
        else
            this.vclass = vclass;
        offset = unsafe.objectFieldOffset(field);
    }

    /**
     * Returns true if the second classloader can be found in the first
     * classloader's delegation chain.
     * Equivalent to the inaccessible: first.isAncestor(second).
     */
    // 校验first ClassLoader是否是second ClassLoader的父类
    private static boolean isAncestor(ClassLoader first, ClassLoader second) {
        ClassLoader acl = first;
        do {
            acl = acl.getParent();
            if (second == acl) {
                return true;
            }
        } while (acl != null);
        return false;
    }
    
    // 读校验
    void targetCheck(T obj) {
        if (!tclass.isInstance(obj))
            throw new ClassCastException();
        if (cclass != null)
            ensureProtectedAccess(obj);
    }

    // 写校验
    void updateCheck(T obj, V update) {
        // obj不是字段持有Class的实例或者update不是字段Class实例
        if (!tclass.isInstance(obj) ||
            (update != null && vclass != null && !vclass.isInstance(update)))
            throw new ClassCastException();
        if (cclass != null)
            ensureProtectedAccess(obj);
    }
    
    // 比较实例字段的值并替换
    public boolean compareAndSet(T obj, V expect, V update) {
        if (obj == null || obj.getClass() != tclass || cclass != null ||
            (update != null && vclass != null &&
             vclass != update.getClass()))
            updateCheck(obj, update);
        return unsafe.compareAndSwapObject(obj, offset, expect, update);
    }

    // 同compareAndSet
    public boolean weakCompareAndSet(T obj, V expect, V update) {
        // same implementation as strong form for now
        if (obj == null || obj.getClass() != tclass || cclass != null ||
            (update != null && vclass != null &&
             vclass != update.getClass()))
            updateCheck(obj, update);
        return unsafe.compareAndSwapObject(obj, offset, expect, update);
    }
    
    // putObjectVolatile保证set后立刻被其他线程看到
    public void set(T obj, V newValue) {
        if (obj == null || obj.getClass() != tclass || cclass != null ||
            (newValue != null && vclass != null &&
             vclass != newValue.getClass()))
            updateCheck(obj, newValue);
        unsafe.putObjectVolatile(obj, offset, newValue);
    }

    // putObjectVolatile的延迟实现 不能保证set后立刻被其他线程看到
    public void lazySet(T obj, V newValue) {
        if (obj == null || obj.getClass() != tclass || cclass != null ||
            (newValue != null && vclass != null &&
             vclass != newValue.getClass()))
            updateCheck(obj, newValue);
        unsafe.putOrderedObject(obj, offset, newValue);
    }

    // 获得引用字段最新值
    @SuppressWarnings("unchecked")
    public V get(T obj) {
        if (obj == null || obj.getClass() != tclass || cclass != null)
            targetCheck(obj);
        return (V)unsafe.getObjectVolatile(obj, offset);
    }

    // 设置引用字段为新值 返回旧值
    @SuppressWarnings("unchecked")
    public V getAndSet(T obj, V newValue) {
        if (obj == null || obj.getClass() != tclass || cclass != null ||
            (newValue != null && vclass != null &&
             vclass != newValue.getClass()))
            updateCheck(obj, newValue);
        return (V)unsafe.getAndSetObject(obj, offset, newValue);
    }

    // 确保obj的Class能访问
    private void ensureProtectedAccess(T obj) {
        if (cclass.isInstance(obj)) {
            return;
        }
        throw new RuntimeException(
            new IllegalAccessException("Class " +
                cclass.getName() +
                " can not access a protected member of class " +
                tclass.getName() +
                " using an instance of " +
                obj.getClass().getName()
            )
        );
    }
}
```

## 方法
### 抽象方法
```java
// 见AtomicReferenceFieldUpdaterImpl中compareAndSet方法
public abstract boolean compareAndSet(T obj, V expect, V update);

// 见AtomicReferenceFieldUpdaterImpl中weakCompareAndSet方法
public abstract boolean weakCompareAndSet(T obj, V expect, V update);

// 见AtomicReferenceFieldUpdaterImpl中set方法
public abstract void set(T obj, V newValue);

// 见AtomicReferenceFieldUpdaterImpl中lazySet方法
public abstract void lazySet(T obj, V newValue);

// 见AtomicReferenceFieldUpdaterImpl中get方法
public abstract V get(T obj);
```

### V getAndSet(T obj, V newValue)
```java
// 修改引用字段值 返回旧值
// 默认cas实现
// AtomicReferenceFieldUpdaterImpl已经重写此方法 使用getAndSetObject实现
public V getAndSet(T obj, V newValue) {
    V prev;
    do {
        prev = get(obj);
    } while (!compareAndSet(obj, prev, newValue));
    return prev;
}
```

### V getAndUpdate(T obj, UnaryOperator&lt;V&gt; updateFunction)
```java
// 对引用字段进行一元操作 返回操作前的值
// cas实现
public final V getAndUpdate(T obj, UnaryOperator<V> updateFunction) {
    V prev, next;
    do {
        prev = get(obj);
        next = updateFunction.apply(prev);
    } while (!compareAndSet(obj, prev, next));
    return prev;
}
```

### V updateAndGet(T obj, UnaryOperator&lt;V&gt; updateFunction)
```java
// 对引用字段进行一元操作 返回操作后的值
// cas实现
public final V updateAndGet(T obj, UnaryOperator<V> updateFunction) {
    V prev, next;
    do {
        prev = get(obj);
        next = updateFunction.apply(prev);
    } while (!compareAndSet(obj, prev, next));
    return next;
}
```

### V getAndAccumulate(T obj, V x, BinaryOperator&lt;V&gt; accumulatorFunction)
```java
// 对引用字段进行二元操作 返回操作前的值
// cas实现
public final V getAndAccumulate(T obj, V x,
                                BinaryOperator<V> accumulatorFunction) {
    V prev, next;
    do {
        prev = get(obj);
        next = accumulatorFunction.apply(prev, x);
    } while (!compareAndSet(obj, prev, next));
    return prev;
}
```

### V accumulateAndGet(T obj, V x, BinaryOperator&lt;V&gt; accumulatorFunction)
```java
// 对引用字段进行二元操作 返回操作后的值
// cas实现
public final V accumulateAndGet(T obj, V x,
                                BinaryOperator<V> accumulatorFunction) {
    V prev, next;
    do {
        prev = get(obj);
        next = accumulatorFunction.apply(prev, x);
    } while (!compareAndSet(obj, prev, next));
    return next;
}
```