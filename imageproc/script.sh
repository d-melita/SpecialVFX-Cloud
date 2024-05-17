#!/usr/bin/env bash

function print_usage() {
    printf "Usage: $0 <input-file.jpg> <output-file.jpg> <request-type> <hostname>\n"
    printf "  <request-type> - blurimage or enhanceimage.\n"
}

function main() {
    if [[ $# -ne 4 ]]; then
        print_usage
        return 1
    fi

    local input_file=$1
    local output_file=$2
    local endpoint_path=$3
    local host=$4
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

