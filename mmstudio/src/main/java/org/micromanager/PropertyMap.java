package org.micromanager;

import org.micromanager.propertymap.PropertyMapBuildAccess;
import org.micromanager.propertymap.PropertyMapReadAccess;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * An immutable typed key-value store for various settings.
 *
 * A property map can store string-keyed values of various primitive types,
 * nested property maps, and uniformly typed (ordered) collections of values.
 * Because it is immutable, it can be passed around between objects without
 * worrying about  thread safety. Null values cannot be stored.
 * <p>
 * To create a PropertyMap, use {@link PropertyMaps#builder()}.
 * <p>
 * Property maps can be converted to a specific JSON format and back in a
 * type-preserving manner.
 * <p>
 * Property maps are similar in concept to {@link java.util.Properties} but
 * stores typed values rather than just strings, and supports storage of nested
 * property maps.
 * <p>
 * <strong>Methods to access primitive types</strong>
 * <pre><code>
 * PropertyMap pm = ...
 *
 * pm.containsLong("key"); // Returns true if "key" exists and type is long
 *
 * pm.getLong("key", 0L); // Returns 0L if "key" is missing;
 *                        // Throws ClassCastException if value exists and is wrong type
 *
 * pm.containsLongList("key2"); // Returns true if "key2" exists and type is list of longs
 *
 * pm.getLongList("key2"); // Returns empty long[] if "key2" is missing
 *                         // Throws ClassCastException if value exists and is wrong type
 *
 * pm.getLongList("key2", 0L, 1L, 2L); // If "key2" is missing, returns new long[] { 0, 1, 2 }
 *
 * pm.getLongList("key2", Collections.emptyList()); // If "key2" is missing, returns the empty list
 *
 * pm.getLongList("key2", null); // If "key2 is missing, returns null
 * </code></pre>
 * Note that, for the get...List methods, the returned value is
 * an array ({@code long[]} if the default value was given as an array or varargs,
 * whereas it is a list ({@code List<long>}) if the
 * default value was given as any collection or iterable.
 * The same pattern applies to {@code boolean}, {@code byte}, {@code short},
 * {@code int}, {@code long}, {@code float}, and {@code double}. (No {@code
 * char} or {@code void}; this is intentional.)
 * <p>
 * <strong>Methods to access primitive types</strong><br>
 * <pre><code>
 * pm.containsString("key3");
 * pm.getString("key3", "foo");
 * pm.containsStringList("key4");
 * // The following both return List{@literal <}String{@literal >}:
 * pm.getStringList("key4", "a", "b", "c"); // Default = List of "a", "b", "c"
 * pm.getStringList("key4", listOfStrings);
 * </code></pre>
 * This differs from the case of primitive types in that the collection (plural)
 * get methods return a {@code List} regardless of the type of the default value.
 * The same pattern applies to {@code String}, {@code Color}, {@code
 * AffineTransform}, {@code Rectangle}, {@code Dimension}, {@code Point}, and
 * nested {@code PropertyMap}.
 * <p>
 * <strong>Methods to access enum types</strong><br>
 * Enum values can be stored by automatically converting to {@code String}.
 * See {@link #containsStringForEnum}, {@link #containsStringListForEnumList},
 * {@link #getStringAsEnum}, and {@link #getStringListAsEnumList}. This is useful for
 * storing multiple-choice settings
 * <p>
 * For how to insert values of the various types, see {@link PropertyMap.Builder}.
 */
public interface PropertyMap extends PropertyMapReadAccess {

   /**
    * Builder for {@code PropertyMap}.
    * <p>
    * See {@link PropertyMaps#builder()} for a usage example.
    */
   interface Builder extends PropertyMapBuildAccess, PropertyMapBuilder {
      // Note: we could conceivably have a single 'put()' method with
      // overloaded parameter types. We avoid this, because code using property
      // maps generally should be conscious of the type choice. The type will
      // need to be known when getting the value out of the map, so it is best
      // to get the compiler to enforce the type when building.

      // MAINTAINER NOTE: These methods should be kept in sync with those of
      // UserProfile. Also note that LegacyMM2BetaUserProfileDeserializer (and
      // possibly others) determine method names reflectively according to a
      // strict pattern.

      /**
       * Create the property map.
       * @return the new property map
       */
      @Override
      PropertyMap build();


      // Deprecated methods, repeated here to add javadoc

      /** @deprecated Use {@link #putStringList} instead. */
      @Deprecated
      @Override
      PropertyMapBuilder putStringArray(String key, String[] values);
      /** @deprecated Use {@link #putInteger(java.lang.String, java.lang.Integer)} instead. */
      @Deprecated
      @Override
      PropertyMapBuilder putInt(String key, Integer value);
      /** @deprecated Use {@link #putIntegerList} instead. */
      @Deprecated
      @Override
      PropertyMapBuilder putIntArray(String key, Integer[] values);
      /** @deprecated Use {@link #putLongList} instead. */
      @Deprecated
      @Override
      PropertyMapBuilder putLongArray(String key, Long[] values);
      /** @deprecated Use {@link #putDoubleList} instead. */
      @Deprecated
      @Override
      PropertyMapBuilder putDoubleArray(String key, Double[] values);
      /** @deprecated Use {@link #putBooleanList} instead. */
      @Deprecated
      @Override
      PropertyMapBuilder putBooleanArray(String key, Boolean[] values);
   }

   /**
    * Legacy builder interface. This type will be deleted in the future.
    * @deprecated Use {@link PropertyMap.Builder} instead.
    */
   @Deprecated
   interface PropertyMapBuilder {
      @Deprecated
      PropertyMap build();
      @Deprecated
      PropertyMapBuilder putString(String key, String value);
      @Deprecated
      PropertyMapBuilder putStringArray(String key, String[] values);
      @Deprecated
      PropertyMapBuilder putInt(String key, Integer value);
      @Deprecated
      PropertyMapBuilder putIntArray(String key, Integer[] values);
      @Deprecated
      PropertyMapBuilder putLong(String key, Long value);
      @Deprecated
      PropertyMapBuilder putLongArray(String key, Long[] values);
      @Deprecated
      PropertyMapBuilder putDouble(String key, Double value);
      @Deprecated
      PropertyMapBuilder putDoubleArray(String key, Double[] values);
      @Deprecated
      PropertyMapBuilder putBoolean(String key, Boolean value);
      @Deprecated
      PropertyMapBuilder putBooleanArray(String key, Boolean[] values);
      @Deprecated
      PropertyMapBuilder putPropertyMap(String key, PropertyMap values);
   }

   /**
    * Return a builder initialized with a copy of this property map.
    * @return the copy builder
    */
   Builder copyBuilder();


   /**
    * Save to a file as JSON.
    *
    * @param file the file to write to
    * @param overwrite if false and file exists, don't write and return false
    * @param createBackup if true and overwriting, first rename the existing
    * file by appending "~" to its name
    * @return true if the file was successfully written
    * @throws IOException if there was an error writing to the file
    * @deprecated A way to replace this functionality will be introduced.
    */
   @Deprecated
   boolean saveJSON(File file, boolean overwrite, boolean createBackup) throws IOException;
   // Reason for deprecation:
   // - The boolean retval is ridiculous given the throws IOException.
   // - "Write a file, perhaps overwriting, perhaps atomically, perhaps
   // creating backup" is a common enough task that it should be its own,
   // composable utility, not something built into PropertyMap.
   // - Does e.g. commons-io already provide something like that?
   // - OTOH, it might be worth providing a method that writes to a given
   // stream, instead of converting the entire map to String.


   //
   // Deprecated old methods (cumbersome to use correctly due to boxed types)
   //

   /**
    * @deprecated Use {@link #copyBuilder} instead.
    * @return
    */
   @Deprecated
   public PropertyMapBuilder copy();

   @Deprecated
   public String getString(String key);
   @Deprecated
   public String[] getStringArray(String key);
   @Deprecated
   public String[] getStringArray(String key, String[] aDefault);
   @Deprecated
   public Integer getInt(String key);
   @Deprecated
   public Integer getInt(String key, Integer aDefault);
   @Deprecated
   public Integer[] getIntArray(String key);
   @Deprecated
   public Integer[] getIntArray(String key, Integer[] aDefault);
   @Deprecated
   public Long getLong(String key);
   @Deprecated
   public Long getLong(String key, Long aDefault);
   @Deprecated
   public Long[] getLongArray(String key);
   @Deprecated
   public Long[] getLongArray(String key, Long[] aDefault);
   @Deprecated
   public Double getDouble(String key);
   @Deprecated
   public Double getDouble(String key, Double aDefault);
   @Deprecated
   public Double[] getDoubleArray(String key);
   @Deprecated
   public Double[] getDoubleArray(String key, Double[] aDefault);
   @Deprecated
   public Boolean getBoolean(String key);
   @Deprecated
   public Boolean getBoolean(String key, Boolean aDefault);
   @Deprecated
   public Boolean[] getBooleanArray(String key);
   @Deprecated
   public Boolean[] getBooleanArray(String key, Boolean[] aDefault);
   @Deprecated
   public PropertyMap getPropertyMap(String key);

   @Deprecated
   public PropertyMap merge(PropertyMap alt);

   @Deprecated
   public Set<String> getKeys();

   @Deprecated
   public Class getPropertyType(String key);

   @Deprecated
   public void save(String path) throws IOException;

   /**
    * @deprecated If necessary, catch ClassCastException instead.
    */
   @Deprecated
   class TypeMismatchException extends ClassCastException {
      /**
       * @param desc
       * @deprecated Constructor should not have been part of API.
       */
      @Deprecated
      public TypeMismatchException(String desc) {
         super(desc);
      }
   }
}
