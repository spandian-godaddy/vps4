ACCESS_KEY_ID=$1
SECRET_ACCESS_KEY=$2
ROLE=$3
ENV=$4


echo 'getting all configuration for '$ENV'.'

#Load config from AWS
chmod +x ./get_config_from_aws.sh;
#base config
./get_config_from_aws.sh $ACCESS_KEY_ID $SECRET_ACCESS_KEY $ROLE 'config.properties' 'base';
./get_config_from_aws.sh $ACCESS_KEY_ID $SECRET_ACCESS_KEY $ROLE 'vps4.api.crt' 'base';
./get_config_from_aws.sh $ACCESS_KEY_ID $SECRET_ACCESS_KEY $ROLE 'vps4.api.key' 'base';
#environment config
./get_config_from_aws.sh $ACCESS_KEY_ID $SECRET_ACCESS_KEY $ROLE 'config.properties' $ENV;
./get_config_from_aws.sh $ACCESS_KEY_ID $SECRET_ACCESS_KEY $ROLE 'hfs.api.crt' $ENV;
./get_config_from_aws.sh $ACCESS_KEY_ID $SECRET_ACCESS_KEY $ROLE 'hfs.api.key' $ENV;
./get_config_from_aws.sh $ACCESS_KEY_ID $SECRET_ACCESS_KEY $ROLE 'messaging.api.crt' $ENV;
./get_config_from_aws.sh $ACCESS_KEY_ID $SECRET_ACCESS_KEY $ROLE 'messaging.api.key' $ENV;
./get_config_from_aws.sh $ACCESS_KEY_ID $SECRET_ACCESS_KEY $ROLE 'sso.client.crt' $ENV;
./get_config_from_aws.sh $ACCESS_KEY_ID $SECRET_ACCESS_KEY $ROLE 'sso.client.key' $ENV;
./get_config_from_aws.sh $ACCESS_KEY_ID $SECRET_ACCESS_KEY $ROLE 'entitlements.api.crt' $ENV;
./get_config_from_aws.sh $ACCESS_KEY_ID $SECRET_ACCESS_KEY $ROLE 'entitlements.api.key' $ENV;
./get_config_from_aws.sh $ACCESS_KEY_ID $SECRET_ACCESS_KEY $ROLE 'firewall.api.crt' $ENV;
./get_config_from_aws.sh $ACCESS_KEY_ID $SECRET_ACCESS_KEY $ROLE 'firewall.api.key' $ENV;
./get_config_from_aws.sh $ACCESS_KEY_ID $SECRET_ACCESS_KEY $ROLE 'vps4.shopper.crt' $ENV;
./get_config_from_aws.sh $ACCESS_KEY_ID $SECRET_ACCESS_KEY $ROLE 'vps4.shopper.key' $ENV;
./get_config_from_aws.sh $ACCESS_KEY_ID $SECRET_ACCESS_KEY $ROLE 'password_encryption.key' $ENV;
./get_config_from_aws.sh $ACCESS_KEY_ID $SECRET_ACCESS_KEY $ROLE 'vps4_end_web_consumerclient.crt' $ENV;
./get_config_from_aws.sh $ACCESS_KEY_ID $SECRET_ACCESS_KEY $ROLE 'vps4_end_web_consumerclient.key' $ENV;
./get_config_from_aws.sh $ACCESS_KEY_ID $SECRET_ACCESS_KEY $ROLE 'vps4_end_web_schedulerclient.crt' $ENV;
./get_config_from_aws.sh $ACCESS_KEY_ID $SECRET_ACCESS_KEY $ROLE 'vps4_end_web_schedulerclient.key' $ENV;
if [[  $ENV != "prod"* ]]; then
    ./get_config_from_aws.sh $ACCESS_KEY_ID $SECRET_ACCESS_KEY $ROLE 'vps4.api.crt' $ENV;
    ./get_config_from_aws.sh $ACCESS_KEY_ID $SECRET_ACCESS_KEY $ROLE 'vps4.api.key' $ENV;
fi
#base scheduler config
./get_config_from_aws.sh $ACCESS_KEY_ID $SECRET_ACCESS_KEY $ROLE 'config.properties' 'base' 'scheduler';
#environment scheduler config
./get_config_from_aws.sh $ACCESS_KEY_ID $SECRET_ACCESS_KEY $ROLE 'config.properties' $ENV 'scheduler';
./get_config_from_aws.sh $ACCESS_KEY_ID $SECRET_ACCESS_KEY $ROLE 'vps4_end_web_schedulerclient.crt' $ENV 'scheduler';
./get_config_from_aws.sh $ACCESS_KEY_ID $SECRET_ACCESS_KEY $ROLE 'vps4_end_web_schedulerclient.key' $ENV 'scheduler';
