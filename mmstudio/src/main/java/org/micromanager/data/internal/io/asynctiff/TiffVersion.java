package org.micromanager.data.internal.io.asynctiff;

import com.google.common.base.Preconditions;
import org.micromanager.data.internal.io.Unsigned;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/*
 * Design notes:
 * - This is where we collect knowledge about version-specific binary layout.
 * - No async code or file access in this class, please.
 */

/**
 * TIFF version, such as classical (32-bit) TIFF or BigTIFF.
 */
public enum TiffVersion {
   TIFF32((short) 42, 4) {
      @Override
      int getHeaderOffsetSizeFieldSize() {
         return 0;
      }

      @Override
      int getHeaderReservedFieldSize() {
         return 0;
      }

      @Override
      int getIFDEntryCountSize() {
         return 2;
      }

      @Override
      long readOffset(ByteBuffer b) {
         return Unsigned.from(b.getInt());
      }

      @Override
      long readIFDEntryCount(ByteBuffer b) {
         return Unsigned.from(b.getShort());
      }

      @Override
      TiffHeader readHeader(ByteOrder order, ByteBuffer b) throws TiffFormatException {
         long offset = readOffset(b);
         return DefaultTiffHeader.createFromReadData(TiffLayout.create(order, this), offset);
      }

      @Override
      void writeOffset(ByteBuffer dest, long offset) {
         Preconditions.checkArgument(offset >= 0 && offset <= Unsigned.UINT_MAX_VALUE);
         dest.putInt((int) offset);
      }

      @Override
      void writeOffset(ByteBuffer dest, int position, long offset) {
         Preconditions.checkArgument(offset >= 0 && offset <= Unsigned.UINT_MAX_VALUE);
         dest.putInt(position, (int) offset);
      }

      @Override
      void writeHeaderMiddlePart(ByteBuffer dest) {
         // No middle part
      }

      @Override
      void writeIFDEntryCount(ByteBuffer dest, long count) {
         Preconditions.checkArgument(count >= 0 && count < Unsigned.USHORT_MAX_VALUE);
         dest.putShort((short) count);
      }
   },

   TIFF64((short) 43, 8) {
      @Override
      int getHeaderOffsetSizeFieldSize() {
         return 2;
      }

      @Override
      int getHeaderReservedFieldSize() {
         return 2;
      }

      @Override
      int getIFDEntryCountSize() {
         return getOffsetSize();
      }

      @Override
      long readOffset(ByteBuffer b) {
         return b.getLong();
      }

      @Override
      long readIFDEntryCount(ByteBuffer b) {
         return b.getLong();
      }

      @Override
      TiffHeader readHeader(ByteOrder order, ByteBuffer b) throws TiffFormatException {
         int offsetSize = Unsigned.from(b.getShort());
         if (offsetSize != getOffsetSize()) {
            throw new TiffFormatException(String.format(
               "Unsupported BigTIFF offset size (expected %d; got %d)",
               getOffsetSize(), offsetSize));
         }

         int shouldBeZero = Unsigned.from(b.getShort());
         if (shouldBeZero != 0) {
            throw new TiffFormatException(String.format(
               "Unsupported value in BigTIFF header reserved field (expected 0; got %d)",
               shouldBeZero));
         }
         long offset = readOffset(b);
         return DefaultTiffHeader.createFromReadData(TiffLayout.create(order, this), offset);
      }

      @Override
      void writeOffset(ByteBuffer dest, long offset) {
         Preconditions.checkArgument(offset >= 0);
         dest.putLong(offset);
      }

      @Override
      void writeOffset(ByteBuffer dest, int position, long offset) {
         Preconditions.checkArgument(offset >= 0);
         dest.putLong(position, offset);
      }

      @Override
      void writeHeaderMiddlePart(ByteBuffer dest) {
         dest.putShort((short) getOffsetSize()).putShort((short) 0);
      }

      @Override
      void writeIFDEntryCount(ByteBuffer dest, long count) {
         Preconditions.checkArgument(count >= 0);
         dest.putLong(count);
      }
   },
   ;

   private final short value_;
   private final int offsetSize_;

   TiffVersion(short value, int offsetSize) {
      value_ = value;
      offsetSize_ = offsetSize;
   }

   static int getHeaderCommonPartSize() {
      return 2 + // byte order mark
         2; // version (magic)
   }

   static TiffVersion fromHeaderValue(short value) throws TiffFormatException {
      for (TiffVersion v : values()) {
         if (value == v.getHeaderValue()) {
            return v;
         }
      }
      throw new TiffFormatException(String.format("Unknown TIFF version (magic): %d", value));
   }

   //
   //
   //

   short getHeaderValue() {
      return value_;
   }

   public int getHeaderSize() {
      return getHeaderCommonPartSize() +
         getHeaderOffsetSizeFieldSize() +
         getHeaderReservedFieldSize() +
         getOffsetSize();
   }

   public int getIFDEntrySize() {
      return 2 + 2 + getOffsetSize() + getOffsetSize();
   }

   public int getOffsetSize() {
      return offsetSize_;
   }

   public long readValueCount(ByteBuffer b) {
      return readOffset(b);
   }

   public void writeValueCount(ByteBuffer b, long count) {
      writeOffset(b, count);
   }

   abstract int getHeaderOffsetSizeFieldSize();
   abstract int getHeaderReservedFieldSize();
   abstract int getIFDEntryCountSize();

   abstract long readOffset(ByteBuffer b);
   abstract long readIFDEntryCount(ByteBuffer b);
   abstract TiffHeader readHeader(ByteOrder order, ByteBuffer b) throws TiffFormatException;
   abstract void writeOffset(ByteBuffer dest, long offset);
   abstract void writeOffset(ByteBuffer dest, int position, long offset);
   abstract void writeIFDEntryCount(ByteBuffer b, long count);
   abstract void writeHeaderMiddlePart(ByteBuffer dest);
}
