// Copyright (C) 2015-2017 Open Imaging, Inc.
//           (C) 2015 Regents of the University of California
//
// LICENSE:      This file is distributed under the BSD license.
//               License text is included with the source distribution.
//
//               This file is distributed in the hope that it will be useful,
//               but WITHOUT ANY WARRANTY; without even the implied warranty
//               of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//
//               IN NO EVENT SHALL THE COPYRIGHT OWNER OR
//               CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
//               INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES.

package org.micromanager.display.internal.displaywindow;


import com.google.common.base.Preconditions;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.common.util.concurrent.MoreExecutors;
import ij.ImagePlus;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import javax.swing.SwingUtilities;
import org.micromanager.data.Coordinates;
import org.micromanager.data.Coords;
import org.micromanager.data.DataProvider;
import org.micromanager.data.Image;
import org.micromanager.display.DisplaySettings;
import org.micromanager.display.DisplayWindow;
import org.micromanager.display.DisplayWindowControlsFactory;
import org.micromanager.display.overlay.Overlay;
import org.micromanager.display.overlay.OverlayListener;
import org.micromanager.display.inspector.internal.panels.intensity.ImageStatsPublisher;
import org.micromanager.display.internal.DefaultDisplaySettings;
import org.micromanager.display.internal.animate.AnimationController;
import org.micromanager.display.internal.animate.DataCoordsAnimationState;
import org.micromanager.display.internal.event.DefaultDisplayDidShowImageEvent;
import org.micromanager.display.internal.imagestats.BoundsRectAndMask;
import org.micromanager.display.internal.imagestats.ImageStatsRequest;
import org.micromanager.display.internal.imagestats.ImagesAndStats;
import org.micromanager.display.internal.imagestats.StatsComputeQueue;
import org.micromanager.display.internal.DefaultDisplayManager;
import org.micromanager.display.internal.event.DisplayWindowDidAddOverlayEvent;
import org.micromanager.display.internal.event.DisplayWindowDidRemoveOverlayEvent;
import org.micromanager.display.internal.event.DataViewerDidBecomeActiveEvent;
import org.micromanager.display.internal.event.DataViewerWillCloseEvent;
import org.micromanager.display.internal.event.DataViewerDidBecomeInvisibleEvent;
import org.micromanager.display.internal.event.DataViewerDidBecomeVisibleEvent;
import org.micromanager.display.internal.link.LinkManager;
import org.micromanager.events.DatastoreClosingEvent;
import org.micromanager.events.internal.DefaultEventManager;
import org.micromanager.internal.utils.CoalescentEDTRunnablePool;
import org.micromanager.internal.utils.CoalescentEDTRunnablePool.CoalescentRunnable;
import org.micromanager.internal.utils.MustCallOnEDT;
import org.micromanager.internal.utils.performance.PerformanceMonitor;
import org.micromanager.internal.utils.performance.gui.PerformanceMonitorUI;
import org.micromanager.data.DataProviderHasNewImageEvent;
import org.micromanager.data.DataProviderHasNewNameEvent;
import org.micromanager.data.Datastore;
import org.micromanager.display.ChannelDisplaySettings;
import org.micromanager.display.DataViewerDelegate;
import org.micromanager.display.internal.ChannelDisplayDefaults;
import org.micromanager.display.internal.export.AbstractImageExporter;
import org.micromanager.display.internal.link.internal.DefaultLinkManager;
import org.micromanager.internal.utils.UserProfileStaticInterface;

/**
 * Main controller for the standard image viewer.
 *
 * This is also the implementation for the DisplayWindow API, for now (it might
 * make sense to refactor and separate API support from controller
 * implementation).
 *
 * @author Mark A. Tsuchida, parts refactored from code by Chris Weisiger
 */
public final class DisplayController extends DisplayWindowAPIAdapter
      implements DisplayWindow,
      ImageStatsPublisher,
      DataCoordsAnimationState.CoordsProvider,
      AnimationController.Listener<Coords>,
      StatsComputeQueue.Listener,
      OverlayListener
{
   private final DataProvider dataProvider_;

   private String customTitle_;
   private static String DEFAULT_TITLE = "Untitled Image";

   // The actually painted images. Accessed only on EDT.
   private ImagesAndStats displayedImages_;

   // Accessed only on EDT
   private long latestStatsSeqNr_ = -1;

   // Not final but set only upon creation
   private DataCoordsAnimationState animationState_;
   // Not final but set only upon creation
   private AnimationController<Coords> animationController_;

   private final Set<String> playbackAxes_ = new HashSet<String>();

   private final StatsComputeQueue computeQueue_ = StatsComputeQueue.create();
   private static final long MIN_REPAINT_PERIOD_NS = Math.round(1e9 / 60.0);

   private final LinkManager linkManager_;

   // The UI controller manages the actual JFrame and all the components in it,
   // including interaction with ImageJ. After being closed, set to null.
   // Must access on EDT
   private DisplayUIController uiController_;

   private final Object selectionLock_ = new Object();
   private BoundsRectAndMask selection_ = BoundsRectAndMask.unselected();

   private final List<Overlay> overlays_ = new ArrayList<Overlay>();

   // A delegate object allowing customization of certain behaviors (such as
   // what to do when user tries to close)
   private DataViewerDelegate delegate_;

   private volatile boolean closed_;

   private final DisplayWindowControlsFactory controlsFactory_;

   private final CoalescentEDTRunnablePool runnablePool_ =
         CoalescentEDTRunnablePool.create();

   private final PerformanceMonitor perfMon_ =
         PerformanceMonitor.createWithTimeConstantMs(1000.0);
   private final PerformanceMonitorUI perfMonUI_ =
         PerformanceMonitorUI.create(perfMon_, "Display Performance");

   @Override
   public void setDelegate(DataViewerDelegate delegate) {
      delegate_ = delegate;
   }

   @Override
   public DataViewerDelegate getDelegate() {
      return delegate_;
   }

   public static class Builder {
      private DataProvider dataProvider_;
      private DisplaySettings displaySettings_;
      private LinkManager linkManager_ = DefaultLinkManager.create();;
      private boolean shouldShow_;
      private DisplayWindowControlsFactory controlsFactory_;

      public Builder(DataProvider dataProvider)
      {
         Preconditions.checkNotNull(dataProvider);
         dataProvider_ = dataProvider;
      }

      public Builder dataProvider(DataProvider provider) {
         if (provider == null) {
            throw new NullPointerException();
         }
         dataProvider_ = provider;
         return this;
      }

      public Builder initialDisplaySettings(DisplaySettings settings) {
         displaySettings_ = settings;
         return this;
      }

      public Builder linkManager(LinkManager manager) {
         linkManager_ = manager;
         return this;
      }

      public Builder shouldShow(boolean flag) {
         shouldShow_ = flag;
         return this;
      }

      public Builder controlsFactory(DisplayWindowControlsFactory factory) {
         controlsFactory_ = factory;
         return this;
      }

      @MustCallOnEDT
      public DisplayController build() {
         return DisplayController.create(this);
      }
   }

   @MustCallOnEDT
   private static DisplayController create(Builder builder)
   {
      DisplaySettings initialDisplaySettings = builder.displaySettings_;
      if (initialDisplaySettings == null) {
         // TODO Should this be handled by upstream code?
         ChannelDisplayDefaults defaults = new ChannelDisplayDefaults(UserProfileStaticInterface.getInstance());
         DisplaySettings.Builder settingsBuilder = DefaultDisplaySettings.builder();
         int numChannels = builder.dataProvider_.getAxisLength(Coords.CHANNEL);
         String groupName = builder.dataProvider_.getSummaryMetadata().getChannelGroup();
         if (groupName != null) {
            for (int i = 0; i < numChannels; ++i) {
               String channelName = builder.dataProvider_.getSummaryMetadata().getSafeChannelName(i);
               ChannelDisplaySettings channelSettings = defaults.getSettingsForChannel(
                     groupName, channelName);
               if (channelSettings != null) {
                  settingsBuilder.channel(i, channelSettings);
               }
            }
         }
         initialDisplaySettings = settingsBuilder.build();
      }

      final DisplayController instance =
            new DisplayController(builder.dataProvider_,
                  initialDisplaySettings, builder.controlsFactory_,
                  builder.linkManager_);
      instance.initialize();

      instance.computeQueue_.addListener(instance);

      if (builder.shouldShow_) {
         // Show the window in a later event handler in order to give the
         // calling code a chance to register for events.
         SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
               instance.setFrameVisible(true);
               instance.toFront();
            }
         });
      }

      if (instance.dataProvider_.getNumImages() > 0) {
         Coords.Builder b = Coordinates.builder();
         for (String axis : instance.dataProvider_.getAxes()) {
            b.index(axis, 0);
         }
         instance.setDisplayPosition(b.build());

         // TODO Cleaner
         instance.animationAcknowledgeDataPosition(instance.getDataProvider().getMaxIndices());
      }

      return instance;
   }

   private DisplayController(DataProvider dataProvider,
         DisplaySettings initialDisplaySettings,
         DisplayWindowControlsFactory controlsFactory,
         LinkManager linkManager)
   {
      super(initialDisplaySettings);
      dataProvider_ = dataProvider;
      controlsFactory_ = controlsFactory;
      linkManager_ = linkManager;

      computeQueue_.setPerformanceMonitor(perfMon_);
   }

   @MustCallOnEDT
   private void initialize() {
      // Initialize some things that would leak 'this' if done in the
      // constructor
      animationState_ = DataCoordsAnimationState.create(this);
      animationController_ = AnimationController.create(animationState_);
      animationController_.setPerformanceMonitor(perfMon_);
      animationController_.addListener(this);

      uiController_ = DisplayUIController.create(this, controlsFactory_,
            animationController_);
      uiController_.setPerformanceMonitor(perfMon_);
      // TODO Make sure frame controller forwards messages to us (e.g.
      // windowClosing() -> requestToClose())

      // Start receiving events
      DefaultEventManager.getInstance().registerForEvents(this);
      dataProvider_.registerForEvents(this);
   }

   // Allow internal objects (in particular, UI controller) to post events
   void postDisplayEvent(Object event) {
      postEvent(event);
   }

   @MustCallOnEDT
   public DisplayUIController getUIController() {
      return uiController_;
   }

   LinkManager getLinkManager() {
      return linkManager_;
   }

   @MustCallOnEDT
   private void setFrameVisible(boolean visible) {
      uiController_.setVisible(visible);
      // TODO Post these events based on ComponentListener on the JFrame
      postEvent(visible ?
            DataViewerDidBecomeVisibleEvent.create(this) :
            DataViewerDidBecomeInvisibleEvent.create(this));
   }

   void frameDidBecomeActive() {
      postEvent(DataViewerDidBecomeActiveEvent.create(this));
   }


   //
   // Scheduling images for display
   //

   @Override
   public long imageStatsReady(ImagesAndStats stats) {
      perfMon_.sampleTimeInterval("Image stats ready");

      scheduleDisplayInUI(stats);

      // Throttle display scheduling
      return MIN_REPAINT_PERIOD_NS;
   }

   private void scheduleDisplayInUI(final ImagesAndStats images) {
      Preconditions.checkArgument(images.getRequest().getNumberOfImages() > 0);

      // A note about congestion of event queue by excessive paint events:
      //
      // We know from experience (since Micro-Manager 1.4) that scheduling
      // repaints too frequently will congest the EDT.
      //
      // One method to avoid congestion (which was previously used) is to
      // explicitly detect and wait for the completion of painting on the EDT.
      // If using that method, here would be the place to wait for previously
      // requested paints to finish.
      //
      // Here, we schedule painting using coalescent runnables. No repaint is
      // scheduled until the EDT catches up to the last of (the InvocationEvent
      // resulting from) our coalescent runnables. Because the EDT will not
      // catch up if congested, we don't exacerbate the situation by scheduling
      // further repaints.
      // It is also guaranteed that a repaint _will_ be scheduled when no
      // longer congested, because before becoming idle the EDT will have to
      // process the last of our coalescent runnable, thereby scheduling a
      // repaint with the latest requested display position.
      // One of the nice things about doing it this way is that we can
      // coalesce the displaying tasks for multiple display windows.

      runnablePool_.invokeAsLateAsPossibleWithCoalescence(new CoalescentRunnable() {
         @Override
         public Class<?> getCoalescenceClass() {
            return getClass();
         }

         @Override
         public CoalescentRunnable coalesceWith(CoalescentRunnable later) {
            // Only the most recent repaint task need be run
            perfMon_.sampleTimeInterval("Scheduling of repaint coalesced");
            return later;
         }

         @Override
         public void run() {
            if (uiController_ == null) { // Closed
               return;
            }

            Image primaryImage = images.getRequest().getImage(0);
            Coords nominalCoords = images.getRequest().getNominalCoords();
            if (nominalCoords.hasAxis(Coords.CHANNEL)) {
               int channel = nominalCoords.getChannel();
               for (Image image : images.getRequest().getImages()) {
                  if (image.getCoords().hasAxis(Coords.CHANNEL) &&
                        image.getCoords().getChannel() == channel)
                  {
                     primaryImage = image;
                     break;
                  }
               }
            }

            boolean imagesDiffer = true;
            if (displayedImages_ != null &&
                  images.getRequest().getNumberOfImages() ==
                  displayedImages_.getRequest().getNumberOfImages())
            {
               imagesDiffer = false;
               for (int i = 0; i < images.getRequest().getNumberOfImages(); ++i) {
                  if (images.getRequest().getImage(i) !=
                        displayedImages_.getRequest().getImage(i)) {
                     imagesDiffer = true;
                  }
               }
            }

            perfMon_.sample("Scheduling identical images (%)", imagesDiffer ? 0.0 : 100.0);

            if (imagesDiffer || getDisplaySettings().isAutostretchEnabled()) {
               uiController_.displayImages(images);
            }

            postEvent(DefaultDisplayDidShowImageEvent.create(
                  DisplayController.this,
                  images.getRequest().getImages(),
                  primaryImage));

            if (images.getStatsSequenceNumber() > latestStatsSeqNr_) {
               postEvent(ImageStatsChangedEvent.create(images));
               latestStatsSeqNr_ = images.getStatsSequenceNumber();
            }
            displayedImages_ = images;

            perfMon_.sampleTimeInterval("Scheduled repaint on EDT");
         }
      });
   }

   @Override
   @MustCallOnEDT
   public ImagesAndStats getCurrentImagesAndStats() {
      return displayedImages_;
   }

   public double getDisplayIntervalQuantile(double q) {
      if (uiController_ == null) {
         return 0.0;
      }
      return uiController_.getDisplayIntervalQuantile(q);
   }

   public void resetDisplayIntervalEstimate() {
      if (uiController_ != null) {
         uiController_.resetDisplayIntervalEstimate();
      }
   }


   //
   // Implementation of DataCoordsAnimationState.CoordsProvider
   //

   @Override
   public List<String> getOrderedAxes() {
      // TODO XXX Check if this is ordered; if not, fix.
      return dataProvider_.getAxes();
   }

   @Override
   public int getMaximumExtentOfAxis(String axis) {
      return dataProvider_.getAxisLength(axis);
   }

   @Override
   public boolean coordsExist(Coords c) {
      return dataProvider_.hasImage(c);
   }

   @Override
   public Collection<String> getAnimatedAxes() {
      synchronized (this) {
         return new ArrayList<String>(playbackAxes_);
      }
   }

   @Override
   protected DisplaySettings handleDisplaySettings(
         DisplaySettings requestedSettings)
   {
      perfMon_.sampleTimeInterval("Handle display settings");

      final DisplaySettings oldSettings = getDisplaySettings();
      final DisplaySettings adjustedSettings = requestedSettings;
      // We don't currently make any adjustments (normalizations) to the
      // settings, but we probably should check that they are consistent.

      if (adjustedSettings.isROIAutoscaleEnabled() != oldSettings.isROIAutoscaleEnabled()) {
         // We can't let this coalesce. No need to run on EDT but it's just as
         // good a thread as any.
         SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
               Coords pos;
               do {
                  pos = getDisplayPosition();
               } while (!compareAndSetDisplayPosition(pos, pos, true));
            }
         });
      }

      if (adjustedSettings.getPlaybackFPS() != oldSettings.getPlaybackFPS()) {
         SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
               setPlaybackSpeedFps(adjustedSettings.getPlaybackFPS());
            }
         });
      }

      runnablePool_.invokeAsLateAsPossibleWithCoalescence(new CoalescentRunnable() {
         @Override
         public Class<?> getCoalescenceClass() {
            return getClass();
         }

         @Override
         public CoalescentRunnable coalesceWith(CoalescentRunnable later) {
            return later;
         }

         @Override
         public void run() {
            if (uiController_ == null) {
               return;
            }

            uiController_.applyDisplaySettings(adjustedSettings);
         }
      });
      return adjustedSettings;
   }

   @Override
   protected Coords handleDisplayPosition(Coords position) {
      perfMon_.sampleTimeInterval("Handle display position");

      // This viewer does not support displaying more than one Coords at a
      // time (with the exception of channels in composite mode, but even
      // then the notional "display position" is a unique Coords).

      // The given coords may match more images than can be displayed. In
      // which case, we take the unsupplied coordinates to be equal to the
      // current position, or zero if that is not available.

      Coords current = getDisplayPosition();
      Coords.Builder cb = Coordinates.builder();
      dataProvider_.getAxes().forEach((axis) -> {
         if (position.hasAxis(axis)) {
            cb.index(axis, position.getIndex(axis));
         }
         else if (current.hasAxis(axis)) {
            cb.index(axis, current.getIndex(axis));
         }
         else {
            cb.index(axis, 0);
         }
      });
      Coords newPosition = cb.build();

      runnablePool_.invokeLaterWithCoalescence(
            new ExpandDisplayRangeCoalescentRunnable(newPosition));

      // Always compute stats for all channels
      Coords channellessPos = newPosition.hasAxis(Coords.CHANNEL) ?
            newPosition.copy().removeAxis(Coords.CHANNEL).build() :
            newPosition;
      List<Image> images;
      try {
         images = dataProvider_.getImagesMatching(channellessPos);
      }
      catch (IOException e) {
         // TODO Should display error
         images = Collections.emptyList();
      }

      // Images are sorted by channel here, since we don't (yet) have any other
      // way to correctly recombine stats with newer images (when update rate
      // is finite).
      if (images.size() > 1) {
         Collections.sort(images, new Comparator<Image>() {
            @Override
            public int compare(Image o1, Image o2) {
               return new Integer(o1.getCoords().getChannel()).
                     compareTo(o2.getCoords().getChannel());
            }
         });
      }

      // TODO XXX We need to handle missing images if so requested. User should
      // be able to enable "filling in Z slices" and "filling in time points".
      // If 'images' is empty, search first in Z, then in time, until at least
      // one channel has an image (Q: or just leave empty when no channel has
      // an image?). Then, for missing channels, first search for nearest Z in
      // either direction. For channels still missing, search back in time,
      // looking for the nearest available slice in each time point.
      // Record the substitutions made (requested and provided coords) in to
      // the ImageStatsRequest! Then display coords of actual displayed image(s)
      // The substitution strategy should be pluggable. ("MissingImageStrategy"),
      // and we should always go through it (default = no substitution)
      // XXX On the other hand, we do NOT want to fill in images if they are
      // about to be acquired! How do we know if this is the case?
      // During acquisition, do not search back in time if newest timepoint.
      // During acquisition, do not search in Z if newest timepoint.

      BoundsRectAndMask selection = BoundsRectAndMask.unselected();
      if (getDisplaySettings().isROIAutoscaleEnabled()) {
         synchronized (selectionLock_) {
            selection = selection_;
         }
      }

      perfMon_.sampleTimeInterval("Submitting compute request");
      computeQueue_.submitRequest(ImageStatsRequest.create(newPosition, images,
            selection));

      return newPosition;
   }


   //
   // Implementation of AnimationController.Listener<Coords>
   //

   @Override
   public void animationShouldDisplayDataPosition(Coords position) {
      perfMon_.sampleTimeInterval("Coords from animation controller");

      // We do not skip handling this position even if it equals the current
      // position, because the image data may have changed (e.g. if we have
      // been displaying a position that didn't yet have an image, or if this
      // is a special datastore such as the one used for snap/live preview).

      // Also, we do not throttle the processing rate here because that is done
      // automatically by the compute queue based on result retrieval.

      // Set the "official" position of this data viewer
      setDisplayPosition(position, true);
   }

   @Override
   public void animationAcknowledgeDataPosition(final Coords position) {
      // Tell the UI controller to expand the display range to include the
      // newly seen position. But use coalescent invocation to avoid doing
      // too much on the EDT.
      runnablePool_.invokeLaterWithCoalescence(
            new ExpandDisplayRangeCoalescentRunnable(position));
   }

   @Override
   public void animationWillJumpToNewDataPosition(Coords position) {
   }

   @Override
   public void animationDidJumpToNewDataPosition(Coords position) {
      perfMon_.sampleTimeInterval("Animation Did Jump To New Data Position");
      uiController_.setNewImageIndicator(true);
   }

   @Override
   public void animationNewDataPositionExpired() {
      perfMon_.sampleTimeInterval("Animation New Data Position Expired");
      uiController_.setNewImageIndicator(false);
   }


   //
   // Overlay support
   //

   @Override
   public void addOverlay(final Overlay overlay) {
      if (!SwingUtilities.isEventDispatchThread()) {
         SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
               addOverlay(overlay);
            }
         });
         return;
      }

      overlays_.add(overlay);
      overlay.addOverlayListener(this);
      if (overlay.isVisible() && uiController_ != null) {
         uiController_.overlaysChanged();
      }
      postEvent(DisplayWindowDidAddOverlayEvent.create(this, overlay));
   }

   @Override
   public void removeOverlay(final Overlay overlay) {
      if (!SwingUtilities.isEventDispatchThread()) {
         SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
               removeOverlay(overlay);
            }
         });
         return;
      }

      overlay.removeOverlayListener(this);
      overlays_.remove(overlay);
      if (overlay.isVisible() && uiController_ != null) {
         uiController_.overlaysChanged();
      }
      postEvent(DisplayWindowDidRemoveOverlayEvent.create(this, overlay));
   }

   @Override
   public List<Overlay> getOverlays() {
      if (!SwingUtilities.isEventDispatchThread()) {
         RunnableFuture<List<Overlay>> edtFuture = new FutureTask(
               new Callable<List<Overlay>>() {
            @Override
            public List<Overlay> call() throws Exception {
               return getOverlays();
            }
         });
         SwingUtilities.invokeLater(edtFuture);
         try {
            return edtFuture.get();
         }
         catch (InterruptedException notUsedByUs) {
            Thread.currentThread().interrupt();
            return getOverlays(); // Bad
         }
         catch (ExecutionException unexpected) {
            throw new RuntimeException(unexpected);
         }
      }

      return new ArrayList<Overlay>(overlays_);
   }

   @Override
   public void overlayTitleChanged(Overlay overlay) {
      // Nothing to do
   }

   @Override
   public void overlayConfigurationChanged(Overlay overlay) {
      if (uiController_ == null) {
         return;
      }
      if (overlay.isVisible()) {
         uiController_.overlaysChanged();
      }
   }

   @Override
   public void overlayVisibleChanged(Overlay overlay) {
      if (uiController_ == null) {
         return;
      }
      uiController_.overlaysChanged();
   }

   //
   // Misc
   //

   // Notification from UI controller
   public void selectionDidChange(BoundsRectAndMask selection) {
      if (selection == null) {
         selection = BoundsRectAndMask.unselected();
      }
      synchronized (selectionLock_) {
         selection_ = selection;
      }
      if (getDisplaySettings().isROIAutoscaleEnabled()) {
         // This is the thread-safe way to trigger a redisplay
         Coords pos;
         do {
            pos = getDisplayPosition();
         } while (!compareAndSetDisplayPosition(pos, pos, true));
      }
   }

   public void setStatsComputeRateHz(double hz) {
      hz = Math.max(0.0, hz);
      long intervalNs;
      if (hz == 0.0) {
         intervalNs = Long.MAX_VALUE;
      }
      else if (Double.isInfinite(hz)) {
         intervalNs = 0;
      }
      else {
         intervalNs = (long) Math.round(1e9 / hz);
      }
      computeQueue_.setProcessIntervalNs(intervalNs);
   }

   public double getStatsComputeRateHz() {
      long intervalNs = computeQueue_.getProcessIntervalNs();
      if (intervalNs == Long.MAX_VALUE) {
         return 0.0;
      }
      if (intervalNs == 0) {
         return Double.POSITIVE_INFINITY;
      }
      return 1e9 / intervalNs;
   }

   public void setPlaybackAnimationAxes(String... axes) {
      synchronized (this) {
         playbackAxes_.clear();
         playbackAxes_.addAll(Arrays.asList(axes));
      }
      if (axes.length > 0) {
         resetDisplayIntervalEstimate();
         int initialTickIntervalMs = Math.max(17, Math.min(500,
               (int) Math.round(1000.0 / getPlaybackSpeedFps())));
         animationController_.setTickIntervalMs(initialTickIntervalMs);
         animationController_.startAnimation();
      }
      else {
         animationController_.stopAnimation();
      }
   }

   public boolean isAnimating() {
      return animationController_.isAnimating();
   }

   public double getPlaybackSpeedFps() {
      return animationController_.getAnimationRateFPS();
   }

   @MustCallOnEDT
   public void setPlaybackSpeedFps(double fps) {
      if (fps == animationController_.getAnimationRateFPS()) {
         return;
      }
      animationController_.setAnimationRateFPS(fps);
      int initialTickIntervalMs = Math.max(17, Math.min(500,
            (int) Math.round(1000.0 / fps)));
      animationController_.setTickIntervalMs(initialTickIntervalMs);
      uiController_.setPlaybackFpsIndicator(fps);
      resetDisplayIntervalEstimate();
   }

   public void setAxisAnimationLock(String state) {
      // TODO This is a string-based prototype
      AnimationController.NewPositionHandlingMode newMode;
      if (state.equals("U")) {
         newMode = AnimationController.NewPositionHandlingMode.JUMP_TO_AND_STAY;
      }
      else if (state.equals("F")) {
         newMode = AnimationController.NewPositionHandlingMode.FLASH_AND_SNAP_BACK;
      }
      else {
         newMode = AnimationController.NewPositionHandlingMode.IGNORE;
      }
      animationController_.setNewPositionHandlingMode(newMode);
   }

   //
   // Event handlers
   //

   // From the datastore
   @Subscribe
   public void onNewImage(final DataProviderHasNewImageEvent event) {
      perfMon_.sampleTimeInterval("NewImageEvent");

      // Generally we want to display new images (if not instructed otherwise
      // by the user), but we let the animation controller coordinate that with
      // any ongoing playback animation. Actual display of new images happens
      // upon receiving callbacks via the AnimationController.Listener
      // interface.
      animationController_.newDataPosition(event.getImage().getCoords());
   }


   //
   //
   //

   // A coalescent runnable to avoid excessively frequent update of the data
   // coords range in the UI
   private class ExpandDisplayRangeCoalescentRunnable
         implements CoalescentRunnable
   {
      private final List<Coords> coords_ = new ArrayList<Coords>();

      ExpandDisplayRangeCoalescentRunnable(Coords coords) {
         coords_.add(coords);
      }

      @Override
      public Class<?> getCoalescenceClass() {
         return getClass();
      }

      @Override
      public CoalescentRunnable coalesceWith(CoalescentRunnable another) {
         coords_.addAll(
               ((ExpandDisplayRangeCoalescentRunnable) another).coords_);
         return this;
      }

      @Override
      public void run() {
         if (uiController_ == null) {
            return;
         }
            uiController_.expandDisplayedRangeToInclude(coords_);
      }
   }


   //
   // Implementation of DisplayWindow interface
   //

   @Override
   public DataProvider getDataProvider() {
      // No threading concerns because final
      return dataProvider_;
   }

   @Override
   public List<Image> getDisplayedImages() throws IOException {
      // TODO Make sure this is accurate for composite and single-channel

      // If the current position is the empty coords, we do not want to
      // return all the images in the datastore!
      Coords pos = getDisplayPosition();
      if (pos.isEmpty()) {
         // TODO This check is not correct for composite mode
         if (dataProvider_.getNumImages() != 1) {
            return Collections.emptyList();
         }
      }

      return dataProvider_.getImagesMatching(pos);
   }

   @Override
   public boolean isVisible() {
      return !isClosed() && uiController_.getFrame().isVisible();
   }

   @Override
   public boolean isClosed() {
      if (!SwingUtilities.isEventDispatchThread()) {
         RunnableFuture<Boolean> edtFuture = new FutureTask(() -> isClosed());
         SwingUtilities.invokeLater(edtFuture::run);
         try {
            return edtFuture.get();
         }
         catch (InterruptedException notUsedByUs) {
            Thread.currentThread().interrupt();
            return isClosed();
         }
         catch (ExecutionException unexpected) {
            throw new RuntimeException(unexpected);
         }
      }

      return (uiController_ == null);
   }

   public String getChannelName(int channelIndex) {
      if (dataProvider_ != null) {
         return dataProvider_.getSummaryMetadata().getChannelNameList().
                 get(channelIndex);
      }
      return null;
   }

   private String getDefaultTitle() {
      if (dataProvider_ != null) {
         return dataProvider_.getName();
      }
      else {
         return DEFAULT_TITLE;
      }
   }

   @Override
   public String getName() {
      String title = customTitle_ != null ? customTitle_ : getDefaultTitle();
      // TODO Replace the hashCode with the (a), (b), ... suffix
      return title + "-" + hashCode();
   }

   @Override
   public void displayStatusString(String status) {
      throw new UnsupportedOperationException();
   }

   @Override
   public double getZoom() {
      throw new UnsupportedOperationException();
   }

   @Override
   public void setZoom(double ratio) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void adjustZoom(double factor) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void autostretch() {
      throw new UnsupportedOperationException();
   }

   @Override
   public ImagePlus getImagePlus() {
      if (!SwingUtilities.isEventDispatchThread()) {
         RunnableFuture<ImagePlus> edtFuture = new FutureTask(
               new Callable<ImagePlus>() {
            @Override
            public ImagePlus call() throws Exception {
               return getImagePlus();
            }
         });
         SwingUtilities.invokeLater(edtFuture);
         try {
            return edtFuture.get();
         }
         catch (InterruptedException notUsedByUs) {
            Thread.currentThread().interrupt();
            return getImagePlus();
         }
         catch (ExecutionException unexpected) {
            throw new RuntimeException(unexpected);
         }
      }

      return uiController_.getIJImagePlus();
   }

   @Override
   public boolean requestToClose() {
      if (!SwingUtilities.isEventDispatchThread()) {
         RunnableFuture<Boolean> edtFuture = new FutureTask(
               new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
               return requestToClose();
            }
         });
         SwingUtilities.invokeLater(edtFuture);
         try {
            return edtFuture.get();
         }
         catch (InterruptedException notUsedByUs) {
            Thread.currentThread().interrupt();
            return requestToClose();
         }
         catch (ExecutionException unexpected) {
            throw new RuntimeException(unexpected);
         }
      }

      // TODO The following logic should be implemented by AbstractDataViewer.
      DataViewerDelegate delegate;
      for (;;) {
         delegate = delegate_;
         if (delegate == null) {
            break;
         }

         boolean okToClose = delegate.dataViewerShouldClose(this);
         if (!okToClose) {
            return false;
         }

         if (delegate != delegate_) {
            // delegate has been changed by the original delegate;
            // we must now check with the new delegate
            continue;
         }
         break;
      }

      close();
      return true;
   }

   @Override
   public void close() {
      // Prevent closing more than once (even though that would be a bug)
      if (closed_) {
         return;
      }
      closed_ = true;

      postEvent(DataViewerWillCloseEvent.create(this));
      // TODO: We need to handle the case of multiple viewers. Of course, if
      // the user closes one by one, only the last remaining viewer's settings
      // will be saved; but if all closed at the same time...?
      // TODO The display settings should be saved periodically, not just when
      // closing the viewer (but handle multiple viewers carefully!)
      if (dataProvider_ instanceof Datastore) {
         Datastore ds = (Datastore) dataProvider_;
         if (ds.getSavePath() != null) {
            ((DefaultDisplaySettings) getDisplaySettings()).save(ds.getSavePath());
         }
      }
      try {
         computeQueue_.shutdown();
      } catch (InterruptedException ie) {
         // TODO: report exception
      }
      animationController_.shutdown();
      DefaultEventManager.getInstance().unregisterForEvents(this);
      dataProvider_.unregisterForEvents(this);
      if (uiController_ != null) {
         uiController_.close();
      }
      uiController_ = null;
      dispose();

      // TODO This event should probably be posted in response to window event
      // (which can be done with a ComponentListener for the JFrame)
      postEvent(DataViewerDidBecomeInvisibleEvent.create(this));
   }

   // TODO Generalize to DataProvider
   @Subscribe
   public void onDatastoreClosing(DatastoreClosingEvent event) {
      if (event.getDatastore().equals(dataProvider_)) {
         requestToClose();
      }
   }

   @Subscribe
   public void onNewDataProviderName(DataProviderHasNewNameEvent dpnne) {
      uiController_.updateTitle();
   }

   @Override
   public void setFullScreen(boolean enable) {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean isFullScreen() {
      throw new UnsupportedOperationException();
   }

   @Override
   public void toggleFullScreen() {
      throw new UnsupportedOperationException();
   }

   @Override
   public DisplayWindow duplicate() {
      DisplayWindow dup = DefaultDisplayManager.getInstance().createDisplay(dataProvider_);
      dup.setDisplaySettings(this.getDisplaySettings());
      return dup;
   }

   @Override
   public void toFront() {
      if (!SwingUtilities.isEventDispatchThread()) {
         SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
               toFront();
            }
         });
      }

      if (uiController_ == null) {
         return;
      }
      uiController_.toFront();
   }

   @Override
   public Window getWindow() throws IllegalStateException {
      if (!SwingUtilities.isEventDispatchThread()) {
         RunnableFuture<Window> edtFuture = new FutureTask(
               new Callable<Window>() {
            @Override
            public Window call() throws Exception {
               return getWindow();
            }
         });
         SwingUtilities.invokeLater(edtFuture);
         try {
            return edtFuture.get();
         }
         catch (InterruptedException notUsedByUs) {
            Thread.currentThread().interrupt();
            return getWindow();
         }
         catch (ExecutionException e) {
            if (e.getCause() instanceof IllegalStateException) {
               throw (IllegalStateException) e.getCause();
            }
            throw new RuntimeException(e);
         }
      }

      if (uiController_ == null) {
         throw new IllegalStateException(
               "Display has closed; no window available");
      }
      return uiController_.getFrame();
   }

   @Override
   public void setCustomTitle(String title) {
      customTitle_ = title;
      uiController_.updateTitle();
   }

   @Override
   public ImageExporter.Builder imageExporterBuilder() {
      return new AbstractImageExporter.Builder() {
         @Override
         public ImageExporter build() throws IllegalArgumentException {
            return new ImageExporter(this);
         }
      };
   }

   private class ImageExporter extends AbstractImageExporter {
      private ImageExporter(AbstractImageExporter.Builder builder) {
         super(builder);
      }

      @Override
      protected ListenableFuture<BufferedImage> render(Coords coords) {
         DisplayController self = DisplayController.this;
         BufferedImage image = new BufferedImage(
               self.uiController_.getImageWidth(),
               self.uiController_.getImageHeight(),
               BufferedImage.TYPE_INT_RGB);

         ListenableFutureTask<BufferedImage> future =
               ListenableFutureTask.create(() -> {}, image);

         SwingUtilities.invokeLater(() -> {
            self.uiController_.scheduleOffScreenImage(image).
                  addListener(future::run, MoreExecutors.directExecutor());
            self.setDisplayPosition(coords, true);
         });

         return future;
      }
   }
}
