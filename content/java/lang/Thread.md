---
layout: page
title: Thread
permalink: java.lang.Thread.html
---

<img src='https://g.gravizo.com/svg?
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
'/>