#!/bin/bash

duration=${SLEEP:-600}
sleep ${duration}

bash /fix_hosts.sh > /tmp/fixhosts 2>&1 &

python /perf.py

tail -f /dev/null
