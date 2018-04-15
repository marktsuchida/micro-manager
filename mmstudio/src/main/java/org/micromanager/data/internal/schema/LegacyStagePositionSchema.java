package org.micromanager.data.internal.schema;

import com.google.common.collect.ImmutableList;
import org.micromanager.data.internal.PropertyKey;

import java.util.Collection;

import static org.micromanager.data.internal.PropertyKey.*;

public class LegacyStagePositionSchema extends LegacyJSONSchema {
   private LegacyStagePositionSchema() {}
   private static LegacyJSONSchema INSTANCE = new LegacyStagePositionSchema();
   public static LegacyJSONSchema getInstance() { return INSTANCE; }


   @Override
   public Collection<PropertyKey> getRequiredInputKeys() {
      return ImmutableList.of(
         STAGE_POSITION__DEVICE,
         STAGE_POSITION__NUMAXES,
         STAGE_POSITION__COORD1_UM
      );
   }

   @Override
   public Collection<PropertyKey> getOptionalInputKeys() {
      return ImmutableList.of(
         STAGE_POSITION__COORD2_UM,
         STAGE_POSITION__COORD3_UM
      );
   }

   @Override
   public Collection<PropertyKey> getOutputKeys() {
      return ImmutableList.<PropertyKey>builder().
         addAll(getRequiredInputKeys()).
         addAll(getOptionalInputKeys()).
         build();
   }
}
