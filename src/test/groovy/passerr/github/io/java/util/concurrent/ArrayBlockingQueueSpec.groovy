package passerr.github.io.java.util.concurrent

import spock.lang.Specification

import java.util.concurrent.ArrayBlockingQueue
/**
 * <p>
 * ArrayBlockingQueue是一个有界的阻塞队列，其内部实现是将对象放到一个数组里。
 * 有界也就意味着，它不能够存储无限多数量的元素。它有一个同一时间能够存储元素数量的上限。
 * 你可以在对其初始化的时候设定这个上限，但之后就无法对这个上限进行修改了
 * (译者注：因为它是基于数组实现的，也就具有数组的特性：一旦初始化，大小就无法修改)。
 * </p>
 * @see java.util.concurrent.ArrayBlockingQueue
 * @author xiehai1
 * @date 2017/09/26 17:50
 * @Copyright ( c ) gome inc Gome Co.,LTD
 */
class ArrayBlockingQueueSpec extends Specification {
    def test() {
        given:
        def queue = new ArrayBlockingQueue(2)
        queue.offer(1)
        queue.offer(2)

        expect:
        true
    }
}

class Queue<T>{
    Node<T> head = null
    Node<T> last = null
    Queue(){
        head = last = new Node<>(null)
    }

    def enqueue(Node<T> node){
        last = last.next = node
    }

    static main(agrs) {
        Queue<String> queue = new Queue<>()
        println queue.head
        queue.enqueue(new Node<String>("Hello"))
        println queue.head
        queue.enqueue(new Node<String>("World"))
        println queue.head
    }
}

class Node<T> {
    Node<T> next
    T item

    Node(T item) {
        this.item = item
    }


    @Override
    String toString() {
        return "Node{" +
            "next=" + next +
            ", item=" + item +
            '}'
    }
}