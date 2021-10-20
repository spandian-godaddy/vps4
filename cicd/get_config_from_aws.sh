#Usage: ./getc_config_from_aws.sh [AWS Access Key ID] [AWS Secret Access Key] [ARN of AWS Role to assume] [File to fetch] [environment]
set +x
pushd .

CONFIG_FILENAME=$4
PROJECT=${6:-core}

echo 'getting '$CONFIG_FILENAME' for '$5' environment from '$PROJECT' project'

if [ $PROJECT == "core" ]
then
  CONFIG_DIR='core/src/main/resources/com/godaddy/vps4/config/'$5
  SECRET_NAME='/'$5'/'$4
elif [ $PROJECT == "scheduler" ]
then
  CONFIG_DIR='vps4-scheduler/src/main/resources/com/godaddy/vps4/scheduler/config/'$5
  SECRET_NAME='/'$6'/'$5'/'$4
else
  echo "Invalid project "$PROJECT 
  exit 1
fi

cd ../$CONFIG_DIR
rm -f $CONFIG_FILENAME    
if [ -f $CONFIG_FILENAME ]; then
    echo "Original File Not Removed!!!"
fi

unset AWS_ACCESS_KEY_ID
unset AWS_SECRET_ACCESS_KEY
unset AWS_SESSION_TOKEN

export AWS_ACCESS_KEY_ID=$1
export AWS_SECRET_ACCESS_KEY=$2

echo "aws sts assume-role..."
ASSUMED_ROLE=$(aws sts assume-role --role-arn $3 --role-session-name accesscredrole 2>/dev/null) || { echo "Error assuming role"; exit 1; }

AWS_ACCESS_KEY_ID=$(echo ${ASSUMED_ROLE} | jq --raw-output ".Credentials[\"AccessKeyId\"]")
AWS_SECRET_ACCESS_KEY=$(echo ${ASSUMED_ROLE} | jq --raw-output ".Credentials[\"SecretAccessKey\"]")
AWS_SESSION_TOKEN=$(echo ${ASSUMED_ROLE} | jq --raw-output ".Credentials[\"SessionToken\"]")

export AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
export AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}
export AWS_SESSION_TOKEN=${AWS_SESSION_TOKEN}

echo "aws secretsmanager get-secret-value..."
if [ $CONFIG_FILENAME == "password_encryption.key" ]; then
  SECRET=$(aws secretsmanager get-secret-value --secret-id $SECRET_NAME --query SecretString --output text)
  printf "%s" $SECRET > $CONFIG_FILENAME
else
  aws secretsmanager get-secret-value --secret-id $SECRET_NAME --query SecretString --output text > $CONFIG_FILENAME
fi

popd
set -x
