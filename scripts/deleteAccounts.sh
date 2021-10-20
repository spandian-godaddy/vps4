#!/bin/bash
while read f1 f2 f3
do
    echo "Shopper id is: $f1"
    echo "IP Address is: $f2"
    echo "Orion GUID is: $f3"

    HTTP_RESPONSE=$(curl --silent --write-out %{http_code} -X POST 'http://cancellation.prod.phx3.gdg/wscgdCancellation/wscgdCancellation.dll?Hander=Default%20HTTP%2F1.1' -H 'cache-control: no-cache' -H 'content: text/html' -H 'content-type: application/xml' -H 'postman-token: 186a5666-c948-ba59-ded2-3dc70f6d3475' -H 'soapaction: #QueueCancelMsg' -d '<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" xmlns:soapenc="http://schemas.xmlsoap.org/soap/encoding/" xmlns:tns="urn:wscgdCancellationService" xmlns:types="urn:wscgdCancellationService/encodedTypes" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema"> <soap:Body soap:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/"> <tns:QueueCancelMsg> <bstrInput xsi:type="xsd:string"> <cancellation shopperid="'${f1}'" cancel_by="DEV-VPS4" UserIP="'${f2}'"> <cancel id="vps4:'${f3}'" type="immediate" IDType="Orion" /> </cancellation> </bstrInput> </tns:QueueCancelMsg> </soap:Body> </soap:Envelope>')

# extract the body
HTTP_BODY=$(echo $HTTP_RESPONSE | sed -e 's/HTTPSTATUS\:.*//g')
echo $HTTP_BODY

done < file

# This script is fed a file (named "file") which has all the entries for shopper id, ip address and orion guids for all the account vm's we want to cancel.
# entries have to be space or tab separated. 
# sql query for getting these entries:
#-- select shopper id, IP, orion guid for each vm
#SELECT vps4_user.shopper_id, ip_address.ip_address, vm.orion_guid
#FROM virtual_machine vm
#JOIN ip_address ON vm.vm_id = ip_address.vm_id
#JOIN project ON vm.project_id = project.project_id
#JOIN user_project_privilege ON project.project_id = user_project_privilege.project_id
#JOIN vps4_user ON user_project_privilege.vps4_user_id = vps4_user.vps4_user_id
#WHERE vm.valid_until = 'infinity'
#ORDER BY vm.valid_on DESC ;
