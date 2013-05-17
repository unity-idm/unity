#
#Installation Directory
#
dir=`dirname $0`
if [ "$dir" != "." ]
then
  INST=`dirname $dir`
else
  pwd | grep -e 'bin$' > /dev/null
  if [ $? = 0 ]
  then
    # we are in the bin directory
    INST=".."
  else
    # we are NOT in the bin directory
    INST=`dirname $dir`
  fi
fi

INST=${INST:-.}

#
#Alternatively specify the installation dir here
#
#INST=


#
#Java command 
#
JAVA=java


#
#Memory for the VM
#
MEM=-Xmx256m

#
#Options
#
#log config file
OPTS=$OPTS" -Dlog4j.configuration=conf/log4j.properties"

cd $INST

#
#put all jars in lib/ on the classpath
#
CP=.$(find "lib" -name '*.jar' -exec printf ":{}" \;)
#echo Reading code from $CP