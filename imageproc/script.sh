#!/usr/bin/env bash

# host=cnv-proj-lb-857563010.us-east-1.elb.amazonaws.com
# host=127.0.0.1
host=54.89.144.216

function print_usage() {
    printf "Usage: $0 <input-file.jpg> <output-file.jpg> <request-type>\n"
    printf "  <request-type> - blurimage or enhanceimage.\n"
}

function encode_base64() {
    local input_file=$1
    local temp_file=$(mktemp)
    mkdir -p resources
    if ! base64 "$input_file" > "$temp_file"; then
        printf "Failed to encode the file in Base64.\n" >&2
        return 1
    fi
}

function append_formatting_string() {
    local temp_file=$(mktemp)
    if ! echo -e "data:image/jpg;base64,$(cat "$temp_file")" > "$temp_file"; then
        printf "Failed to append formatting string.\n" >&2
        return 1
    fi
}

function send_request() {
    # local temp_file="resources/temp.txt"
    local temp_file=$(mktemp)
    # local result_file="resources/result.txt"
    local result_file=$(mktemp)
    local endpoint_path=$1
    if ! curl -X POST "http://$host:8000/$endpoint_path" --data @"$temp_file" > "$result_file"; then
        printf "Failed to send request to the server.\n" >&2
        return 1
    fi
}

function remove_formatting() {
    # local result_file="resources/result.txt"
    local result_file=$(mktemp)
    if ! sed -i 's/^[^,]*,//' "$result_file"; then
        printf "Failed to remove formatting from result.\n" >&2
        return 1
    fi
}

function decode_base64() {
    # local result_file="resources/result.txt"
    local result_file=$(mktemp)
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
    local tmp_file=$(mktemp)
    local tmp_result=$(mktemp)

    # Encode in Base64.
    base64 $input_file > $tmp_file

    # Append a formatting string.
    echo -e "data:image/jpg;base64,$(cat $tmp_file)" > $tmp_file

    # Send the request.
    curl -X POST http://$host:8000/$endpoint_path --data @"$tmp_file" > $tmp_result

    # Remove a formatting string (remove everything before the comma).
    sed -i 's/^[^,]*,//' $tmp_result

    # Decode from Base64.
    base64 -d $tmp_result > $output_file

    printf "Processing completed successfully.\n"
}

main "$@"

