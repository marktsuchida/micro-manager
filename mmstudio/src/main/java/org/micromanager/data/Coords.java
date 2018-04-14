///////////////////////////////////////////////////////////////////////////////
//PROJECT:       Micro-Manager
//SUBSYSTEM:     Data API
//-----------------------------------------------------------------------------
//
// AUTHOR:       Chris Weisiger, 2015
//
// COPYRIGHT:    University of California, San Francisco, 2015
//
// LICENSE:      This file is distributed under the BSD license.
//               License text is included with the source distribution.
//
//               This file is distributed in the hope that it will be useful,
//               but WITHOUT ANY WARRANTY; without even the implied warranty
//               of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//
//               IN NO EVENT SHALL THE COPYRIGHT OWNER OR
//               CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
//               INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES.

package org.micromanager.data;

import java.util.List;

/**
 * The multi-dimensional coordinate of a 2D image within a dataset.
 *
 * This is typically used to represent the time point, stage position, z slice,
 * and channel indices of images.
 * <p>
 * The {@code Coords} object is a mapping from axes (represented by strings) to
 * non-negative integer indices.
 * <p>
 * Although methods are included to support any arbitrary axes, such usage is
 * not yet fully supported. <strong>If you do use custom axes, give them names
 * that include upper case letters</strong> so that they do not clash with
 * standard axes added in the future.
 * <p>
 * {@code Coords} objects are immutable.
 *
 * @author Chris Weisiger, Mark A. Tsuchida
 */
public interface Coords {
   /** Axis label for the time point (frame) axis. */
   public static final String TIME_POINT = "time";

   /**
    * Same as {@code TIME_POINT} or {@code T}.
    * @deprecated Use discouraged because it reads like a physical time rather
    * than the time point index that it is.
    */
   @Deprecated public static final String TIME = TIME_POINT;

   /** Axis label for the time point (frame) axis (short form).
    * Same as {@code TIME_POINT}. */
   public static final String T = TIME_POINT;

   /** Axis label for the stage position axis. */
   public static final String STAGE_POSITION = "position";

   /** Axis label for the stage position axis (short form).
    * Same as {@code STAGE_POSTITION}. */
   public static final String P = STAGE_POSITION;

   /** Axis label for the Z slice axis. */
   public static final String Z_SLICE = "z";

   /** Axis label for the Z slice axis (short form).
    * Same as {@code Z_SLICE}. */
   public static final String Z = Z_SLICE;

   /** Axis label for the channel axis. */
   public static final String CHANNEL = "channel";

   /** Axis label for the channel axis (short form). Same as {@code CHANNEL}. */
   public static final String C = CHANNEL;

   interface Builder extends CoordsBuilder {
      @Override Coords build();

      /**
       * Set the channel index.
       * Equivalent to {@code index(Coords.CHANNEL, channel)}.
       *
       * @param channel channel index (0-based)
       * @return this
       */
      @Override Builder channel(int channel);

      /** Shorthand for {@link channel}. */
      Builder c(int channel);

      /**
       * Set the time point (frame) index.
       * Equivalent to {@code index(Coords.TIME_POINT, frame)}.
       *
       * @param frame time point (frame) index (0-based)
       * @return this
       */
      Builder timePoint(int frame);

      /**
       * Same as {@link timePoint}.
       * @deprecated Due to being confusing with physical time.
       */
      @Override @Deprecated Builder time(int timepoint);

      /** Shorthand for {@link time}. */
      Builder t(int timepoint);

      /**
       * Set the Z slice index.
       * Equivalent to {@code index(Coords.Z_SLICE, slice)}.
       *
       * @param slice z slice index (0-based)
       * @return this
       */
      Builder zSlice(int slice);

      /** Shorthand for {@link zSlice}. */
      @Override Builder z(int slice);

      /**
       * Set the stage position index.
       * Equivalent to {@code index(Coords.STAGE_POSITION, index)}.
       *
       * @param index stage position index (0-based)
       * @return this
       */
      @Override Builder stagePosition(int index);

      /** Shorthand for {@link stagePosition}. */
      Builder p(int index);

      /**
       * Set the index along a given axis.
       *
       * If you set a negative value, the axis will be removed.
       *
       * @param axis coordinate axis, such as {@code Coords.CHANNEL}
       * @param index 0-based index
       * @return this
       */
      @Override Builder index(String axis, int index);

      /**
       * Remove the specified axis.
       *
       * @param axis coordinate axis, such as {@code Coords.CHANNEL}
       * @return this
       */
      @Override Builder removeAxis(String axis);

      /**
       * Offset the given axis by a given count.
       *
       * @param axis coordinate axis, such as {@code Coords.CHANNEL}
       * @param offset offset to be applied to {@code axis}
       * @return this
       * @throws IllegalArgumentException if {@code axis} does not exist
       * @throws IndexOutOfBoundsException if applying the offset would result
       * in a negative index.
       */
      @Override Builder offset(String axis, int offset)
            throws IllegalArgumentException, IndexOutOfBoundsException;
   }

   interface CoordsBuilder {
      Coords build();
      CoordsBuilder channel(int channel);
      CoordsBuilder time(int time);
      CoordsBuilder z(int z);
      CoordsBuilder stagePosition(int stagePosition);
      CoordsBuilder index(String axis, int index);
      CoordsBuilder removeAxis(String axis);
      CoordsBuilder offset(String axis, int offset) throws IllegalArgumentException;
   }

   /**
    * Check if this coords has no axes.
    * @return true if this coords is empty, else false
    */
   default boolean isEmpty() {
      return getAxes().isEmpty();
   }

   /**
    * Get the index for the given axis.
    *
    * @param axis coordinate axis such as {@code Coords.CHANNEL}
    * @return index along {@code axis}, or {@code -1} if {@code axis} does not
    * exist
    */
   default int getIndex(String axis) {
      return getIndex(axis, -1);
   }

   int getIndex(String axis, int defaultIndex);

   /**
    * Get the channel index.
    *
    * Equivalent to {@code getIndex(Coords.CHANNEL)}.
    *
    * @return channel index, or {@code -1} if this {@code Coords} doesn't
    * contain a channel index.
    */
   default int getChannel() {
      return getChannel(-1);
   }

   int getChannel(int defaultChannelIndex);

   /** Shorthand for {@link getChannel}. */
   default int getC() {
      return getChannel();
   }

   /**
    * Get the time point (frame) index.
    *
    * Equivalent to {@code getIndex(Coords.TIME_POINT)}.
    *
    * @return time point index, or {@code -1} if this {@code Coords} doesn't
    * contain a time point index.
    */
   default int getTimePoint() {
      return getTimePoint(-1);
   }

   int getTimePoint(int defaultTimePointIndex);

   /** Same as {@link getTimePoint}.
    * @deprecated Due to looking like the physical time rather than an index.
    */
   default int getTime() {
      return getTimePoint();
   }

   /** Shorthand for {@link getTimePoint}. */
   default int getT() {
      return getTimePoint();
   }

   /**
    * Get the Z slice index.
    *
    * Equivalent to {@code getIndex(Coords.Z_SLICE)}.
    *
    * @return Z slice index, or {@code -1} if this {@code Coords} doesn't
    * contain a Z slice index.
    */
   default int getZSlice() {
      return getZSlice(-1);
   }

   int getZSlice(int defaultZSliceIndex);

   /** Shorthand for {@link getZSlice} */
   default int getZ() {
      return getZSlice();
   }

   /**
    * Get the stage position index.
    *
    * Equivalent to {@code getIndex(Coords.STAGE_POSITION)}.
    *
    * @return stage position index, or {@code -1} if this {@code Coords}
    * doesn't contain a stage position index.
    */
   default int getStagePosition() {
      return getStagePosition(-1);
   }

   int getStagePosition(int defaultStagePositionIndex);

   /** Shorthand for {@link getStagePosition}. */
   default int getP() {
      return getStagePosition();
   }

   /**
    * Return all axes that this {@code Coords} has an index for.
    *
    * @return List of all axis
    */
   public List<String> getAxes();

   /**
    * Returns whether this coords has the given axis.
    * @param axis the axis to test for presence
    * @return true if this coords includes {@code axis}
    */
   public boolean hasAxis(String axis);

   boolean hasTimePointAxis();

   default boolean hasT() {
      return hasTimePointAxis();
   }

   boolean hasStagePositionAxis();

   default boolean hasP() {
      return hasStagePositionAxis();
   }

   boolean hasZSliceAxis();

   default boolean hasZ() {
      return hasZSliceAxis();
   }

   boolean hasChannelAxis();

   default boolean hasC() {
      return hasChannelAxis();
   }

   /**
    * Return true if this instance contains equal indices for every axis in the
    * given instance.
    *
    * @param other the instance to compare with
    * @return whether this instance is a superspace coords of {@code other}
    */
   public boolean isSuperspaceCoordsOf(Coords other);

   /**
    * Return true if the given instance contains equal indices for every axis
    * in this instance.
    *
    * @param other the instance to compare with
    * @return whether this instance is a subspace coords of {@code other}
    */
   public boolean isSubspaceCoordsOf(Coords other);

   /** @deprecated Use the equivalent {@link isSubspaceCoordsOf} instead. */
   @Deprecated
   public boolean matches(Coords alt);

   public Builder copyBuilder();

   @Deprecated
   default CoordsBuilder copy() {
      return copyBuilder();
   }

   Coords copyRemovingAxes(String... axes);
   Coords copyRetainingAxes(String... axes);
}
