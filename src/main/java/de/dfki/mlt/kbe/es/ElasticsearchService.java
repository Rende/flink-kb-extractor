package de.dfki.mlt.kbe.es;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import com.google.common.collect.ImmutableList;

import de.dfki.mlt.kbe.App;
import de.dfki.mlt.kbe.preferences.Config;

public class ElasticsearchService {

	private Client client;

	public ElasticsearchService() {
		getClient();
	}

	private Client getClient() {
		if (client == null) {
			Map<String, String> userConfig = getUserConfig();
			List<InetSocketAddress> transportAddresses = getTransportAddresses();
			List<TransportAddress> transportNodes;
			transportNodes = new ArrayList<>(transportAddresses.size());
			for (InetSocketAddress address : transportAddresses) {
				transportNodes.add(new InetSocketTransportAddress(address));
			}
			Settings settings = Settings.settingsBuilder().put(userConfig)
					.build();

			TransportClient transportClient = TransportClient.builder()
					.settings(settings).build();
			for (TransportAddress transport : transportNodes) {
				transportClient.addTransportAddress(transport);
			}

			// verify that we actually are connected to a cluster
			ImmutableList<DiscoveryNode> nodes = ImmutableList
					.copyOf(transportClient.connectedNodes());
			if (nodes.isEmpty()) {
				throw new RuntimeException(
						"Client is not connected to any Elasticsearch nodes!");
			}

			client = transportClient;
		}
		return client;
	}

	public boolean checkAndCreateIndex(String indexName) throws IOException,
			InterruptedException {
		boolean result = false;
		IndicesAdminClient indicesAdminClient = getClient().admin().indices();
		final IndicesExistsResponse indexExistReponse = indicesAdminClient
				.prepareExists(indexName).execute().actionGet();
		if (indexExistReponse.isExists()) {
			deleteIndex(indicesAdminClient, indexName);
		}
		if (createIndex(indicesAdminClient, indexName)) {
			result = putMappingForEntity(indicesAdminClient)
					&& putMappingForClaim(indicesAdminClient);
		}
		return result;
	}

	private void deleteIndex(IndicesAdminClient indicesAdminClient,
			String indexName) {
		final DeleteIndexRequestBuilder delIdx = indicesAdminClient
				.prepareDelete(indexName);
		delIdx.execute().actionGet();
	}

	private boolean createIndex(IndicesAdminClient indicesAdminClient,
			String indexName) {
		final CreateIndexRequestBuilder createIndexRequestBuilder = indicesAdminClient
				.prepareCreate(indexName).setSettings(
						Settings.settingsBuilder()
								.put(Config.NUMBER_OF_SHARDS,
										Config.getInstance().getInt(
												Config.NUMBER_OF_SHARDS))
								.put(Config.NUMBER_OF_REPLICAS,
										Config.getInstance().getInt(
												Config.NUMBER_OF_REPLICAS)));
		CreateIndexResponse createIndexResponse = null;
		createIndexResponse = createIndexRequestBuilder.execute().actionGet();
		return createIndexResponse != null
				&& createIndexResponse.isAcknowledged();
	}

	private boolean putMappingForEntity(IndicesAdminClient indicesAdminClient)
			throws IOException {
		XContentBuilder mappingBuilder = XContentFactory
				.jsonBuilder()
				.startObject()
				.startObject(
						Config.getInstance().getString(Config.ENTITY_TYPE_NAME))
				.startObject("properties").startObject("id")
				.field("type", "string").field("index", "not_analyzed")
				.endObject().startObject("type").field("type", "string")
				.field("index", "not_analyzed").endObject()
				.startObject("label").field("type", "string").endObject()
				.endObject() // properties
				.endObject()// documentType
				.endObject();
		App.logger.info("Mapping for entity:" + mappingBuilder.string());
		PutMappingResponse putMappingResponse = indicesAdminClient
				.preparePutMapping(
						Config.getInstance().getString(Config.INDEX_NAME))
				.setType(
						Config.getInstance().getString(Config.ENTITY_TYPE_NAME))
				.setSource(mappingBuilder).execute().actionGet();

		return putMappingResponse.isAcknowledged();
	}

	private boolean putMappingForClaim(IndicesAdminClient indicesAdminClient)
			throws IOException {
		XContentBuilder mappingBuilder = XContentFactory
				.jsonBuilder()
				.startObject()
				.startObject(
						Config.getInstance().getString(Config.CLAIM_TYPE_NAME))
				.startObject("properties").startObject("entity_id")
				.field("type", "string").field("index", "not_analyzed")
				.endObject().startObject("property_id").field("type", "string")
				.field("index", "not_analyzed").endObject()
				.startObject("data_type").field("type", "string")
				.field("index", "not_analyzed").endObject()
				.startObject("data_value").field("type", "string").endObject()
				.endObject() // properties
				.endObject()// document Type
				.endObject();
		App.logger.info("Mapping for claim:" + mappingBuilder.string());
		PutMappingResponse putMappingResponse = indicesAdminClient
				.preparePutMapping(
						Config.getInstance().getString(Config.INDEX_NAME))
				.setType(Config.getInstance().getString(Config.CLAIM_TYPE_NAME))
				.setSource(mappingBuilder).execute().actionGet();
		return putMappingResponse.isAcknowledged();
	}

	public static Map<String, String> getUserConfig() {
		Map<String, String> config = new HashMap<>();
		config.put(Config.BULK_FLUSH_MAX_ACTIONS, Config.getInstance()
				.getString(Config.BULK_FLUSH_MAX_ACTIONS));
		config.put(Config.CLUSTER_NAME,
				Config.getInstance().getString(Config.CLUSTER_NAME));

		return config;
	}

	public static List<InetSocketAddress> getTransportAddresses() {
		List<InetSocketAddress> transports = new ArrayList<>();
		try {
			transports.add(new InetSocketAddress(InetAddress.getByName(Config
					.getInstance().getString(Config.HOST)), Config
					.getInstance().getInt(Config.PORT)));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return transports;
	}

	public void stopConnection() {
		getClient().close();
	}

}