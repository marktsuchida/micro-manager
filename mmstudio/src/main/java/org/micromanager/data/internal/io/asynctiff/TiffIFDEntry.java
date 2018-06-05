package org.micromanager.data.internal.io.asynctiff;

import org.micromanager.data.internal.io.Alignment;
import org.micromanager.data.internal.io.Async;
import org.micromanager.data.internal.io.BufferedPositionGroup;
import org.micromanager.data.internal.io.UnbufferedPosition;
import org.micromanager.data.internal.io.Unsigned;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public abstract class TiffIFDEntry {
   protected final TiffLayout layout_;
   private final TiffTag tag_;
   private final TiffFieldType type_;
   private final long count_;

   //
   //
   //

   public static TiffIFDEntry read(TiffLayout layout, ByteBuffer b) throws TiffFormatException, EOFException {
      TiffTag tag = TiffTag.fromTiffConstant(Unsigned.from(b.getShort()));
      TiffFieldType type = TiffFieldType.fromTiffConstant(Unsigned.from(b.getShort()));
      tag.checkType(type);

      long count = layout.version().readValueCount(b);

      if (type.getElementSize() * count <= layout.version().getOffsetSize()) {
         ByteBuffer bb = b.slice().order(layout.order());
         b.position(b.position() + layout.version().getOffsetSize());
         TiffValue value = TiffValue.read(type, (int) count, bb);
         return new Immediate(layout, tag, type, count, value);
      }

      // BigTIFF can have 64-bit count but our implementation cannot handle it
      // due to the size limit of ByteBuffer (This limitation can in principle
      // be removed)
      if (type.getElementSize() * count > Integer.MAX_VALUE) {
         throw new TiffFormatException("Tag data size exceeds limit supported by this implementation");
      }

      long offset = layout.version().readOffset(b);
      return new Pointer(layout, tag, type, count, offset);
   }

   public static TiffIFDEntry createForWrite(TiffLayout layout,
                                             TiffTag tag,
                                             TiffValue value,
                                             TiffOffsetFieldGroup fieldGroup) {
      // BigTIFF can have 64-bit count but our implementation cannot handle it
      // due to the size limit of ByteBuffer (This limitation can in principle
      // be removed)
      if (value.getByteCount() > Integer.MAX_VALUE) {
         throw new UnsupportedOperationException("Tag data size exceeds limit supported by this implementation");
      }

      if (value.getByteCount() <= layout.version().getOffsetSize()) {
         return new Immediate(layout, tag, value);
      }

      TiffOffsetField offsetField = TiffOffsetField.create("Value of " + tag);
      fieldGroup.add(offsetField);
      return new Pointer(layout, tag, value, offsetField);
   }

   protected TiffIFDEntry(TiffLayout layout, TiffTag tag, TiffFieldType type, long count) {
      layout_ = layout;
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

   public long getCount() {
      return count_;
   }

   public abstract CompletionStage<TiffValue> readValue(AsynchronousFileChannel chan);

   public abstract CompletionStage<Void> writeValue(AsynchronousFileChannel chan);

   public abstract void writeValue(ByteBuffer dest, BufferedPositionGroup posGroup);

   public abstract void write(ByteBuffer dest, BufferedPositionGroup posGroup);

   //
   //
   //

   public static class Immediate extends TiffIFDEntry {
      TiffValue value_;

      // Read
      private Immediate(TiffLayout layout, TiffTag tag, TiffFieldType type, long count, TiffValue value) {
         super(layout, tag, type, count);
         value_ = value;
      }

      // Writing
      private Immediate(TiffLayout layout, TiffTag tag, TiffValue value) {
         super(layout, tag, value.getTiffType(), value.getCount());
         value_ = value;
      }

      @Override
      public CompletionStage<TiffValue> readValue(AsynchronousFileChannel chan) {
         return CompletableFuture.completedFuture(value_);
      }

      @Override
      public CompletionStage<Void> writeValue(AsynchronousFileChannel chan) {
         return CompletableFuture.completedFuture(null);
      }

      @Override
      public void writeValue(ByteBuffer dest, BufferedPositionGroup posGroup) {
         // no-op
      }

      @Override
      public void write(ByteBuffer dest, BufferedPositionGroup posGroup) {
         dest.putShort((short) getTag().getTiffConstant()).
            putShort((short) getType().getTiffConstant());
         layout_.version().writeValueCount(dest, getCount());
         value_.writeAndPad(dest, posGroup, layout_.version().getOffsetSize());
      }
   }

   public static class Pointer extends TiffIFDEntry {
      private TiffValue value_;
      private final TiffOffsetField valueOffset_;

      // Read
      private Pointer(TiffLayout layout, TiffTag tag, TiffFieldType type, long count, long offset) {
         super(layout, tag, type, count);

         valueOffset_ = TiffOffsetField.forOffsetValue(
            UnbufferedPosition.at(offset),
            "Read-only TiffIFDEntry.Pointer value");
      }

      // Writing
      private Pointer(TiffLayout layout, TiffTag tag, TiffValue value, TiffOffsetField valueOffset) {
         super(layout, tag, value.getTiffType(), value.getCount());
         value_ = value;
         valueOffset_ = valueOffset;
      }

      private int dataSize() {
         // We are assuming that the size has been checked to fit in int
         return (int) (getType().getElementSize() * getCount());
      }

      @Override
      public CompletionStage<TiffValue> readValue(AsynchronousFileChannel chan) {
         ByteBuffer buffer = ByteBuffer.allocateDirect(dataSize()).
            order(layout_.order());
         return Async.read(chan, buffer, valueOffset_.getOffsetValue().get()).
            thenComposeAsync(b -> {
               b.rewind();
               try {
                  return CompletableFuture.completedFuture(
                     TiffValue.read(getType(), (int) getCount(), b));
               }
               catch (IOException e) {
                  return Async.completedExceptionally(e);
               }
            });
      }

      @Override
      public CompletionStage<Void> writeValue(AsynchronousFileChannel chan) {
         ByteBuffer buffer = ByteBuffer.allocateDirect(dataSize()).
            order(layout_.order());
         BufferedPositionGroup posGroup = BufferedPositionGroup.create();
         value_.write(buffer, posGroup);
         buffer.rewind();

         // The TIFF spec does not require this padding
         return Async.pad(chan, layout_.version().getOffsetSize()).
            thenCompose(v -> {
               try {
                  long offset = chan.size();
                  posGroup.setBufferFileOffset(offset);
                  valueOffset_.setOffsetValue(UnbufferedPosition.at(offset));
                  return Async.write(chan, buffer, offset);
               }
               catch (IOException e) {
                  return Async.completedExceptionally(e);
               }
            });
      }

      @Override
      public void writeValue(ByteBuffer dest, BufferedPositionGroup posGroup) {
         // The TIFF spec does not require this padding
         int position = Alignment.align(dest.position(), layout_.version().getOffsetSize());
         dest.position(position);
         valueOffset_.setOffsetValue(posGroup.positionInBuffer(position));
         value_.write(dest, posGroup);
      }

      @Override
      public void write(ByteBuffer dest, BufferedPositionGroup posGroup) {
         dest.putShort((short) getTag().getTiffConstant()).
            putShort((short) getType().getTiffConstant());
         layout_.version().writeValueCount(dest, getCount());
         valueOffset_.write(layout_, dest, posGroup);
      }
   }
}
