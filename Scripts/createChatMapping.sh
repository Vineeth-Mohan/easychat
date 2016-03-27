#!/bin/bash
if [ -z $1 ];then
   echo "please enter hostname"
   exit
fi
hostname=$1
curl -XDELETE "http://$hostname:9200/chats"
echo
curl -X PUT "http://$hostname:9200/chats" -d '{
   "settings": {
        "number_of_shards" :   1,
        "number_of_replicas" : 0
   },
  "mappings": {
    "chat": {
      "_all": {
        "enabled": false
      },
      "properties": {
        "receivedTime": {
          "type": "date",
	  "format": "dd-MM-yyyy HH:mm:ss"
        },
        "groupID": {
          "type": "string",
          "index":"not_analyzed"
        }
      }
    }
  }
}'
echo
