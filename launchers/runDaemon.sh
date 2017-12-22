classpath=$classpath:../build/libs/*

MainClass="jpvm.daemon.Daemon"

java -cp $classpath -DJpvmClasspath=/Users/mitryl/Dev/jpvm/build/libs/* $MainClass

