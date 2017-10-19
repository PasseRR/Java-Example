---
layout: page
title: AtomicIntegerFieldUpdater
permalink: java.util.concurrent.atomic.AtomicIntegerFieldUpdater.html
---
> 原子更新对象的int字段，字段的类型必须为int，且必须为volatile修饰。AtomicIntegerFieldUpdater
> 是一个抽象类，通过一个工厂方法获得该抽象类子类的实例。

## 构造方法
- protected AtomicIntegerFieldUpdater() 受保护的构造方法 使用下面工厂方法代替  
```java
public static <U> AtomicIntegerFieldUpdater<U> newUpdater(Class<U> tclass,
                                                          String fieldName) {
    return new AtomicIntegerFieldUpdaterImpl<U>
        (tclass, fieldName, Reflection.getCallerClass());
}
```

## 静态内部类
```java
private static class AtomicIntegerFieldUpdaterImpl<T>
            extends AtomicIntegerFieldUpdater<T> {
    // Unsafe实例
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    // 字段偏移量
    private final long offset;
    // 字段所属Class
    private final Class<T> tclass;
    // 调用者Class
    private final Class<?> cclass;

    AtomicIntegerFieldUpdaterImpl(final Class<T> tclass,
                                  final String fieldName,
                                  final Class<?> caller) {
        final Field field;
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
        } catch (PrivilegedActionException pae) {
            throw new RuntimeException(pae.getException());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        Class<?> fieldt = field.getType();
        // 字段必须为int类型
        if (fieldt != int.class)
            throw new IllegalArgumentException("Must be integer type");
        
        // 字段必须为volatile修饰
        if (!Modifier.isVolatile(modifiers))
            throw new IllegalArgumentException("Must be volatile type");

        this.cclass = (Modifier.isProtected(modifiers) &&
                       caller != tclass) ? caller : null;
        this.tclass = tclass;
        // 字段偏移量
        offset = unsafe.objectFieldOffset(field);
    }

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

    // 访问权限校验
    private void fullCheck(T obj) {
        // 确保obj是tclass的实例
        if (!tclass.isInstance(obj))
            throw new ClassCastException();
        if (cclass != null)
            ensureProtectedAccess(obj);
    }

    // 比较实例字段的值并替换
    public boolean compareAndSet(T obj, int expect, int update) {
        if (obj == null || obj.getClass() != tclass || cclass != null) fullCheck(obj);
        return unsafe.compareAndSwapInt(obj, offset, expect, update);
    }

    // 跟compareAndSet一致
    public boolean weakCompareAndSet(T obj, int expect, int update) {
        if (obj == null || obj.getClass() != tclass || cclass != null) fullCheck(obj);
        return unsafe.compareAndSwapInt(obj, offset, expect, update);
    }

    // putIntVolatile保证set后立刻被其他线程看到
    public void set(T obj, int newValue) {
        if (obj == null || obj.getClass() != tclass || cclass != null) fullCheck(obj);
        unsafe.putIntVolatile(obj, offset, newValue);
    }

    // putIntVolatile的延迟实现 不能保证set后立刻被其他线程看到
    public void lazySet(T obj, int newValue) {
        if (obj == null || obj.getClass() != tclass || cclass != null) fullCheck(obj);
        unsafe.putOrderedInt(obj, offset, newValue);
    }

    // 获得int字段最新值
    public final int get(T obj) {
        if (obj == null || obj.getClass() != tclass || cclass != null) fullCheck(obj);
        return unsafe.getIntVolatile(obj, offset);
    }

    // 设置int字段为新值 返回旧值
    public int getAndSet(T obj, int newValue) {
        if (obj == null || obj.getClass() != tclass || cclass != null) fullCheck(obj);
        return unsafe.getAndSetInt(obj, offset, newValue);
    }

    // int字段做原子i++操作 返回旧值
    public int getAndIncrement(T obj) {
        return getAndAdd(obj, 1);
    }

    // int字段做原子i--操作 返回旧值
    public int getAndDecrement(T obj) {
        return getAndAdd(obj, -1);
    }

    // int字段增加delta 返回旧值
    public int getAndAdd(T obj, int delta) {
        if (obj == null || obj.getClass() != tclass || cclass != null) fullCheck(obj);
        return unsafe.getAndAddInt(obj, offset, delta);
    }

    // int字段做原子++i 返回新值
    public int incrementAndGet(T obj) {
        return getAndAdd(obj, 1) + 1;
    }

    // int字段做原子--i 返回新值
    public int decrementAndGet(T obj) {
         return getAndAdd(obj, -1) - 1;
    }

    // int字段增加delta 返回新值
    public int addAndGet(T obj, int delta) {
        return getAndAdd(obj, delta) + delta;
    }

    // 确保obj的Class能访问
    private void ensureProtectedAccess(T obj) {
        // 如果obj是cclass的实例
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
抽象方法均由静态内部类AtomicIntegerFieldUpdaterImpl实现，其他为AtomicIntegerFieldUpdater实现  
### 抽象方法
```java
// 见AtomicIntegerFieldUpdaterImpl中compareAndSet方法
public abstract boolean compareAndSet(T obj, int expect, int update);

// 见AtomicIntegerFieldUpdaterImpl中weakCompareAndSet方法
public abstract boolean weakCompareAndSet(T obj, int expect, int update);

// 见AtomicIntegerFieldUpdaterImpl中set方法
public abstract void set(T obj, int newValue);

// 见AtomicIntegerFieldUpdaterImpl中lazySet方法
public abstract void lazySet(T obj, int newValue);

// 见AtomicIntegerFieldUpdaterImpl中get方法
public abstract int get(T obj);
```
### int getAndSet(T obj, int newValue)
```java
// 修改int字段值 返回旧值
// 默认cas实现
// AtomicIntegerFieldUpdaterImpl已经重写此方法 使用getAndSetInt实现
public int getAndSet(T obj, int newValue) {
    int prev;
    do {
        // 子类get实现
        prev = get(obj);
    } while (!compareAndSet(obj, prev, newValue));
    return prev;
}
```

### int getAndIncrement(T obj)
```java
// 原子i++操作 返回旧值
// 默认cas实现
// AtomicIntegerFieldUpdaterImpl已经重写此方法 使用getAndAddInt实现
public int getAndIncrement(T obj) {
    int prev, next;
    do {
        prev = get(obj);
        next = prev + 1;
    } while (!compareAndSet(obj, prev, next));
    return prev;
}
```

### int getAndDecrement(T obj)
```java
// 原子i--操作 返回旧值
// 默认cas实现
// AtomicIntegerFieldUpdaterImpl已经重写此方法 使用getAndAddInt实现
public int getAndDecrement(T obj) {
    int prev, next;
    do {
        prev = get(obj);
        next = prev - 1;
    } while (!compareAndSet(obj, prev, next));
    return prev;
}
```

### int getAndAdd(T obj, int delta)
```java
// 原子i+n操作 返回旧值
// 默认cas实现
// AtomicIntegerFieldUpdaterImpl已经重写此方法 使用getAndAddInt实现
public int getAndAdd(T obj, int delta) {
    int prev, next;
    do {
        prev = get(obj);
        next = prev + delta;
    } while (!compareAndSet(obj, prev, next));
    return prev;
}
```

### int incrementAndGet(T obj)
```java
// 原子++i操作 返回新值
// 默认cas实现
// AtomicIntegerFieldUpdaterImpl已经重写此方法 使用getAndAddInt实现
public int incrementAndGet(T obj) {
    int prev, next;
    do {
        prev = get(obj);
        next = prev + 1;
    } while (!compareAndSet(obj, prev, next));
    return next;
}
```

### int decrementAndGet(T obj)
```java
// 原子--i操作 返回新值
// 默认cas实现
// AtomicIntegerFieldUpdaterImpl已经重写此方法 使用getAndAddInt实现
public int decrementAndGet(T obj) {
    int prev, next;
    do {
        prev = get(obj);
        next = prev - 1;
    } while (!compareAndSet(obj, prev, next));
    return next;
}
```

### int addAndGet(T obj, int delta)
```java
// 原子i+n操作 返回新值
// 默认cas实现
// AtomicIntegerFieldUpdaterImpl已经重写此方法 使用getAndAddInt实现
public int addAndGet(T obj, int delta) {
    int prev, next;
    do {
        prev = get(obj);
        next = prev + delta;
    } while (!compareAndSet(obj, prev, next));
    return next;
}
```

### int getAndUpdate(T obj, IntUnaryOperator updateFunction)
```java
// int字段进行一元操作 返回旧值
// cas实现
public final int getAndUpdate(T obj, IntUnaryOperator updateFunction) {
    int prev, next;
    do {
        prev = get(obj);
        next = updateFunction.applyAsInt(prev);
    } while (!compareAndSet(obj, prev, next));
    return prev;
}
```

### int updateAndGet(T obj, IntUnaryOperator updateFunction)
```java
// int字段进行一元操作 返回新值
// cas实现
public final int updateAndGet(T obj, IntUnaryOperator updateFunction) {
    int prev, next;
    do {
        prev = get(obj);
        next = updateFunction.applyAsInt(prev);
    } while (!compareAndSet(obj, prev, next));
    return next;
}
```

### int getAndAccumulate(T obj, int x, IntBinaryOperator accumulatorFunction)
```java
// int字段进行二元操作 返回旧值
// cas实现
public final int getAndAccumulate(T obj, int x,
                                  IntBinaryOperator accumulatorFunction) {
    int prev, next;
    do {
        prev = get(obj);
        next = accumulatorFunction.applyAsInt(prev, x);
    } while (!compareAndSet(obj, prev, next));
    return prev;
}
```

### int accumulateAndGet(T obj, int x, IntBinaryOperator accumulatorFunction)
```java
// int字段进行二元操作 返回新值
// cas实现
public final int accumulateAndGet(T obj, int x,
                                  IntBinaryOperator accumulatorFunction) {
    int prev, next;
    do {
        prev = get(obj);
        next = accumulatorFunction.applyAsInt(prev, x);
    } while (!compareAndSet(obj, prev, next));
    return next;
}
```

