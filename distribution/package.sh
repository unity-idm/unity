#!/bin/bash

if [ $# -lt 1 ]
then
	echo Mandatory argument: release version
	exit
fi

mvn clean
ant -Dbasedir=`pwd` -Dpackage.release=$1 -f src/main/packman/main.xml
