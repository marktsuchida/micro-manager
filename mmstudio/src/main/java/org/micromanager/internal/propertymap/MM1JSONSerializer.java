/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.micromanager.internal.propertymap;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.micromanager.PropertyMap;
import org.micromanager.PropertyMaps;

/**
 * Serialize and deserialize generic JSON (as opposed to property map).
 * <p>
 * This JSON format is used in the Micro-Manager image file formats (despite
 * the 'MM1' name, the file format is used by MM 2.0, too, with some
 * extensions).
 * The format is defined as follows.
 * <ul>
 * <li>The outermost JSON objects have a certain primary nesting level, of
 * which the reader has prior knowledge.
 * <li>At the primary level, values are JSON booleans, JSON strings, or JSON
 * numbers. Distinguishing between number subtypes requires prior knowledge of
 * per-key value types.
 * <li>Additionally, simple nested JSON objects or arrays are used in some
 * cases. The nested values are booleans, strings, or numbers only.
 * <li>However, the flat map may also contain JSON objects with PropType -
 * PropVal pairs (Property Map 1.0 format). This format was erroneously
 * generated by a range of beta versions of Micro-Manager 2.0. This quirk is
 * no longer generated.
 * <li>JSON null is not expected but if encountered should be treated as if the
 * key is absent.
 * </ul>
 * Thus it is always possible to read the format into a property map (with some
 * uncertainty of numerical type mapping), but only specially restricted
 * property maps can be written to the format. Such property maps contain
 * primitive types and strings only.
 * <p>
 * Our strategy for reading this format is to use a generic reader to slurp the
 * JSON into a property map, then use high-level-format-specific knowledge to
 * convert them into the canonical modern property map format for the target
 * data structure.
 * <p>
 * Writing the format works in the opposite order: data is first packaged into
 * a modern property map, then translated into a restricted property map
 * representing the required persistence format for the data structure.
 * <p>
 * This class implements the bidirectional conversion between JSON and the
 * restricted property map.
 *
 * @author Mark A. Tsuchida
 */
public final class MM1JSONSerializer {
   /**
    * Read an MM1 JSON string into a property map.
    * <p>
    * <strong>Warning</Strong>: There is no way to determine exact numerical
    * types from this JSON format, so all numbers will be placed in the
    * returned map as longs (if representable as such) or doubles. As a special
    * case, empty arrays are treated as absent.
    *
    * @param json the JSON input in MM1 format
    * @return the deserialized property map
    * @throws IOException if there was a syntax or format error
    */
   @SuppressWarnings("UseSpecificCatch")
   public static PropertyMap fromJSON(String json) throws IOException {
      try {
         JsonReader reader = new JsonReader(new StringReader(json));
         reader.setLenient(true);
         JsonParser parser = new JsonParser();
         return fromGson(parser.parse(reader));
      }
      catch (Exception e) {
         throw new IOException("Invalid data", e);
      }
   }

   public static PropertyMap fromGson(JsonElement element) {
      PropertyMap.Builder builder = PropertyMaps.builder();
      JsonObject map = element.getAsJsonObject();
      for (Map.Entry<String, JsonElement> entry : map.entrySet()) {
         String key = entry.getKey();
         JsonElement value = entry.getValue();
         if (value.isJsonNull()) {
            continue;
         }
         if (value.isJsonObject()) {
            // Nested object may be either nested data structure (= property
            // list) or a PropType-PropVal pair
            JsonObject jo = value.getAsJsonObject();
            if (jo.has("PropType") && jo.has("PropVal")) {
               if (jo.get("PropVal").isJsonObject()) {
               LegacyPropertyMap1Deserializer.
                  deserializeProperty(builder, key,
                           jo.get("PropType").getAsString(),
                           jo.get("PropVal").getAsJsonObject());
               } else if (jo.get("PropVal").isJsonPrimitive() ){
                  LegacyPropertyMap1Deserializer.
                     deserializeProperty(builder, key,
                           jo.get("PropType").getAsString(),
                           jo.get("PropVal").getAsJsonPrimitive());
               }
            }
            else {
               builder.putPropertyMap(key, fromGson(jo));
            }
         }
         else if (value.isJsonArray()) {
            if (value.getAsJsonArray().size() == 0) {
               continue;
            }
            // Elements can be boolean, number, or string; no further nesting
            List<String> strings = Lists.newArrayList();
            List<Boolean> booleans = Lists.newArrayList();
            List<Long> longs = Lists.newArrayList();
            List<Double> doubles = Lists.newArrayList();
            for (JsonElement je : value.getAsJsonArray()) {
               JsonPrimitive jp = je.getAsJsonPrimitive();
               boolean ok = false;

               if (strings != null && jp.isString()) {
                  strings.add(jp.getAsString());
                  ok = true;
               }
               else {
                  strings = null;
               }

               if (booleans != null && jp.isBoolean()) {
                  booleans.add(jp.getAsBoolean());
                  ok = true;
               }
               else {
                  booleans = null;
               }

               if ((longs != null || doubles != null) && jp.isNumber()) {
                  BigDecimal n = jp.getAsBigDecimal();
                  if (longs != null) {
                     try {
                        longs.add(n.longValueExact());
                        ok = true;
                     }
                     catch (ArithmeticException e) {
                        longs = null;
                     }
                  }
                  if (doubles != null) {
                     doubles.add(n.doubleValue());
                     ok = true;
                  }
               }
               else {
                  longs = null;
                  doubles = null;
               }

               if (!ok) {
                  throw new JsonParseException("Value(s) of unsupported type in JSON array");
               }
            }
            if (strings != null) {
               builder.putStringList(key, strings);
            }
            else if (booleans != null) {
               builder.putBooleanList(key, booleans);
            }
            else if (longs != null) {
               builder.putLongList(key, longs);
            }
            else if (doubles != null) {
               builder.putDoubleList(key, doubles);
            }
         }
         else if (value.isJsonPrimitive()) {
            JsonPrimitive jp = value.getAsJsonPrimitive();
            if (jp.isString()) {
               builder.putString(key, value.getAsString());
            }
            else if (jp.isBoolean()) {
               builder.putBoolean(key, value.getAsBoolean());
            }
            else if (jp.isNumber()) {
               BigDecimal n = jp.getAsBigDecimal();
               try {
                  builder.putLong(key, n.longValueExact());
               }
               catch (ArithmeticException e) {
                  builder.putDouble(key, n.doubleValue());
               }
            }
         }
      }
      return builder.build();
   }

   /**
    * @param map
    * @return
    * @throws IllegalArgumentException if {@code map} contains value types not
    * supported by the legacy JSON format
    */
   public static String toJSON(PropertyMap map) {
      Gson gson = new GsonBuilder().disableHtmlEscaping().create();
      return gson.toJson(toGson(map));
   }

   public static JsonElement toGson(PropertyMap map) {
      JsonObject jo = new JsonObject();
      for (String key : map.keySet()) {
         Class<?> valueClass = map.getValueTypeForKey(key);
         if (valueClass.isArray()) {
            Class<?> elementClass = valueClass.getComponentType();
            jo.add(key, arrayToGson(map, key, elementClass));
         }
         else {
            jo.add(key, scalarToGson(map, key, valueClass));
         }
      }
      return jo;
   }

   private static JsonElement arrayToGson(PropertyMap source,
         String key, Class<?> elementClass)
   {
      JsonArray ja = new JsonArray();
      if (elementClass == String.class) {
         for (String s : source.getStringList(key)) {
            ja.add(new JsonPrimitive(s));
         }
      }
      else if (elementClass == boolean.class) {
         for (boolean b : source.getBooleanList(key)) {
            ja.add(new JsonPrimitive(b));
         }
      }
      else if (elementClass == byte.class) {
         for (byte n : source.getByteList(key)) {
            ja.add(new JsonPrimitive(n));
         }
      }
      else if (elementClass == short.class) {
         for (short n : source.getShortList(key)) {
            ja.add(new JsonPrimitive(n));
         }
      }
      else if (elementClass == int.class) {
         for (int n : source.getIntegerList(key)) {
            ja.add(new JsonPrimitive(n));
         }
      }
      else if (elementClass == long.class) {
         for (long n : source.getLongList(key)) {
            ja.add(new JsonPrimitive(n));
         }
      }
      else if (elementClass == float.class) {
         for (float n : source.getFloatList(key)) {
            ja.add(new JsonPrimitive(n));
         }
      }
      else if (elementClass == double.class) {
         for (double n : source.getDoubleList(key)) {
            ja.add(new JsonPrimitive(n));
         }
      }
      else {
         throw new IllegalArgumentException(
               "Legacy JSON format cannot encode array of type " +
                     elementClass.getSimpleName());
      }
      return ja;
   }

   private static JsonElement scalarToGson(PropertyMap source,
         String key, Class<?> valueClass)
   {
      if (valueClass == String.class) {
         return new JsonPrimitive(source.getString(key, null));
      }
      else if (valueClass == boolean.class) {
         return new JsonPrimitive(source.getBoolean(key, false));
      }
      else if (valueClass == byte.class) {
         return new JsonPrimitive(source.getByte(key, (byte) 0));
      }
      else if (valueClass == short.class) {
         return new JsonPrimitive(source.getShort(key, (short) 0));
      }
      else if (valueClass == int.class) {
         return new JsonPrimitive(source.getInteger(key, 0));
      }
      else if (valueClass == long.class) {
         return new JsonPrimitive(source.getLong(key, 0L));
      }
      else if (valueClass == float.class) {
         return new JsonPrimitive(source.getFloat(key, 0.0f));
      }
      else if (valueClass == double.class) {
         return new JsonPrimitive(source.getDouble(key, 0.0));
      }
      else if (valueClass == PropertyMap.class) {
         PropertyMap nested = source.getPropertyMap(key, null);
         return toGson(nested);
      }
      else {
         throw new IllegalArgumentException(
               "Legacy JSON format cannot encode value of type " +
                     valueClass.getSimpleName());
      }
   }
}
