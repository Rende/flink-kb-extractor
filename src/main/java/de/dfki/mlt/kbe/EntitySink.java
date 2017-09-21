/**
 *
 */
package de.dfki.mlt.kbe;

import java.io.IOException;

import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.streaming.connectors.elasticsearch2.ElasticsearchSinkFunction;
import org.apache.flink.streaming.connectors.elasticsearch2.RequestIndexer;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Aydan Rende, DFKI
 *
 */
public class EntitySink implements ElasticsearchSinkFunction<JSONObject> {
	private static final long serialVersionUID = 1L;

	public IndexRequest createEntityIndexRequest(JSONObject jsonObj)
			throws IOException, JSONException {
		String type = jsonObj.getString("type");
		String id = jsonObj.getString("id");
		JSONObject labelObj = jsonObj
				.getJSONObject("labels")
				.getJSONObject("en");
		String label = labelObj.getString("value");
		XContentBuilder builder = XContentFactory.jsonBuilder().startObject()
				.field("type", type).field("id", id).field("label", label)
				.endObject();

		String json = builder.string();
		IndexRequest indexRequest = Requests.indexRequest()
				.index("wikidata-index").type("wikidata-entity").source(json);

		return indexRequest;
	}

	@Override
	public void process(JSONObject element, RuntimeContext ctx,
			RequestIndexer indexer) {
		try {
			indexer.add(createEntityIndexRequest(element));
		} catch (JSONException e) {
			// e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
