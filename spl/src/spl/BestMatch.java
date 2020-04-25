package spl;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Class for finding the best match to a desired title,
 * from the set of titles found in the Web catalog.
 * @author Ken Fishkin
 *
 */
public class BestMatch {
  public MediaItem desired;
  public String bestTitle;
  public int titleDistance;
  public String bestAuthor;
  public int authorDistance;
  public Format bestFormat;
  
  public BestMatch(MediaItem desired, String bestTitle, int titleDistance, String bestAuthor,
      int authorDistance, Format bestFormat) {
    super();
    this.desired = desired;
    this.bestTitle = bestTitle;
    this.titleDistance = titleDistance;
    this.bestAuthor = bestAuthor;
    this.authorDistance = authorDistance;
    this.bestFormat = bestFormat;
  }



  @Override
  public String toString() {
    return "BestMatch: title=" + bestTitle + " ("
        + titleDistance + "), author=" + bestAuthor + " (" + authorDistance
        + "), format =" + bestFormat + "]";
  }



  /**
   * Finds the best match to a desired book.
   * @param desired The desired book
   * @param contentList the search results from the Web site.
   * @return the Best match, null if none.
   */
  public static BestMatch bestOnWeb(Book desired, Elements contentList) {
    if (desired == null || contentList == null || contentList.isEmpty()) {
      return null;
    }
    BestMatch best = new BestMatch(desired, null, Integer.MAX_VALUE, null, Integer.MAX_VALUE, Format.NO);
    // strip off everything after a ":" in the title - subheadings are in a different place on the web page,
    // and can vary too much.
    int idx = desired.title.indexOf(':');
    String compareTitle = (idx == -1) ? desired.title : desired.title.substring(0, idx);
    compareTitle = compareTitle.trim().toLowerCase();
    String compareAuthor = desired.author.trim().toLowerCase();
    
    for (Element result: contentList) {
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
