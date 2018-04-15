package org.micromanager.data.internal.schema;

import com.google.common.collect.ImmutableList;
import org.micromanager.data.internal.PropertyKey;

import java.util.Collection;
import java.util.Collections;

import static org.micromanager.data.internal.PropertyKey.*;

public class LegacyMultiStagePositionSchema extends LegacyJSONSchema {
   private LegacyMultiStagePositionSchema() {}
   private static LegacyJSONSchema INSTANCE = new LegacyMultiStagePositionSchema();
   public static LegacyJSONSchema getInstance() { return INSTANCE; }


   @Override
   public Collection<PropertyKey> getRequiredInputKeys() {
      return Collections.emptyList();
   }

   @Override
   public Collection<PropertyKey> getOptionalInputKeys() {
      return ImmutableList.of(
         MULTI_STAGE_POSITION__LABEL,
         MULTI_STAGE_POSITION__DEFAULT_XY_STAGE,
         MULTI_STAGE_POSITION__DEFAULT_Z_STAGE,
         MULTI_STAGE_POSITION__GRID_ROW,
         MULTI_STAGE_POSITION__GRID_COLUMN,
         MULTI_STAGE_POSITION__PROPERTIES,
         MULTI_STAGE_POSITION__DEVICE_POSITIONS
      );
   }

   @Override
   public Collection<PropertyKey> getOutputKeys() {
      return getOptionalInputKeys();
   }
}
