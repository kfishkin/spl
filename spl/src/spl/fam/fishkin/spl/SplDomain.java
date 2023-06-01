package fam.fishkin.spl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class SplDomain implements MediaDomain {
  final static  String COMMENT = "#";

  @Override
  public String getName() {
    return "SPL Library";
  }

  @Override
  public Iterable<MediaItem> readFile(String fileName) {
    ArrayList<MediaItem> bookList = new ArrayList<MediaItem>();
    final String splitOn = "\t";
    try {
      BufferedReader br = new BufferedReader(new FileReader(fileName));
      for (;;) {
        String line;
        try {
          line = br.readLine();
        } catch (IOException e) {
          break;
        }
        if (line == null) {
          break;
        }
        if (line.startsWith(COMMENT)) {
          continue;
        }
        // each line is <author><title>[<recommender>]<status>,
        // separated by tabs. Could use a CSV reader here, but it's so straightforwards, just do it
        // by hand.
        String[] args = line.split(splitOn);
        int len = args.length;
        if (len < 3) {
          System.err.printf("only %d fields in line '%s' - need at least 3\n", len, line);
          continue;
        } else if (len > 4) {
          System.err.printf("%d fields in line '%s' - need at most 4\n", len, line);
          continue;
        }
        String author = args[0];
        String title = args[1];
        String recommender = args[2];
        String status = "??";
        if (len >= 4) {
          status = args[3];
        } else {
          status = recommender;
          recommender = "";
        }
        Format format = Format.fromFile(status);
        Book book = new Book(author, title, recommender, format);
        book.rawFormat = status;
        bookList.add(book);
      }
      return bookList;
    } catch (FileNotFoundException e) {
      System.err.printf("could not read input file %s\n", fileName);
      return null;
    }
  }

  @Override
  public void updateFile(Iterable<MediaItem> newList, String fileName) {
    File destFile = new File(fileName);
    if (!destFile.canWrite()) {
      System.err.printf("cannot write destination file %s\n", fileName);
      return;
    }    
    // to protect against a crash in mid-write, write to a temp file,
    // then rename that temp file.
    File tempFile;
    PrintWriter out;
    try {
      tempFile = File.createTempFile("temp", null);
      System.out.println(tempFile.getAbsolutePath());
    } catch (IOException e) {
      System.err.println("could not create temp file:" + e);
      return;
    }
    try {
      out = new PrintWriter(new BufferedWriter(new FileWriter(tempFile)));
    } catch (IOException e) {
      System.err.println("Could not create PrintWriter:" + e);
      return;
    }
    DateFormat dateFormat = new SimpleDateFormat("EE, MMMM d yyyy");
    Calendar cal = Calendar.getInstance();
    out.printf("%s status as of %s\n", COMMENT, dateFormat.format(cal.getTime()));

    for (MediaItem mediaItem : newList) {
      if (!(mediaItem instanceof Book)) {
        continue;
      }
      Book book = (Book) mediaItem;
      out.printf("%s\t%s", book.author, book.title);
      if (book.recommender != null && !book.recommender.isEmpty()) {
        out.printf("\t%s", book.recommender);
      }
      out.printf("\t%s\n", book.rawFormat);
    }
    out.close();
    try {
      if (!destFile.delete()) {
        System.err.println("could not delete destination file " + destFile);
        return;
      }
      tempFile.renameTo(destFile);
    } catch (SecurityException e) {
      System.err.printf("couldn't rename output file: %s\n", e);
    }
    
  }

  @Override
  public Candidates findWebCandidates(MediaItem item) {
    if (!(item instanceof Book)) {
      return null;
    }
    return new Candidates(WebHelper.findCandidates((Book) item));
  }

  @Override
  public BestMatch findBestMatch(MediaItem desired, Candidates candidates) {
    if (desired == null || candidates == null) {
      return null;
    }
    Elements elts = candidates.asElements();
    if (elts == null || elts.isEmpty()) {
    	return null;
    }
    BestMatch best = new BestMatch(desired, null, Integer.MAX_VALUE, null, Integer.MAX_VALUE, Format.NO);
    // strip off everything after a ":" in the title - subheadings are in a different place on the web page,
    // and can vary too much.
    String title = desired.getTitle();
    int idx = title.indexOf(':');
    String compareTitle = (idx == -1) ? title : title.substring(0, idx);
    compareTitle = compareTitle.trim().toLowerCase();
    String compareAuthor = desired.getAuthor().trim().toLowerCase();
    
    for (Element result: elts) {
      BestMatch bestThisItem = new BestMatch(desired, null, Integer.MAX_VALUE, null, Integer.MAX_VALUE, Format.UNKNOWN);
      Element titleElt = result.selectFirst("h2.cp-title span.title-content");
      int dist = Distance.LevenshteinDistance(compareTitle, titleElt.text().trim().toLowerCase());
      if (dist < best.titleDistance) {
        bestThisItem.titleDistance = dist;
        bestThisItem.bestTitle = titleElt.text().trim();
      }
      Elements authorElts = result.select("span.cp-author-link");
      for (Element authorElt: authorElts) {
        dist = Distance.LevenshteinDistance(compareAuthor, authorElt.text().trim().toLowerCase());
        if (dist < best.authorDistance) {
          bestThisItem.authorDistance = dist;
          bestThisItem.bestAuthor = authorElt.text().trim();
        }
      }
      Elements formatElts = result.select("span.cp-format-indicator");
      for (Element formatElt : formatElts) {
        Format format = Format.fromWeb(formatElt.text());
        if (format.value > best.bestFormat.value) {
          bestThisItem.bestFormat = format;
        }
      }
      if (bestThisItem.titleDistance <= best.titleDistance
          && bestThisItem.authorDistance <= best.authorDistance) {
        best = bestThisItem;
      }
    }
    return best;
  }
    
}
