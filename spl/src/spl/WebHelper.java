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
import org.jsoup.select.Elements;

/**
 * Helper class for some of the Web querying/scraping.
 * @author Ken Fishkin
 *
 */
public class WebHelper {
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
  public static Elements toResultsList(Book book) {
    if (book == null) {
      return null;
    }
    if (book.format.equals(Format.READ)) {
      return null;
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
      return resultList;
  }
}
