#!/usr/bin/env bash

function print_usage() {
    printf "Usage: $0 <input-file.txt> <output-file.bmp> <scols> <srows> <wcols> <wrows> <coff> <roff> [<host>] [<texture-file.bmp>]\n"
    printf "\n"
    printf "Arguments:\n"
    printf "  <input-file.txt>      The input text file\n"
    printf "  <output-file.bmp>     The output BMP file\n"
    printf "  <scols>               The number of source columns\n"
    printf "  <srows>               The number of source rows\n"
    printf "  <wcols>               The number of window columns\n"
    printf "  <wrows>               The number of window rows\n"
    printf "  <coff>                The column offset\n"
    printf "  <roff>                The row offset\n"
    printf "  <host>                The host\n"
    printf "  [<texture-file.bmp>]  (Optional) The texture BMP file\n"
}

function main() {
    if [[ $# -lt 9 || $# -gt 10 ]]; then
        print_usage
        return 1
    fi

    port=8000
    # port=9000

    local input_file=$1
    local output_file=$2
    local scols=$3
    local srows=$4
    local wcols=$5
    local wrows=$6
    local coff=$7
    local roff=$8
    local host=$9
    local texture_file=${10}
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

    echo "Running: curl -X POST http://$host:$port/raytracer?scols=$scols\&srows=$srows\&wcols=$wcols\&wrows=$wrows\&coff=$coff\&roff=$roff\&aa=false --data @"$payload" > $result"

    # Send the request.
    curl -X POST http://$host:$port/raytracer?scols=$scols\&srows=$srows\&wcols=$wcols\&wrows=$wrows\&coff=$coff\&roff=$roff\&aa=false --data @"$payload" > $result

    # Remove a formatting string (remove everything before the comma).
    sed -i 's/^[^,]*,//' $result

    # Decode from Base64.
    base64 -d $result > $output_file

    printf "Processing completed successfully.\n"
}

main "$@"
