package com.hascode.tutorial.Elasticsearch;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.vertx.java.core.json.JsonObject;

public class ESResponse {

	List<JsonObject> results;
	Date readTill;
	long countOfUnreadEvents;

	public ESResponse(List<JsonObject> results, Date readTill,long countOfUnreadEvents) {
		super();
		this.results = results;
		this.readTill = readTill;
		this.countOfUnreadEvents = countOfUnreadEvents;
	}

	public List<JsonObject> getResults() {
		return results;
	}

	public Date getReadTill() {
		return readTill;
	}

	public long getCountOfUnreadEvents() {
		return countOfUnreadEvents;
	}

}
