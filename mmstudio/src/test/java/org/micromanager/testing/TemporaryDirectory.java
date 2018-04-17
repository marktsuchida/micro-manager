package org.micromanager.testing;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TemporaryDirectory implements Closeable, AutoCloseable {
   private final Path tempDir_;

   public TemporaryDirectory(Class<?> testClass) throws IOException {
      tempDir_ = Files.createTempDirectory(testClass.getSimpleName());
   }

   @Override
   public void close() throws IOException {
      List<Path> paths = Files.walk(tempDir_).collect(Collectors.toList());
      Collections.reverse(paths);
      for (Path p : paths) {
         Files.deleteIfExists(p);
      }
      Files.deleteIfExists(tempDir_);
   }

   public Path getPath() {
      return tempDir_;
   }
}
