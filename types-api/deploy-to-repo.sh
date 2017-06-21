#!/bin/bash

mvn clean deploy
cd ..
mvn -N deploy -DaltDeploymentRepository=unicore.eu::default::dav:https://unicore-dev.zam.kfa-juelich.de/maven


