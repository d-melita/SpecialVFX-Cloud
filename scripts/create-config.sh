#!/bin/bash

function replace_config_values() {
    local input_file=$1
    local output_file=$2
    local env_vars=("AWS_DEFAULT_REGION" "AWS_ACCOUNT_ID" "AWS_ACCESS_KEY_ID" "AWS_SECRET_ACCESS_KEY"
                    "AWS_EC2_SSH_KEYPAIR_PATH" "AWS_SECURITY_GROUP" "AWS_KEYPAIR_NAME" "PATH")

    cp "$input_file" "$output_file"  # Copy the example file to a new config file

    for var in "${env_vars[@]}"; do
        local env_val="${!var}"
        if [[ -n $env_val ]]; then
            # Use sed to safely replace values in the new config file, ensuring we escape special characters in replacement string
            sed -i "s|^\(export $var=\).*|\1$env_val|" "$output_file"
        else
            printf "Environment variable '$var' is not set, leaving default in config.\n" >&2
        fi
    done
}

function main() {
    local example_file="config.sh.example"
    local config_file="config.sh"

    if [[ ! -f "$example_file" ]]; then
        printf "Example config file does not exist.\n" >&2
        return 1
    fi

    replace_config_values "$example_file" "$config_file"
    printf "Configuration updated successfully and saved as '$config_file'.\n"
}

main "$@"

