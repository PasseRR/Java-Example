# Unsafe
> Unsafe类在jdk源码的多个类中用到，这个类的提供了一些绕开JVM的更底层功能，基于它的实现可以提高效率。
> 但是，它是一把双刃剑：正如它的名字所预示的那样，它是Unsafe的，它所分配的内存需要手动free（不被GC回收）。
> Unsafe类，提供了JNI某些功能的简单替代：确保高效性的同时，使事情变得更简单。
> 更多信息请前往[API](http://www.docjar.com/docs/api/sun/misc/Unsafe.html)查看。

## API
Unsafe API的大部分方法都是native实现，它由105个方法组成，主要包括以下几类  
### Info
获得某些低级别的内存信息  
#### int addressSize()
返回**内存地址长度4或者8字节** 取决于jvm内存多少 如果堆大于32G则为8字节 小一点的堆则为4字节   

### Objects
提供Object和它的Field操纵方法  
#### Object allocateInstance(Class cls)
绕过构造方法直接初始化实例对象且不会初始化属性 对象类型属性为null 基本类型为默认值 

### Class
提供Class和它的静态Field操纵方法  

### Arrays
数组操纵方法
#### int arrayBaseOffset(Class arrayClass)
返回**给定arrayClass(数组类型Class)第一个元素相对于数组起始地址的偏移量**
#### int arrayIndexScale(Class arrayClass)
返回**给定arrayClass(数组类型Class)元素大小(占用多少字节)**  
将arrayBaseOffset与arrayIndexScale配合使用，可以定位数组中每个元素在内存中的位置。

### Synchronization
提供低级别同步原语（如基于CPU的CAS（Compare-And-Swap）原语）  
#### boolean compareAndSwapInt(Object o, long offset, int expected, int x)
若当前持有值为expected 原子地更新对象o偏移量offset为x
#### boolean compareAndSwapLong(Object o, long offset, long expected, long x)
若当前持有值为expected 原子地更新对象o偏移量offset为x
#### boolean compareAndSwapObject(Object o, long offset, Object expected, Object x)
若当前持有值为expected 原子地更新对象o偏移量offset为x

### Memory
直接内存访问方法（绕过JVM堆直接操纵本地内存）  
#### long allocateMemory(long bytes)
分配指定字节的内存块 返回**分配的内存地址**  
#### void copyMemory(long srcAddress, long destAddress, long bytes)
内存拷贝 从src拷贝到dest 长度为bytes字节
#### void copyMemory(Object srcBase, long srcOffset, Object destBase, long destOffset, long bytes)
内存拷贝 从srcBase偏移量srcOffset拷贝至destBase偏移量destOffset 长度为bytes字节

