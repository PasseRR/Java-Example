package passerr.github.io.java.util.concurrent

import spock.lang.Specification

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * <p>
 * 阻塞队列有四种行为:
 * 无法操作：入队时队列满了或者出队时队列为空
 * <ol>
 *     <li> 抛异常:无法操作时抛出异常</li>
 *     <li> 返回特定值:无法操作时返回一个特定值，通常为true/false</li>
 *     <li> 阻塞:无法操作时会一直阻塞，直到可以正常操作</li>
 *     <li> 超时:无法操作时会阻塞不超过给定超时时间，返回一个特定值</li>
 * </ol>
 * </p>
 * <pre>
 * ----------------------------------------------------------------------------------------
 * ｜ 　　 ｜  抛异常　  ｜ 返回特定值(true/false) ｜  阻塞　　｜          超时             　　｜
 * ----------------------------------------------------------------------------------------
 * ｜ 入队 ｜  add(o)   ｜      offer(o)       　｜ put(o)  ｜ offer(o, timeout, timeunit)  ｜
 * ----------------------------------------------------------------------------------------
 * ｜ 出队 ｜ remove()  ｜       poll()        　｜ take()  ｜ poll(timeout, timeunit)      ｜
 * ----------------------------------------------------------------------------------------
 * </pre>
 * @see java.util.Queue
 * @see java.util.concurrent.BlockingQueue
 * @see java.util.concurrent.BlockingDeque
 * @see java.util.concurrent.TransferQueue
 * @author xiehai1
 * @date 2017/09/20 10:07
 * @Copyright ( c ) gome inc Gome Co.,LTD
 */
class BlockingQueueSpec extends Specification {
    def "throw exception when add element"() {
        given:
        def queue = new ArrayBlockingQueue(1)

        expect:
        queue.add(1)

        when:
        queue.add(2)
        then:
        thrown(IllegalStateException.class)
    }

    def "throw exception when remove element"() {
        given:
        def queue = new ArrayBlockingQueue(1)

        when:
        queue.remove()
        then:
        thrown(NoSuchElementException.class)
    }

    def "return value when offer element"() {
        given:
        def queue = new ArrayBlockingQueue(1)

        expect:
        queue.offer(1)
        !queue.offer(2)
    }

    def "return value when poll element"() {
        given:
        def queue = new ArrayBlockingQueue(1)

        expect:
        queue.poll() == null
    }

    def "put and take"(){
        given:
        def queue = new ArrayBlockingQueue(1)
        // producer
        new Thread({
            TimeUnit.SECONDS.sleep(2)
            def food = "race"
            queue.put(food)
            println "${Thread.currentThread().getName()} producer ${food}"
        }).start()
        // consumer
        new Thread({
            def food = queue.take()
            println "${Thread.currentThread().getName()} consumer ${food}"
        }).start()

        // main thread wait consumer
        TimeUnit.SECONDS.sleep(3)

        expect:
        true
    }

    def "offer timeout"(){
        given:
        def queue = new ArrayBlockingQueue(1)

        expect:
        // offer success
        queue.offer(1, 2, TimeUnit.SECONDS)
        // offer fail
        !queue.offer(2, 2, TimeUnit.SECONDS)
    }

    def "poll timeout"(){
        given:
        def queue = new ArrayBlockingQueue(1)
        queue.put(1)

        expect:
        queue.poll(2, TimeUnit.SECONDS)
        !queue.poll(2, TimeUnit.SECONDS)
    }
}