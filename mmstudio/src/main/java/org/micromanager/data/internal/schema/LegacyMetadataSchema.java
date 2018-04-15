package org.micromanager.data.internal.schema;

import com.google.common.collect.ImmutableList;
import org.micromanager.data.internal.PropertyKey;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.micromanager.data.internal.PropertyKey.*;

public class LegacyMetadataSchema extends LegacyJSONSchema {
   private LegacyMetadataSchema() {}
   private static LegacyJSONSchema INSTANCE = new LegacyMetadataSchema();
   public static LegacyJSONSchema getInstance() { return INSTANCE; }


   private static List<PropertyKey> BASIC_KEYS = ImmutableList.of(
      UUID,
      CAMERA,
      BINNING,
      ROI,
      BIT_DEPTH,
      EXPOSURE_MS,
      ELAPSED_TIME_MS,
      IMAGE_NUMBER,
      RECEIVED_TIME,
      PIXEL_SIZE_UM,
      PIXEL_ASPECT,
      POSITION_NAME,
      X_POSITION_UM,
      Y_POSITION_UM,
      Z_POSITION_UM,
      SCOPE_DATA,
      USER_DATA,
      FILE_NAME
   );

   @Override
   public Collection<PropertyKey> getRequiredInputKeys() {
      return Collections.emptyList();
   }

   @Override
   public Collection<PropertyKey> getOptionalInputKeys() {
      return ImmutableList.<PropertyKey>builder().
         addAll(BASIC_KEYS).
         // Although the pixel type belongs to Image, not Metadata,
         // we need it here due to the design of MultipageTiffReader.
         // TODO Really? Why? Can we avoid it?
         add(PIXEL_TYPE).
         build();
   }

   @Override
   public Collection<PropertyKey> getOutputKeys() {
      return ImmutableList.<PropertyKey>builder().
         addAll(BASIC_KEYS).
         add(SCOPE_DATA_KEYS).
         build();
   }
}
