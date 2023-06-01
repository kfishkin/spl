package fam.fishkin.spl;

// e.g. Seattle Public Library books, or Netflix movies, or ...
public interface MediaDomain {
  public String getName();
  /**
   * Read the file for the desired list of media items
   * @param fileName the file name
   * @return the list of media items
   */
  public Iterable<MediaItem> readFile(String fileName);
  /**
   * Update the file of media items and their status
   * @param newList the new list
   * @param fileName the file to write to
   */
  public void updateFile(Iterable<MediaItem> newList, String fileName);
  /**
   * Find the web elements with the set of candidates for the desired media item.
   * @param item the desired item
   * @return the web elements that span the candidates.
   */
  public Candidates findWebCandidates(MediaItem item);
  /**
   * find the best match for the desired item from a web list of candidates.
   * @param item The item you want
   * @param candidates the initial candidates
   * @return the BestMatch.
   */
  public BestMatch findBestMatch(MediaItem item, Candidates candidates);
}
