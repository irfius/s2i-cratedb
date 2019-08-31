#!/bin/bash -ae

mkdir -pv $CRATE_GC_LOG_DIR $CRATE_HEAP_DUMP_PATH
CRATE_JAVA_OPTS="-XX:+UnlockExperimentalVMOptions -Des.cgroups.hierarchy.override=/ $CRATE_JAVA_OPTS"
exec crate