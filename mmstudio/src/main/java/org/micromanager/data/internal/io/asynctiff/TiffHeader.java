package org.micromanager.data.internal.io.asynctiff;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.util.concurrent.CompletionStage;

public interface TiffHeader {
   static CompletionStage<TiffHeader> read(AsynchronousFileChannel chan) {
      return DefaultTiffHeader.read(chan);
   }

   static TiffHeader createForWrite(TiffLayout layout, TiffOffsetField firstIFDOffsetField) {
      return DefaultTiffHeader.createForWrite(layout, firstIFDOffsetField);
   }

   TiffLayout getTiffLayout();

   CompletionStage<TiffIFD> readFirstIFD(AsynchronousFileChannel chan);

   CompletionStage<Void> write(AsynchronousFileChannel chan);

   void write(ByteBuffer dest);
}
