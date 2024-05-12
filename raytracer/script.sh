#!/bin/bash

function print_usage() {
    printf "Usage: $0 <input-file.txt> <texture-file.bmp> <output-file.bmp>\n"
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
    if ! hexdump -ve '1/1 "%u\n"' "$texture_file" | jq -s --argjson original "$(cat resources/payload.json)" '$original * {texmap: .}' > resources/payload.json; then
        printf "Failed to add texture data to JSON.\n" >&2
        return 1
    fi
}

function send_request() {
    if ! curl -X POST "http://127.0.0.1:8000/raytracer?scols=400&srows=300&wcols=400&wrows=300&coff=0&roff=0&aa=false" --data "@./resources/payload.json" > resources/result.txt; then
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
    if [[ $# -ne 3 ]]; then
        print_usage
        return 1
    fi

    local input_file=$1
    local texture_file=$2
    local output_file=$3

    add_scene_to_json "$input_file" || return 1
    add_texture_to_json "$texture_file" || return 1
    send_request || return 1
    remove_formatting || return 1
    decode_base64 "$output_file" || return 1

    printf "Processing completed successfully.\n"
}

main "$@"

