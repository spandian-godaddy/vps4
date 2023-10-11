ACCESS_KEY_ID=$1
SECRET_ACCESS_KEY=$2
ROLE=$3
ENV=$4


echo 'getting all configuration for '$ENV'.'

#Load config from AWS
chmod +x ./get_config_from_aws.sh;
#base config
./get_config_from_aws.sh $ACCESS_KEY_ID $SECRET_ACCESS_KEY $ROLE 'config.properties' 'base';
#prod_iad2 config
./get_config_from_aws.sh $ACCESS_KEY_ID $SECRET_ACCESS_KEY $ROLE 'config.properties' $ENV;
./get_config_from_aws.sh $ACCESS_KEY_ID $SECRET_ACCESS_KEY $ROLE 'hfs.api.crt' $ENV;
./get_config_from_aws.sh $ACCESS_KEY_ID $SECRET_ACCESS_KEY $ROLE 'hfs.api.key' $ENV;
./get_config_from_aws.sh $ACCESS_KEY_ID $SECRET_ACCESS_KEY $ROLE 'messaging.api.crt' $ENV;
./get_config_from_aws.sh $ACCESS_KEY_ID $SECRET_ACCESS_KEY $ROLE 'messaging.api.key' $ENV;
./get_config_from_aws.sh $ACCESS_KEY_ID $SECRET_ACCESS_KEY $ROLE 'password_encryption.key' $ENV;
./get_config_from_aws.sh $ACCESS_KEY_ID $SECRET_ACCESS_KEY $ROLE 'vps4_end_web_consumerclient.crt' $ENV;
./get_config_from_aws.sh $ACCESS_KEY_ID $SECRET_ACCESS_KEY $ROLE 'vps4_end_web_consumerclient.key' $ENV;
./get_config_from_aws.sh $ACCESS_KEY_ID $SECRET_ACCESS_KEY $ROLE 'vps4_end_web_schedulerclient.crt' $ENV;
./get_config_from_aws.sh $ACCESS_KEY_ID $SECRET_ACCESS_KEY $ROLE 'vps4_end_web_schedulerclient.key' $ENV;
#base scheduler config
./get_config_from_aws.sh $ACCESS_KEY_ID $SECRET_ACCESS_KEY $ROLE 'config.properties' 'base' 'scheduler';
#dev scheduler config
./get_config_from_aws.sh $ACCESS_KEY_ID $SECRET_ACCESS_KEY $ROLE 'config.properties' $ENV 'scheduler';
./get_config_from_aws.sh $ACCESS_KEY_ID $SECRET_ACCESS_KEY $ROLE 'vps4_end_web_schedulerclient.crt' $ENV 'scheduler';
./get_config_from_aws.sh $ACCESS_KEY_ID $SECRET_ACCESS_KEY $ROLE 'vps4_end_web_schedulerclient.key' $ENV 'scheduler';
