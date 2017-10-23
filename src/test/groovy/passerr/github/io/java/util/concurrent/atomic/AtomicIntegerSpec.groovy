package passerr.github.io.java.util.concurrent.atomic

import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
/**
 * {@link java.util.concurrent.atomic.AtomicLong}类似
 * @see java.util.concurrent.atomic.AtomicInteger
 * @author xiehai1
 * @date 2017/10/23 10:33
 * @Copyright ( c ) gome inc Gome Co.,LTD
 */
class AtomicIntegerSpec extends Specification {
    def "AtomicInteger"(){
        given:
        def total = new AtomicInteger()
        expect:
        total.get() == 0

        when:
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
        def times = 10
        def countDownLatch = new CountDownLatch(times)
        // 从0累加到9 0+1+...+9
        times.times {time ->
            executorService.execute({
                try {
                    total.getAndAdd(time)
                } finally {
                    countDownLatch.countDown()
                }
            })
        }

        countDownLatch.await()

        then:
        total.get() == 45
    }

    def "AtomicInteger accumulateAndGet"(){
        given:
        def total = new AtomicInteger(1)
        expect:
        total.get() == 1

        when:
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
        def times = 5
        def countDownLatch = new CountDownLatch(times)
        // 5的阶乘
        times.times {time ->
            executorService.execute({
                try {
                    total.accumulateAndGet(time + 1, { x, y -> x * y})
                } finally {
                    countDownLatch.countDown()
                }
            })
        }

        countDownLatch.await()

        then:
        total.get() == 120
    }
}