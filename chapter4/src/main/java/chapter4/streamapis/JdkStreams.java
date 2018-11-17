package chapter4.streamapis;

import java.io.*;

public class JdkStreams {

  public static void main(String[] args) {
    try {
      File file = new File("build.gradle");
      BufferedReader reader = new BufferedReader(
        new InputStreamReader(new FileInputStream(file)));
      String line = reader.readLine();
      while (line != null) {
        System.out.println(line);
        line = reader.readLine();
      }
      reader.close();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      System.out.println("\n--- DONE");
    }
  }
}
