package org.micromanager.data.internal.io.mmtiff;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.util.stream.Collectors;

public abstract class TiffIFDEntry {
   protected final ByteOrder byteOrder_;
   private final TiffTag tag_;
   private final TiffFieldType type_;
   private final int count_;

   //
   //
   //

   public static TiffIFDEntry read(ByteBuffer b) throws TiffFormatException, EOFException {
      TiffTag tag = TiffTag.fromTiffConstant(Unsigned.from(b.getShort()));
      TiffFieldType type = TiffFieldType.fromTiffConstant(Unsigned.from(b.getShort()));
      tag.checkType(type);

      long longCount = Unsigned.from(b.getInt());
      if (longCount > Integer.MAX_VALUE) {
         throw new TiffFormatException(
            "IFD entry count greater than INT_MAX not supported");
      }
      int count = (int) longCount;

      if (type.fitsInIFDEntry(count)) {
         ByteBuffer bb = b.slice().order(b.order());
         b.getInt();
         TiffValue value = TiffValue.read(type, count, bb);
         return new Immediate(b.order(), tag, type, count, value);
      }
      else {
         long offset = Unsigned.from(b.getInt());
         return new Pointer(b.order(), tag, type, count, offset);
      }
   }

   protected TiffIFDEntry(ByteOrder order, TiffTag tag, TiffFieldType type, int count) {
      byteOrder_ = order;
      tag_ = tag;
      type_ = type;
      count_ = count;
   }

   //
   //
   //

   public TiffTag getTag() {
      return tag_;
   }

   public TiffFieldType getType() {
      return type_;
   }

   public int getCount() {
      return count_;
   }

   public abstract TiffValue readValue(SeekableByteChannel channel) throws IOException;

   //
   //
   //

   public static class Immediate extends TiffIFDEntry {
      TiffValue value_;

      Immediate(ByteOrder order, TiffTag tag, TiffFieldType type, int count, TiffValue value) {
         super(order, tag, type, count);
         value_ = value;
      }

      public TiffValue getValue() {
         return value_;
      }

      @Override
      public TiffValue readValue(SeekableByteChannel channel) {
         return getValue();
      }
   }

   public static class Pointer extends TiffIFDEntry {
      long offset_;

      Pointer(ByteOrder order, TiffTag tag, TiffFieldType type, int count, long offset) {
         super(order, tag, type, count);
         offset_ = offset;
      }

      @Override
      public TiffValue readValue(SeekableByteChannel channel) throws IOException {
         ByteBuffer b = ByteBuffer.allocateDirect(
            getType().getElementSize() * getCount()).order(byteOrder_);
         channel.position(offset_).read(b);
         b.rewind();
         return TiffValue.read(getType(), getCount(), b);
      }
   }
}
