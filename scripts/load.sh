#! /usr/bin/env bash

function parse_blur {
    obj=$1
    in="resources/$(echo $obj | jq -r '.input')"
    out="resources/$(echo $obj | jq -r '.output')"
    echo "Parsing blur (in=$in, out=$out)"

    pushd ../imageproc
    ./script.sh "$in" "$out" blurimage
    popd
}

function parse_enhance {
    obj=$1
    in="resources/$(echo $obj | jq -r '.input')"
    out="resources/$(echo $obj | jq -r '.output')"
    echo "Parsing enhance (in=$in, out=$out)"

    pushd ../imageproc
    ./script.sh "$in" "$out" enhanceimage
    popd
}

function parse_raytracer {
    obj=$1
    in="resources/$(echo $obj | jq -r '.input')"
    out="resources/$(echo $obj | jq -r '.output')"
    scols=$(echo $obj | jq -r '.scols')
    srows=$(echo $obj | jq -r '.srows')
    wcols=$(echo $obj | jq -r '.wcols')
    wrows=$(echo $obj | jq -r '.wrows')
    coff=$(echo $obj | jq -r '.coff')
    roff=$(echo $obj | jq -r '.roff')
    text=$(echo $obj | jq -r '.texture // empty')

    if [ -z "$text" ]; then
        # nop
        true
    else
        text="resources/$text"
    fi

    echo "Parsing raytracer (in=$in, texture=$text, out=$out)"

    pushd ../raytracer
    ./script.sh $in $out $scols $srows $wcols $wrows $coff $roff $text
    popd
}

function parse() {
    obj=$1
    type=$(echo $obj | jq -r '.type')

    case "$type" in
        blur)
            parse_blur $obj
            ;;

        enhance)
            parse_enhance $obj
            ;;

        ray-tracer)
            parse_raytracer $obj
            ;;

        *)
            echo "unknown type of request";
            exit 1
            ;;
    esac
}

function run_client() {
    for request in $(cat $load_config | jq -c '.[]'); do
        reps=$(echo $request | jq -r '.reps // empty')

        if [ -z "$reps" ]; then
            reps=1
        fi

        echo "Running $reps repetitions"
        for i in $(seq 1 $reps); do
            parse $request
        done
    done
}

if [ $# -ne 2 ]; then
    echo "Usage: $0 <load_config> <client_count>"
    exit 1
fi

load_config="$1"
client_count=$2

if [ ! -f "$load_config" ]; then
    echo "Config file not found: $load_config"
    exit 1
fi

echo "Loading configuration from: $load_config"

run_client;
# For each client run 
