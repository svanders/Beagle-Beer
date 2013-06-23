#!/bin/bash

play clean dist
cd dist
unzip beagle-beer-web-1.0-SNAPSHOT.zip
tar -czf beagle-beer-web-1.0-SNAPSHOT.tgz beagle-beer-web-1.0-SNAPSHOT
scp beagle-beer-web-1.0-SNAPSHOT.tgz root@192.168.1.106:
