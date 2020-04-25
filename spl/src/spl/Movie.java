package spl;

public class Movie implements MediaItem, Comparable<Movie> {
  String title;
  int year; // year of the movie.
  Format format;

  public Movie(String title, int year) {
    super();
    this.title = title;
    this.year = year;
    this.format = Format.UNKNOWN;
  }
  
  @Override
  public String toString() {
    return "Movie '" + title + "' (" + year + ")";
  }

  @Override
  public Format getFormat() {
    return format;
  }

  @Override
  public boolean isRead() {
    return false;
  }

  @Override
  public boolean isNew() {
    return format.equals(Format.UNKNOWN);
  }

  @Override
  public boolean wouldBeAnUpgrade(Format current) {
    return format.value <= current.value;
  }

  @Override
  public boolean isInDesiredFormat() {
    return format.equals(Format.VIDEO);
  }

  @Override
  public String getTitle() {
    return title;
  }

  @Override
  public String getAuthor() {
    return "Netflix";
  }

  @Override
  public void upgrade(Format bestFormat, int year) {
    format = bestFormat;
  }

  @Override
  public int compareTo(Movie other) {
      return this.title.toLowerCase().compareTo(other.title.toLowerCase());
  }

}
