package fam.fishkin.spl;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

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
   * Picks a subset of media items that are in the best format.
   * @param bookList list of books
   * @param k how many to pick
   * @return subset of size (k) or less.
   */
  public static Iterable<MediaItem> pickK(Iterable<MediaItem> bookList, int k) {
    // only take the ones in ebook form:
    Set<MediaItem> ebooks = new HashSet<MediaItem>();
    for (MediaItem book : bookList) {
      if (book.isInDesiredFormat()) {
        ebooks.add(book);
      }
    }
    Iterable<MediaItem> winners = MediaItem.pickK(ebooks, NUM_WINNERS);
    return winners;
  }

  public static void main(String[] args) {
    int skip = 0;
    //ArgumentParser parser = ArgumentParsers.newFor("spl").build().description("spl looker-upper");
    ArgumentParser parser = ArgumentParsers.newArgumentParser("spl");
    parser.addArgument("-f", "-file", "--f", "--file").help("path to the input file");
    parser.addArgument("-skip").help("skip <x> input records");
    parser.addArgument("-from").help("FROM email address");
    parser.addArgument("-to").help("TO email address");
    parser.addArgument("-apikey").help("Sendgrid API key");
    parser.addArgument("-domain").help("domain: either 'spl' (default) or or 'splrss' or 'netflix'").setDefault("spl");
    Namespace nameSpace = parser.parseArgsOrFail(args);
    String f = nameSpace.getString("f");
    // 'skip' is useful when debugging, to get right to a problematic input record.
    String temp = nameSpace.getString("skip");
    if (temp != null && !temp.isBlank()) {
      skip = Integer.parseInt(temp);
    }
    String domainName = nameSpace.getString("domain");
    MediaDomain domain = null;
    if (domainName.equalsIgnoreCase("spl")) {
      domain = new SplDomain();
    } else if (domainName.equalsIgnoreCase("netflix")) {
      domain = new NetflixDomain();
    } else if (domainName.equalsIgnoreCase("splrss")) {
    	domain = new SplRssDomain();
    } else {
      System.err.printf("unknown domain: %s. Must be 'spl' or 'netflix'", domainName);
      System.exit(1);;
    }

    // read in all the books at once so we can close the input file.
    Iterable<MediaItem> bookList = domain.readFile(f);
    if (bookList == null) {
      System.exit(0);
    }

    int numIn = 0;
    int numChecked = 0;
    int numUpgrades = 0;
    Map<MediaItem, BestMatch> upgrades = new TreeMap<MediaItem, BestMatch>();
    Vector<String> messages = new Vector<String>();

    final int MIN_DISTANCE = 3;
    final int MAX_DISTANCE = 8;
    final int REPORT_EVERY = 4;
    for (MediaItem mediaItem : bookList) {
      numIn++;
      if (skip > 0) {
        skip--;
        continue;
      }
      System.out.printf("%d: %s\n", numIn, mediaItem.toString());
      if (mediaItem.isRead()) {
        continue;
      }
      numChecked++;
      Candidates candidates = domain.findWebCandidates(mediaItem);

      if (candidates == null) {
        System.out.println("no elements");
        continue;
      }
      BestMatch best = domain.findBestMatch(mediaItem, candidates);
      if (best == null) {
        System.out.printf("line %d: no best match for %s\n", numIn, mediaItem.toString());
        continue;
      }
      if ((numIn % REPORT_EVERY) == 0) {
        System.out.printf("\t%s\n", best.toString());
      }
      if (best.titleDistance > MAX_DISTANCE || best.authorDistance > MAX_DISTANCE) {
        // if the book was UNKNOWN, upgrade to NO.
        if (mediaItem.isNew()) {
          numUpgrades++;
          best.bestFormat = Format.NO;
          upgrades.put(mediaItem, best);
        }
        continue;
      }
      if (best.titleDistance > MIN_DISTANCE) {
        String msg = String.format("best title found, [%s], too far away (%d) on [%s]",
            best.bestTitle, best.titleDistance, mediaItem.toString());
        System.err.printf("line %d: %s.\n", numIn, msg);
        messages.add(msg);
        continue;
      }
      if (best.authorDistance > MIN_DISTANCE) {
        String msg = String.format("best author found, [%s], too far away (%d) on [%s]",
            best.bestAuthor, best.authorDistance, mediaItem.toString());
        System.err.printf("line %d: %s.\n", numIn, msg);
        messages.add(msg);
        continue;
      }
      if (mediaItem.wouldBeAnUpgrade(best.bestFormat)) {
        System.out.printf("line %d: UPGRADE of %s to %s\n", numIn, mediaItem.toString(),
            best.bestFormat.toString());
        numUpgrades++;
        upgrades.put(mediaItem, best);
      }
    }
    System.out.printf("Done: %d books read, %d checked, %d upgrades\n", numIn, numChecked,
        numUpgrades);
    Calendar cal = Calendar.getInstance();
    int year = cal.get(Calendar.YEAR);
    for (Entry<MediaItem, BestMatch> entry : upgrades.entrySet()) {
      MediaItem book = entry.getKey();
      BestMatch match = entry.getValue();
      book.upgrade(match.bestFormat, year);
    }
    domain.updateFile(bookList, f);
    Iterable<MediaItem> winners = pickK(bookList, 2);
    System.out.printf("Here are %d ebooks from the list:\n", NUM_WINNERS);
    for (MediaItem book : winners) {
      System.out.println(book);
    }
    String fromEmail = nameSpace.getString("from");
    String toEmail = nameSpace.getString("to");
    String apiKey = nameSpace.getString("apikey");
    if (fromEmail != null && !fromEmail.isBlank() && toEmail != null && !toEmail.isBlank()
        && apiKey != null && !apiKey.isBlank()) {
      Mailer.sendMail(fromEmail, toEmail, numChecked, messages, upgrades, winners, apiKey);
    }
  }

}
