package de.dfki.mlt.kbe;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.elasticsearch2.ElasticsearchSink;
import org.json.JSONObject;

import de.dfki.mlt.kbe.preferences.Config;

/**
 * @author Aydan Rende, DFKI
 *
 */
public class App {
	public static void main(String[] args) throws Exception {
		final StreamExecutionEnvironment env = StreamExecutionEnvironment
				.getExecutionEnvironment().setParallelism(
						Config.getInstance().getInt(Config.THREAD_NUM));

		// read entities from json dump file
		// create json object stream
		DataStream<JSONObject> jsonStream = env.addSource(new WikidataSource(
				Config.getInstance().getString(Config.DIRECTORY_PATH)));

		sinkEntitiesToES(jsonStream);
		sinkClaimsToES(jsonStream);

		env.execute();
	}

	private static void sinkEntitiesToES(DataStream<JSONObject> jsonStream) {
		jsonStream.addSink(new ElasticsearchSink<>(getElasticSearchConfig(),
				getTransportAddresses(), new EntitySink()));
	}

	private static void sinkClaimsToES(DataStream<JSONObject> jsonStream) {
		jsonStream.addSink(new ElasticsearchSink<>(getElasticSearchConfig(),
				getTransportAddresses(), new ClaimSink()));
	}

	public static Map<String, String> getElasticSearchConfig() {
		Map<String, String> config = new HashMap<>();
		config.put("cluster.name",
				Config.getInstance().getString(Config.CLUSTER_NAME));
		config.put("bulk.flush.max.actions",
				Config.getInstance().getString(Config.BULK_FLUSH_MAX_ACTIONS));
		return config;
	}

	public static List<InetSocketAddress> getTransportAddresses() {
		List<InetSocketAddress> transportAddresses = new ArrayList<>();
		try {
			transportAddresses.add(new InetSocketAddress(InetAddress
					.getByName(Config.getInstance().getString(Config.HOST)),
					Config.getInstance().getInt(Config.PORT)));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return transportAddresses;
	}
}
