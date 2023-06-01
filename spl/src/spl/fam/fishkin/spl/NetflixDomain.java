package fam.fishkin.spl;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class NetflixDomain implements MediaDomain {
  // get all the Netflix movies at once. We want the query for a particular
  // movie to still be in HTML form (as Elements). And we want to support near-matches,
  // so don't just put 'em all in a hash table keyed by Table.
  // however, we will map by first two letters, as otherwise it's way too slow.
  private Map<String, Elements> moviesMap = null;

  @Override
  public String getName() {
    return "Netflix movies";
  }

  @Override
  public Iterable<MediaItem> readFile(String fileName) {
    ArrayList<MediaItem> movieList = new ArrayList<MediaItem>();
    final String splitOn = ",";
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
        // IMDb watchlists, in CSV, are comma-separated, with dquotes used
        // if a field contains commas. The title is field #6, the year
        // is field #11 (numbering starting at 1).
        // separated by tabs. Could use a CSV reader here, but it's so straightforwards, just do it
        // by hand.
        String[] args = line.split(splitOn);
        int len = args.length;
        if (len < 11) {
          System.err.printf("only %d fields in line '%s' - need at least 11\n", len, line);
          continue;
        }
        String title = args[5];
        title = title.replace('"', ' ').strip();
        int year = 0;
        try {
          year = Integer.parseInt(args[10]);
        } catch (NumberFormatException e) {

        }
        System.out.printf("movie: title=[%s], year = %d\n", title, year);
        if (year > 0) {
          Movie movie = new Movie(title, year);
          movieList.add(movie);
        }
      }
      return movieList;
    } catch (FileNotFoundException e) {
      System.err.printf("could not read input file %s\n", fileName);
      return null;
    }
  }

  @Override
  public void updateFile(Iterable<MediaItem> newList, String fileName) {
    for (MediaItem item : newList) {
      if (item.isInDesiredFormat()) {
        System.out.println(item);
      }
    }
  }
  
  private String getKey(String k) {
    if (k == null || k.isEmpty()) {
      return "";
    }
    k = k.toLowerCase();
    if (k.length() < 2) {
      return k;
    }
    return k.substring(0,2);
  }

  @Override
  public Candidates findWebCandidates(MediaItem item) {
    if (!(item instanceof Movie)) {
      return null;
    }
    if (moviesMap == null) {
      moviesMap = new HashMap<String, Elements>();
      String base = "https://www.finder.com/netflix-movies";
      String encoded = base; // no query params
      final String userAgent =
          "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.122 Safari/537.36";
      Connection connection = Jsoup.connect(encoded).userAgent(userAgent);
      Document doc;
      try {
        doc = connection.get();
      } catch (IOException e) {
        System.err.printf("toResultsList: IO exception %s\n", e.toString());
        return null;
      }
      Elements resultList = doc.select("div.ts-table-container");
      System.out.printf("size1 = %d\n", resultList.size());
      resultList = resultList.select("table.luna-table");
      System.out.printf("size2 = %d\n", resultList.size());
      resultList = resultList.select("tr");
      System.out.printf("size3 = %d\n", resultList.size());
      for (Element row : resultList) {
        Element titleElement = row.selectFirst("td[data-title='Title']");
        Element yearElement = row.selectFirst("td[data-title='Year of release']");
        if (titleElement == null || yearElement == null) {
          continue;
        }
        if (titleElement.text() == null || yearElement.text() == null) {
          continue;
        }
        String first = getKey(titleElement.text());
        Elements current = moviesMap.get(first);
        if (current == null) {
          current = new Elements();
        }
        current.add(row);
        moviesMap.put(first, current);
        
      }
    }
    String first = getKey(item.getTitle());
    return new Candidates(moviesMap.get(first));
  }

  @Override
  public BestMatch findBestMatch(MediaItem desiredItem, Candidates candidates) {


	  if (desiredItem == null || candidates == null) {
		  return null;
	  }
	  Elements elts = candidates.asElements();
	  if (elts == null || elts.isEmpty()) {
		  return null;
	  }
	  if (!(desiredItem instanceof Movie)) {
		  return null;
	  }
    Movie desired = (Movie) desiredItem;
    BestMatch best = new BestMatch(desiredItem, null, Integer.MAX_VALUE, null, 0, Format.NO);
    String desiredTitle = desired.getTitle() + " (" + desired.year + ")";
    // by adding the year into the title, we can have a single distance metric.
    for (Element candidate : elts) {
      Element titleElement = candidate.selectFirst("td[data-title='Title']");
      Element yearElement = candidate.selectFirst("td[data-title='Year of release']");
      if (titleElement == null || yearElement == null) {
        continue;
      }
      if (titleElement.text() == null || yearElement.text() == null) {
        continue;
      }
      String thisTitle = titleElement.text() + " (" + yearElement.text() + ")";
      int thisDist = Distance.LevenshteinDistance(desiredTitle, thisTitle);
      if (thisDist < best.titleDistance) {

        best.bestTitle = thisTitle;
        best.titleDistance = thisDist;
        best.bestFormat = Format.VIDEO;
      }
    }
    /*
    System.out.printf("looking for [%s], found [%s],  best distance of [%d]\n", desiredTitle,
        best.bestTitle, best.titleDistance);
        */
    return best;
  }
}
