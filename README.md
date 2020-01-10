# Demo Application: KNX TTY Monitor

This is a demo application to demonstrating how to work / implement a project
using [knx-link](/knx-link) and the goal is to visualize the KNX monitoring, 
audit the KNX traffic to a JSON file and write the statistic of all KNX packets 
in a 5 minutes interval.

For demo purposes the KNX Monitor only supports terminal that is ANSI escape code
capable (e.g. Linux, MacOS).

It includes three plugins:
* KNX Monitoring
  * Terminal visualization of KNX traffic (similar to the _Group Monitor_ in ETS from KNX Association)
* KNX File Auditing (re-used: [knx-core-plugin-audit](https://github.com/pitschr/knx-link/tree/master/knx-core-plugins/audit))
  * Auditing the KNX packets in JSON structure that can be used for further analysis 
* KNX Statistic (re-used: [knx-core-plugin-statistic](https://github.com/pitschr/knx-link/tree/master/knx-core-plugins/statistic))
  * Textual format of statistic to see how many KNX packets and which type of KNX packets were sent/received 

## How to use it?

**Main Class:** [`li.pitschmann.knx.examples.tty.Main`](src/main/java/li/pitschmann/knx/examples/tty/Main.java)

#### Arguments

| Name&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; | Default&nbsp;Value&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; | Description |
| ---- | ------------- | ----------- |
| `-t <number>` <br> `--time <number>` | `3600` (=1 hour) | The time in seconds how the KNX monitor should run. |
| `-p <path>` <br> `--knxproj <path>` | _latest *.knxproj in the folder_ | The path to _*.knxproj file_ that is created by the ETS to encode the values in correct data point types. In case there are more than one _*.knxproj_ in the working directory, the most recent _*.knxproj file_ (based on timestamp) will be taken. <br><br> If no _*.knxproj file_ is provided then all values are displayed in raw bytes |
| `-g` <br> `--generate-fake-data` | `false` | Indicates if the fake data should be generated for demo purposes |

#### Examples
```shell script
# Tunneling (auto-discovery)
java -jar knx-demo-tty-monitor.jar

# Tunneling (auto-discovery with NAT)
java -jar knx-demo-tty-monitor.jar --nat

# Tunneling (IP Address)
java -jar knx-demo-tty-monitor.jar --ip 192.168.1.16

# Tunneling (IP Address with NAT)
java -jar knx-demo-tty-monitor.jar --ip 192.168.1.16 --nat

# Routing
java -jar knx-demo-tty-monitor.jar --routing

# Tunneling (auto-discovery) and limited to 5 minutes
java -jar knx-demo-tty-monitor.jar -t 300

# Tunneling (auto-discovery) with specific KNXPROJ file located at ~/my-house.knxproj
java -jar knx-demo-tty-monitor.jar -p ~/my-house.knxproj
```
