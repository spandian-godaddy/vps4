## How to setup a local environment with Message Consumer

* Setup your run configuration for your IDE
    1. Add these vm options in your run configuration for your IDE.
     
        * _-DSkipZkRegistration=true_
        * _-Dorchestration.engine.clustered=false_
        * _-Dvps4.web.useJwtAuth=true_
        * _-Dvps4.user.fake=false_ *-> this is optional*
        * _-Dvps4.config.mode=file_ *-> this is optional*
    
    2. Main class is _com.godaddy.vps4.consumer.Vps4ConsumerApplication_
    3. Working directory: Use the correct directory structure where the vps4-message-consumer project is located in your local environment. For example:  _/Users/**username**/git/vps4/vps4-message-consumer_
    4. Use classpath of module _vps4-message-consumer_
    5. Change config file in the core project _core/config/local/config.properties_ to use the ssoJwt token. Login to CRM in dev (crm.int.dev-godaddy.com) and grab the **auth_jomax** cookie and paste it in the config file.
        - *used when running message consumer locally to connect with the web micro-service*
        - *#ssoJwt=REPLACE_WITH_SSO_JWT_TOKEN*

* Download the latest version of kafka client and install it in a local directory location. 

#### Various commands required to use kafka client effectively

Change directory to where the kafka client is installed and use the commands shown below.

* command to list the kafka topics 
    * ./bin/kafka-topics.sh --list --bootstrap-server p3dlkckafka01.cloud.phx3.gdg:9092 > alltopics; cat alltopics |grep vps4
    ```
    vps4-account-updates
    vps4-account-updates-akash
    vps4-account-updates-dev
    vps4-account-updates-george
    vps4-account-updates-local
    vps4-account-updates-stage
    vps4-account-updates-test
    vps4-monitoring
    vps4-monitoring-dev
    vps4-monitoring-local
    vps4-monitoring-stage
    vps4-monitoring-test
    ```
     
* command to show topic details
    * ./bin/kafka-topics.sh --describe --bootstrap-server p3dlkckafka01.cloud.phx3.gdg:9092 --topic vps4-account-updates-test > show_topics ; cat show_topics
    ```
    Topic:vps4-account-updates-test	PartitionCount:3	ReplicationFactor:2	Configs:
	 Topic: vps4-account-updates-test	Partition: 0	Leader: 3	Replicas: 3,1	Isr: 3,1
	 Topic: vps4-account-updates-test	Partition: 1	Leader: 1	Replicas: 1,2	Isr: 1,2
	 Topic: vps4-account-updates-test	Partition: 2	Leader: 2	Replicas: 2,3	Isr: 3,2
    ```
 
* command to list partitions and offsets
    * ./bin/kafka-run-class.sh kafka.tools.GetOffsetShell --broker-list p3dlkckafka01.cloud.phx3.gdg:9092 --topic vps4-account-updates-test
    ```
    vps4-account-updates-test:0:34270
    vps4-account-updates-test:1:36661
    vps4-account-updates-test:2:9251
    ```

* command to show messages from the beginning for a topic. (Note that the production kafka server is used in the commands below instead of the development server, due to availability of messages in the production environment.)
    * bin/kafka-console-consumer.sh --bootstrap-server p3plkckafka01.cloud.phx3.gdg:9092 --topic   vps4-account-updates --from-beginning 1>> messages 2>> jmxlogs;
    ```
    {"id":"9b8564f5-363a-42bc-9127-213609ccdf81","version":"2.0","notification":{"type":"added","account_guid":"eb155908-5fc3-11e9-8177-3417ebe73f0e"}}
    {"id":"e0f9ae80-5f82-444a-bd80-e2d940216808","version":"2.0","notification":{"type":"renewed","account_guid":"e863ce7f-3af1-11e9-8167-3417ebe60eb6"}}
    {"id":"c937d5a3-e8cf-4005-a210-25ca8433ef51","version":"2.0","notification":{"type":"removed","account_guid":"f43e58dc-5c98-11e9-817c-3417ebe7253b"}}
    {"id":"0c585064-9389-48fb-ac70-5cb14031e184","version":"2.0","notification":{"type":"added","account_guid":"93eecf5d-5fed-11e9-817c-3417ebe7253b"}}
    {"id":"64dacbf0-3aee-4172-82fa-a7a493417f25","version":"2.0","notification":{"type":"added","account_guid":"50d06fa4-5ff1-11e9-8177-3417ebe73f0e"}}
    {"id":"bc64b2e8-5661-4336-9b5e-e105fb26d04b","version":"2.0","notification":{"type":"added","account_guid":"3b4188d3-5ff4-11e9-816f-3417ebe72601"}}
    {"id":"6c938e75-6ce6-4325-bc36-13ad406dd255","version":"2.0","notification":{"type":"removed","account_guid":"771aac71-5ccd-11e9-8176-3417ebe73f0e"}}
    {"id":"8222d432-16f7-4f96-a3ec-cc538e48c318","version":"2.0","notification":{"type":"suspended","account_guid":"9af05545-3e6b-11e9-8162-3417ebe72601"}}
    {"id":"37dbf66e-ed63-4125-8fd0-94757b1d19ed","version":"2.0","notification":{"type":"reinstated","account_guid":"e0f9a0b2-f0c2-11e8-815d-3417ebe725c2"}}
    ```
* command to show messages from an offset. Following command should list the messages starting from 9720 onwards.
    * ./bin/kafka-console-consumer.sh --bootstrap-server p3plkckafka01.cloud.phx3.gdg:9092 --topic   vps4-account-updates --offset 9720 --partition 0
     
    
* command to Post to the topic
    * ./bin/kafka-console-producer.sh --broker-list p3dlkckafka01.cloud.phx3.gdg:9092 --topic vps4-account-updates-local