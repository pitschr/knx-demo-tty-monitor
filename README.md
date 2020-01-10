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
* KNX File Auditing (re-used: [knx-core-plugin-audit](/knx-link/tree/master/knx-core-plugins/audit))
  * Auditing the KNX packets in JSON structure that can be used for further analysis 
* KNX Statistic (re-used: [knx-core-plugin-statistic](/knx-link/tree/master/knx-core-plugins/statistic))
  * Textual format of statistic to see how many KNX packets and which type of KNX packets were sent/received 

#### How to use it?

**Class:** [`li.pitschmann.knx.examples.tty.Main`](/knx-demo-tty-monitor/src/main/java/li/pitschmann/knx/examples/tty/Main.java)

**Arguments:**
* `-t <number>`, `--time <number>` the time in seconds how long the monitoring should run (default: `3600` = 1 hour)
* `-p <file path>`, `--knxproj <file path>` KNX project file to show the values in ETS configured data point types (default: _latest *.knxproj in the folder_)
* `-g`, `--generate-fake-data` if the fake data should be generated (default: `false`)

**Examples:**
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
