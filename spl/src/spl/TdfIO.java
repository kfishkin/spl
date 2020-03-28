package spl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Performs I/O on tab-delimited-format (TDF) data files.
 * @author User
 *
 */
public class TdfIO {
  final static  String COMMENT = "#";
  /**
   * Read a file
   * @param fileName the file to read
   * @return the books in it.
   */
  public static Iterable<Book> Read(String fileName) {
    ArrayList<Book> bookList = new ArrayList<Book>();
    final String splitOn = "\t";
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
        if (line.startsWith(COMMENT)) {
          continue;
        }
        // each line is <author><title>[<recommender>]<status>,
        // separated by tabs. Could use a CSV reader here, but it's so straightforwards, just do it
        // by hand.
        String[] args = line.split(splitOn);
        int len = args.length;
        if (len < 3) {
          System.err.printf("only %d fields in line '%s' - need at least 3\n", len, line);
          continue;
        } else if (len > 4) {
          System.err.printf("%d fields in line '%s' - need at most 4\n", len, line);
          continue;
        }
        String author = args[0];
        String title = args[1];
        String recommender = args[2];
        String status = "??";
        if (len >= 4) {
          status = args[3];
        } else {
          status = recommender;
          recommender = "";
        }
        Format format = Format.fromFile(status);
        Book book = new Book(author, title, recommender, format);
        book.rawFormat = status;
        bookList.add(book);
      }
      return bookList;
    } catch (FileNotFoundException e) {
      System.err.printf("could not read input file %s\n", fileName);
      return null;
    }
  }

  /**
   * Write books to a file
   * @param books the books
   * @param fileName the file
   */
  public static void write(Iterable<Book> books, String fileName) {
    File destFile = new File(fileName);
    if (!destFile.canWrite()) {
      System.err.printf("cannot write destination file %s\n", fileName);
      return;
    }    
    // to protect against a crash in mid-write, write to a temp file,
    // then rename that temp file.
    File tempFile;
    PrintWriter out;
    try {
      tempFile = File.createTempFile("temp", null);
      System.out.println(tempFile.getAbsolutePath());
    } catch (IOException e) {
      System.err.println("could not create temp file:" + e);
      return;
    }
    try {
      out = new PrintWriter(new BufferedWriter(new FileWriter(tempFile)));
    } catch (IOException e) {
      System.err.println("Could not create PrintWriter:" + e);
      return;
    }
    DateFormat dateFormat = new SimpleDateFormat("EE, MMMM d yyyy");
    Calendar cal = Calendar.getInstance();
    out.printf("%s status as of %s\n", COMMENT, dateFormat.format(cal.getTime()));

    for (Book book : books) {
      out.printf("%s\t%s", book.author, book.title);
      if (book.recommender != null && !book.recommender.isEmpty()) {
        out.printf("\t%s", book.recommender);
      }
      out.printf("\t%s\n", book.rawFormat);
    }
    out.close();
    try {
      if (!destFile.delete()) {
        System.err.println("could not delete destination file " + destFile);
        return;
      }
      tempFile.renameTo(destFile);
    } catch (SecurityException e) {
      System.err.printf("couldn't rename output file: %s\n", e);
    }
    
  }

}
