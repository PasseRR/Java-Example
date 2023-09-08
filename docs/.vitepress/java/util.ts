function bars() {
    return {
        '/java/util': {
            base: '/java/util',
            items: [{
                'text': 'concurrent',
                base: '/java/util/concurrent',
                collapsed: false,
                items: [{
                    'text': 'atomic',
                    collapsed: true,
                    base: '/java/util/concurrent/atomic',
                    items: [
                        {text: '包介绍', link: '/package'},
                        {text: 'AtomicBoolean', link: '/AtomicBoolean'},
                        {text: 'AtomicInteger', link: '/AtomicInteger'},
                        {text: 'AtomicIntegerArray', link: '/AtomicIntegerArray'},
                        {text: 'AtomicIntegerFieldUpdater', link: '/AtomicIntegerFieldUpdater'},
                        {text: 'AtomicLong', link: '/AtomicLong'},
                        {text: 'AtomicLongArray', link: '/AtomicLongArray'},
                        {text: 'AtomicLongFieldUpdater', link: '/AtomicLongFieldUpdater'},
                        {text: 'AtomicMarkableReference', link: '/AtomicMarkableReference'},
                        {text: 'AtomicReference', link: '/AtomicReference'},
                        {text: 'AtomicReferenceArray', link: '/AtomicReferenceArray'},
                        {text: 'AtomicReferenceFieldUpdater', link: '/AtomicReferenceFieldUpdater'},
                        {text: 'AtomicStampedReference', link: '/AtomicStampedReference'},
                        {text: 'DoubleAccumulator', link: '/DoubleAccumulator'},
                        {text: 'DoubleAdder', link: '/DoubleAdder'},
                        {text: 'LongAccumulator', link: '/LongAccumulator'},
                        {text: 'LongAdder', link: '/LongAdder'},
                        {text: 'Striped64', link: '/Striped64'},
                    ]
                }, {
                    'text': 'locks',
                    collapsed: true,
                    base: '/java/util/concurrent/locks',
                    items: [
                        {text: '包介绍', link: '/package'},
                        {text: 'AbstractOwnableSynchronizer', link: '/AbstractOwnableSynchronizer'},
                        {text: 'AbstractQueuedLongSynchronizer', link: '/AbstractQueuedLongSynchronizer'},
                        {text: 'AbstractQueuedSynchronizer', link: '/AbstractQueuedSynchronizer'},
                        {text: 'Condition', link: '/Condition'},
                        {text: 'LockSupport', link: '/LockSupport'},
                    ]
                },
                    {text: 'BlockingQueue', link: '/BlockingQueue'},
                    {text: 'ArrayBlockingQueue', link: '/ArrayBlockingQueue'},
                    {text: 'DelayQueue', link: '/DelayQueue'},
                    {text: 'LinkedBlockingQueue', link: '/LinkedBlockingQueue'},
                    {text: 'PriorityBlockingQueue', link: '/PriorityBlockingQueue'},
                    {text: 'SynchronousQueue', link: '/SynchronousQueue'},
                ]
            }]
        }
    }
}

function nav() {
    return {
        text: 'util',
        link: '/java/util/index',
        activeMatch: '/java/util/'
    }
}

export default {bars, nav}
