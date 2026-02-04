package io.github.jamsesso.jsonlogic.utils;

public final class JsonValueExtractor {
	private JsonValueExtractor() { }

	// public static Object extract(com.google.gson.JsonElement element) {
	//   if (element.isJsonObject()) {
	//     Map<String, Object> map = new HashMap<>();
	//     com.google.gson.JsonObject object = element.getAsJsonObject();
	//
	//     for (String key : object.keySet()) {
	//       map.put(key, extract(object.get(key)));
	//     }
	//
	//     return map;
	//   }
	//   else if (element.isJsonArray()) {
	//     List<Object> values = new ArrayList<>();
	//     for (com.google.gson.JsonElement item : element.getAsJsonArray()) {
	//       values.add(extract(item));
	//     }
	//     return values;
	//   }
	//   else if (element.isJsonNull()) {
	//     return null;
	//   }
	//   else if (element.isJsonPrimitive()) {
	//   	com.google.gson.JsonPrimitive primitive = element.getAsJsonPrimitive();
	//
	//     if (primitive.isBoolean()) {
	//       return primitive.getAsBoolean();
	//     }
	//     else if (primitive.isNumber()) {
	//       return primitive.getAsNumber().doubleValue();
	//     }
	//     else {
	//       return primitive.getAsString();
	//     }
	//   }
	//
	//   return element.toString();
	// }
}
