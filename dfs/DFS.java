import java.rmi.*;
import java.net.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.math.BigInteger;
import java.security.*;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

public class DFS {
  int port;
  Chord  chord;

  private long md5(String objectName) {
    try {
      MessageDigest m = MessageDigest.getInstance("MD5");
      m.reset();
      m.update(objectName.getBytes());
      BigInteger bigInt = new BigInteger(1,m.digest());
      return Math.abs(bigInt.longValue());
    } catch(NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
      return 0;
  }

  public DFS(int port) throws Exception {
    this.port = port;
    long guid = md5("" + port);
    chord = new Chord(port, guid);
    Files.createDirectories(Paths.get(guid+"/repository"));
  }

  public void join(String Ip, int port) throws Exception {
    chord.joinRing(Ip, port);
    chord.Print();
  }

  public void ls() throws Exception {
    Result result = readMetaData();
    Metadata metadata = result.getMetadata();
    List<File> files = metadata.getFiles();
    for (File file : files) {
      System.out.println(file.getName());
    }
  }

  public void touch(String fileName) throws Exception {
    long guid = md5("" + port + "" + fileName + "" + "1");
    Result result = readMetaData();
    Metadata metadata = result.getMetadata();

    File newFile = new File();
    ArrayList<Page> newPages = new ArrayList<Page>();
    Page newPage = new Page();

    newPage.setNumber("1");
    newPage.setGuid(String.valueOf(guid));
    newPage.setSize("0");
    newPages.add(newPage);

    newFile.setName(fileName);
    newFile.setNumberOfPages("1");
    newFile.setPageSize("1024");
    newFile.setSize("0");
    newFile.setPages(newPages);

    metadata.addFile(newFile);
    FileStream stream = formatMetaData(metadata);
    writeMetaData(stream);
  }

  public void rm(String fileName) throws Exception {
    Result result = readMetaData();
    Metadata metadata = result.getMetadata();
    List<File> files = metadata.getFiles();
    Iterator<File> ite = files.iterator();
    while(ite.hasNext()) {
      File file = ite.next();
      if(file.getName().equals(fileName)) {
        ite.remove();
      }
    }
    FileStream stream = formatMetaData(metadata);
    writeMetaData(stream);
  }

  public void mv(String oldName, String newName) throws Exception {
    Result result = readMetaData();
    Metadata metadata = result.getMetadata();
    List<File> files = metadata.getFiles();
    Iterator<File> ite = files.iterator();
    while(ite.hasNext()) {
      File file = ite.next();
      if(file.getName().equals(oldName)) {
        file.setName(newName);
      }
    }
    FileStream stream = formatMetaData(metadata);
    writeMetaData(stream);
  }

  public Byte[] read(String fileName, int pageNumber) throws Exception {
    // TODO: read pageNumber from fileName
    return null;
  }

  public Byte[] tail(String fileName) throws Exception {
    // TODO: return the last page of the fileName
    return null;
  }

  public Byte[] head(String fileName) throws Exception {
    // TODO: return the first page of the fileName
    return null;
  }

  public void append(String filename, Byte[] data) throws Exception {
    // TODO: append data to fileName. If it is needed, add a new page.
    // Let guid be the last page in Metadata.filename
    //ChordMessageInterface peer = chord.locateSuccessor(guid);
    //peer.put(guid, data);
    // Write Metadata
  }

  public Result readMetaData() throws Exception {
    long guid = md5("Metadata");
    ChordMessageInterface peer = chord.locateSuccessor(guid);
    InputStream metadataRaw = peer.get(guid);
    JsonParser parser = new JsonParser();
    JsonObject jsonObject = null;

    try {
      jsonObject = parser.parse(new JsonReader(new InputStreamReader(metadataRaw, "UTF-8"))).getAsJsonObject();
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
    return new Gson().fromJson(jsonObject, Result.class);
  }

  public void writeMetaData(InputStream stream) throws Exception {
    long guid = md5("Metadata");
    ChordMessageInterface peer = chord.locateSuccessor(guid);
    peer.put(guid, stream);
  }

  public FileStream formatMetaData(Metadata metadata) throws Exception {
    PrintWriter pw = new PrintWriter("./temp.txt");
    List<File> files = metadata.getFiles();
    pw.print("{");
    pw.print("\"metadata\":");
    pw.print("{");
    pw.print("\"files\":");
    pw.print("[");
    for (int i = 0; i < files.size(); i++) {
      File file = files.get(i);
      pw.print("{");
      pw.print("\"name\": \"" + file.getName() + "\",");
      pw.print("\"numberOfPages\": \"" + file.getNumberOfPages() + "\",");
      pw.print("\"pageSize\": \"" + file.getPageSize() + "\",");
      pw.print("\"size\": \"" + file.getSize() + "\",");
      pw.print("\"pages\": [");
      List<Page> pages = file.getPages();
      for (int j = 0; j < pages.size(); j++) {
        Page page = pages.get(j);
        pw.print("{");
        pw.print("\"number\": \"" + page.getNumber() + "\",");
        pw.print("\"guid\": \"" + page.getGuid() + "\",");
        pw.print("\"size\": \"" + page.getSize() + "\"");
        if(j == pages.size() - 1) pw.print("}");
        else pw.print("},");
      }
      pw.print("]");
      if(i == files.size() - 1) pw.print("}");
      else pw.print("},");
    }
    pw.print("]");
    pw.print("}");
    pw.print("}");
    pw.close();
    return new FileStream("./temp.txt");
  }
}
