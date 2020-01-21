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

| Name&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; | Default&nbsp;Value&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; | Description |
| ---- | ------------- | ----------- |
| `-t <number>` <br> `--time <number>` | `3600` (=1 hour) | The time in seconds how the KNX monitor should run. |
| `-p <path>` <br> `--project <path>` | _latest *.knxproj in the folder_ | The path to _*.knxproj file_ that is created by the ETS to encode the values in correct data point types. In case there are more than one _*.knxproj_ in the working directory, the most recent _*.knxproj file_ (based on timestamp) will be taken. <br><br> If no _*.knxproj file_ is provided then all values are displayed in raw bytes |
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
java -jar knx-demo-tty-monitor.jar --time 300

# Tunneling (auto-discovery) with specific *.knxproj file located at ~/my-house.knxproj
java -jar knx-demo-tty-monitor.jar -p ~/my-house.knxproj
java -jar knx-demo-tty-monitor.jar --project ~/my-house.knxproj

# Tunneling (auto-discovery) with generating fake data
java -jar knx-demo-tty-monitor.jar -g
java -jar knx-demo-tty-monitor.jar --generate-fake-data
```

## Demo

*Talk is cheap, demo it!* I launched the KNX monitor using auto-discovery with a `*.knxproj` 
in the same working directory:
```
java -jar knx-demo-tty-monitor.jar
```
![demo-knx-monitor-knxproj](./assets/demo-knx-monitor-knxproj.gif)

In case the data point type of group address is not known in the KNX project file created 
by ETS the values are displayed in raw data format, see highlighted rows.

![demo-knx-monitor-not-known-ga](./assets/demo-knx-monitor-not-known-ga.png)

## Now I want to try out it by myself

You're lucky! I have created a ready-to-use docker image for you. 

1. Just pull & run the docker image using:
    * The `--network host` is required because UDP communication doesn't work with docker's default network setting
    ```
    docker run --rm -it --network host --name knx-demo-tty-monitor pitschr/knx-demo-tty-monitor
    ```
1. Then you can launch with one of the command above. Example: 
    ```
    java -jar knx-demo-tty-monitor.jar
    ```
1. To stop the KNX monitor application just press `CTRL` + `C` and if you want to quit 
the docker container just enter `exit` in the terminal.
