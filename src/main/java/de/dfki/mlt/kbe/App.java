package de.dfki.mlt.kbe;

import java.io.IOException;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.elasticsearch2.ElasticsearchSink;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import de.dfki.mlt.kbe.es.ElasticsearchService;
import de.dfki.mlt.kbe.preferences.Config;
import de.dfki.mlt.kbe.sink.ClaimSink;
import de.dfki.mlt.kbe.sink.EntitySink;
import de.dfki.mlt.kbe.source.WikidataSource;

/**
 * @author Aydan Rende, DFKI
 *
 */
public class App {
	public static final Logger logger = Logger.getLogger(App.class);

	public static void main(String[] args) throws Exception {
		logger.info("Wikidata knowledgebase extraction started.");
		final StreamExecutionEnvironment env = StreamExecutionEnvironment
				.getExecutionEnvironment().setParallelism(
						Config.getInstance().getInt(Config.THREAD_NUM));
		ElasticsearchService esService = new ElasticsearchService();
		try {
			esService.checkAndCreateIndex(Config.getInstance().getString(
					Config.INDEX_NAME));

		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		// read entities from json dump file
		// create json object stream
		DataStream<JSONObject> jsonStream = env.addSource(new WikidataSource(
				Config.getInstance().getString(Config.DIRECTORY_PATH)));

		sinkEntitiesToES(jsonStream);
		sinkClaimsToES(jsonStream);

		env.execute();
	}

	private static void sinkEntitiesToES(DataStream<JSONObject> jsonStream) {
		jsonStream.addSink(new ElasticsearchSink<>(ElasticsearchService
				.getUserConfig(), ElasticsearchService.getTransportAddresses(),
				new EntitySink()));
	}

	private static void sinkClaimsToES(DataStream<JSONObject> jsonStream) {
		jsonStream.addSink(new ElasticsearchSink<>(ElasticsearchService
				.getUserConfig(), ElasticsearchService.getTransportAddresses(),
				new ClaimSink()));
	}

}
