# Raft Consensus Algorithm implementation for Java

Raft paper : https://raft.github.io/

***Project is in early stages of development. Not ready for any kind of use cases!***


**Goal :** Although there are some cluster communication tools/libraries/systems whether they are implementing Raft or not, yet, 
none of them is consistent, easy to use and provide high performance. This projects aims to create a simple library to provide cluster communication/replication facility/high availability to existing/ongoing projects. I believe high availability and consistency can be done
in a simple and performant manner.

**Dependencies :** There are not third party dependencies. Pure java, no native code yet(aim is not to use any). Requires Java8 at least.

**Design :** Raft defines a strong consistency algorithm. I will try to follow it, so no plan for stale reads/reads from followers.
             We don't have write command - read command difference, all of them are commands, goes to leader and gets answer from leader.
             
**Architecture :**
![Architecture](docs/image/arch.jpg?raw=true "Architecture")


You are either peer or client. Clients are only required when you want to send commands to a remote cluster. There is nothing static in the code so you can make a single node part of multiple clusters. Sharding/distributed clusters can be built this way.

![Sharding](docs/image/shard.jpg?raw=true "Sharding")
