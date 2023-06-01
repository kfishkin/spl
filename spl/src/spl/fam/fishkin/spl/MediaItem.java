package fam.fishkin.spl;

import java.util.ArrayList;

/**
 * A 'MediaItem' is a piece of consumable media. Presently, book or movie.
 * @author Us
 *
 */
public interface MediaItem {
  public Format getFormat();
  public boolean isRead();
  public boolean isNew();
  public boolean wouldBeAnUpgrade(Format current);
  public boolean isInDesiredFormat();
  public String getTitle();
  public String getAuthor();

  /**
   * Chooses (k) books from a list of them of unknown size.
   * could read them all into an array and then shuffle them, but this way it scales to lists
   * of arbitrary size, and was a standard google interview question :)
   */
  public static Iterable<MediaItem> pickK(Iterable<MediaItem> items, int k) {
    // when you see the i'th value in the stream, it has odds of becoming a winner of k/i
    // if it becomes a winner, a loser is chosen randomly.
    ArrayList<MediaItem> winners = new ArrayList<MediaItem>();
    int i = 0;
    for (MediaItem item: items) {
      i++;
      if (i <= k) { // automatic winner
        winners.add(item);
        continue;
      }
      double odds = ((double) k) / ((double) i);
      double roll = Math.random();
      if (roll < odds) {
        // we've got a winner. First, pick a loser.....
        int loserIndex =(int) (Math.random() * winners.size());
        winners.remove(loserIndex);
        winners.add(item);
      }
    }
    return winners;
  }
  public void upgrade(Format bestFormat, int year);
}
