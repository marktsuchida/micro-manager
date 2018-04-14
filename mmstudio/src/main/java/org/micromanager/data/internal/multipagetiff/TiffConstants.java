package org.micromanager.data.internal.multipagetiff;

import ij.io.TiffDecoder;

import java.nio.ByteOrder;

final class TiffConstants {
   private TiffConstants() {}

   // 32-bit TIFF file limited to 4GiB
   static final long MAX_FILE_SIZE = 4L << 30;

   // TODO Questionable
   static final int DISPLAY_SETTINGS_BYTES_PER_CHANNEL = 256;
   static final long SPACE_FOR_COMMENTS = 1L << 20;

   // MM-specific magic numbers
   static final int INDEX_MAP_OFFSET_HEADER = 54773648;
   static final int INDEX_MAP_HEADER = 3453623;
   static final int DISPLAY_SETTINGS_OFFSET_HEADER = 483765892;
   static final int DISPLAY_SETTINGS_HEADER = 347834724;
   static final int COMMENTS_OFFSET_HEADER = 99384722;
   static final int COMMENTS_HEADER = 84720485;

   // TODO Questionable
   static final char ENTRIES_PER_IFD = 13;

   // TIFF Tags
   static final char WIDTH = 256;
   static final char HEIGHT = 257;
   static final char BITS_PER_SAMPLE = 258;
   static final char COMPRESSION = 259;
   static final char PHOTOMETRIC_INTERPRETATION = 262;
   static final char IMAGE_DESCRIPTION = 270;
   static final char STRIP_OFFSETS = 273;
   static final char SAMPLES_PER_PIXEL = 277;
   static final char ROWS_PER_STRIP = 278;
   static final char STRIP_BYTE_COUNTS = 279;
   static final char X_RESOLUTION = 282;
   static final char Y_RESOLUTION = 283;
   static final char RESOLUTION_UNIT = 296;
   static final char IJ_METADATA_BYTE_COUNTS = TiffDecoder.META_DATA_BYTE_COUNTS;
   static final char IJ_METADATA = TiffDecoder.META_DATA;
   static final char MM_METADATA = 51123;

   static final int SUMMARY_MD_HEADER = 2355492;

   static final ByteOrder BYTE_ORDER = ByteOrder.nativeOrder();
}
