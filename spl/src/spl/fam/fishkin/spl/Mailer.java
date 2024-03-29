package fam.fishkin.spl;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;


/**
 * Mailer mails the results, using SendGrid.
 * @author Ken Fishkin
 *
 */
public class Mailer {
  /**
   * Sends mail
   * @param fromAddr the 'from' address in the mail
   * @param toAddr the 'to' address in the mail
   * @param numChecked how many books were checked
   * @param messages 
   * @param upgrades the upgrades found, if any
   * @param winners the lucky subset of available eBooks to recommend in this email
   * @param apiKey the SendGrid API key
   */
  public static void sendMail(String fromAddr, String toAddr, int numChecked, Vector<String> messages, Map<MediaItem, BestMatch> upgrades,
      Iterable<MediaItem> winners, String apiKey) {
    try {
      SendGrid sg = new SendGrid(apiKey);
      Request request = new Request();
      request.setMethod(Method.POST);
      request.setEndpoint("mail/send");
      StringBuilder body = new StringBuilder();
      body.append("{\"personalizations\":[{\"to\":[{\"email\":");
      body.append("\"" + toAddr + "\"}],");
      body.append("\"subject\":\"Seattle Public Library EBook report\"}]");
      body.append(",\"from\":{\"email\":\"" + fromAddr + "\"}");
      StringBuilder content = new StringBuilder();
      content.append("<h2>Seattle Public Library EBook Report</h2>");
      content.append("<p>" + numChecked + " books were checked.");
      boolean dump = false;
      if (upgrades == null || upgrades.isEmpty()) {
        content.append(" <b>No</b> upgrades");        
      } else if (upgrades.size() == 1) {
        dump = true;
        content.append(" <b>1</b> upgrade");
      } else {
        dump = true;
        content.append("<b>" + upgrades.size() + "</b>" + " upgrades");
      }
      if (messages != null && !messages.isEmpty()) {
        content.append("<p><b> messages:</b>");
        content.append("<ul>");
        for (String msg : messages) {
          content.append("<li>" + msg + "</li>");
        }
        content.append("</ul>");   
        content.append("</p>");
      }
      if (dump) {
        content.append("<ul>");
        for (Entry<MediaItem, BestMatch> upgrade : upgrades.entrySet()) {
          content.append("<li><b>" + upgrade.getKey().getTitle() + "</b> by " + upgrade.getKey().getAuthor() +
              " to " + upgrade.getValue().bestFormat + "</li>");          
        }
        content.append("</ul>");
      }
      content.append("</p>");
      content.append("<p>Some from the list of ebooks:</p>");
      content.append("<ul>");
      for (MediaItem sample: winners) {
        content.append("<li><b>" + sample.getTitle() + "</b> by " + sample.getAuthor() + "</li>");
      }
      content.append("</ul>");
      body.append(",\"content\":[{\"type\":\"text/html\",\"value\": \"" + content.toString() + "\"}]}");
      request.setBody(body.toString());
      Response response = sg.api(request);
      System.out.printf("sendMail returned code %d\n", response.getStatusCode());
      //System.out.println(response.getBody());
    } catch (IOException ex) {
      System.err.println(ex);
    }
  }

}
