#!/usr/bin/env bash

# host=cnv-proj-lb-857563010.us-east-1.elb.amazonaws.com
# host=127.0.0.1
host=54.89.144.216

function print_usage() {
    # TODO: update
    printf "Usage: $0 <input-file.txt> <output-file.bmp> [<texture-file.bmp>]\n"
}

function add_scene_to_json() {
    local input_file=$1
    if ! jq -sR '{scene: .}' "$input_file" > resources/$payload; then
        printf "Failed to add scene data to JSON.\n" >&2
        return 1
    fi
}

function add_texture_to_json() {
    local texture_file=$1
    if [[ -f "$texture_file" ]]; then
        if ! hexdump -ve '1/1 "%u\n"' "$texture_file" | jq -s --argjson original "$(cat resources/$payload)" '$original * {texmap: .}' > resources/$payload; then
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
    if ! curl -X POST "http://$host:8000/raytracer?scols=$scols&srows=$srows&wcols=$wcols&wrows=$wrows&coff=$coff&roff=$roff&aa=false" --data "@./resources/$payload" > resources/result.txt; then
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
    local payload=$(mktemp)
    local result=$(mktemp)

    # Add scene.txt raw content to JSON.
    cat $input_file | jq -sR '{scene: .}' > $payload

    # Add texmap.bmp binary to JSON (optional step, required only for some scenes).
    if [[ -f "$texture_file" ]]; then
        echo "Texture file provided"
        if ! hexdump -ve '1/1 "%u\n"' "$texture_file" | jq -s --argjson original "$(cat $payload)" '$original * {texmap: .}' > $payload; then
            printf "Failed to add texture data to JSON.\n" >&2
            return 1
        fi
    else
        printf "No texture file provided. Skipping texture data addition to JSON.\n"
    fi

    echo "Running: curl -X POST http://$host:8000/raytracer?scols=$scols\&srows=$srows\&wcols=$wcols\&wrows=$wrows\&coff=$coff\&roff=$roff\&aa=false --data @"$payload" > $result"

    # Send the request.
    curl -X POST http://$host:8000/raytracer?scols=$scols\&srows=$srows\&wcols=$wcols\&wrows=$wrows\&coff=$coff\&roff=$roff\&aa=false --data @"$payload" > $result

    # Remove a formatting string (remove everything before the comma).
    sed -i 's/^[^,]*,//' $result

    # Decode from Base64.
    base64 -d $result > $output_file

    printf "Processing completed successfully.\n"
}

main "$@"
