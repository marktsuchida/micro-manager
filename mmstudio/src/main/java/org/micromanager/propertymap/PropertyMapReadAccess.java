package org.micromanager.propertymap;

import org.micromanager.PropertyMap;
import org.micromanager.PropertyMaps;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface PropertyMapReadAccess {
   /**
    * Return an unmodifiable set containing all of the keys.
    * @return the set of keys
    */
   Set<String> keySet();

   boolean containsKey(String key);

   boolean containsAll(Collection<?> keys);

   boolean isEmpty();

   int size();

   /**
    * (Advanced) Return a class indicating the type of the value for the given key.
    * <p>
    * This method is intended for special code that handles serialization or
    * conversion of property maps. In most cases, code that uses PropertyMap
    * as a container for settings or data should use a fixed, pre-determined
    * type for each key.
    * <p>
    * For object (non-primitive) types, the class is returned (e.g. {@code
    * String.class}, {@code PropertyMap.class}).
    * <p>
    * For primitive types, the primitive type class is returned (e.g. {@code
    * int.class}, {@code float.class}). Note that {@code int.class !=
    * Integer.class}.
    * <p>
    * For collection (plural) types, the corresponding array type is returned
    * (e.g. {@code int[].class}, {@code String[].class}).
    * <p>
    * Keys stored as "EnumAsString" return {@code String.class}.
    *
    * @param key
    * @return the class representing the type of the value for key
    * @throws IllegalArgumentException if {@code key} is not found
    */
   Class<?> getValueTypeForKey(String key);

   /**
    * (Advanced) Get a value without indicating its type.
    *
    * Use of this method should be restricted to special-purpose code dealing
    * with interconversion with other data formats. Do not use in everyday
    * programming.
    *
    * @param key the key
    * @return the value
    */
   OpaqueValue getAsOpaqueValue(String key);

   String getValueAsString(String key, String aDefault);

   boolean containsBoolean(String key);

   boolean getBoolean(String key, boolean aDefault);

   boolean containsBooleanList(String key);

   boolean[] getBooleanList(String key, boolean... defaults);

   List<Boolean> getBooleanList(String key, Iterable<Boolean> defaults);

   boolean containsByte(String key);

   byte getByte(String key, byte aDefault);

   boolean containsByteList(String key);

   byte[] getByteList(String key, byte... defaults);

   List<Byte> getByteList(String key, Iterable<Byte> defaults);

   boolean containsShort(String key);

   short getShort(String key, short aDefault);

   boolean containsShortList(String key);

   short[] getShortList(String key, short... defaults);

   List<Short> getShortList(String key, Iterable<Short> defaults);

   boolean containsInteger(String key);

   int getInteger(String key, int aDefault);

   boolean containsIntegerList(String key);

   int[] getIntegerList(String key, int... defaults);

   List<Integer> getIntegerList(String key, Iterable<Integer> defaults);

   boolean containsLong(String key);

   long getLong(String key, long aDefault);

   boolean containsLongList(String key);

   long[] getLongList(String key, long... defaults);

   List<Long> getLongList(String key, Iterable<Long> defaults);

   boolean containsFloat(String key);

   float getFloat(String key, float aDefault);

   boolean containsFloatList(String key);

   float[] getFloatList(String key, float... defaults);

   List<Float> getFloatList(String key, Iterable<Float> defaults);

   boolean containsDouble(String key);

   double getDouble(String key, double aDefault);

   boolean containsDoubleList(String key);

   double[] getDoubleList(String key, double... defaults);

   List<Double> getDoubleList(String key, Iterable<Double> defaults);

   boolean containsNumber(String key);

   /**
    * Retrieve a numerical value, without checking its specific type.
    * <p>
    * This method may be useful for providing backward compatibility (for
    * example, if a value currently stored as a {@code long} used to be stored
    * as an {@code int}). It should not be used when the type-specific get
    * method can be used.
    * <p>
    * Code that calls this method should be prepared to handle any of the
    * following types correctly: {@code Byte, Short, Integer, Long, Float,
    * Double}.
    *
    * @param key
    * @param aDefault
    * @return
    */
   Number getAsNumber(String key, Number aDefault);

   boolean containsNumberList(String key);

   List<Number> getAsNumberList(String key, Number... defaults);

   List<Number> getAsNumberList(String key, Iterable<Number> defaults);

   boolean containsString(String key);

   String getString(String key, String aDefault);

   boolean containsStringList(String key);

   List<String> getStringList(String key, String... defaults);

   List<String> getStringList(String key, Iterable<String> defaults);

   boolean containsUUID(String key);

   UUID getUUID(String key, UUID aDefault);

   boolean containsUUIDList(String key);

   List<UUID> getUUIDList(String key, UUID... defaults);

   List<UUID> getUUIDList(String key, Iterable<UUID> defaults);

   boolean containsColor(String key);

   Color getColor(String key, Color aDefault);

   boolean containsColorList(String key);

   List<Color> getColorList(String key, Color... defaults);

   List<Color> getColorList(String key, Iterable<Color> defaults);

   boolean containsAffineTransform(String key);

   AffineTransform getAffineTransform(String key, AffineTransform aDefault);

   boolean containsAffineTransformList(String key);

   List<AffineTransform> getAffineTransformList(String key, AffineTransform... defaults);

   List<AffineTransform> getAffineTransformList(String key, Iterable<AffineTransform> defaults);

   boolean containsPropertyMap(String key);

   PropertyMap getPropertyMap(String key, PropertyMap aDefault);

   boolean containsPropertyMapList(String key);

   List<PropertyMap> getPropertyMapList(String key, PropertyMap... defaults);

   List<PropertyMap> getPropertyMapList(String key, Iterable<PropertyMap> defaults);

   boolean containsRectangle(String key);

   Rectangle getRectangle(String key, Rectangle aDefault);

   boolean containsRectangleList(String key);

   List<Rectangle> getRectangleList(String key, Rectangle... defaults);

   List<Rectangle> getRectangleList(String key, Iterable<Rectangle> defaults);

   boolean containsDimension(String key);

   Dimension getDimension(String key, Dimension aDefault);

   boolean containsDimensionList(String key);

   List<Dimension> getDimensionList(String key, Dimension... defaults);

   List<Dimension> getDimensionList(String key, Iterable<Dimension> defaults);

   boolean containsPoint(String key);

   Point getPoint(String key, Point aDefault);

   boolean containsPointList(String key);

   List<Point> getPointList(String key, Point... defaults);

   List<Point> getPointList(String key, Iterable<Point> defaults);

   <E extends Enum<E>> boolean containsStringForEnum(String key, Class<E> enumType);

   /**
    * Get a string value, converted to an enum value.
    * <p>
    * The property map does not record the specific class of enum values. It is
    * the caller's responsibility to specify the correct enum class.
    * <p>
    * If the value stored in the property map is not one of the allowed values
    * for {@code enumType}, {@code aDefault} will be returned.
    *
    * @param <E> the enum class
    * @param key the property key
    * @param enumType the enum class
    * @param aDefault a default value to return if the key is missing or
    * the stored value is an enum but not a known value for the given enum
    * class.
    * @return the enum value for {@code key}
    */
   <E extends Enum<E>> E getStringAsEnum(String key, Class<E> enumType, E aDefault);

   <E extends Enum<E>> boolean containsStringListForEnumList(String key, Class<E> enumType);

   <E extends Enum<E>> List<E> getStringListAsEnumList(String key, Class<E> enumType, E... defaults);

   /**
    * Get a collection of strings, converted to a collection of enum values.
    * <p>
    * The property map does not record the specific class of enum values. It is
    * the caller's responsibility to specify the correct enum class.
    * <p>
    * Unless all of the values stored in the property map are allowed values of
    * {@code enumType}, {@code defaults} will be returned.
    *
    * @param <E> the enum class
    * @param key the property key
    * @param enumType the enum class
    * @param defaults a default collection
    * @return the list of enum values for {@code key}
    */
   <E extends Enum<E>> List<E> getStringListAsEnumList(String key, Class<E> enumType, Iterable<E> defaults);

   /**
    * Create a JSON representation of this property map.
    * <p>
    * To create a property map back from the JSON representation, see {@link
    * PropertyMaps#fromJSON}.
    *
    * @return the JSON-serialized contents
    */
   String toJSON();

   /**
    * (Advanced) A value that can be stored in a property map.
    *
    * This is only used for interchange purposes, for example when performing
    * bulk operations on property map. It should not be used in everyday code,
    * where it would defeat the whole point of having the property map data
    * structure.
    */
   abstract class OpaqueValue {
      public abstract Class<?> getValueType();
   }
}
