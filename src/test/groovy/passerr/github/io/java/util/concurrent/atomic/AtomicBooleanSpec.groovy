package passerr.github.io.java.util.concurrent.atomic

import spock.lang.Specification

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean


/**
 * @see java.util.concurrent.atomic.AtomicBoolean
 * @author xiehai1
 * @date 2017/09/29 18:26
 * @Copyright ( c ) gome inc Gome Co.,LTD
 */
class AtomicBooleanSpec extends Specification {
    def "AtomicBoolean"() {
        given:
        def flag = new AtomicBoolean()
        expect:
        !flag.get()

        when:
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
        // 设置收银台服务中
        flag.set(true)
        // 10个人排队买东西
        10.times { it ->
            def id = it
            executorService.submit({
                // 自旋锁
                while(!flag.compareAndSet(true, false)){
                    TimeUnit.SECONDS.sleep(1)
                }
                // 开始结账
                TimeUnit.SECONDS.sleep(1)
                // 结账完成
                flag.set(true)
                println "customer-${id}结账完成"
            })
        }

        TimeUnit.SECONDS.sleep(15)
        if(!executorService.isShutdown()){
            executorService.shutdown()
        }

        then:
        notThrown(Exception.class)
        executorService.isShutdown()
        flag.get()
    }
}