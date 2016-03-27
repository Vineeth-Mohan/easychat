package com.hascode.tutorial.Elasticsearch;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.lucene.queryparser.xml.FilterBuilderFactory;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest.OpType;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.InternalFilter;
import org.elasticsearch.search.sort.SortOrder;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import static org.elasticsearch.common.xcontent.XContentFactory.*;

import com.fasterxml.jackson.databind.JsonNode;

public class ESInterface {

	static ESInterface INSTANCE;
	Client client;
	String ip, port;
	String clusterName;
	String indexName, indexType;

	private static Logger logger;

	String stateStorageIndex = "states";
	String stateStorageIndexType = "state";

	public static ESInterface createInstance(String cluserName, String ip,
			String port, String indexName, String indexType,Logger logger) {
		ESInterface.logger = logger;
		if (INSTANCE == null) {
			INSTANCE = new ESInterface(ip, cluserName, port, indexName,
					indexType);
		}
		return INSTANCE;
	}

	public static ESInterface getInstance() {
		return INSTANCE;
	}

	public ESInterface(String ip, String clusterName, String port,
			String indexName, String indexType) {
		this.clusterName = clusterName;
		this.ip = ip;
		this.port = port;
		this.indexName = indexName;
		this.indexType = indexType;
		Settings settings = ImmutableSettings.settingsBuilder()
				.put("cluster.name", clusterName).build();
		this.client = new TransportClient(settings)
				.addTransportAddress(new InetSocketTransportAddress(ip, Integer
						.parseInt(port)));
	}

	public Boolean insertChat(JsonNode data) {
		IndexResponse response = client
				.prepareIndex(this.indexName, this.indexType)
				.setOpType(OpType.INDEX).setSource(data.toString()).execute()
				.actionGet();
		return response.isCreated();
	}

	public ESResponse getRecentChat(String userID, String groupIS) {
		List<JsonObject> results = new ArrayList<JsonObject>();

		GetResponse getResponse = client
				.prepareGet(stateStorageIndex, stateStorageIndexType, userID)
				.execute().actionGet();

		String lastReadTime = "01-01-1970 00:00:00";
		if (getResponse.isExists()) {
			lastReadTime = (String) getResponse.getSource().get("readTill");
		}

		SearchResponse response = client
				.prepareSearch(this.indexName)
				.setTypes(this.indexType)
				.setQuery(QueryBuilders.termQuery("groupID", groupIS))
				.addSort("receivedTime", SortOrder.ASC)
				.addAggregation(
						AggregationBuilders.filter("unreadEvents").filter(
								FilterBuilders.rangeFilter("receivedTime")
										.from(lastReadTime))).setFrom(0)
				.setSize(60).execute().actionGet();
		for (SearchHit hit : response.getHits()) {
			JsonObject jsonHit = new JsonObject(hit.getSourceAsString());
			results.add(jsonHit);
		}

		InternalFilter aggResult = response.getAggregations().get("unreadEvents");
		logger.info("Aggregation response is " + aggResult.getDocCount());
		return new ESResponse(results, null, aggResult.getDocCount());
	}

	public void updateReadTill(String userID, String time)
			throws ElasticsearchException, IOException {
		logger.info("Update " + userID + " to time " + time);
		IndexResponse response = client
				.prepareIndex(stateStorageIndex, stateStorageIndexType, userID)
				.setSource(
						jsonBuilder().startObject().field("readTill", time)
								.endObject()).get();
	}


}
