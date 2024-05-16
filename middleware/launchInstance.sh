#!/usr/bin/env bash

cd ../webserver && ./run $1 &> $(mktemp)
