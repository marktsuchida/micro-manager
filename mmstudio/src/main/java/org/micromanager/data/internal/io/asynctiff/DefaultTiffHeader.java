package org.micromanager.data.internal.io.asynctiff;

import com.spotify.futures.CompletableFutures;
import org.micromanager.data.internal.io.Async;
import org.micromanager.data.internal.io.BufferedPositionGroup;
import org.micromanager.data.internal.io.UnbufferedPosition;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.AsynchronousFileChannel;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

class DefaultTiffHeader implements TiffHeader {
   private static final short BIG_ENDIAN_MARK = 0x4D4D; // 'MM'
   private static final short LITTLE_ENDIAN_MARK = 0x4949; // 'II'

   private final TiffLayout layout_;
   private final TiffOffsetField firstIFDOffset_;

   static CompletionStage<TiffHeader> read(AsynchronousFileChannel chan) {
      ByteBuffer buffer1 = ByteBuffer.
         allocateDirect(TiffVersion.getHeaderCommonPartSize());
      CompletionStage<TiffLayout> layoutStage = Async.read(chan, buffer1, 0).
         thenCompose(b -> readCommonPart(b));

      CompletionStage<ByteBuffer> readStage = layoutStage.
         thenApply(layout ->
            ByteBuffer.
               allocateDirect(layout.version().getHeaderSize()).
               order(layout.order())).
         thenCompose(b -> Async.read(chan, b, 0));

      return CompletableFutures.combineFutures(layoutStage, readStage, (layout, b) -> {
         b.rewind().position(TiffVersion.getHeaderCommonPartSize());
         try {
            TiffHeader ret = layout.version().readHeader(layout.order(), b);
            return CompletableFuture.completedFuture(ret);
         }
         catch (TiffFormatException e) {
            return Async.completedExceptionally(e);
         }
      });
   }

   private static CompletionStage<TiffLayout> readCommonPart(ByteBuffer b) {
      b.rewind();

      short bom = b.getShort();
      ByteOrder order;
      switch (bom) {
         case LITTLE_ENDIAN_MARK:
            order = ByteOrder.LITTLE_ENDIAN;
            break;
         case BIG_ENDIAN_MARK:
            order = ByteOrder.BIG_ENDIAN;
            break;
         default:
            return Async.completedExceptionally(new TiffFormatException(
               String.format("Invalid TIFF byte order marker (0x%04X)", bom)));
      }

      b.order(order);
      short magic = b.getShort();
      TiffVersion version;
      try {
         version = TiffVersion.fromHeaderValue(magic);
      }
      catch (TiffFormatException e) {
         return Async.completedExceptionally(e);
      }

      TiffLayout layout = TiffLayout.create(order, version);
      return CompletableFuture.completedFuture(layout);
   }

   static TiffHeader createFromReadData(TiffLayout layout, long firstIFDOffset) throws TiffFormatException {
      // TODO This should be in a single location for reading IFD offsets
      // TODO Also, it should probably be a warning, not exception
      if (firstIFDOffset % 2 != 0) {
         throw new TiffFormatException(
            "Incorrect TIFF IFD offset (must be word-aligned)");
      }

      return new DefaultTiffHeader(layout,
         TiffOffsetField.forOffsetValue(UnbufferedPosition.at(firstIFDOffset),
            "Read-only FirstIFDOffset"));
   }

   static TiffHeader createForWrite(TiffLayout layout, TiffOffsetField firstIFDOffsetField) {
      return new DefaultTiffHeader(layout, firstIFDOffsetField);
   }

   private DefaultTiffHeader(TiffLayout layout, TiffOffsetField firstIFDOffsetField) {
      layout_ = layout;
      firstIFDOffset_ = firstIFDOffsetField;
   }

   //
   //
   //

   @Override
   public TiffLayout getTiffLayout() {
      return layout_;
   }

   @Override
   public CompletionStage<TiffIFD> readFirstIFD(AsynchronousFileChannel chan) {
      return DefaultTiffIFD.read(layout_, chan, firstIFDOffset_.getOffsetValue().get());
   }

   //
   //
   //

   @Override
   public CompletionStage<Void> write(AsynchronousFileChannel chan) {
      ByteBuffer buffer = ByteBuffer.allocateDirect(layout_.version().getHeaderSize()).order(layout_.order());
      write(buffer);
      buffer.rewind();
      return Async.write(chan, buffer, 0);
   }

   @Override
   public void write(ByteBuffer dest) {
      dest.order(layout_.order()).
         putShort(layout_.order().equals(ByteOrder.BIG_ENDIAN) ? BIG_ENDIAN_MARK : LITTLE_ENDIAN_MARK).
         putShort(layout_.version().getHeaderValue());

      layout_.version().writeHeaderMiddlePart(dest);

      BufferedPositionGroup posGroup = BufferedPositionGroup.forBufferAt(0);
      firstIFDOffset_.write(layout_, dest, posGroup);
   }
}
