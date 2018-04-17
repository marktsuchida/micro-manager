package org.micromanager.data.internal.singlepagetiff;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.micromanager.data.Coordinates;
import org.micromanager.data.Image;
import org.micromanager.data.Metadata;
import org.micromanager.data.Storage;
import org.micromanager.data.internal.DefaultDatastore;
import org.micromanager.data.internal.DefaultImage;
import org.micromanager.data.internal.DefaultMetadata;
import org.micromanager.testing.TemporaryDirectory;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class StorageSinglePlaneTiffSeriesTest {
   static Image SMALL_IMAGE;
   TemporaryDirectory tempDir_;
   Path tempPath_;

   @BeforeAll
   public static void initAll() {
      Metadata md = new DefaultMetadata.Builder().build();
      SMALL_IMAGE = new DefaultImage(new byte[] { 0, 1, 2, 3 }, 2, 2, 1, 1, Coordinates.emptyCoords(), md);
   }

   @BeforeEach
   public void init() throws IOException {
      tempDir_ = new TemporaryDirectory(getClass());
      tempPath_ = tempDir_.getPath();
   }

   @AfterEach
   public void tearDown() throws IOException {
      tempDir_.close();
   }

   @Test
   public void testRejectExistingDirectory() {
      DefaultDatastore store = new DefaultDatastore();
      assertThrows(IOException.class, () -> new StorageSinglePlaneTiffSeries(store, tempPath_.toString(), true));
   }

   @Test
   public void testWriteEmpty() throws IOException {
      Path storageDir = tempPath_.resolve("foo");
      DefaultDatastore store = new DefaultDatastore();
      Storage storage = new StorageSinglePlaneTiffSeries(store, storageDir.toString(), true);
      storage.close();

      // Directory is not created until the first image is written
      assertFalse(Files.exists(storageDir));
   }

   @Test
   public void testWriteSingleImageProducesCorrectFiles() throws IOException {
      Path storageDir = tempPath_.resolve("foo");
      DefaultDatastore store = new DefaultDatastore();
      Storage storage = new StorageSinglePlaneTiffSeries(store, storageDir.toString(), true);
      storage.putImage(SMALL_IMAGE);
      storage.close();

      assertTrue(Files.isDirectory(storageDir));

      Path metadataPath = null;
      Path tiffPath = null;
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(storageDir)) {
         for (Path entry : stream) {
            System.out.println(entry);
            if (entry.getFileName().toString().equals("metadata.txt")) {
               metadataPath = entry;
            }
            if (entry.getFileName().toString().equals("img.tif")) {
               tiffPath = entry;
            }
         }
      }
      assertNotNull(metadataPath);
      assertNotNull(tiffPath);

      System.out.println(String.join("\n", Files.readAllLines(metadataPath)));

      // Validate JSON with Gson
      JsonElement je = new JsonParser().parse(new FileReader(metadataPath.toFile()));

      assertEquals(Collections.emptyMap(), JsonPath.read(metadataPath.toFile(), "$.Summary.UserData"));
      assertEquals(Integer.valueOf(0), JsonPath.read(metadataPath.toFile(), "$.['Coords-img.tif'].Frame"));
      assertEquals(Collections.emptyMap(), JsonPath.read(metadataPath.toFile(), "$.['Metadata-img.tif'].UserData"));
   }
}
