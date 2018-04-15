package org.micromanager.data.internal.schema;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import org.micromanager.PropertyMap;
import org.micromanager.PropertyMaps;
import org.micromanager.data.internal.PropertyKey;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;

/**
 * Serialize and deserialize property-map-like data.
 *
 * This uses a common implementation based on representations of required and
 * optional keys supplied by subclasses.
 *
 * A subclass must implement all of {@code getRequiredInputKeys},
 * {@code getOptionalInputKeys}, and {@code getOutputKeys}.
 *
 * When necessary, a subclass can also override {@code fromGson} and
 * {@code addToGson}, in which case the {@code get...Keys} methods will not be
 * called.
 */
public abstract class LegacyJSONSchema {
   protected abstract Collection<PropertyKey> getRequiredInputKeys();
   protected abstract Collection<PropertyKey> getOptionalInputKeys();
   protected abstract Collection<PropertyKey> getOutputKeys();

   public final PropertyMap fromJSON(String json) throws IOException {
      try {
         JsonReader reader = new JsonReader(new StringReader(json));
         reader.setLenient(true);
         JsonParser parser = new JsonParser();
         return fromGson(parser.parse(reader).getAsJsonObject());
      }
      catch (JsonParseException | IllegalStateException e) {
         throw new IOException("Invalid data", e);
      }
   }

   public final String toJSON(PropertyMap pmap) {
      Gson gson = new GsonBuilder().
         disableHtmlEscaping().
         setPrettyPrinting().
         create();
      return gson.toJson(toGson(pmap));
   }

   public PropertyMap fromGson(JsonElement je) {
      PropertyMap.Builder builder = PropertyMaps.builder();

      for (PropertyKey key : getRequiredInputKeys()) {
         key.extractFromGsonObject(je.getAsJsonObject(), builder);
      }

      for (PropertyKey key : getOptionalInputKeys()) {
         key.extractFromGsonObject(je.getAsJsonObject(), builder);
      }

      return builder.build();
   }

   public final JsonElement toGson(PropertyMap pmap) {
      JsonObject jo = new JsonObject();
      addToGson(jo, pmap);
      return jo;
   }

   public void addToGson(JsonObject jo, PropertyMap pmap) {
      for (PropertyKey key : getOutputKeys()) {
         key.storeInGsonObject(pmap, jo);
      }
   }
}
