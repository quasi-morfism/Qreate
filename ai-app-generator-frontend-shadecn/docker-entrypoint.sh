#!/bin/sh

# Substitute environment variables in nginx config
envsubst '${API_BASE_URL}' < /etc/nginx/conf.d/default.conf.template > /etc/nginx/conf.d/default.conf

# Execute the original command
exec "$@"