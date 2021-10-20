#!/bin/bash

read -p "Jomax username:" username
read -s -p "Password:" password

curl -s -H "Accept: application/json" -H "Content-Type: application/json" -d '{"username":"'$username'","password":"'$password'","realm":"jomax"}' https://sso.godaddy.com/v1/api/token | jq -r '.data'
