package spl;

import java.util.ArrayList;

/**
 * Book represents a book.
 * @author Ken Fishkin
 *
 */
public class Book implements Comparable<Book>, MediaItem {
  public String author;
  public String title;
  private Format format;
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
  
  @Override
  public Format getFormat() {
    return format;
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
  


  @Override
  public boolean isRead() {
    return format.equals(Format.READ);
  }

  @Override
  public boolean isNew() {
    return format.equals(Format.UNKNOWN);
  }

  @Override
  public boolean wouldBeAnUpgrade(Format current) {
    return this.format.value > current.value;
  }

  @Override
  public boolean isInDesiredFormat() {
    return format.equals(Format.EBOOK);
  }

  @Override
  public String getTitle() {
    return title;
  }

  @Override
  public String getAuthor() {
    return author;
  }

}
