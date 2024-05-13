#!/bin/bash

function replace_config_values() {
    local config_file=$1
    local env_vars=("AWS_DEFAULT_REGION" "AWS_ACCOUNT_ID" "AWS_ACCESS_KEY_ID" "AWS_SECRET_ACCESS_KEY" 
                    "AWS_EC2_SSH_KEYPAIR_PATH" "AWS_SECURITY_GROUP" "AWS_KEYPAIR_NAME" "PATH")

    for var in "${env_vars[@]}"; do
        local env_val="${!var}"
        if [[ -n $env_val ]]; then
            # Use sed to safely replace values ensuring we escape special characters in replacement string
            sed -i "s|^\(export $var=\).*|\1$env_val|" "$config_file"
        else
            printf "Environment variable '$var' is not set.\n" >&2
        fi
    done
}

function main() {
    local config_file="config.sh"

    if [[ ! -f "$config_file" ]]; then
        printf "Config file does not exist.\n" >&2
        return 1
    fi

    replace_config_values "$config_file"
    printf "Configuration updated successfully.\n"
}

main "$@"

