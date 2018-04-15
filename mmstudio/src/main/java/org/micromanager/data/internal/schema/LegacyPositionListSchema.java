package org.micromanager.data.internal.schema;

import com.google.common.collect.ImmutableList;
import org.micromanager.data.internal.PropertyKey;

import java.util.Collection;

import static org.micromanager.data.internal.PropertyKey.*;

/**
 * PositionList JSON schema.
 *
 * This schema is used for:
 * <ul>
 *     <li>A JSON object saved in its own file (from the Position List dialog)</li>
 *     <li>A JSON array embedded in SummaryMetadata "InitialPositionList" (1.4)</li>
 *     <li>A JSON array embedded in SummaryMetadata STAGE_POSITIONS (2.x)</li>
 * </ul>
 */
public class LegacyPositionListSchema extends LegacyJSONSchema {
   private LegacyPositionListSchema() {}
   private static LegacyJSONSchema INSTANCE = new LegacyPositionListSchema();
   public static LegacyJSONSchema getInstance() { return INSTANCE; }


   @Override
   public Collection<PropertyKey> getRequiredInputKeys() {
      return ImmutableList.of(STAGE_POSITIONS);
   }

   @Override
   public Collection<PropertyKey> getOptionalInputKeys() {
      return ImmutableList.of(POSITION_LIST__ID, POSITION_LIST__VERSION);
   }

   @Override
   public Collection<PropertyKey> getOutputKeys() {
      // TODO
      throw new UnsupportedOperationException("Not implemented");
   }
}
