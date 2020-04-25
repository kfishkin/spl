package spl;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * The possible formats a book can be available in.
 * @author Ken Fishkin
 *
 */
public enum Format implements Comparable<Format> {
  UNKNOWN(0, "??", Arrays.asList("??")),
  NO(1, "", Arrays.asList("NO", "XX")), 
  OTHER(2, "Audiobook",
      Arrays.asList("Audio")), 
  VIDEO(2, "Video", Arrays.asList("Video")),
  PHYSICAL(3, "Book", Arrays.asList("H", "Book")),
  EBEPUB(4, "",
          Arrays.asList("EBEPUB")),
  EBOOK(5, "eBook",
              Arrays.asList("eBook", "EB")),
  READ(6, "", Arrays.asList("READ"));

  Format(int value, String webFormat, Iterable<String> fileFormats) {
    this.value = value;
    this.webFormat = webFormat;
    this.fileFormats = fileFormats;
  }

  public int value; // higher values are more desirable
  public String webFormat; // as described on the web site
  public Iterable<String> fileFormats; // as described in the TDF file

  public static Format fromWeb(String onWeb) {
    onWeb = onWeb.toLowerCase();
    final Set<Format> values = EnumSet.of(OTHER, PHYSICAL, EBEPUB, EBOOK);
    for (Format format : values) {
      if (onWeb.equals(format.webFormat.toLowerCase())) {
        return format;
      }
    }
    return Format.UNKNOWN;
  }

  public static Format fromFile(String inFile) {
    inFile = inFile.toLowerCase();
    // and may end with a number....
    int idx = inFile.length() - 1;
    for (; idx > 0; idx--) {
      char c = inFile.charAt(idx);
      if (c < '0' || c > '9') {
        break;
      }
    }
    inFile = inFile.substring(0, idx + 1);
    final Set<Format> values = EnumSet.of(UNKNOWN, OTHER, NO, PHYSICAL, EBEPUB, EBOOK, READ);
    for (Format format : values) {
      for (String option : format.fileFormats) {
        if (!option.isEmpty() && inFile.startsWith(option.toLowerCase())) {
          return format;
        }
      }
    }
    return Format.UNKNOWN;
  }
}
