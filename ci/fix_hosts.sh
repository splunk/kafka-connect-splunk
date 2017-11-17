#!/bin/bash

function fix_hosts() {
    if [ -f /etc/hosts2 ] && [ ! -f /fixed_host ]; then
        cat /etc/hosts2 >> /etc/hosts
        touch /fixed_host
    fi
}

while :
do
    fix_hosts
    sleep 1
done
