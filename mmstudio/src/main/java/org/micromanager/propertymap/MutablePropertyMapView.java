/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.micromanager.propertymap;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import org.micromanager.PropertyMap;

/**
 * An interface to a property-map-like object that can be modified.
 *
 * @author Mark A. Tsuchida
 */
public interface MutablePropertyMapView extends PropertyMapReadAccess, PropertyMapBuildAccess {
   /**
    * Return a set view of all of the keys in this view.
    * <p>
    * The returned view is updated when this MutablePropertyMapView changes.
    * Removing keys from the returned set view will remove those keys from the
    * MutablePropertyMapView. Adding keys is not supported.
    *
    * @return set view of property keys
    */
   @Override
   Set<String> keySet();

   /**
    * Return a property map copy of this view.
    * @return the property map copy
    */
   PropertyMap toPropertyMap();

   @Override MutablePropertyMapView putBoolean(String key, Boolean value);
   @Override MutablePropertyMapView putBooleanList(String key, boolean... values);
   @Override MutablePropertyMapView putBooleanList(String key, Iterable<Boolean> values);
   @Override MutablePropertyMapView putByte(String key, Byte value);
   @Override MutablePropertyMapView putByteList(String key, byte... values);
   @Override MutablePropertyMapView putByteList(String key, Iterable<Byte> values);
   @Override MutablePropertyMapView putShort(String key, Short value);
   @Override MutablePropertyMapView putShortList(String key, short... values);
   @Override MutablePropertyMapView putShortList(String key, Iterable<Short> values);
   @Override MutablePropertyMapView putInteger(String key, Integer value);
   @Override MutablePropertyMapView putIntegerList(String key, int... values);
   @Override MutablePropertyMapView putIntegerList(String key, Iterable<Integer> values);
   @Override MutablePropertyMapView putLong(String key, Long value);
   @Override MutablePropertyMapView putLongList(String key, long... values);
   @Override MutablePropertyMapView putLongList(String key, Iterable<Long> values);
   @Override MutablePropertyMapView putFloat(String key, Float value);
   @Override MutablePropertyMapView putFloatList(String key, float... values);
   @Override MutablePropertyMapView putFloatList(String key, Iterable<Float> values);
   @Override MutablePropertyMapView putDouble(String key, Double value);
   @Override MutablePropertyMapView putDoubleList(String key, double... values);
   @Override MutablePropertyMapView putDoubleList(String key, Iterable<Double> values);
   @Override MutablePropertyMapView putString(String key, String value);
   @Override MutablePropertyMapView putStringList(String key, String... values);
   @Override MutablePropertyMapView putStringList(String key, Iterable<String> values);
   @Override MutablePropertyMapView putUUID(String key, UUID value);
   @Override MutablePropertyMapView putUUIDList(String key, UUID... values);
   @Override MutablePropertyMapView putUUIDList(String key, Iterable<UUID> values);
   @Override MutablePropertyMapView putColor(String key, Color value);
   @Override MutablePropertyMapView putColorList(String key, Color... values);
   @Override MutablePropertyMapView putColorList(String key, Iterable<Color> values);
   @Override MutablePropertyMapView putAffineTransform(String key, AffineTransform value);
   @Override MutablePropertyMapView putAffineTransformList(String key, AffineTransform... values);
   @Override MutablePropertyMapView putAffineTransformList(String key, Iterable<AffineTransform> values);
   @Override MutablePropertyMapView putPropertyMap(String key, PropertyMap value);
   @Override MutablePropertyMapView putPropertyMapList(String key, PropertyMap... values);
   @Override MutablePropertyMapView putPropertyMapList(String key, Iterable<PropertyMap> values);
   @Override MutablePropertyMapView putRectangle(String key, Rectangle value);
   @Override MutablePropertyMapView putRectangleList(String key, Rectangle... values);
   @Override MutablePropertyMapView putRectangleList(String key, Iterable<Rectangle> values);
   @Override MutablePropertyMapView putDimension(String key, Dimension value);
   @Override MutablePropertyMapView putDimensionList(String key, Dimension... values);
   @Override MutablePropertyMapView putDimensionList(String key, Iterable<Dimension> values);
   @Override MutablePropertyMapView putPoint(String key, Point value);
   @Override MutablePropertyMapView putPointList(String key, Point... values);
   @Override MutablePropertyMapView putPointList(String key, Iterable<Point> values);
   @Override <E extends Enum<E>> MutablePropertyMapView putEnumAsString(String key, E value);
   @Override <E extends Enum<E>> MutablePropertyMapView putEnumListAsStringList(String key, E... values);
   @Override <E extends Enum<E>> MutablePropertyMapView putEnumListAsStringList(String key, Iterable<E> values);
   @Override MutablePropertyMapView putOpaqueValue(String key, PropertyMapReadAccess.OpaqueValue value);
   @Override MutablePropertyMapView putAll(PropertyMap map);
   @Override MutablePropertyMapView clear();
   @Override MutablePropertyMapView remove(String key);
   @Override MutablePropertyMapView removeAll(Collection<?> keys);
   @Override MutablePropertyMapView retainAll(Collection<?> keys);
   @Override MutablePropertyMapView replaceAll(PropertyMap map);
}
