# DoubleAccumulator

DoubleAccumulator和[DoubleAdder](DoubleAdder.md)类似，也基于[Striped64](Striped64.md)实现。
但要比DoubleAdder更加灵活(要传入一个DoubleBinaryOperator二元操作方法)，
DoubleAdder相当于是DoubleAccumulator的一种特例。

## 域
```java
// 二元操作方法
private final DoubleBinaryOperator function;
// double初始值
// Double.doubleToRawLongBits(identity)使用long代表
private final long identity; // use long representation
```

## 构造方法
### DoubleAccumulator(DoubleBinaryOperator accumulatorFunction, double identity)
```java
public DoubleAccumulator(DoubleBinaryOperator accumulatorFunction,
                         double identity) {
    // 设置方法
    this.function = accumulatorFunction;
    // 设置base和初始化值为给定值
    base = this.identity = Double.doubleToRawLongBits(identity);
}
```

## 方法
### void accumulate(double x)
```java
public void accumulate(double x) {
    Cell[] as; long b, v, r; int m; Cell a;
    if ((as = cells) != null || // hash表非空
        // r != b 且 cas修改base不成功
        (r = Double.doubleToRawLongBits
         (function.applyAsDouble
          (Double.longBitsToDouble(b = base), x))) != b  && !casBase(b, r)) {
        boolean uncontended = true; // 设置为非竞争
        if (as == null || (m = as.length - 1) < 0 || // hash表未初始化
            // hash表根据hash值计算的索引位置的Cell为null
            (a = as[getProbe() & m]) == null ||
            !(uncontended =
               // cell计算前后的值一致或在cell做cas失败
               // 并更新竞争状态逻辑计算结果
              (r = Double.doubleToRawLongBits
               (function.applyAsDouble
                (Double.longBitsToDouble(v = a.value), x))) == v 
                || a.cas(v, r)))
            // 调用父类的doubleAccumulate方法
            doubleAccumulate(x, function, uncontended);
    }
}
```

### double get()
```java
// 获得当前实例的double值
// 跟DoubleAdder.sum()类似
// 用于Number类的方法longValue、intValue、doubleValue等
public double get() {
    Cell[] as = cells; Cell a;
    // 获得base值
    double result = Double.longBitsToDouble(base);
    if (as != null) {
        // 计算hash表所有非空单元的double值
        for (int i = 0; i < as.length; ++i) {
            if ((a = as[i]) != null)
                result = function.applyAsDouble
                    (result, Double.longBitsToDouble(a.value));
        }
    }
    return result;
}
```

### void reset()
```java
// 重置当前实例的double值
// 跟DoubleAdder.reset()类似
// 这里重置后的值为identity
public void reset() {
    Cell[] as = cells; Cell a;
    base = identity; // 重置base
    if (as != null) {
        // 重置hash表所有非空单元
        for (int i = 0; i < as.length; ++i) {
            if ((a = as[i]) != null)
                a.value = identity;
        }
    }
}
```

### double getThenReset() 
```java
// 计算并重置当前实例的值
public double getThenReset() {
    Cell[] as = cells; Cell a;
    // 获得并重置base的值
    double result = Double.longBitsToDouble(base);
    base = identity;
    if (as != null) {
        // 计算所有hash表非空单元的值并重置
        for (int i = 0; i < as.length; ++i) {
            if ((a = as[i]) != null) {
                double v = Double.longBitsToDouble(a.value);
                a.value = identity;
                result = function.applyAsDouble(result, v);
            }
        }
    }
    return result;
}
```