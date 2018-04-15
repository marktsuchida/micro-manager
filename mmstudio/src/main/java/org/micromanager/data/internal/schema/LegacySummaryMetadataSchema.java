package org.micromanager.data.internal.schema;

import com.google.common.collect.ImmutableList;
import org.micromanager.data.internal.PropertyKey;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.micromanager.data.internal.PropertyKey.*;

public class LegacySummaryMetadataSchema extends LegacyJSONSchema {
   private LegacySummaryMetadataSchema() {}
   private static LegacyJSONSchema INSTANCE = new LegacySummaryMetadataSchema();
   public static LegacyJSONSchema getInstance() { return INSTANCE; }


   private static List<PropertyKey> BASIC_KEYS = ImmutableList.of(
      MICRO_MANAGER_VERSION,
      METADATA_VERSION,
      COMPUTER_NAME,
      USER_NAME,
      PROFILE_NAME,
      DIRECTORY,
      PREFIX,
      CHANNEL_GROUP,
      CHANNEL_NAMES,
      Z_STEP_UM,
      INTERVAL_MS,
      CUSTOM_INTERVALS_MS,
      AXIS_ORDER,
      INTENDED_DIMENSIONS,
      START_TIME,
      STAGE_POSITIONS,
      KEEP_SHUTTER_OPEN_SLICES,
      KEEP_SHUTTER_OPEN_CHANNELS,
      USER_DATA
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
         // Keys to store for backward-compatibility:
         add(
            TIME_FIRST,
            SLICES_FIRST,
            FRAMES,
            POSITIONS,
            SLICES,
            CHANNELS,
            WIDTH,
            HEIGHT,
            PIXEL_TYPE
         ).
         // add(DISPLAY_SETTINGS). // TODO
         build();
   }
}
