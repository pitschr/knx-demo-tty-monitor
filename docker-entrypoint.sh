#!/bin/sh

# if a host volume is mounted, try to copy KNXPROJ files 
# to same working directory
if [ -d "/mnt/host" ]; then 
	find /mnt/host -name "*.mp4" -type f -mindepth 1 -maxdepth 1 -exec cp {} /app \; 
fi

#
# Header with options
#
function header() {
	clear
	echo
	echo "############################################################################"
	echo "#                                                                          #"
	echo "# KNX MONITOR ENTRYPOINT                                                   #"
	echo "#                                                                          #"
	echo "############################################################################"
	echo
	echo "1) Use TUNNELING .............. java -jar <file>.jar"
	echo "2) Use TUNNELLING + NAT ....... java -jar <file>.jar --nat"
	echo "3) Use ROUTING ................ java -jar <file>.jar --routing"
	echo "4) Use IP ADDRESS ............. java -jar <file>.jar --ip <address>"
	echo "5) Use IP ADDRESS + NAT........ java -jar <file>.jar --ip <address> --nat"
	echo
	echo "S) Open container shell"
	echo "E) Exit container"
	echo
	echo "############################################################################"
	echo
}

#
# Main Program
#
while true; do
	header
	read -p "Select: " ACTION
	
	case "${ACTION}" in
		1)  java -jar knx-demo-tty-monitor.jar
			break;;
		2)  java -jar knx-demo-tty-monitor.jar --nat
			break;;
		3)  java -jar knx-demo-tty-monitor.jar --routing
			break;;
		4)
			read -p "Enter IP address: " IP_ADDRESS
			java -jar knx-demo-tty-monitor.jar --ip ${IP_ADDRESS}
			break;;
		5)
			read -p "Enter IP address: " IP_ADDRESS
			java -jar knx-demo-tty-monitor.jar --ip ${IP_ADDRESS} --nat
			break;;
		[s]* | [S]*  )
			/bin/sh
			break;;
		[e]* | [E]* ) 
			exit 0;;
		*) ;;
	esac
done
