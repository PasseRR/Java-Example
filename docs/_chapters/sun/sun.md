---
layout: post
title: sun
k: sun
order: 3
last_modified_at: 2022-01-10
---

## sun包
> 是sun的hotspot虚拟机中java.* 和javax.*的实现类。因为包含在rt中，所以我们也可以调用。但是因为不是sun对外公开承诺的接口，所以根据实现的需要随时增减，因此在不同版本的hotspot中可能是不同的，而且在其他的jdk实现中是没有的，调用这些类，可能不会向后兼容，所以一般不推荐使用