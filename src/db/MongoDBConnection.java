package db;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;

import model.Restaurant;
import yelp.YelpAPI;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.regex;

public class MongoDBConnection implements DBConnection {
	private static final int MAX_RECOMMENDED_RESTAURANTS = 10;

	private MongoClient mongoClient;
	private MongoDatabase db;

	public MongoDBConnection() {
		// Connects to local mongodb server.
		mongoClient = new MongoClient();
		db = mongoClient.getDatabase(DBUtil.DB_NAME);

	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		if (mongoClient != null) {
			mongoClient.close();
		}
	}

	@Override
	public void setVisitedRestaurants(String userId, List<String> businessIds) {
		// TODO Auto-generated method stub
		db.getCollection("users").updateOne(new Document("user_id", userId),
				new Document("$pushAll", new Document("visited", businessIds)));
	}

	@Override
	public void unsetVisitedRestaurants(String userId, List<String> businessIds) {
		// TODO Auto-generated method stub
		db.getCollection("users").updateOne(new Document("user_id", userId),
				new Document("$pullAll", new Document("visited", businessIds)));
	}

	@Override
	public Set<String> getVisitedRestaurants(String userId) {
		// TODO Auto-generated method stub
		Set<String> set = new HashSet<>();
		// db.users.find({user_id:1111})
		FindIterable<Document> iterable = db.getCollection("users").find(
				eq("user_id", userId));
		if (iterable.first().containsKey("visited")) {
	   		List<String> list = (List<String>) iterable.first().get("visited");
			set.addAll(list);
	   	}
		return set;
	}

	@Override
	public JSONObject getRestaurantsById(String businessId, boolean isVisited) {
		// TODO Auto-generated method stub
		FindIterable<Document> iterable = db.getCollection("restaurants").find(eq("business_id", businessId));
		try {
			JSONObject obj = new JSONObject(iterable.first().toJson());
			obj.put("is_visited", isVisited);
			return obj;
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public JSONArray recommendRestaurants(String userId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> getCategories(String businessId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> getBusinessId(String category) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JSONArray searchRestaurants(String userId, double lat, double lon, String term) {
		// TODO Auto-generated method stub
		try {
			YelpAPI api = new YelpAPI();
			JSONObject response = new JSONObject(api.searchForBusinessesByLocation(lat, lon));
			JSONArray array = (JSONArray) response.get("businesses");

			List<JSONObject> list = new ArrayList<JSONObject>();
			Set<String> visited = getVisitedRestaurants(userId);

			for (int i = 0; i < array.length(); i++) {
				JSONObject object = array.getJSONObject(i);
				Restaurant restaurant = new Restaurant(object);
				String businessId = restaurant.getBusinessId();
				String name = restaurant.getName();
				String categories = restaurant.getCategories();
				String city = restaurant.getCity();
				String state = restaurant.getState();
				String fullAddress = restaurant.getFullAddress();
				double stars = restaurant.getStars();
				double latitude = restaurant.getLatitude();
				double longitude = restaurant.getLongitude();
				String imageUrl = restaurant.getImageUrl();
				String url = restaurant.getUrl();
				JSONObject obj = restaurant.toJSONObject();
				if (visited.contains(businessId)) {
					obj.put("is_visited", true);
				} else {
					obj.put("is_visited", false);
				}
				// Question: why using upsert instead of insert directly?
				UpdateOptions options = new UpdateOptions().upsert(true);

				db.getCollection("restaurants").updateOne(new Document().append("business_id", businessId),
						new Document("$set", new Document().append("business_id", businessId).append("name", name)
								.append("categories", categories).append("city", city).append("state", state)
								.append("full_address", fullAddress).append("stars", stars).append("latitude", latitude)
								.append("longitude", longitude).append("image_url", imageUrl).append("url", url)),
						options);
				list.add(obj);
			}
			// return new JSONArray(list);
			if (term == null || term.isEmpty()) {
				return new JSONArray(list);
			} else {
				// Use text search to perform better efficiency
				return filterRestaurants(term);
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return null;
	}

	private JSONArray filterRestaurants(String term) {
		try {
			Set<JSONObject> set = new HashSet<JSONObject>();
			FindIterable<Document> iterable = db.getCollection("restaurants").find(Filters.text(term));

			iterable.forEach(new Block<Document>() {
				@Override
				public void apply(final Document document) {
					set.add(getRestaurantsById(document.getString("business_id"), false));
				}
			});
			return new JSONArray(set);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return null;
	}

	@Override
	public Boolean verifyLogin(String userId, String password) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getFirstLastName(String userId) {
		// TODO Auto-generated method stub
		return null;
	}

}
