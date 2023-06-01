package fam.fishkin.spl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.imageio.metadata.IIOMetadataNode;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

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
	public static Elements findCandidates(Book book) {
		if (book == null) {
			return null;
		}
		if (book.isRead()) {
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
		// JSoup recommends waiting a few seconds between pings...
		final long SLEEP_TIME_MS = 2000;
		try {
			Thread.sleep(SLEEP_TIME_MS);
		} catch (InterruptedException e) {
		}
		return resultList;
	}

	public static Vector<Node> findRSSCandidates(Book book) {
		if (book == null) {
			return null;
		}
		if (book.isRead()) {
			return null;
		}
		Vector<Node> cands = new Vector<Node>();
		Map<String, String> queryParams = new HashMap<String, String>();
		queryParams.put("custom_edit", "false");
		queryParams.put("suppress", "true");
		queryParams.put("f_FORMAT", "EBOOK|BK");
		queryParams.put("searchType", "title");
		// strip subtitle, indicated by a colon:
		int idx = book.title.indexOf(':');
		String searchTitle = (idx == -1) ? book.title : book.title.substring(0, idx); 
		queryParams.put("query", searchTitle);
		// https://gateway.bibliocommons.com/v2/libraries/seattle/rss/search?query=title%3A%28the%20secret%20gift%29%20%20%20formatcode%3A%28BK%20OR%20EBOOK%20%29&searchType=bl&custom_edit=false&suppress=true&view=grouped&_ga=2.226994717.124022377.1685555432-1439596961.1683266732&_gl=1*ciuhhy*_ga*MTQzOTU5Njk2MS4xNjgzMjY2NzMy*_ga_G99DMMNG39*MTY4NTU2OTA3NC41LjEuMTY4NTU2OTEzMy4wLjAuMA..

		String base = "https://gateway.bibliocommons.com/v2/libraries/seattle/rss/search";
		URL url = null;
		try {
			url = new URL(WebHelper.toURL(base, queryParams));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		final String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.122 Safari/537.36";
		HttpURLConnection con = null;
		try {
			con = (HttpURLConnection) url.openConnection();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			con.setRequestMethod("GET");
		} catch (ProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		con.setRequestProperty("User-Agent", userAgent);
		int responseCode = 0;
		try {
			responseCode = con.getResponseCode();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// System.out.println("GET Response Code :: " + responseCode);
		String bodyAsString = null;
		if (responseCode == HttpURLConnection.HTTP_OK) { // success
			BufferedReader in = null;
			try {
				in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			String inputLine;
			StringBuffer response = new StringBuffer();

			try {
				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				in.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// print result
			// System.out.println(response.toString());
			bodyAsString = response.toString();
		} else {
			System.out.println("GET request did not work.");
			return null;
		}
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		 DocumentBuilder builder = null;
		 try {
			builder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			org.w3c.dom.Document doc = null;
			try {
				doc = builder.parse(new InputSource(new StringReader(bodyAsString)));
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (doc == null) {
				return cands;
			}
			NodeList nodeList = doc.getElementsByTagName("item");
		    for (int itr = 0; itr < nodeList.getLength(); itr++) {
		        Node node = nodeList.item(itr);
		        if (node.getNodeType() == Node.ELEMENT_NODE) {
		          //lement eElement = (Element) node;
		          //System.out.println("Author: "+ extractFieldValue(eElement, "dc:creator"));
		          //System.out.println("Title: "+ extractFieldValue(eElement, "title"));
		          //System.out.println("Format: "+ extractFieldValue(eElement, "format"));
		          cands.add(node);
		      }
		    }
		    return cands;		 
	}

	private static String extractFieldValue(Element eElement, String tagName) {
		if (eElement == null) return null;
		NodeList nodeList =  eElement.getElementsByTagName(tagName);
		if (nodeList == null || nodeList.getLength() < 1) return null;
		Node node = nodeList.item(0);
		if (node == null) return null;
		return node.getTextContent();
	
	}

}

