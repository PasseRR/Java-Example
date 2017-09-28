package passerr.github.io.java.util.concurrent

import spock.lang.Specification


/**
 * @author xiehai1
 * @date 2017/09/28 14:49
 * @Copyright ( c ) gome inc Gome Co.,LTD
 */
class PriorityBlockingQueueSpec extends Specification {
    def half() {
        given:
        def half = (n >> 1) - 1
        println n >> 1
        expect:
        half == result

        where:
        n  || result
        16 || 7
        9  || 3
    }
}