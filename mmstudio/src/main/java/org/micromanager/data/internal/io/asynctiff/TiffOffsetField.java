package org.micromanager.data.internal.io.asynctiff;

import com.google.common.base.Preconditions;
import org.micromanager.data.internal.io.Async;
import org.micromanager.data.internal.io.BufferedPositionGroup;
import org.micromanager.data.internal.io.FilePosition;
import org.micromanager.data.internal.io.UnbufferedPosition;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.util.concurrent.CompletionStage;

public class TiffOffsetField {
   private static final long OFFSET_PLACEHOLDER = 0;

   private FilePosition fieldPosition_;
   private FilePosition offsetValue_;

   private final String debugAnnotation_;

   private TiffOffsetField(FilePosition position, FilePosition value, String annotation) {
      fieldPosition_ = position;
      offsetValue_ = value;
      debugAnnotation_ = annotation;
   }

   public static TiffOffsetField create(String annotation) {
      return new TiffOffsetField(null, null, annotation);
   }

   public static TiffOffsetField atPosition(FilePosition position,
                                            String annotation) {
      Preconditions.checkNotNull(position);
      return new TiffOffsetField(position, null, annotation);
   }

   public static TiffOffsetField forOffsetValue(FilePosition offset,
                                                String annotation) {
      Preconditions.checkNotNull(offset);
      return new TiffOffsetField(null, offset, annotation);
   }

   public void setFieldPosition(FilePosition position) {
      Preconditions.checkNotNull(position);
      Preconditions.checkState(fieldPosition_ == null,
         "Attempt to overwrite field position in " + this);
      fieldPosition_ = position;
   }

   public void setOffsetValue(FilePosition offset) {
      Preconditions.checkNotNull(offset);
      Preconditions.checkState(offsetValue_ == null,
         "Attempt to overwrite offset value in " + this);
      offsetValue_ = offset;
   }

   public FilePosition getFieldPosition() {
      Preconditions.checkState(fieldPosition_ != null,
         "Missing field position in " + this);
      return fieldPosition_;
   }

   public FilePosition getOffsetValue() {
      Preconditions.checkState(offsetValue_ != null,
         "Missing offset value in " + this);
      return offsetValue_;
   }

   //
   //
   //

   private void checkCompleteFieldPosition() {
      Preconditions.checkState(fieldPosition_ != null &&
            fieldPosition_.isComplete(),
         "Missing or incomplete field position in " + this);
   }

   private void checkBufferRelativeFieldPosition() {
      Preconditions.checkState(fieldPosition_ != null &&
            fieldPosition_.hasPositionInBuffer(),
         "Field position does not have a buffer-relative offset in " + this);
   }

   private void checkCompleteOffsetValue() {
      Preconditions.checkState(isOffsetValueComplete(),
         "Missing or incomplete offset value in " + this);
   }

   private boolean isOffsetValueComplete() {
      return offsetValue_ != null && offsetValue_.isComplete();
   }

   private static ByteBuffer makeWriteBuffer(TiffLayout layout, long value) {
      ByteBuffer buffer = ByteBuffer.allocateDirect(
         layout.version().getOffsetSize()).
         order(layout.order());
      layout.version().writeOffset(buffer, value);
      buffer.rewind();
      return buffer;
   }

   //
   //
   //

   /**
    * Write this offset field at the given position of the file.
    *
    * If the absolute offset value is not known yet, a placeholder is written.
    *
    * @param layout
    * @param chan
    * @param offset
    * @return
    */
   CompletionStage<Void> write(TiffLayout layout,
                               AsynchronousFileChannel chan,
                               long offset) {
      setFieldPosition(UnbufferedPosition.at(offset));
      return Async.write(chan,
         makeWriteBuffer(layout,
            isOffsetValueComplete() ? offsetValue_.get() : OFFSET_PLACEHOLDER),
         fieldPosition_.get());
   }

   /**
    * Rewrite this offset field to the file, overwriting a placeholder.
    *
    * @param layout
    * @param chan
    * @return
    */
   CompletionStage<Void> update(TiffLayout layout, AsynchronousFileChannel chan) {
      checkCompleteFieldPosition();
      checkCompleteOffsetValue();
      return Async.write(chan, makeWriteBuffer(layout, offsetValue_.get()),
         fieldPosition_.get());
   }

   /**
    * Write this offset field to a buffer, tying the position of the offset
    * field to the given position group.
    *
    * If the absolute offset value is not known yet, a placeholder is written.
    *
    * Later, when the buffer is written to a specific file position, the
    * position group can be used to update this offset field, making the
    * absolute position of the field available.
    *
    * @param layout
    * @param buffer
    * @param posGroup
    */
   public void write(TiffLayout layout,
                     ByteBuffer buffer,
                     BufferedPositionGroup posGroup) {
      setFieldPosition(posGroup.positionInBuffer(buffer.position()));
      if (isOffsetValueComplete()) {
         write(layout, buffer);
      }
      else {
         layout.version().writeOffset(buffer, OFFSET_PLACEHOLDER);
      }
   }

   /**
    * Write this offset field to a buffer, without recording the position of the
    * offset field.
    *
    * This method can be used when the offset value is available and therefore
    * it is not necessary to later update it.
    *
    * @param layout
    * @param buffer
    */
   public void write(TiffLayout layout, ByteBuffer buffer) {
      checkCompleteOffsetValue();
      layout.version().writeOffset(buffer, offsetValue_.get());
   }

   /**
    * Rewrite this offset field to a buffer, overwriting a placeholder.
    *
    * @param layout
    * @param buffer
    */
   public void update(TiffLayout layout, ByteBuffer buffer) {
      checkCompleteOffsetValue();
      checkBufferRelativeFieldPosition();
      layout.version().writeOffset(buffer,
         fieldPosition_.getPositionInBuffer(),
         offsetValue_.get());
   }

   @Override
   public String toString() {
      return String.format("<TiffOffsetField (%s) fieldPosition=%s offsetValue=%s>",
         debugAnnotation_, fieldPosition_, offsetValue_);
   }
}
