package fam.fishkin.spl;

import java.util.Vector;

import org.jsoup.select.Elements;
import org.w3c.dom.Node;

/**
 * Encapsulates the set of candidates in response to a query.
 * If the query is scraping the web page, the candidates are a set of JSoup elements.
 * If the query comes back as RSS, it's an XML doc.
 * This is therefore a union type, which is a bit of a kludge...
 * @author Us
 *
 */
public class Candidates {
	private Elements asElements = null;
	private Vector<Node> asNodeVector = null;
	
	public Candidates(Elements asElements) {
		this.asElements = asElements;
	}
	
	public Candidates(Vector<Node> asNodeList) {
		this.asNodeVector = asNodeList;
	}
	
	public Elements asElements() { return asElements; }
	public  Vector<Node> asNodeList() { return asNodeVector; }

}
