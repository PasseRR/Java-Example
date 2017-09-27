# BlockingQueue
> BlockingQueue提供了线程安全的队列访问方式：当阻塞队列进行插入数据时，如果队列已满，线程将会阻塞等待直到队列非满；从阻塞队列取数据时，如果队列已空，线程将会阻塞等待直到队列非空。并发包下很多高级同步类的实现都是基于BlockingQueue实现的。  

阻塞队列有四种行为  
`无法操作：入队时队列满了或者出队时队列为空`

|行为|描述|enqueue|dequeue|
|:---:|:---|:---:|:---:|
|抛异常|无法操作时抛出异常|add(o)|remove()|
|返回特定值|无法操作时返回一个特定值，通常为true/false|offer(o)|poll()|
|阻塞|无法操作时会一直阻塞，直到可以正常操作|put(o)|take()|
|超时|无法操作时会阻塞不超过给定超时时间，返回一个特定值|offer(o, timeout, timeunit)|poll(timeout, timeunit)||

## BlockingQueue由来及实现
`看不清楚新标签中打开大图查看`
```uml
@startuml
interface Queue{
  +boolean add(E e)
  +boolean offer(E e)
  +E remove()
  +E poll()
  +E element()
  +E peek()
}
abstract AbstractQueue

interface Deque {
  +void addFirst(E e)
  +void addLast(E e)
  +boolean offerFirst(E e)
  +boolean offerLast(E e)
  +E removeFirst()
  +E removeLast()
  +E pollFirst()
  +E pollLast()
  +E getFirst()
  +E getLast()
  +E peekFirst()
  +E peekLast()
  +boolean removeFirstOccurrence(Object o)
  +boolean removeLastOccurrence(Object o)
}
interface BlockingQueue{
  +void put(E e)
  +E take()
  +boolean offer(E e, long timeout, TimeUnit unit)
  +E poll(long timeout, TimeUnit unit)
  +int drainTo(Collection<? super E> c)
  +int drainTo(Collection<? super E> c, int maxElements)
}
class ArrayBlockingQueue
class DelayQueue
class LinkedBlockingQueue
class PriorityBlockingQueue
class SynchronousQueue

interface TransferQueue{
  +boolean tryTransfer(E e)
  +void transfer(E e)
  +boolean tryTransfer(E e, long timeout, TimeUnit unit)
  +boolean hasWaitingConsumer()
  +int getWaitingConsumerCount()
}
class LinkedTransferQueue

interface BlockingDeque{
  +void putFirst(E e)
  +void putLast(E e)
  +boolean offerFirst(E e, long timeout, TimeUnit unit)
  +boolean offerLast(E e, long timeout, TimeUnit unit)
  +E takeFirst()
  +E takeLast()
  +E pollFirst(long timeout, TimeUnit unit)
  +E pollLast(long timeout, TimeUnit unit)
}
class LinkedBlockingDeque

Queue <|.. AbstractQueue
Queue <|-- Deque
Queue <|-- BlockingQueue
Queue <|-- TransferQueue
Deque <|-- BlockingDeque
BlockingQueue <|-- BlockingDeque

AbstractQueue <|-- LinkedBlockingDeque
BlockingDeque <|.. LinkedBlockingDeque

AbstractQueue <|-- ArrayBlockingQueue
BlockingQueue <|.. ArrayBlockingQueue
AbstractQueue <|-- DelayQueue
BlockingQueue <|.. DelayQueue
AbstractQueue <|-- LinkedBlockingQueue
BlockingQueue <|.. LinkedBlockingQueue
AbstractQueue <|-- PriorityBlockingQueue
BlockingQueue <|.. PriorityBlockingQueue
AbstractQueue <|-- SynchronousQueue
BlockingQueue <|.. SynchronousQueue

AbstractQueue <|-- LinkedTransferQueue
TransferQueue <|.. LinkedTransferQueue
@enduml
```