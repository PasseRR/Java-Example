---
layout: page
title: Condition
permalink: java.util.concurrent.locks.Condition.html
---

> 任何一个Java对象，都拥有一组监视器方法，主要包括wait()、notify()、notifyAll()方法，
> 这些方法与synchronized关键字配合使用可以实现等待/通知机制。
> Condition接口也提供类似的Object的监视器的方法，对应包括await()、signal()、signalAll()方法，
> 这些方法与Lock锁配合使用也可以实现等待/通知机制。

- Condition可以支持多个等待队列，一个Lock实例可以绑定多个Condition  
- Condition支持响应中断  
- Condition支持当前线程释放锁并进入等待状态到将来的某个时间(awaitUntil方法)，也就是支持定时功能  

## 接口方法

```java
public interface Condition {
    // 当前线程阻塞直到被唤醒或线程被中断(Thread.interrupt)
    // 对应Object.wait()
    void await() throws InterruptedException;
    // 当前线程阻塞直到被唤醒
    // Object没有此实现
    void awaitUninterruptibly();
    // 当前线程阻塞直到被唤醒或线程被中断或给定的阻塞时间耗尽
    // 返回剩余阻塞时间
    // 对应Object.wait(long timeout)
    long awaitNanos(long nanosTimeout) throws InterruptedException;
    // 当前线程阻塞直到被唤醒或线程被中断或给定的阻塞时间耗尽
    // 若给定的阻塞时间耗尽返回true 否则返回false
    boolean await(long time, TimeUnit unit) throws InterruptedException;
    // 当前线程阻塞直到被唤醒或线程被中断或达到给定日期
    // Object没有对应实现
    boolean awaitUntil(Date deadline) throws InterruptedException;
    // 唤醒一个阻塞线程
    // 对应Object.notify()
    void signal();
    // 唤醒所有阻塞线程
    // 对应Object.notifyAll()
    void signalAll();
}
```