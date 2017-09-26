package passerr.github.io.java.util.concurrent
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
 * @author xiehai1
 * @date 2017/09/20 10:07
 * @Copyright ( c ) gome inc Gome Co.,LTD
 */

