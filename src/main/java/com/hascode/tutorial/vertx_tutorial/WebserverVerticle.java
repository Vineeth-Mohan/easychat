package com.hascode.tutorial.vertx_tutorial;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.elasticsearch.ElasticsearchException;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.http.ServerWebSocket;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hascode.tutorial.Elasticsearch.ESInterface;
import com.hascode.tutorial.Elasticsearch.ESResponse;
import com.hascode.tutorial.Elasticsearch.Helper;

public class WebserverVerticle extends Verticle {

	
	@Override
	public void start() {
		final Pattern chatUrlPattern = Pattern.compile("/chat/(\\w+)/(\\w+)");
		final EventBus eventBus = vertx.eventBus();
		final Logger logger = container.logger();

		RouteMatcher httpRouteMatcher = new RouteMatcher().get("/", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest request) {
				request.response().sendFile("web/chat.html");
			}
		}).get(".*\\.(css|js)$", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest request) {
				request.response().sendFile("web/" + new File(request.path()));
			}
		});

		ESInterface.createInstance("vm", "localhost", "9300", "chats", "chat",logger);
		vertx.createHttpServer().requestHandler(httpRouteMatcher).listen(8080);

		vertx.createHttpServer().websocketHandler(new Handler<ServerWebSocket>() {
			@Override
			public void handle(final ServerWebSocket ws) {
				logger.info("Path is " + ws.path());
				final Matcher m = chatUrlPattern.matcher(ws.path());
				if (!m.matches()) {
					ws.reject();
					return;
				}

				final String chatRoom = m.group(1);
				final String userID = m.group(2);
				final String id = ws.textHandlerID();
				logger.info("registering new connection with id: " + id + " for chat-room: " + chatRoom);
				String chatRoomID = "chat.room." + chatRoom;
				vertx.sharedData().getSet("chat.room." + chatRoom).add(id);
				
				JsonObject unreadMsg = new JsonObject();
				ESResponse previousChats = ESInterface.getInstance().getRecentChat(userID,chatRoomID);
				unreadMsg.putValue("unreadMsgCount", previousChats.getCountOfUnreadEvents());
				eventBus.send(id, unreadMsg.toString());
				for (JsonObject  message : previousChats.getResults()) {
					logger.info("Sending message to " + id);
					eventBus.send(id, message.toString());
				}
				try {
					ESInterface.getInstance().updateReadTill(userID, Helper.getCurrentDate());
				} catch (ElasticsearchException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				}

				ws.closeHandler(new Handler<Void>() {
					@Override
					public void handle(final Void event) {
						logger.info("un-registering connection with id: " + id + " from chat-room: " + chatRoom);
						try {
							ESInterface.getInstance().updateReadTill(userID, Helper.getCurrentDate());
						} catch (ElasticsearchException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						vertx.sharedData().getSet("chat.room." + chatRoom).remove(id);
					}
				});

				ws.dataHandler(new Handler<Buffer>() {
					@Override
					public void handle(final Buffer data) {

						ObjectMapper m = new ObjectMapper();
						try {
							String chatRoomID = "chat.room." + chatRoom;
							JsonNode rootNode = m.readTree(data.toString());
							((ObjectNode) rootNode).put("receivedTime", Helper.getCurrentDate());
							((ObjectNode) rootNode).put("groupID", chatRoomID);
							String jsonOutput = m.writeValueAsString(rootNode);
							logger.info("json generated: " + jsonOutput);
							ESInterface.getInstance().insertChat(rootNode);
							for (Object chatter : vertx.sharedData().getSet(chatRoomID)) {
								eventBus.send((String) chatter, jsonOutput);
							}
						} catch (IOException e) {
							ws.reject();
						}
					}
				});

			}
		}).listen(8090);
	}
}
