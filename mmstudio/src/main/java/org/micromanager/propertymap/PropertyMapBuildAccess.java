package org.micromanager.propertymap;

import org.micromanager.PropertyMap;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.Collection;
import java.util.UUID;

public interface PropertyMapBuildAccess {
   // Primitive
   PropertyMapBuildAccess putBoolean(String key, Boolean value);

   PropertyMapBuildAccess putBooleanList(String key, boolean... values);

   PropertyMapBuildAccess putBooleanList(String key, Iterable<Boolean> values);

   PropertyMapBuildAccess putByte(String key, Byte value);

   PropertyMapBuildAccess putByteList(String key, byte... values);

   PropertyMapBuildAccess putByteList(String key, Iterable<Byte> values);

   PropertyMapBuildAccess putShort(String key, Short value);

   PropertyMapBuildAccess putShortList(String key, short... values);

   PropertyMapBuildAccess putShortList(String key, Iterable<Short> values);

   PropertyMapBuildAccess putInteger(String key, Integer value);

   PropertyMapBuildAccess putIntegerList(String key, int... values);

   PropertyMapBuildAccess putIntegerList(String key, Iterable<Integer> values);

   PropertyMapBuildAccess putLong(String key, Long value);

   PropertyMapBuildAccess putLongList(String key, long... values);

   PropertyMapBuildAccess putLongList(String key, Iterable<Long> values);

   PropertyMapBuildAccess putFloat(String key, Float value);

   PropertyMapBuildAccess putFloatList(String key, float... values);

   PropertyMapBuildAccess putFloatList(String key, Iterable<Float> values);

   PropertyMapBuildAccess putDouble(String key, Double value);

   PropertyMapBuildAccess putDoubleList(String key, double... values);

   PropertyMapBuildAccess putDoubleList(String key, Iterable<Double> values);

   // Immutable
   PropertyMapBuildAccess putString(String key, String value);

   PropertyMapBuildAccess putStringList(String key, String... values);

   PropertyMapBuildAccess putStringList(String key, Iterable<String> values);

   PropertyMapBuildAccess putUUID(String key, UUID value);

   PropertyMapBuildAccess putUUIDList(String key, UUID... values);

   PropertyMapBuildAccess putUUIDList(String key, Iterable<UUID> values);

   PropertyMapBuildAccess putColor(String key, Color value);

   PropertyMapBuildAccess putColorList(String key, Color... values);

   PropertyMapBuildAccess putColorList(String key, Iterable<Color> values);

   PropertyMapBuildAccess putAffineTransform(String key, AffineTransform value);

   PropertyMapBuildAccess putAffineTransformList(String key, AffineTransform... values);

   PropertyMapBuildAccess putAffineTransformList(String key, Iterable<AffineTransform> values);

   PropertyMapBuildAccess putPropertyMap(String key, PropertyMap value);

   PropertyMapBuildAccess putPropertyMapList(String key, PropertyMap... values);

   PropertyMapBuildAccess putPropertyMapList(String key, Iterable<PropertyMap> values);

   // TODO Java 8 java.time.ZonedDateTime and LocalDateTime

   // Mutable
   PropertyMapBuildAccess putRectangle(String key, Rectangle value);

   PropertyMapBuildAccess putRectangleList(String key, Rectangle... values);

   PropertyMapBuildAccess putRectangleList(String key, Iterable<Rectangle> values);

   PropertyMapBuildAccess putDimension(String key, Dimension value);

   PropertyMapBuildAccess putDimensionList(String key, Dimension... values);

   PropertyMapBuildAccess putDimensionList(String key, Iterable<Dimension> values);

   PropertyMapBuildAccess putPoint(String key, Point value);

   PropertyMapBuildAccess putPointList(String key, Point... values);

   PropertyMapBuildAccess putPointList(String key, Iterable<Point> values);

   // Enums-as-strings
   <E extends Enum<E>> PropertyMapBuildAccess putEnumAsString(String key, E value);

   <E extends Enum<E>> PropertyMapBuildAccess putEnumListAsStringList(String key, E... values);

   <E extends Enum<E>> PropertyMapBuildAccess putEnumListAsStringList(String key, Iterable<E> values);

   /**
    * (Advanced) Add a value without knowing its type.
    *
    * Use of this method should be reserved for special-purpose code
    * dealing with interconversion with other data formats. Do no use in
    * everyday programming.
    *
    * @param key the key
    * @param value the value
    * @return this builder
    */
   PropertyMapBuildAccess putOpaqueValue(String key, PropertyMapReadAccess.OpaqueValue value);

   PropertyMapBuildAccess putAll(PropertyMap map);

   PropertyMapBuildAccess clear();

   PropertyMapBuildAccess remove(String key);

   PropertyMapBuildAccess removeAll(Collection<?> keys);

   PropertyMapBuildAccess retainAll(Collection<?> keys);

   PropertyMapBuildAccess replaceAll(PropertyMap map);
}
