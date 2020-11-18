#!/bin/sh

set -e

V1_7_R4_CB="https://cdn.getbukkit.org/craftbukkit/craftbukkit-1.7.10-R0.1-20140808.005431-8.jar"
V1_8_R3_S="https://cdn.getbukkit.org/spigot/spigot-1.8.8-R0.1-SNAPSHOT-latest.jar"
V1_9_R2_S="https://cdn.getbukkit.org/spigot/spigot-1.9.4-R0.1-SNAPSHOT-latest.jar"
V1_10_R1_S="https://cdn.getbukkit.org/spigot/spigot-1.10-R0.1-SNAPSHOT-latest.jar"
V1_11_R1_S="https://cdn.getbukkit.org/spigot/spigot-1.11.jar"
V1_12_R1_S="https://cdn.getbukkit.org/spigot/spigot-1.12.jar"
V1_13_R2_S="https://cdn.getbukkit.org/spigot/spigot-1.13.2.jar"
V1_14_R1_S="https://cdn.getbukkit.org/spigot/spigot-1.14.4.jar"
V1_15_R1_S="https://cdn.getbukkit.org/spigot/spigot-1.15.2.jar"
V1_16_R1_S="https://cdn.getbukkit.org/spigot/spigot-1.16.1.jar"
V1_16_R2_S="https://cdn.getbukkit.org/spigot/spigot-1.16.3.jar"
V1_16_R3_S="https://cdn.getbukkit.org/spigot/spigot-1.16.4.jar"

mkdir -p ./dl

wget $V1_7_R4_CB -O "./dl/1_7_R4_CB.jar"
wget $V1_8_R3_S -O "./dl/1_8_R3_S.jar"
wget $V1_9_R2_S -O "./dl/1_9_R2_S.jar"
wget $V1_10_R1_S -O "./dl/1_10_R1_S.jar"
wget $V1_11_R1_S -O "./dl/1_11_R1_S.jar"
wget $V1_12_R1_S -O "./dl/1_12_R1_S.jar"
wget $V1_13_R2_S -O "./dl/1_13_R2_S.jar"
wget $V1_14_R1_S -O "./dl/1_14_R1_S.jar"
wget $V1_15_R1_S -O "./dl/1_15_R1_S.jar"
wget $V1_16_R1_S -O "./dl/1_16_R1_S.jar"
wget $V1_16_R2_S -O "./dl/1_16_R2_S.jar"
wget $V1_16_R3_S -O "./dl/1_16_R3_S.jar"

mvn install:install-file -D"file=./dl/1_7_R4_CB.jar" -D"version=1.7.10-R0.1-SNAPSHOT" -D"groupId=org.bukkit" -D"artifactId=craftbukkit" -D"packaging=jar"

mvn install:install-file -D"file=./dl/1_8_R3_S.jar" -D"version=1.8.8-R0.1-SNAPSHOT" -D"groupId=org.spigotmc" -D"artifactId=spigot" -D"packaging=jar"
mvn install:install-file -D"file=./dl/1_9_R2_S.jar" -D"version=1.9.4-R0.1-SNAPSHOT" -D"groupId=org.spigotmc" -D"artifactId=spigot" -D"packaging=jar"
mvn install:install-file -D"file=./dl/1_10_R1_S.jar" -D"version=1.10-R0.1-SNAPSHOT" -D"groupId=org.spigotmc" -D"artifactId=spigot" -D"packaging=jar"
mvn install:install-file -D"file=./dl/1_11_R1_S.jar" -D"version=1.11-R0.1-SNAPSHOT" -D"groupId=org.spigotmc" -D"artifactId=spigot" -D"packaging=jar"
mvn install:install-file -D"file=./dl/1_12_R1_S.jar" -D"version=1.12-R0.1-SNAPSHOT" -D"groupId=org.spigotmc" -D"artifactId=spigot" -D"packaging=jar"
mvn install:install-file -D"file=./dl/1_13_R2_S.jar" -D"version=1.13.2-R0.1-SNAPSHOT" -D"groupId=org.spigotmc" -D"artifactId=spigot" -D"packaging=jar"
mvn install:install-file -D"file=./dl/1_14_R1_S.jar" -D"version=1.14.4-R0.1-SNAPSHOT" -D"groupId=org.spigotmc" -D"artifactId=spigot" -D"packaging=jar"
mvn install:install-file -D"file=./dl/1_15_R1_S.jar" -D"version=1.15.2-R0.1-SNAPSHOT" -D"groupId=org.spigotmc" -D"artifactId=spigot" -D"packaging=jar"
mvn install:install-file -D"file=./dl/1_16_R1_S.jar" -D"version=1.16.1-R0.1-SNAPSHOT" -D"groupId=org.spigotmc" -D"artifactId=spigot" -D"packaging=jar"
mvn install:install-file -D"file=./dl/1_16_R2_S.jar" -D"version=1.16.3-R0.1-SNAPSHOT" -D"groupId=org.spigotmc" -D"artifactId=spigot" -D"packaging=jar"
mvn install:install-file -D"file=./dl/1_16_R3_S.jar" -D"version=1.16.4-R0.1-SNAPSHOT" -D"groupId=org.spigotmc" -D"artifactId=spigot" -D"packaging=jar"

#rm -r './dl'
