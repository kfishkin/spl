package spl;

import java.util.ArrayList;

/**
 * Book represents a book.
 * @author Ken Fishkin
 *
 */
public class Book implements Comparable<Book> {
  public String author;
  public String title;
  public Format format;
  public String recommender;
  public String rawFormat;

  public String toString() {
    String answer = author + ":" + title;
    answer += ". " + rawFormat;
    if (recommender != null && !recommender.isEmpty()) {
      answer += " [from " + recommender + "]";
    }
    return answer;
  }

  public Book(String author, String title, String recommender, Format format) {
    this.author = author;
    this.title = title;
    this.recommender = recommender;
    this.format = format;
  }

  @Override
  public int compareTo(Book other) {
    int sigma = this.author.toLowerCase().compareTo(other.author.toLowerCase());
    if (sigma != 0) {
      return sigma;
    }
    return this.title.toLowerCase().compareTo(other.title.toLowerCase());
  }

  /**
   * Upgrade a book to a more desirable format.
   */
  public void upgrade(Format newFormat, int year) {
    System.out.printf("UPGRADE of %s to %s\n", this.toString(), newFormat.toString());
    this.format = newFormat;
    String prefix = newFormat.fileFormats.iterator().next();
    // put the last 2 digits of the year.
    String year4 = String.format("%d", year);
    String year2 = year4.substring(2);
    this.rawFormat = prefix + year2;
    
  }
  
  /**
   * Chooses (k) books from a list of them of unknown size.
   * could read them all into an array and then shuffle them, but this way it scales to lists
   * of arbitrary size, and was a standard google interview question :)
   */
  public static Iterable<Book> pickK(Iterable<Book> books, int k) {
    // when you see the i'th value in the stream, it has odds of becoming a winner of k/i
    // if it becomes a winner, a loser is chosen randomly.
    ArrayList<Book> winners = new ArrayList<Book>();
    int i = 0;
    for (Book book: books) {
      i++;
      if (i <= k) { // automatic winner
        winners.add(book);
        continue;
      }
      double odds = ((double) k) / ((double) i);
      double roll = Math.random();
      if (roll < odds) {
        // we've got a winner. First, pick a loser.....
        int loserIndex =(int) (Math.random() * winners.size());
        winners.remove(loserIndex);
        winners.add(book);
      }
    }
    return winners;
  }
}
