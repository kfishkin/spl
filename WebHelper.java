package spl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Helper class for some of the Web querying/scraping.
 * @author Ken Fishkin
 *
 */
public class WebHelper {
  private static boolean firstTime = true;
  
  /**
   * finds the URL to use to ping the web site.
   * @param base the base part of the URL
   * @param queryParams the query params to use.
   * @return the final URL
   */
  public static String toURL(String base, Map<String, String> queryParams) {
    String charSet = StandardCharsets.UTF_8.toString();
    StringBuilder buf = new StringBuilder();
    buf.append(base);
    boolean firstTime = true;
    for (Map.Entry<String, String> entry: queryParams.entrySet()) {
      try {
        String encoded = URLEncoder.encode(entry.getValue(), charSet);
        buf.append(firstTime ? '?' : '&');
        firstTime = false;
        buf.append(entry.getKey() + "=" + encoded);
      } catch (UnsupportedEncodingException e) {
        System.err.printf("could not encode %s\n",  entry.getValue());
      }
    }
    return buf.toString();
  }
  
  /**
   * Query the SPL web site on a book
   * @param book the desired book
   * @return the list of results
   */
  public static Elements findCandidates(Book book) {
    if (book == null) {
      return null;
    }
    if (book.isRead()) {
      return null;
    }
    if (firstTime) {
      slurpNetflix();
      firstTime = false;
    }
    Map<String, String> queryParams = new HashMap<String, String>();
    queryParams.put("custom_edit", "false");
    queryParams.put("suppress", "true");
    queryParams.put("f_FORMAT", "EBOOK|BK");
    queryParams.put("searchType", "title");
    // strip subtitle, indicated by a colon:
    int idx = book.title.indexOf(':');
    String searchTitle = (idx == -1) ? book.title : book.title.substring(0, idx); 
    queryParams.put("query", searchTitle);
    String base = "https://seattle.bibliocommons.com/v2/search";
    String encoded = WebHelper.toURL(base, queryParams);
    final String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.122 Safari/537.36";
    Connection connection = Jsoup.connect(encoded)
        .userAgent(userAgent);
    Document doc;
      try {
        doc = connection.get();
      } catch (IOException e) {
        System.err.printf("toResultsList: IO exception on book '%s': %s\n", book.title, e.toString());
        return null;
      }
      Elements resultList = doc.select("div.cp-search-result-item-content");
      // JSoup recommends waiting a few seconds between pings...
      final long SLEEP_TIME_MS = 2000;
      try {
        Thread.sleep(SLEEP_TIME_MS);
      } catch (InterruptedException e) {
      }
      return resultList;
  }

  private static void slurpNetflix() {
    Map<String, String> queryParams = new HashMap<String, String>();
    //queryParams.put("custom_edit", "false");
    //queryParams.put("suppress", "true");
    //queryParams.put("f_FORMAT", "EBOOK|BK");
    //queryParams.put("searchType", "title");
    // strip subtitle, indicated by a colon:
    //int idx = book.title.indexOf(':');
    //String searchTitle = (idx == -1) ? book.title : book.title.substring(0, idx); 
    //queryParams.put("query", searchTitle);
    String base = "https://www.finder.com/netflix-movies";
    String encoded = WebHelper.toURL(base, queryParams);
    final String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.122 Safari/537.36";
    Connection connection = Jsoup.connect(encoded)
        .userAgent(userAgent);
    Document doc;
      try {
        doc = connection.get();
      } catch (IOException e) {
        System.err.printf("toResultsList: IO exception %s\n", e.toString());
        return;
      }
      Elements resultList = doc.select("div.ts-table-container");
      System.out.printf("size1 = %d\n", resultList.size());
      resultList = resultList.select("table.luna-table");
      System.out.printf("size2 = %d\n", resultList.size());
      resultList = resultList.select("tr");
      System.out.printf("size3 = %d\n", resultList.size());
      for (Element x : resultList) {
        Element title = x.selectFirst("td[data-title='Title']");
        Element year = x.selectFirst("td[data-title='Year of release']");
        
        //System.out.println(x);
        String t1 = title == null ? "NULL" : title.text();
        String y1 = year == null ? "NULL" : year.text();
        System.out.printf("\ttitle=[%s], year=[%s]\n", t1, y1);
      }
    
  }
}
