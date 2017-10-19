---
layout: page
title: BlockingQueue
permalink: java.util.concurrent.BlockingQueue.html
---
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
![BlockingQueue类图](http://www.plantuml.com/plantuml/png/ZLHDRzim3BtxL_3eeMb_GT6XIxDJWRPOXfqLcKcrs5AgZwB1bly-MOvTCMOMNmo9VAGVgKSgxT6kXKHu2HZmJm5mjJAcHg51L5Mv0FoIsynwZNQmBi1YOzwnFDvsfgxxCzROeFQ32t4RpuT2h9otGdguFnxIZuj23KmUy2qW92hlHbKjZ-_AEZ_yjhS-Yin8658ySP6Wle0EJqm9bzL58AqX1spG4t-ya_XOF8sFXaz0po_ZyYHbi1QrnF9fzHjZkmm3RLEmo4FMpc-raLkbDvyFdFgt2taRlDXYoR_q36gZD-1LWoRu6IpZuQTM7aByv0z_2HOfGMM5qajJpcD0vAgCllqABkpGmk8Ev5JS35fG8puMdSfSMxAiXNDmRwtOavgFZWUinJutF2gznOg3FbjbhF9icXzxBL-jqIQuaNANLcWNspQU9M_tlPFAs9zQpqHSVeK-y5MuNqBvI7jkj0jDZ1_Q6dLvufkRa8RqG9f22YaOCQKP9M9YHZHQ9upezCh8_4wHPQjgCdUjWH_iQHfFiV-57bXnfNOLHMedtFwzkJdPVhtZ-hhRVhc1YZ1pq4VgjYQ9w5AHU0vG42uTWgDFRbqLR9LCEcOeJscrFHcZc5pP73CvCk_5iiucuM1CDdvDCFbu89Dnj5oOPEC5TBPGgX1oomibZdy0)