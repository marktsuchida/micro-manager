package org.micromanager.data.internal.io.asynctiff;

import org.micromanager.data.internal.io.BufferedPositionGroup;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.util.List;
import java.util.concurrent.CompletionStage;

public interface TiffIFD {
   interface Builder {
      Builder entry(TiffTag tag, TiffValue value);
      TiffIFD build();
   }

   static CompletionStage<TiffIFD> read(TiffLayout layout, AsynchronousFileChannel chan, long offset) {
      return DefaultTiffIFD.read(layout, chan, offset);
   }

   static Builder builder(TiffLayout layout, TiffOffsetField nextIFDOffsetField, TiffOffsetFieldGroup ifdFieldGroup) {
      return new DefaultTiffIFD.Builder(layout, nextIFDOffsetField, ifdFieldGroup);
   }

   int getEntryCount();

   List<TiffIFDEntry> getEntries();

   TiffIFDEntry getEntryWithTag(TiffTag tag);

   TiffIFDEntry getRequiredEntryWithTag(TiffTag tag) throws TiffFormatException;

   List<TiffIFDEntry> getAllEntriesWithTag(TiffTag tag);

   boolean hasNextIFD();

   CompletionStage<TiffIFD> readNextIFD(AsynchronousFileChannel chan) throws IOException;

   CompletionStage<ByteBuffer> readPixels(AsynchronousFileChannel chan);

   CompletionStage<Long> write(AsynchronousFileChannel chan);

   int write(ByteBuffer dest, BufferedPositionGroup posGroup);
}
