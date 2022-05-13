#!/bin/bash

## This script will launch the main components for data-api (config, eureka, key, filedatabase)
## Please run the following components in debug mode: res, data-edge, htsget


#Save current dir for future usage
DIR=$(pwd)
RUNDIR=$DIR/local_run
LOG_DIR=$RUNDIR/logs


VERSION=1.2.1-SNAPSHOT
mkdir -p $LOG_DIR

echo "Building project with maven in background..."
mvn clean install > /dev/null
echo "Done compiling."


#-------------------------------
echo "Launching config server..."

cd ega-data-api-netflix/ega-data-api-config
mvn package spring-boot:repackage > /dev/null

cd $DIR
nohup java -jar ega-data-api-netflix/ega-data-api-config/target/ega-data-api-config-$VERSION.jar > $LOG_DIR/config.logs  2>&1 &
echo "config: $!" > $RUNDIR/process_ids.txt


#-------------------------------
echo "Launching eureka server..."

cd ega-data-api-netflix/ega-data-api-eureka
mvn package spring-boot:repackage > /dev/null

cd $DIR
nohup java -jar ega-data-api-netflix/ega-data-api-eureka/target/ega-data-api-eureka-$VERSION.jar > $LOG_DIR/eureka.logs  2>&1 &
echo "eureka: $!" >> $RUNDIR/process_ids.txt


#-------------------------------
echo "Launching key server..."

cd ega-data-api-key
mvn package spring-boot:repackage > /dev/null

cd $DIR
nohup java -jar ega-data-api-key/target/ega-data-api-key-$VERSION.jar > $LOG_DIR/key.logs  2>&1 &
echo "key: $!" >> $RUNDIR/process_ids.txt


#-------------------------------
echo "Launching file-database server..."

cd ega-data-api-filedatabase
mvn package spring-boot:repackage > /dev/null

cd $DIR
nohup java -jar ega-data-api-filedatabase/target/ega-data-api-filedatabase-$VERSION.jar > $LOG_DIR/file.logs  2>&1 &
echo "filedatabase: $!" >> $RUNDIR/process_ids.txt


#-------------------------------
echo "Launching res server..."

cd ega-data-api-res
mvn package spring-boot:repackage > /dev/null

cd $DIR
nohup java -jar ega-data-api-res/target/ega-data-api-res-$VERSION.jar > $LOG_DIR/res.logs  2>&1 &
echo "res: $!" >> $RUNDIR/process_ids.txt


#-------------------------------
echo "Launching dataedge server..."

cd ega-data-api-dataedge
mvn package spring-boot:repackage > /dev/null

cd $DIR
nohup java -jar ega-data-api-dataedge/target/ega-data-api-dataedge-$VERSION.jar > $LOG_DIR/dataedge.logs  2>&1 &
echo "dataedge: $!" >> $RUNDIR/process_ids.txt


#-------------------------------
echo "Launching htsget server..."

cd ega-data-api-htsget
mvn package spring-boot:repackage > /dev/null

cd $DIR
nohup java -jar ega-data-api-htsget/target/ega-data-api-htsget-$VERSION.jar > $LOG_DIR/htsget.logs  2>&1 &
echo "htsget: $!" >> $RUNDIR/process_ids.txt


#-------------------------------
echo ""
echo "All processes started (Name: PID)"
cat $RUNDIR/process_ids.txt

echo ""
echo "Logs: $LOG_DIR"
ls -ltrh $LOG_DIR
echo ""
read -n 1 -r -s -p $'Press enter to exit...'

#-------------------------------
echo "Killing all processes:"

PIDS="$(cat $RUNDIR/process_ids.txt | awk -F' ' '{print $2}' | paste -s -d' ')"
KILL_COMMAND="kill -9 $PIDS"
echo "    -> $KILL_COMMAND"
eval $KILL_COMMAND

