#!/usr/bin/env bash

# host=cnv-proj-lb-857563010.us-east-1.elb.amazonaws.com
host=127.0.0.1

function print_usage() {
    printf "Usage: $0 <input-file.txt> <output-file.bmp> [<texture-file.bmp>]\n"
}

function add_scene_to_json() {
    local input_file=$1
    if ! jq -sR '{scene: .}' "$input_file" > resources/payload.json; then
        printf "Failed to add scene data to JSON.\n" >&2
        return 1
    fi
}

function add_texture_to_json() {
    local texture_file=$1
    if [[ -f "$texture_file" ]]; then
        if ! hexdump -ve '1/1 "%u\n"' "$texture_file" | jq -s --argjson original "$(cat resources/payload.json)" '$original * {texmap: .}' > resources/payload.json; then
            printf "Failed to add texture data to JSON.\n" >&2
            return 1
        fi
    else
        printf "No texture file provided. Skipping texture data addition to JSON.\n"
    fi
}

function send_request() {
    local scols=$1
    local srows=$2
    local wcols=$3
    local wrows=$4
    local coff=$5
    local roff=$6

    echo "Requesting to: http://$host:8000/raytracer?scols=$scols&srows=$srows&wcols=$wcols&wrows=$wrows&coff=$coff&roff=$roff&aa=false"
    if ! curl -X POST "http://$host:8000/raytracer?scols=$scols&srows=$srows&wcols=$wcols&wrows=$wrows&coff=$coff&roff=$roff&aa=false" --data "@./resources/payload.json" > resources/result.txt; then
        printf "Failed to send request to the server.\n" >&2
        return 1
    fi
}

function remove_formatting() {
    if ! sed -i 's/^[^,]*,//' resources/result.txt; then
        printf "Failed to remove formatting from result.\n" >&2
        return 1
    fi
}

function decode_base64() {
    local output_file=$1
    if ! base64 -d resources/result.txt > "$output_file"; then
        printf "Failed to decode base64 content.\n" >&2
        return 1
    fi
}

function main() {
    if [[ $# -lt 8 || $# -gt 9 ]]; then
        print_usage
        return 1
    fi

    local input_file=$1
    local output_file=$2
    local scols=$3
    local srows=$4
    local wcols=$5
    local wrows=$6
    local coff=$7
    local roff=$8
    local texture_file=$9

    add_scene_to_json "$input_file" || return 1
    add_texture_to_json "$texture_file" || return 1
    send_request $scols $srows $wcols $wrows $coff $roff || return 1
    remove_formatting || return 1
    decode_base64 "$output_file" || return 1

    printf "Processing completed successfully.\n"
}

main "$@"
