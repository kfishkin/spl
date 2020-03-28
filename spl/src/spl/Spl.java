package spl;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import org.jsoup.select.Elements;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;

/**
 * The main class for the program. Given a tab-delimited-format (TDF)
 * file with known information about desired books, scrapes the
 * Seattle Public Library web site to find whether those books are in the
 * catalog, and if so in which best format.
 * @author Ken Fishkin
 *
 */
public class Spl {
  private static final int NUM_WINNERS = 2;

  /**
   * Picks a subset of eBooks from a list.
   * @param bookList list of books
   * @param k how many to pick
   * @return subset of size (k) or less.
   */
  public static Iterable<Book> pickK(Iterable<Book> bookList, int k) {
    // only take the ones in ebook form:
    Set<Book> ebooks = new HashSet<Book>();
    for (Book book : bookList) {
      if (book.format.equals(Format.EBOOK)) {
        ebooks.add(book);
      }
    }
    Iterable<Book> winners = Book.pickK(ebooks, NUM_WINNERS);
    return winners;
  }

  public static void main(String[] args) {
    int skip = 0;
    ArgumentParser parser =
        ArgumentParsers.newFor("spl").build().description("Check books against a library catalog");
    parser.addArgument("-f", "-file", "--f", "--file").help("path to the input file");
    parser.addArgument("-skip").help("skip <x> input records");
    parser.addArgument("-from").help("FROM email address");
    parser.addArgument("-to").help("TO email address");
    parser.addArgument("-apikey").help("Sendgrid API key");
    Namespace nameSpace = parser.parseArgsOrFail(args);
    String f = nameSpace.getString("f");
    // 'skip' is useful when debugging, to get right to a problematic input record.
    String temp = nameSpace.getString("skip");
    if (temp != null && !temp.isBlank()) {
      skip = Integer.parseInt(temp);
    }

    // read in all the books at once so we can close the input file.
    Iterable<Book> bookList = TdfIO.Read(f);
    if (bookList == null) {
      System.exit(0);
    }

    int numIn = 0;
    int numChecked = 0;
    int numUpgrades = 0;
    Map<Book, BestMatch> upgrades = new TreeMap<Book, BestMatch>();

    final int MIN_DISTANCE = 3;
    final int MAX_DISTANCE = 9;
    final int REPORT_EVERY = 2;
    for (Book book : bookList) {
      numIn++;
      if (skip > 0) {
        skip--;
        continue;
      }
      System.out.printf("%d: %s\n", numIn, book.toString());
      if (book.format.equals(Format.READ)) {
        continue;
      }
      numChecked++;
      Elements elements = WebHelper.toResultsList(book);
      // JSoup recommends waiting a few seconds between pings...
      final long SLEEP_TIME_MS = 2000;
      try {
        Thread.sleep(SLEEP_TIME_MS);
      } catch (InterruptedException e) {
      }
      if (elements == null) {
        continue;
      }
      BestMatch best = BestMatch.bestOnWeb(book, elements);
      if (best == null) {
        // System.out.printf("line %d: no best match for %s\n", numIn, book.toString());
        continue;
      }
      if ((numIn % REPORT_EVERY) == 0) {
        System.out.printf("\t%s\n", best.toString());
      }
      if (best.titleDistance > MAX_DISTANCE || best.authorDistance > MAX_DISTANCE) {
        // if the book was UNKNOWN, upgrade to NO.
        if (book.format.equals(Format.UNKNOWN)) {
          numUpgrades++;
          best.bestFormat = Format.NO;
          upgrades.put(book, best);
        }
        continue;
      }
      if (best.titleDistance > MIN_DISTANCE) {
        System.err.printf("line %d: best title found, [%s], too far away (%d) on [%s].\n", numIn,
            best.bestTitle, best.titleDistance, book.toString());
        continue;
      }
      if (best.authorDistance > MIN_DISTANCE) {
        System.err.printf("line %d: best author found, [%s], too far away (%d) on [%s].\n", numIn,
            best.bestAuthor, best.authorDistance, book.toString());
        continue;
      }
      if (best.bestFormat.value > book.format.value) {
        System.out.printf("line %d: UPGRADE of %s to %s\n", numIn, book.toString(),
            best.bestFormat.toString());
        numUpgrades++;
        upgrades.put(book, best);
      }
    }
    System.out.printf("Done: %d books read, %d checked, %d upgrades\n", numIn, numChecked,
        numUpgrades);
    Calendar cal = Calendar.getInstance();
    int year = cal.get(Calendar.YEAR);
    for (Entry<Book, BestMatch> entry : upgrades.entrySet()) {
      Book book = entry.getKey();
      BestMatch match = entry.getValue();
      book.upgrade(match.bestFormat, year);
    }
    TdfIO.write(bookList, f);
    Iterable<Book> winners = pickK(bookList, 2);
    System.out.printf("Here are %d ebooks from the list:\n", NUM_WINNERS);
    for (Book book : winners) {
      System.out.println(book);
    }
    String fromEmail = nameSpace.getString("from");
    String toEmail = nameSpace.getString("to");
    String apiKey = nameSpace.getString("apikey");
    if (fromEmail != null && !fromEmail.isBlank() && toEmail != null && !toEmail.isBlank()
        && apiKey != null && !apiKey.isBlank()) {
      Mailer.sendMail(fromEmail, toEmail, numChecked, upgrades, winners, apiKey);
    }
  }

}
