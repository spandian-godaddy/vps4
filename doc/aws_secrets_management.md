# AWS Secrets
First, install the AWS CLI - follow the AWS docs at https://docs.aws.amazon.com/cli/latest/userguide/install-cliv2.html
Also, ensure you have jq installed - `brew install jq` if on a Mac

Before working with the AWS secrets you must authenticate and assume the correct role in AWS. Use these commands:

    export AWS_ACCESS_KEY_ID=<access key id>
    export AWS_SECRET_ACCESS_KEY=$2<aws secret access key>
    ASSUMED_ROLE=$(aws sts assume-role --role-arn <arn of the role to assume> --role-session-name accesscredrole 2>/dev/null) || { echo "Error assuming role"; exit 1; }
    
    AWS_ACCESS_KEY_ID=$(echo ${ASSUMED_ROLE} | jq --raw-output ".Credentials[\"AccessKeyId\"]")
    AWS_SECRET_ACCESS_KEY=$(echo ${ASSUMED_ROLE} | jq --raw-output ".Credentials[\"SecretAccessKey\"]")
    AWS_SESSION_TOKEN=$(echo ${ASSUMED_ROLE} | jq --raw-output ".Credentials[\"SessionToken\"]")
    
    export AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
    export AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}
    export AWS_SESSION_TOKEN=${AWS_SESSION_TOKEN}

`<access key id>` and `<aws secret access key>` can be found in the AWS secrets manager. If you are using the prod account you must first elevate access in the public cloud portal  

## Creating an AWS Secret
Change directory to the directory of the file to upload as a secret. Then run the following command example, substituting the filename, tag data, and secret name:

    aws secretsmanager create-secret --name /stage/config.properties --description "config file for stage environment" --secret-string file://./config.properties --tags '[{"Key":"env","Value":"stage"}]'

- Use the naming convention "/environment/secret_file_name"
- Set the environment as the value of the `env` tag.

Repeat this step for every AWS secret. 

## Changing an AWS Secret
Change directory to the directory of the file to upload as a secret. Then run the following command example, substituting the filename, and secret id:
    
    aws secretsmanager put-secret-value --secret-id /prod_ams3/config.properties --secret-string file://./config.properties
