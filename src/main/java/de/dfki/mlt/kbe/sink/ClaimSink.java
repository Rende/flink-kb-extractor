/**
 *
 */
package de.dfki.mlt.kbe.sink;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
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
public class ClaimSink implements ElasticsearchSinkFunction<JSONObject> {
	private static final long serialVersionUID = 1L;

	public List<IndexRequest> createClaimsIndexRequest(JSONObject jsonObj)
			throws IOException {
		List<IndexRequest> requestList = new ArrayList<IndexRequest>();
		String entityType = jsonObj.getString("type");
		String entityId = jsonObj.getString("id");
		if (entityType.equals("item")) {
			IndexRequest indexRequest = new IndexRequest();
			JSONObject claims = jsonObj.getJSONObject("claims");
			Iterator<String> itr = claims.keys();
			while (itr.hasNext()) {
				String propertyId = itr.next();
				JSONArray snakArray = claims.getJSONArray(propertyId);
				XContentBuilder builder = buildClaimRequest(entityId,
						propertyId, snakArray);
				if (builder != null) {
					indexRequest = Requests.indexRequest()
							.index("wikidata-index").type("wikidata-claim")
							.source(builder.string());
				}
				requestList.add(indexRequest);
			}
		}

		return requestList;
	}

	private XContentBuilder buildClaimRequest(String entityId,
			String propertyId, JSONArray snakArray) throws IOException {
		XContentBuilder builder = null;
		if (((JSONObject) snakArray.get(0)).getJSONObject("mainsnak").has(
				"datavalue")
				&& !((JSONObject) snakArray.get(0)).getJSONObject("mainsnak")
						.isNull("datavalue")) {
			JSONObject dataJson = ((JSONObject) snakArray.get(0))
					.getJSONObject("mainsnak").getJSONObject("datavalue");
			String dataType = dataJson.getString("type");
			String dataValue = "";
			switch (dataType) {
			case "string":
				dataValue = dataJson.getString("value");
				break;
			case "wikibase-entityid":
				dataValue = dataJson.getJSONObject("value").getString("id");
				break;
			case "globecoordinate":
				dataValue = dataJson.getJSONObject("value").get("latitude")
						+ " ; "
						+ dataJson.getJSONObject("value").get("longitude");
				break;
			case "quantity":
				dataValue = dataJson.getJSONObject("value").getString("amount")
						+ " ; "
						+ dataJson.getJSONObject("value").getString("unit");
				break;
			case "time":
				dataValue = dataJson.getJSONObject("value").getString("time");
				break;
			default:
				break;
			}
			builder = XContentFactory.jsonBuilder().startObject()
					.field("entity_id", entityId)
					.field("property_id", propertyId)
					.field("data_type", dataType)
					.field("data_value", dataValue).endObject();
		}
		return builder;
	}

	@Override
	public void process(JSONObject element, RuntimeContext ctx,
			RequestIndexer indexer) {
		List<IndexRequest> requestList = new ArrayList<IndexRequest>();
		try {
			requestList = createClaimsIndexRequest(element);
			IndexRequest[] requestArray = requestList
					.toArray(new IndexRequest[0]);
			indexer.add(requestArray);
		} catch (JSONException e) {
			// e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
