package org.micromanager.data.internal.io.asynctiff;

import com.google.common.collect.ImmutableList;
import org.micromanager.data.internal.io.Async;
import org.micromanager.data.internal.io.BufferedPositionGroup;
import org.micromanager.data.internal.io.UnbufferedPosition;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;

class DefaultTiffIFD implements TiffIFD {
   private final TiffLayout layout_;
   private final List<TiffIFDEntry> entries_;
   private final TiffOffsetField nextIFDOffset_;

   //
   //
   //

   static class Builder implements TiffIFD.Builder {
      private final TiffLayout layout_;
      private final TiffOffsetField nextIFDOffset_;
      private final TiffOffsetFieldGroup ifdFieldGroup_;
      private List<TiffIFDEntry> entries_ = new ArrayList<>();

      Builder(TiffLayout layout, TiffOffsetField nextIFDOffsetField, TiffOffsetFieldGroup ifdFieldGroup) {
         layout_ = layout;
         nextIFDOffset_ = nextIFDOffsetField;
         ifdFieldGroup_ = ifdFieldGroup;
      }

      @Override
      public Builder entry(TiffTag tag, TiffValue value) {
         entries_.add(TiffIFDEntry.createForWrite(layout_, tag, value, ifdFieldGroup_));
         return this;
      }

      @Override
      public TiffIFD build() {
         return new DefaultTiffIFD(layout_, entries_, nextIFDOffset_);
      }
   }

   //
   //
   //

   public static CompletionStage<TiffIFD> read(TiffLayout layout, AsynchronousFileChannel chan, long offset) {
      ByteBuffer countBuffer = ByteBuffer.allocateDirect(
         layout.version().getIFDEntryCountSize()).
         order(layout.order());

      return Async.read(chan, countBuffer, offset).thenComposeAsync(cb -> {
         cb.rewind();

         long entryCount = layout.version().readIFDEntryCount(cb);
         long remainingSize = entryCount * layout.version().getIFDEntrySize() + layout.version().getOffsetSize();
         ByteBuffer bodyBuffer = ByteBuffer.allocateDirect((int) remainingSize).
            order(layout.order());

         return Async.read(chan, bodyBuffer, offset + layout.version().getIFDEntryCountSize()).
            thenComposeAsync(bb -> {
               bb.rewind();
               try {
                  return CompletableFuture.completedFuture(
                     readEntriesAndNextOffset(layout, bb, entryCount));
               }
               catch (IOException e) {
                  return Async.completedExceptionally(e);
               }
            });
      });
   }

   private static TiffIFD readEntriesAndNextOffset(TiffLayout layout, ByteBuffer b, long entryCount) throws IOException {
      List<TiffIFDEntry> entries = new ArrayList<>();
      for (int i = 0; i < entryCount; ++i) {
         entries.add(TiffIFDEntry.read(layout, b));
      }
      long nextIFDOffset = layout.version().readOffset(b);
      return new DefaultTiffIFD(layout, entries, nextIFDOffset);
   }

   //
   //
   //

   // Read
   private DefaultTiffIFD(TiffLayout layout, List<TiffIFDEntry> entries,
                          long nextIFDOffset) {
      layout_ = layout;
      entries_ = ImmutableList.copyOf(entries);
      nextIFDOffset_ = TiffOffsetField.forOffsetValue(
         UnbufferedPosition.at(nextIFDOffset),
         "Read-only NextIFDOffset");
   }

   // Writing
   private DefaultTiffIFD(TiffLayout layout, Collection<TiffIFDEntry> entries,
                          TiffOffsetField nextIFDOffsetField) {
      layout_ = layout;
      List<TiffIFDEntry> sortEntries = new ArrayList<>(entries);
      sortEntries.sort(Comparator.comparingInt(e -> e.getTag().getTiffConstant()));
      entries_ = ImmutableList.copyOf(sortEntries);
      nextIFDOffset_ = nextIFDOffsetField;
   }

   //
   //
   //

   @Override
   public int getEntryCount() {
      return entries_.size();
   }

   @Override
   public List<TiffIFDEntry> getEntries() {
      return entries_;
   }

   @Override
   public TiffIFDEntry getEntryWithTag(TiffTag tag) {
      for (TiffIFDEntry e : entries_) {
         if (e.getTag().equals(tag)) {
            return e;
         }
      }
      return null;
   }

   @Override
   public TiffIFDEntry getRequiredEntryWithTag(TiffTag tag) throws TiffFormatException {
      TiffIFDEntry ret = getEntryWithTag(tag);
      if (ret == null) {
         throw new TiffFormatException(String.format(
            "Required TIFF IFD entry %s is missing", tag.name()));
      }
      return ret;
   }

   /**
    * Get all entries whose type matches the given value.
    *
    * Although the TIFF specification does not allow multiple entries with
    * the same type, in practice we see this in the wild. One example is the
    * inclusion of two ImageDescription entries by Micro-Manager, one for OME
    * and one for ImageJ.
    *
    * @param tag the TIFF tag
    * @return
    */
   @Override
   public List<TiffIFDEntry> getAllEntriesWithTag(TiffTag tag) {
      return entries_.stream().
         filter(entry -> entry.getTag().equals(tag)).
         collect(Collectors.toList());
   }

   @Override
   public boolean hasNextIFD() {
      return nextIFDOffset_.getOffsetValue().get() != 0;
   }

   @Override
   public CompletionStage<TiffIFD> readNextIFD(AsynchronousFileChannel chan) {
      return DefaultTiffIFD.read(layout_, chan, nextIFDOffset_.getOffsetValue().get());
   }

   //
   //
   //

   private CompletionStage<Boolean> isSingleStrip(AsynchronousFileChannel chan) {
      try {
         CompletionStage<TiffValue> getImageLength = getRequiredEntryWithTag(
            TiffTag.Known.ImageLength.get()).readValue(chan);
         CompletionStage<TiffValue> getRowsPerStrip = getRequiredEntryWithTag(
            TiffTag.Known.RowsPerStrip.get()).readValue(chan);
         return getImageLength.thenCombine(getRowsPerStrip,
               (length, rows) -> length.longValue(0) == rows.longValue(0));
      }
      catch (IOException e) {
         return Async.completedExceptionally(e);
      }
   }

   @Override
   public CompletionStage<ByteBuffer> readPixels(AsynchronousFileChannel chan) {
      return isSingleStrip(chan).thenComposeAsync(ok ->
         ok ? readPixelsSingleStrip(chan) :
            Async.completedExceptionally(new TiffFormatException(
               "Only images stored in a single strip are currently supported")));
   }

   private CompletionStage<ByteBuffer> readPixelsSingleStrip(AsynchronousFileChannel chan) {
      try {
         CompletionStage<TiffValue> getStripOffsets = getRequiredEntryWithTag(
            TiffTag.Known.StripOffsets.get()).readValue(chan);
         CompletionStage<TiffValue> getStripByteCounts = getRequiredEntryWithTag(
            TiffTag.Known.StripByteCounts.get()).readValue(chan);
         return getStripOffsets.thenCombine(getStripByteCounts,
            (offsets, sizes) -> readBlock(chan, offsets.longValue(0), sizes.longValue(0))).
            thenCompose(Function.identity());
      }
      catch (IOException e) {
         return Async.completedExceptionally(e);
      }
   }

   private CompletionStage<ByteBuffer> readBlock(AsynchronousFileChannel chan,
                                                 long offset, long size) {
      ByteBuffer buffer = ByteBuffer.allocateDirect((int) size);
      return Async.read(chan, buffer, offset);
   }

   //
   //
   //

   @Override
   public CompletionStage<Long> write(AsynchronousFileChannel chan) {
      ByteBuffer buffer = ByteBuffer.allocateDirect(
         layout_.version().getIFDEntryCountSize() +
            layout_.version().getIFDEntrySize() * getEntryCount() +
            layout_.version().getOffsetSize()).
         order(layout_.order());
      BufferedPositionGroup posGroup = BufferedPositionGroup.create();
      write(buffer, posGroup);

      // Technically, the TIFF spec requires IFDs to be 2-byte aligned
      // (although it is not clear how many readers/writers care). We align to
      // the offset size just to be extra safe.
      return Async.pad(chan, layout_.version().getOffsetSize()).thenCompose(v -> {
         try {
            long offset = chan.size();
            posGroup.setBufferFileOffset(offset);
            return Async.write(chan, buffer, offset).thenApply(v2 -> offset);
         }
         catch (IOException e) {
            return Async.completedExceptionally(e);
         }
      });
   }

   @Override
   public int write(ByteBuffer dest, BufferedPositionGroup posGroup) {
      int pos = dest.position();
      layout_.version().writeIFDEntryCount(dest, getEntryCount());
      for (TiffIFDEntry entry : entries_) {
         entry.write(dest, posGroup);
      }
      nextIFDOffset_.write(layout_, dest, posGroup);
      return pos;
   }
}
