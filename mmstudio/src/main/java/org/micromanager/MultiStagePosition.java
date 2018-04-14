///////////////////////////////////////////////////////////////////////////////
//PROJECT:       Micro-Manager
//SUBSYSTEM:     mmstudio
//-----------------------------------------------------------------------------
//
// DESCRIPTION:  Generalized mechanical position implementation - using multiple
//               stages.
//
// AUTHOR:       Nenad Amodaj, nenad@amodaj.com, March 8, 2007
//
// COPYRIGHT:    University of California, San Francisco, 2007
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
//
// CVS:          $Id: MultiStagePosition.java 10143 2012-10-18 19:02:04Z nico $
//
package org.micromanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mmcorej.CMMCore;
import org.micromanager.data.internal.PropertyKey;

public final class MultiStagePosition {
   private ArrayList<StagePosition> stagePosList_;
   private String label_;
   private String defaultZStage_;
   private String defaultXYStage_;
   private int gridRow_ = 0;
   private int gridCol_ = 0;
   private HashMap<String, String> properties_;

   /**
    * Default constructor.
    */
   public MultiStagePosition() {
      stagePosList_ = new ArrayList<StagePosition>();
      label_ = "Undefined";
      defaultZStage_ = "";
      defaultXYStage_ = "";
      properties_ = new HashMap<String, String>();
   }

   /**
    * Convenient constructor where the motion system consists of one XY stage and one focus stage.
    * @param xyStage name
    * @param x - coordinate in microns
    * @param y - coordinate in microns
    * @param zStage name
    * @param z - focus position in microns
    */
   public MultiStagePosition(String xyStage, double x, double y, String zStage, double z) {
      this();

      // create and add xy position
      StagePosition xyPos = new StagePosition();
      xyPos.numAxes = 2;
      xyPos.stageName = xyStage;
      xyPos.x = x;
      xyPos.y = y;
      defaultXYStage_ = xyStage;
      add(xyPos);

      // create and add z position
      StagePosition zPos = new StagePosition();
      zPos.numAxes = 1;
      zPos.stageName = zStage;
      zPos.x = z;
      defaultZStage_ = zStage;
      add(zPos);
   }

   /**
    * Copy constructor.
    * @param aMps - another instance of the MultiStagePoswition class
    * @return multistageposition
    */
   public static MultiStagePosition newInstance(MultiStagePosition aMps) {
      MultiStagePosition mps = new MultiStagePosition();
      mps.label_ = aMps.label_;
      mps.defaultXYStage_ = aMps.defaultXYStage_;
      mps.defaultZStage_ = aMps.defaultZStage_;
      mps.properties_.putAll(aMps.properties_);
      for (StagePosition sp : aMps.stagePosList_) {
         mps.add(StagePosition.newInstance(sp));
      }
      return mps;
   }

   /**
    * Add one stage position point
    * @param sp Stageposition to be added
    */
   public void add(StagePosition sp) {
      stagePosList_.add(sp);
   }

   public void remove(StagePosition sp) {
      stagePosList_.remove(sp);
   }

   /**
    * Number of stages
    * @return number of stages in this MultiStagePosition object
    */
   public int size() {
      return stagePosList_.size();
   }

   /**
    * Return stage position based on index
    * @param idx - position index
    * @return stageposition
    */
   public StagePosition get(int idx) {
      return stagePosList_.get(idx);
   }

   /**
    * Add a generalized property-value par to the position.
    * @param key
    * @param value
    */
   public void setProperty(String key, String value) {
      properties_.put(key, value);
   }

   /**
    * Return the array of property keys (names) associated with this position
    * @return array with property names
    */
   public String[] getPropertyNames() {
      return new ArrayList<String>(properties_.keySet()).
            toArray(new String[properties_.size()]);
   }

   /**
    * Checks if the position has a particular property
    * @param key
    * @return true if the position has this key
    */
   public boolean hasProperty(String key) {
      return properties_.containsKey(key);
   }

   /**
    * Returns property value for a given key (name)
    * @param key
    * @return value associated with the key
    */
   public String getProperty(String key) {
      if (properties_.containsKey(key))
         return properties_.get(key);
      else
         return null;
   }

   /**
    * Returns position for a specific stage.
    * @param stageName
    * @return position of the specified stage
    */
   public StagePosition get(String stageName) {
      for (StagePosition sp : stagePosList_) {
         if (sp.stageName.compareTo(stageName) == 0)
            return sp;
      }
      return null;
   }

   /**
    * Returns position label.
    * @return label associated with this MultiStagePosition
    */
   public String getLabel() {
      return label_;
   }

   /**
    * Sets position label (such as well name, etc.)
    * @param lab new MultiStagePosition Label
    */
   public void setLabel(String lab) {
      label_ = lab;
   }

   /**
    * Defines which stage serves as focus control
    * @param stage new focus stage
    */
   public void setDefaultZStage(String stage) {
      defaultZStage_ = stage;
   }

   public String getDefaultZStage() {
      return defaultZStage_;
   }

   public String getDefaultXYStage() {
      return defaultXYStage_;
   }

   /**
    * Defines which stage serves as the XY motion control device
    * @param stage new default XY stage
    */
   public void setDefaultXYStage(String stage) {
      defaultXYStage_ = stage;
   }

   /**
    * Moves all stages to the specified positions.
    * @param msp position to move to
    * @param core_ - microscope API
    * @throws Exception If there is an error moving the stage.
    */
   public static void goToPosition(MultiStagePosition msp, CMMCore core_) throws Exception {
      for (int i=0; i<msp.size(); i++) {
         StagePosition sp = msp.get(i);
         if (sp.numAxes == 1) {
            core_.setPosition(sp.stageName, sp.x);
         } else if (sp.numAxes == 2) {
            core_.setXYPosition(sp.stageName, sp.x, sp.y);
         }

         // wait for one device at the time
         // TODO: this should not be here
         core_.waitForDevice(sp.stageName);
      }

   }

   /**
    * Returns "X" coordinate of the position.
    * @return X position of the default XY stage
    */
   public double getX() {
      // TODO: implement more efficient position calculation
      for (StagePosition sp : stagePosList_) {
         if (sp.numAxes == 2 && sp.stageName.compareTo(defaultXYStage_) == 0) {
            return sp.x;
         }
      }
      return 0.0;
   }

   /**
    * Returns "Y" coordinate of the position.
    * @return Y position of the default XY stage
    */
  public double getY() {
      for (StagePosition sp : stagePosList_) {
         if (sp.numAxes == 2 && sp.stageName.compareTo(defaultXYStage_) == 0) {
            return sp.y;
         }
      }
      return 0.0;
   }

  /**
   * Returns "Z" - focus coordinate of the position.
    * @return Position of the default Focus stage
   */
   public double getZ() {
      for (StagePosition sp : stagePosList_) {
         if (sp.numAxes == 1 && sp.stageName.compareTo(defaultZStage_) == 0) {
            return sp.x;
         }
      }
      return 0.0;
   }

   /**
    * Sets grid parameters for the rectangular grid
    * @param row
    * @param col
    */
   public void setGridCoordinates(int row, int col) {
      gridRow_ = row;
      gridCol_ = col;
   }

   /**
    * Returns rectangular grid row.
    * @return row
    */
   public int getGridRow() {
      return gridRow_;
   }

   /**
    * Returns rectangular grid column.
    * @return column
    */
   public int getGridColumn() {
      return gridCol_;
   }

   /**
    * Compares this MultiStagePosition to another, and returns true if they
    * are equal in all aspects.
    * @param alt The MultiStagePosition to compare against.
    * @return true if every field of this MultiStagePosition matches every
    *         field of the provided MultiStagePosition, false otherwise.
    */
   @Override
   public boolean equals(Object alt) {
      if (!(alt instanceof MultiStagePosition)) {
         return false;
      }
      MultiStagePosition multiAlt = (MultiStagePosition) alt;
      if (!(label_.equals(multiAlt.getLabel()) &&
               defaultZStage_.equals(multiAlt.getDefaultZStage()) &&
               defaultXYStage_.equals(multiAlt.getDefaultXYStage()) &&
               gridRow_ == multiAlt.getGridRow() &&
               gridCol_ == multiAlt.getGridColumn() &&
               stagePosList_.size() == multiAlt.size())) {
         return false;
      }
      for (int i = 0; i < stagePosList_.size(); ++i) {
         if (!stagePosList_.get(i).equals(multiAlt.get(i))) {
            return false;
         }
      }
      for (String key : properties_.keySet()) {
         if (!properties_.get(key).equals(multiAlt.getProperty(key))) {
            return false;
         }
      }
      // And ensure they don't have any keys we don't.
      for (String key : multiAlt.getPropertyNames()) {
         if (multiAlt.getProperty(key).equals(getProperty(key))) {
            return false;
         }
      }
      return true;
   }

   @Override
   public String toString() {
      return String.format("<MultiStagePosition %s with defaults XY %s, Z %s; grid %d/%d, properties %s>",
            label_, defaultXYStage_, defaultZStage_, gridCol_, gridRow_,
            properties_);
   }

   public PropertyMap toPropertyMap() {
      PropertyMap.Builder properties = PropertyMaps.builder();
      for (Map.Entry<String, String> e : properties_.entrySet()) {
         properties.putString(e.getKey(), e.getValue());
      }
      List<PropertyMap> positions = new ArrayList<PropertyMap>();
      for (StagePosition sp : stagePosList_) {
         positions.add(sp.toPropertyMap());
      }
      return PropertyMaps.builder().
            putString(PropertyKey.MULTI_STAGE_POSITION__LABEL.key(), label_).
            putString(PropertyKey.MULTI_STAGE_POSITION__DEFAULT_XY_STAGE.key(),
                  defaultXYStage_).
            putString(PropertyKey.MULTI_STAGE_POSITION__DEFAULT_Z_STAGE.key(),
                  defaultZStage_).
            putInteger(PropertyKey.MULTI_STAGE_POSITION__GRID_ROW.key(),
                  gridRow_).
            putInteger(PropertyKey.MULTI_STAGE_POSITION__GRID_COLUMN.key(),
                  gridCol_).
            putPropertyMap(PropertyKey.MULTI_STAGE_POSITION__PROPERTIES.key(),
                  properties.build()).
            putPropertyMapList(
                  PropertyKey.MULTI_STAGE_POSITION__DEVICE_POSITIONS.key(),
                  positions).build();
   }

   public static MultiStagePosition fromPropertyMap(PropertyMap pmap) {
      MultiStagePosition ret = new MultiStagePosition();
      ret.label_ = pmap.getString(PropertyKey.MULTI_STAGE_POSITION__LABEL.
            key(), null);
      ret.defaultXYStage_ = pmap.getString(
            PropertyKey.MULTI_STAGE_POSITION__DEFAULT_XY_STAGE.key(), null);
      ret.defaultZStage_ = pmap.getString(
            PropertyKey.MULTI_STAGE_POSITION__DEFAULT_Z_STAGE.key(), null);
      ret.gridRow_ = pmap.getAsNumber(
            PropertyKey.MULTI_STAGE_POSITION__GRID_ROW.key(), 0).intValue();
      ret.gridCol_ = pmap.getAsNumber(
            PropertyKey.MULTI_STAGE_POSITION__GRID_COLUMN.key(), 0).intValue();
      ret.properties_ = new HashMap<String, String>();
      for (String key : pmap.getPropertyMap(
            PropertyKey.MULTI_STAGE_POSITION__PROPERTIES.key(),
            PropertyMaps.emptyPropertyMap()).keySet()) {
         ret.properties_.put(key, pmap.getString(key, ""));
      }
      ret.stagePosList_ = new ArrayList<StagePosition>();
      for (PropertyMap spmap : pmap.getPropertyMapList(
            PropertyKey.MULTI_STAGE_POSITION__DEVICE_POSITIONS.key())) {
         ret.stagePosList_.add(StagePosition.fromPropertyMap(spmap));
      }
      return ret;
   }
}
