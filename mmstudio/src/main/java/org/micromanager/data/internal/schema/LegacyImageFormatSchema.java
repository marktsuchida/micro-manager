package org.micromanager.data.internal.schema;

import com.google.common.collect.ImmutableList;
import org.micromanager.data.internal.PropertyKey;

import java.util.Collection;
import java.util.Collections;

import static org.micromanager.data.internal.PropertyKey.*;

public class LegacyImageFormatSchema extends LegacyJSONSchema {
   private LegacyImageFormatSchema() {}
   private static LegacyJSONSchema INSTANCE = new LegacyImageFormatSchema();
   public static LegacyJSONSchema getInstance() { return INSTANCE; }


   @Override
   public Collection<PropertyKey> getRequiredInputKeys() {
      return ImmutableList.of(WIDTH, HEIGHT, PIXEL_TYPE);
   }

   @Override
   public Collection<PropertyKey> getOptionalInputKeys() {
      return Collections.emptyList();
   }

   @Override
   public Collection<PropertyKey> getOutputKeys() {
      return ImmutableList.of(WIDTH, HEIGHT, PIXEL_TYPE);
   }
}
