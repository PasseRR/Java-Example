---
layout: page
title: Thread
permalink: java.lang.Thread.html
---

![Alt text](http://g.gravizo.com/source/gravizosample?https%3A%2F%2Fbitbucket.org%2FTLmaK0%2Fgravizo-example%2Fraw%2Fmaster%2FREADME.md#
gravizosample
    @startuml
    (*) -> [start()]Runnable
    -> [Cpu scheduler]Running
    -> [run() completes](*)
    
    Running -> [Cpu scheduler]Runnable
    
    Running -left-> [blocking event]Blocked
    -->Runnable
    
    Running --> [synchronized]Blocked in object's lock pool
    
    Running --> [wait() must have lock]Blocked in object's wait pool
    --> [notify()/interrupt()]Blocked in object's lock pool
    
    "Blocked in object's lock pool" -up-> [acquires lock]Runnable
    @enduml
gravizosample
)