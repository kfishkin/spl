package fam.fishkin.spl;

import java.util.Vector;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The SPL domain, but where we read the results in RSS, not display HTML
 * @author Us
 *
 */
public class SplRssDomain extends SplDomain {

	  @Override
	  public Candidates findWebCandidates(MediaItem item) {
	    if (!(item instanceof Book)) {
	      return null;
	    }
	    return new Candidates(WebHelper.findRSSCandidates((Book) item));
	  }
	  
		private static String extractFieldValue(Node node, String tagName) {
	          org.w3c.dom.Element eElement = (org.w3c.dom.Element) node;
			if (eElement == null) return null;
			NodeList nodeList =  eElement.getElementsByTagName(tagName);
			if (nodeList == null || nodeList.getLength() < 1) return null;
			Node first = nodeList.item(0);
			if (first == null) return null;
			String val = first.getTextContent();
			return val == null ? val : val.trim();
		
		}

	  @Override
	  public BestMatch findBestMatch(MediaItem desired, Candidates candidates) {
	    if (desired == null || candidates == null) {
	      return null;
	    }
	    Vector<Node> cands  = candidates.asNodeList();
	    if (cands == null || cands.isEmpty()) {
	    	return null;
	    }
	    BestMatch best = new BestMatch(desired, null, Integer.MAX_VALUE, null, Integer.MAX_VALUE, Format.NO);
	    // strip off everything after a ":" in the title - subheadings are in a different place on the web page,
	    // and can vary too much.
	    String title = desired.getTitle();
	    int idx = title.indexOf(':');
	    String compareTitle = (idx == -1) ? title : title.substring(0, idx);
	    compareTitle = compareTitle.trim().toLowerCase();
	    String compareAuthor = desired.getAuthor().trim().toLowerCase();
	    
	    for (Node cand: cands) {
	    	BestMatch bestThisItem = new BestMatch(desired, null, Integer.MAX_VALUE, null, Integer.MAX_VALUE, Format.UNKNOWN);
	    	String val = extractFieldValue(cand, "title");
	    	int dist = Distance.LevenshteinDistance(compareTitle, val);
	    	if (dist < best.titleDistance) {
	    		bestThisItem.titleDistance = dist;
	    		bestThisItem.bestTitle = val;
	    	}
	    	val = extractFieldValue(cand, "dc:creator");
	    	dist = Distance.LevenshteinDistance(compareAuthor, val);
	    	if (dist < best.authorDistance) {
	    		bestThisItem.authorDistance = dist;
	    		bestThisItem.bestAuthor = val;
	    	}

	    	val = extractFieldValue(cand, "format");
	    	Format format = Format.fromWeb(val);
	    	if (format.value > best.bestFormat.value) {
	    		bestThisItem.bestFormat = format;
	    	}
	    	if (bestThisItem.titleDistance <= best.titleDistance
	    			&& bestThisItem.authorDistance <= best.authorDistance) {
	    		best = bestThisItem;
	    	}
	    }
	    return best;
	  }	  
}
