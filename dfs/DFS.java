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
  Chord chord;
  Chord metadataChord;

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
    long guid = md5("" + fileName + "" + "1");
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
    ChordMessageInterface peer = chord.locateSuccessor(guid);
    peer.put(guid, new FileStream());
  }

  public void rm(String fileName) throws Exception {
    Result result = readMetaData();
    Metadata metadata = result.getMetadata();
    List<File> files = metadata.getFiles();
    Iterator<File> ite = files.iterator();

    while(ite.hasNext()) {
      File file = ite.next();
      if(file.getName().equals(fileName)) {
        for(int i = 0; i < file.getPages().size(); i++) {
          long guid = md5("" + fileName + "" + (i + 1));
          ChordMessageInterface peer = chord.locateSuccessor(guid);
          peer.delete(guid);
        }
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

        for(int i = 0; i < file.getPages().size(); i++) {
          Page page = file.getPages().get(i);
          long oldGuid = Long.parseLong(page.getGuid());
          ChordMessageInterface peer = chord.locateSuccessor(oldGuid);
          InputStream stream = peer.get(oldGuid);
          peer.delete(oldGuid);
          file.removePage(page);

          long newGuid = md5("" + newName + "" + (i+1));
          page.setGuid(Long.toString(newGuid));
          file.addPage(page);
          peer = chord.locateSuccessor(newGuid);
          peer.put(newGuid, stream);
        }
      }
    }
    FileStream stream = formatMetaData(metadata);
    writeMetaData(stream);
  }

  public void read(String fileName, int pageNumber) throws Exception {
    long guid = md5("" + fileName + "" + pageNumber);
    ChordMessageInterface peer = chord.locateSuccessor(guid);
    InputStream inputStream = peer.get(guid);
    FileOutputStream outputStream = new FileOutputStream("./temp.txt");

    while (inputStream.available() > 0)
        outputStream.write(inputStream.read());
    outputStream.close();

    java.io.File tempFile = new java.io.File("./temp.txt");
    Scanner scan = new Scanner(tempFile);

    while(scan.hasNext()) {
      System.out.println(scan.nextLine());
    }
  }

  public void tail(String fileName) throws Exception {
    Result result = readMetaData();
    Metadata metadata = result.getMetadata();
    List<File> files = metadata.getFiles();
    int lastPage = -1;
    for(File file: files) {
      if(file.getName().equals(fileName)) {
        lastPage = Integer.parseInt(file.getNumberOfPages());
        break;
      }
    }
    read(fileName, lastPage);
  }

  public void head(String fileName) throws Exception {
    read(fileName, 1);
  }

  public void append(String filename, String filepath) throws Exception {
    Result result = readMetaData();
    Metadata metadata = result.getMetadata();
    List<File> files = metadata.getFiles();
    int pageNo = -1;
    long pageLength = new java.io.File(filepath).length();

    for(File file : files) {
      if(file.getName().equals(filename)) {
        pageNo = Integer.parseInt(file.getNumberOfPages());
        if((Long.parseLong(file.getPages().get(pageNo - 1).getSize()) + pageLength) > Long.parseLong(file.getPageSize())) {
          file.getPages().add(new Page());
          pageNo++;
          file.setNumberOfPages(Integer.toString(pageNo));
        }
        break;
      }
    }
    long guid = md5("" + filename + "" + pageNo);
    ChordMessageInterface peer = chord.locateSuccessor(guid);
    InputStream inputStream = peer.get(guid);
    FileOutputStream outputStream = new FileOutputStream("./temp.txt");

    while (inputStream.available() > 0)
        outputStream.write(inputStream.read());

    FileStream file = new FileStream(filepath);
    while(file.available() > 0)
      outputStream.write(file.read());
    outputStream.close();
    FileStream f = new FileStream("./temp.txt");
    peer.put(guid, f);
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
      if(pages != null) {
        for (int j = 0; j < pages.size(); j++) {
          Page page = pages.get(j);
          pw.print("{");
          pw.print("\"number\": \"" + page.getNumber() + "\",");
          pw.print("\"guid\": \"" + page.getGuid() + "\",");
          pw.print("\"size\": \"" + page.getSize() + "\"");
          if(j == pages.size() - 1) pw.print("}");
          else pw.print("},");
        }
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
