package org.micromanager.data.internal.schema;

import com.google.common.collect.ImmutableList;
import org.micromanager.data.internal.PropertyKey;

import java.util.Collection;
import java.util.Collections;

import static org.micromanager.data.internal.PropertyKey.*;

public class LegacyCoordsSchema extends LegacyJSONSchema {
   private LegacyCoordsSchema() {}
   private static LegacyJSONSchema INSTANCE = new LegacyCoordsSchema();
   public static LegacyJSONSchema getInstance() { return INSTANCE; }


   @Override
   public Collection<PropertyKey> getRequiredInputKeys() {
      return ImmutableList.of(COMPLETE_COORDS);
   }

   @Override
   public Collection<PropertyKey> getOptionalInputKeys() {
      return Collections.emptyList();
   }

   @Override
   public Collection<PropertyKey> getOutputKeys() {
      return ImmutableList.of(
         COMPLETE_COORDS,
         FRAME_INDEX,
         POSITION_INDEX,
         SLICE_INDEX,
         CHANNEL_INDEX
      );
   }
}
