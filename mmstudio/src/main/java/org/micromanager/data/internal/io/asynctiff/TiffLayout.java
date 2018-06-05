package org.micromanager.data.internal.io.asynctiff;

import java.nio.ByteOrder;

/**
 * The binary layout of a TIFF file, consisting of the byte order and version.
 */
class TiffLayout {
   private final ByteOrder order_;
   private final TiffVersion version_;

   public static TiffLayout create(ByteOrder order, TiffVersion version) {
      return new TiffLayout(order, version);
   }

   private TiffLayout(ByteOrder order, TiffVersion version) {
      order_ = order;
      version_ = version;
   }

   public ByteOrder order() {
      return order_;
   }

   public TiffVersion version() {
      return version_;
   }

   @Override
   public String toString() {
      return String.format("<TiffLayout %s, %s>", order(), version());
   }
}
