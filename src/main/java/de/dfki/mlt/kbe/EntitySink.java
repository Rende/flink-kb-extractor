/**
 *
 */
package de.dfki.mlt.kbe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.streaming.connectors.elasticsearch2.ElasticsearchSinkFunction;
import org.apache.flink.streaming.connectors.elasticsearch2.RequestIndexer;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Aydan Rende, DFKI
 *
 */
public class EntitySink implements ElasticsearchSinkFunction<JSONObject> {
	private static final long serialVersionUID = 1L;

	public List<IndexRequest> createEntityIndexRequest(JSONObject jsonObj)
			throws IOException, JSONException {
		List<IndexRequest> requestList = new ArrayList<IndexRequest>();
		String type = jsonObj.getString("type");
		String id = jsonObj.getString("id");
		JSONObject labelObj = jsonObj.getJSONObject("labels").getJSONObject(
				"en");
		String label = labelObj.getString("value");
		XContentBuilder builder = XContentFactory.jsonBuilder().startObject()
				.field("id", id).field("type", type).field("label", label)
				.endObject();

		requestList.add(Requests.indexRequest().index("wikidata-index")
				.type("wikidata-entity").source(builder.string()));

		JSONArray aliasArr = jsonObj.getJSONObject("aliases").getJSONArray("en");

		requestList.addAll(createEntityIndexRequestFromAliases(id, type,
				aliasArr));
		return requestList;
	}

	private List<IndexRequest> createEntityIndexRequestFromAliases(String id,
			String type, JSONArray aliasArr) throws IOException {
		List<IndexRequest> requestList = new ArrayList<IndexRequest>();
		String label = "";
		XContentBuilder builder = null;
		for (Object aliasObj : aliasArr) {
			label = ((JSONObject) aliasObj).getString("value");
			builder = XContentFactory.jsonBuilder().startObject()
					.field("id", id).field("type", type).field("label", label)
					.endObject();

			requestList.add(Requests.indexRequest().index("wikidata-index")
					.type("wikidata-entity").source(builder.string()));
		}
		return requestList;
	}

	@Override
	public void process(JSONObject element, RuntimeContext ctx,
			RequestIndexer indexer) {
		List<IndexRequest> requestList = new ArrayList<IndexRequest>();
		try {
			requestList = createEntityIndexRequest(element);
			IndexRequest[] requestArray = requestList
					.toArray(new IndexRequest[0]);
			indexer.add(requestArray);
		} catch (JSONException e) {
			 e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
