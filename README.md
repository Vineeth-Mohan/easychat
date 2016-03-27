# Vert.x Websocket Chat and persistaince using Elasticsearch
 
 Make sure Elasticsearch is running on localhost with cluster name as elasticsearch ( Default one ) ( Will move this to confiuration ) <br/>
 <br/>

## Schema
 Run the below command to create the index <br/>
 Scripts/createChatMapping.sh localhost <br/>

## Start server
 Run the below on the code and make sure you are using maven 3 <br/>
 mvn package vertx:runMod -DskipTests <br/>

## Access UI
 Access the UI at localhost:8080 <br/>

## Features
 1. Real time chat using websocekts in Vert.x <br/>
 2. Chat is persisted using Elasticsearch <br/>
 3. We are using another index called state to store the point till a user has seen his chat. Using this we can tell how many unread messages are there for the user and what they are <br/>
