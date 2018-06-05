package org.micromanager.data.internal.io;

public final class Unsigned {
   public static final int UBYTE_MAX_VALUE = 0xff;
   public static final int USHORT_MAX_VALUE = 0xffff;
   public static final long UINT_MAX_VALUE = 0xffffffffL;

   public static int from(byte u) {
      return ((short) u) & 0xff;
   }

   public static int from(short u) {
      return ((int) u) & 0xffff;
   }

   public static long from(int u) {
      return ((long) u) & 0xffffffffL;
   }
}
