# AtomicLongFieldUpdater
> 原子更新对象的long字段，字段的类型必须为long，且必须为volatile修饰。AtomicLongFieldUpdater
> 是一个抽象类，通过一个工厂方法获得该抽象类子类的实例。

## 构造方法
- protected AtomicLongFieldUpdater() 受保护的构造方法 使用下面工厂方法代替  
```java
public static <U> AtomicLongFieldUpdater<U> newUpdater(Class<U> tclass,
                                                       String fieldName) {
    Class<?> caller = Reflection.getCallerClass();
    // 若JVM支持long型的cas直接使用Unsafe的compareAndSwapLong
    if (AtomicLong.VM_SUPPORTS_LONG_CAS) 
        return new CASUpdater<U>(tclass, fieldName, caller);
    else // 若JVM不支持cas 则使用synchronized方式实现cas
        return new LockedUpdater<U>(tclass, fieldName, caller);
}
```

## 静态内部类
### CASUpdater
```java
// Unsafe CAS Updater
private static class CASUpdater<T> extends AtomicLongFieldUpdater<T> {
    // Unsafe实例
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    // long字段的偏移量
    private final long offset;
    // 字段所属Class
    private final Class<T> tclass;
    // caller Class
    private final Class<?> cclass;

    CASUpdater(final Class<T> tclass, final String fieldName,
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
        // 字段必须为long类型
        if (fieldt != long.class)
            throw new IllegalArgumentException("Must be long type");

        // 字段必须为volatile修饰
        if (!Modifier.isVolatile(modifiers))
            throw new IllegalArgumentException("Must be volatile type");

        this.cclass = (Modifier.isProtected(modifiers) &&
                       caller != tclass) ? caller : null;
        this.tclass = tclass;
        // 字段偏移量
        offset = unsafe.objectFieldOffset(field);
    }

    // 访问权限校验
    private void fullCheck(T obj) {
        // 确保obj是tclass的实例
        if (!tclass.isInstance(obj))
            throw new ClassCastException();
        // 校验caller Class权限
        if (cclass != null)
            ensureProtectedAccess(obj);
    }

    // 比较实例字段的值并替换
    public boolean compareAndSet(T obj, long expect, long update) {
        if (obj == null || obj.getClass() != tclass || cclass != null) fullCheck(obj);
        return unsafe.compareAndSwapLong(obj, offset, expect, update);
    }

    // 跟compareAndSet一致
    public boolean weakCompareAndSet(T obj, long expect, long update) {
        if (obj == null || obj.getClass() != tclass || cclass != null) fullCheck(obj);
        return unsafe.compareAndSwapLong(obj, offset, expect, update);
    }

    // putLongVolatile保证set后立刻被其他线程看到
    public void set(T obj, long newValue) {
        if (obj == null || obj.getClass() != tclass || cclass != null) fullCheck(obj);
        unsafe.putLongVolatile(obj, offset, newValue);
    }
    
    // putLongVolatile的延迟实现 不能保证set后立刻被其他线程看到
    public void lazySet(T obj, long newValue) {
        if (obj == null || obj.getClass() != tclass || cclass != null) fullCheck(obj);
        unsafe.putOrderedLong(obj, offset, newValue);
    }

    // 获得long字段最新值
    public long get(T obj) {
        if (obj == null || obj.getClass() != tclass || cclass != null) fullCheck(obj);
        return unsafe.getLongVolatile(obj, offset);
    }

    // 设置long字段为新值 返回旧值
    public long getAndSet(T obj, long newValue) {
        if (obj == null || obj.getClass() != tclass || cclass != null) fullCheck(obj);
        return unsafe.getAndSetLong(obj, offset, newValue);
    }

    // long字段做原子i++操作 返回旧值
    public long getAndIncrement(T obj) {
        return getAndAdd(obj, 1);
    }
    
    // long字段做原子i--操作 返回旧值
    public long getAndDecrement(T obj) {
        return getAndAdd(obj, -1);
    }

    // long字段增加delta 返回旧值
    public long getAndAdd(T obj, long delta) {
        if (obj == null || obj.getClass() != tclass || cclass != null) fullCheck(obj);
        return unsafe.getAndAddLong(obj, offset, delta);
    }

    // long字段做原子++i 返回新值
    public long incrementAndGet(T obj) {
        return getAndAdd(obj, 1) + 1;
    }

    // long字段做原子--i 返回新值
    public long decrementAndGet(T obj) {
         return getAndAdd(obj, -1) - 1;
    }

    // long字段增加delta 返回新值 自增自减的基础实现
    public long addAndGet(T obj, long delta) {
        return getAndAdd(obj, delta) + delta;
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
### LockedUpdater
```java
// synchronized实现原子操作
// 锁AtomicLongFieldUpdater实例 同时只能有一个线程修改或读取
// synchronized性能比内存屏障差
private static class LockedUpdater<T> extends AtomicLongFieldUpdater<T> {
    // Unsafe实例
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    // 字段偏移量
    private final long offset;
    // 字段所属Class
    private final Class<T> tclass;
    // caller Class
    private final Class<?> cclass;

    LockedUpdater(final Class<T> tclass, final String fieldName,
                  final Class<?> caller) {
        // 构造方法初始检查同CASUpdater一致
        Field field = null;
        int modifiers = 0;
        try {
            field = AccessController.doPrivileged(
                new PrivilegedExceptionAction<Field>() {
                    public Field run() throws NoSuchFieldException {
                        return tclass.getDeclaredField(fieldName);
                    }
                });
            modifiers = field.getModifiers();
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
        if (fieldt != long.class)
            throw new IllegalArgumentException("Must be long type");

        if (!Modifier.isVolatile(modifiers))
            throw new IllegalArgumentException("Must be volatile type");

        this.cclass = (Modifier.isProtected(modifiers) &&
                       caller != tclass) ? caller : null;
        this.tclass = tclass;
        offset = unsafe.objectFieldOffset(field);
    }

    // 访问权限检查 同CASUpdater一致
    private void fullCheck(T obj) {
        if (!tclass.isInstance(obj))
            throw new ClassCastException();
        if (cclass != null)
            ensureProtectedAccess(obj);
    }
    
    // 比较实例long字段的值并替换
    public boolean compareAndSet(T obj, long expect, long update) {
        if (obj == null || obj.getClass() != tclass || cclass != null) fullCheck(obj);
        // 锁当前实例 获得内存中long字段的值并与预期值比较 相同则更新内存中的值 否则返回false
        synchronized (this) {
            long v = unsafe.getLong(obj, offset);
            if (v != expect)
                return false;
            unsafe.putLong(obj, offset, update);
            return true;
        }
    }
    
    // 同compareAndSet
    public boolean weakCompareAndSet(T obj, long expect, long update) {
        return compareAndSet(obj, expect, update);
    }
    
    // 设置long字段的值
    public void set(T obj, long newValue) {
        if (obj == null || obj.getClass() != tclass || cclass != null) fullCheck(obj);
        // 锁当前实例 更新内存中的值
        synchronized (this) {
            unsafe.putLong(obj, offset, newValue);
        }
    }
    
    // 同set
    public void lazySet(T obj, long newValue) {
        set(obj, newValue);
    }
    
    // 锁当前实例 获得long字段值
    public long get(T obj) {
        if (obj == null || obj.getClass() != tclass || cclass != null) fullCheck(obj);
        synchronized (this) {
            return unsafe.getLong(obj, offset);
        }
    }

    // 同CASUpdater一致
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
// 见CASUpdater及LockedUpdater中compareAndSet方法
public abstract boolean compareAndSet(T obj, long expect, long update);
// 见CASUpdater及LockedUpdater中weakCompareAndSet方法
public abstract boolean weakCompareAndSet(T obj, long expect, long update);
// 见CASUpdater及LockedUpdater中set方法
public abstract void set(T obj, long newValue);
// 见CASUpdater及LockedUpdater中lazySet方法
public abstract void lazySet(T obj, long newValue);
// 见CASUpdater及LockedUpdater中get方法
public abstract long get(T obj);
```

### long getAndSet(T obj, long newValue)
```java
// 修改long字段值 返回旧值
// 默认cas实现 
// CASUpdater已经重写此方法 LockedUpdater未重写
public long getAndSet(T obj, long newValue) {
    long prev;
    do {
        prev = get(obj);
    } while (!compareAndSet(obj, prev, newValue));
    return prev;
}
```

### long getAndIncrement(T obj)
```java
// 原子i++操作 返回旧值
// 默认cas实现 
// CASUpdater已经重写此方法 LockedUpdater未重写
public long getAndIncrement(T obj) {
    long prev, next;
    do {
        prev = get(obj);
        next = prev + 1;
    } while (!compareAndSet(obj, prev, next));
    return prev;
}
```

### long getAndDecrement(T obj)
```java
// 原子i--操作 返回旧值
// 默认cas实现
// CASUpdater已经重写此方法 LockedUpdater未重写
public long getAndDecrement(T obj) {
    long prev, next;
    do {
        prev = get(obj);
        next = prev - 1;
    } while (!compareAndSet(obj, prev, next));
    return prev;
}
```

### long getAndAdd(T obj, long delta)
```java
// 原子i+n操作 返回旧值
// 默认cas实现
// CASUpdater已经重写此方法 LockedUpdater未重写
public long getAndAdd(T obj, long delta) {
    long prev, next;
    do {
        prev = get(obj);
        next = prev + delta;
    } while (!compareAndSet(obj, prev, next));
    return prev;
}
```

### long incrementAndGet(T obj)
```java
// 原子++i操作 返回自增后的值
// 默认cas实现
// CASUpdater已经重写此方法 LockedUpdater未重写
public long incrementAndGet(T obj) {
    long prev, next;
    do {
        prev = get(obj);
        next = prev + 1;
    } while (!compareAndSet(obj, prev, next));
    return next;
}
```

### long decrementAndGet(T obj)
```java
// 原子--i操作 返回自减后的值
// 默认cas实现
// CASUpdater已经重写此方法 LockedUpdater未重写
public long decrementAndGet(T obj) {
    long prev, next;
    do {
        prev = get(obj);
        next = prev - 1;
    } while (!compareAndSet(obj, prev, next));
    return next;
}
```

### long addAndGet(T obj, long delta)
```java
// 原子i+n操作 返回增加后的值
// 默认cas实现
// CASUpdater已经重写此方法 LockedUpdater未重写
public long addAndGet(T obj, long delta) {
    long prev, next;
    do {
        prev = get(obj);
        next = prev + delta;
    } while (!compareAndSet(obj, prev, next));
    return next;
}
```

### long getAndUpdate(T obj, LongUnaryOperator updateFunction)
```java
// cas实现long字段进行一元操作 返回操作前的值
public final long getAndUpdate(T obj, LongUnaryOperator updateFunction) {
    long prev, next;
    do {
        prev = get(obj);
        next = updateFunction.applyAsLong(prev);
    } while (!compareAndSet(obj, prev, next));
    return prev;
}
```

### long updateAndGet(T obj, LongUnaryOperator updateFunction)
```java
// cas实现long字段进行一元操作 返回操作后的值
public final long updateAndGet(T obj, LongUnaryOperator updateFunction) {
    long prev, next;
    do {
        prev = get(obj);
        next = updateFunction.applyAsLong(prev);
    } while (!compareAndSet(obj, prev, next));
    return next;
}
```

### long getAndAccumulate(T obj, long x, LongBinaryOperator accumulatorFunction)
```java
// cas实现long字段进行二元操作 返回操作前的值
public final long getAndAccumulate(T obj, long x,
                                   LongBinaryOperator accumulatorFunction) {
    long prev, next;
    do {
        prev = get(obj);
        next = accumulatorFunction.applyAsLong(prev, x);
    } while (!compareAndSet(obj, prev, next));
    return prev;
}
```

### long accumulateAndGet(T obj, long x, LongBinaryOperator accumulatorFunction)
```java
// cas实现long字段进行二元操作 返回操作后的值
public final long accumulateAndGet(T obj, long x,
                                   LongBinaryOperator accumulatorFunction) {
    long prev, next;
    do {
        prev = get(obj);
        next = accumulatorFunction.applyAsLong(prev, x);
    } while (!compareAndSet(obj, prev, next));
    return next;
}
```