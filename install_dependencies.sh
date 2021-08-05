#!/bin/sh

set -e

V1_8_R3_S="https://cdn.getbukkit.org/spigot/spigot-1.8.8-R0.1-SNAPSHOT-latest.jar"
V1_9_R2_S="https://cdn.getbukkit.org/spigot/spigot-1.9.4-R0.1-SNAPSHOT-latest.jar"
V1_12_R1_S="https://cdn.getbukkit.org/spigot/spigot-1.12.jar"
V1_16_R1_S="https://cdn.getbukkit.org/spigot/spigot-1.16.1.jar"
V1_16_R2_S="https://cdn.getbukkit.org/spigot/spigot-1.16.5.jar"
V1_17_R1_S = "https://download.getbukkit.org/spigot/spigot-1.17.jar"

mkdir -p ./dl

wget $V1_8_R3_S -O "./dl/1_8_R3_S.jar" --no-verbose
wget $V1_9_R2_S -O "./dl/1_9_R2_S.jar" --no-verbose
wget $V1_12_R1_S -O "./dl/1_12_R1_S.jar" --no-verbose
wget $V1_16_R1_S -O "./dl/1_16_R1_S.jar" --no-verbose
wget $V1_16_R2_S -O "./dl/1_16_R2_S.jar" --no-verbose
wget $V1_17_R1_S -O "./dl/1_17_R1_S.jar" --no-verbose


mvn install:install-file -D"file=./dl/1_8_R3_S.jar" -D"version=1.8.8-R0.1-SNAPSHOT" -D"groupId=org.spigotmc" -D"artifactId=spigot" -D"packaging=jar"
mvn install:install-file -D"file=./dl/1_9_R2_S.jar" -D"version=1.9.4-R0.1-SNAPSHOT" -D"groupId=org.spigotmc" -D"artifactId=spigot" -D"packaging=jar"
mvn install:install-file -D"file=./dl/1_12_R1_S.jar" -D"version=1.12-R0.1-SNAPSHOT" -D"groupId=org.spigotmc" -D"artifactId=spigot" -D"packaging=jar"
mvn install:install-file -D"file=./dl/1_16_R1_S.jar" -D"version=1.16.1-R0.1-SNAPSHOT" -D"groupId=org.spigotmc" -D"artifactId=spigot" -D"packaging=jar"
mvn install:install-file -D"file=./dl/1_16_R2_S.jar" -D"version=1.16.5-R0.1-SNAPSHOT" -D"groupId=org.spigotmc" -D"artifactId=spigot" -D"packaging=jar"
mvn install:install-file -D"file=.\dl\1_17_R1_S.jar" -D"version=1.17-R0.1-SNAPSHOT" -D"groupId=org.spigotmc" -D"artifactId=spigot" -D"packaging=jar"

#rm -r './dl'
