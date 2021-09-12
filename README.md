# containment-course-of-action

A prototype implementation of the thesis work on **Containment Strategy Formalism in a Probabillistic Threat Modelling Framework**. This software allows for reasoning about containment strategies in MAL-based threat models. Recommendations for containment strategies can be tailored to a specific threat model, an organizational valuation and a set of indicated incidents. In the future, this work could be of interest for systematic and/or automatic approaches to containment or simply serve as a platform for proactive learning about how to contain various threats that could emerge.

Note, that this implementation currently supports coreLang-based threat models. 

**Incidents supported:**
* [Phishing](https://attack.mitre.org/techniques/T1566/)
* [Network Denial of Service](https://attack.mitre.org/techniques/T1498/)

**Containment actions supported:**
* BlockSpecificConnection: Block a specific connection (by physical unplug, port, IP / MAC-address, ingress / egress filtering, etc..)
* DropInboundTraffic: Drop inbound network packets (by port, IP / MAC-address, ingress filtering, etc..) sent to a receiver (a firewall / application / network).
* DropOutboundTraffic: Drop outbound network packets (by port, IP / MAC-address, egress filtering, etc..) from sender (a firewall / application / network).
* IsolateHost: Isolate a host from the network (by unplugging physical cables, blocking all connections in the firewall or router, etc.. )
* SuspendHost: Suspend (or shutdown, hibernate, put to sleep, etc...) a host
* LockUserAccount: Lock a user account and associated credentials.
* StopService: Stop a service (SIGKILL, etc...)

Video explanation: https://www.youtube.com/watch?v=1o8cBlAErus
