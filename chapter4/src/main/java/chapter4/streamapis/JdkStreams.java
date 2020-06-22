package chapter4.streamapis;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class JdkStreams {

  public static void main(String[] args) {
    File file = new File("build.gradle.kts");
    byte[] buffer = new byte[1024];
    try (FileInputStream in = new FileInputStream(file)) {
      int count = in.read(buffer);
      while (count != -1) {
        System.out.println(new String(buffer, 0, count));
        count = in.read(buffer);
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      System.out.println("\n--- DONE");
    }
  }
}
