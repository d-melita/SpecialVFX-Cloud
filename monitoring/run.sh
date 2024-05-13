#!/usr/bin/env bash

echo "Running monitoring webserver..."
java -cp webserver/build/libs/webserver.jar pt.ulisboa.tecnico.cnv.webserver.WebServer

