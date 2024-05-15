#!/usr/bin/env bash

# host=cnv-proj-lb-857563010.us-east-1.elb.amazonaws.com
host=127.0.0.1

function print_usage() {
    printf "Usage: $0 <input-file.jpg> <output-file.jpg> <request-type>\n"
    printf "  <request-type> - blurimage or enhanceimage.\n"
}

function encode_base64() {
    local input_file=$1
    local temp_file="resources/temp.txt"
    mkdir -p resources
    if ! base64 "$input_file" > "$temp_file"; then
        printf "Failed to encode the file in Base64.\n" >&2
        return 1
    fi
}

function append_formatting_string() {
    local temp_file="resources/temp.txt"
    if ! echo -e "data:image/jpg;base64,$(cat "$temp_file")" > "$temp_file"; then
        printf "Failed to append formatting string.\n" >&2
        return 1
    fi
}

function send_request() {
    local temp_file="resources/temp.txt"
    local result_file="resources/result.txt"
    local endpoint_path=$1
    if ! curl -X POST "http://$host:8000/$endpoint_path" --data @"$temp_file" > "$result_file"; then
        printf "Failed to send request to the server.\n" >&2
        return 1
    fi
}

function remove_formatting() {
    local result_file="resources/result.txt"
    if ! sed -i 's/^[^,]*,//' "$result_file"; then
        printf "Failed to remove formatting from result.\n" >&2
        return 1
    fi
}

function decode_base64() {
    local result_file="resources/result.txt"
    local output_file=$1
    if ! base64 -d "$result_file" > "$output_file"; then
        printf "Failed to decode Base64 content.\n" >&2
        return 1
    fi
}

function main() {
    if [[ $# -ne 3 ]]; then
        print_usage
        return 1
    fi

    local input_file=$1
    local output_file=$2
    local endpoint_path=$3

    encode_base64 "$input_file" || return 1
    append_formatting_string || return 1
    send_request "$endpoint_path" || return 1
    remove_formatting || return 1
    decode_base64 "$output_file" || return 1

    printf "Processing completed successfully.\n"
}

main "$@"

