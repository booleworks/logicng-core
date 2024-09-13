#!/bin/sh

# SPDX-License-Identifier: MIT
# Copyright 2023 BooleWorks GmbH

mkdir -p src/main/generated/java
protoc -I=./src/main/proto --java_out=./src/main/generated/java ./src/main/proto/*.proto

