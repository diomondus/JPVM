@set classpath=%classpath%;%~pd0..\build\libs\*

@set MainClass=jpvm.daemon.Daemon

java -DJpvmClasspath=Z:\Users\mitryl\Dev\jpvm\build\libs\* %MainClass%