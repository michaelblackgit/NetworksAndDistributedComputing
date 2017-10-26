//Michael Black and Derrick Nguyen
import java.rmi.*;
import java.net.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.math.BigInteger;
import java.security.*;
//imports for Gson
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

/**
 * Distributed File System that utilizes the methods of a
 * Chord object.
 */
public class DFS {
  int port;
  Chord chord;
  Chord metadataChord;

  /**
   * This method creates a hashed object for unique
   * identifiers.
   * @param  String objectName  The name of the object to be hashed.
   * @return                    Return the hashed objectName as a long.
   */
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

  /**
   * Create a new instance of the Distributed File System.
   * @param  int       port for specifying the port on this system to be connected.
   * @throws Exception in the case that a file or address not found.
   */
  public DFS(int port) throws Exception {
    this.port = port;
    long guid = md5("" + port);
    chord = new Chord(port, guid);
    Files.createDirectories(Paths.get(guid+"/repository"));
  }

  /**
   * Method to join this node to another instance of the Distributed File System.
   * @param  String    Ip            The address of a node in the DFS to be connected to.
   * @param  int       port          The port of the node in the DFS to be connected to.
   * @throws Exception
   */
  public void join(String Ip, int port) throws Exception {
    chord.joinRing(Ip, port);
    chord.Print();
  }

  /**
   * Method to list the files in the DFS.
   * @throws Exception
   */
  public void ls() throws Exception {
    // Get the metadata.
    Metadata metadata = readMetaData();
    // Get the files from the metadata.
    List<File> files = metadata.getFiles();
    // For each file in the files, print the name of the file.
    for (File file : files) {
      System.out.println(file.getName());
    }
  }

  /**
   * Method to touch a new file in the DFS.
   * @param  String    fileName      The name of the file to be touched.
   * @throws Exception
   */
  public void touch(String fileName) throws Exception {
    // Create a new unique identifier for the file's first page.
    long guid = md5("" + fileName + "" + "1");
    // Get the metadata.
    Metadata metadata = readMetaData();
    // Create a new file.
    File newFile = new File();
    // Create a new list of pages for the file.
    ArrayList<Page> newPages = new ArrayList<Page>();
    // Create a new page for the file's list of pages.
    Page newPage = new Page();
    // Set the page's default values.
    newPage.setNumber("1");
    newPage.setGuid(String.valueOf(guid));
    newPage.setSize("0");
    // Add the page to file's list of pages.
    newPages.add(newPage);
    // Set the file's default values.
    newFile.setName(fileName);
    newFile.setNumberOfPages("1");
    newFile.setPageSize("1024");
    newFile.setSize("0");
    // Add the list of pages to the file.
    newFile.setPages(newPages);
    // Add the file to the metadata.
    metadata.addFile(newFile);
    // Format the metadata.
    FileStream stream = formatMetaData(metadata);
    // Write the metadata.
    writeMetaData(stream);
    // Replace the metadata in chord.
    ChordMessageInterface peer = chord.locateSuccessor(guid);
    peer.put(guid, new FileStream());
  }

  /**
   * Method to remove a file from the DFS.
   * @param  String    fileName      Name of the file to be removed.
   * @throws Exception
   */
  public void rm(String fileName) throws Exception {
    // Get files from metadata.
    Metadata metadata = readMetaData();
    List<File> files = metadata.getFiles();
    // Create an iterator to iterate through the files.
    Iterator<File> ite = files.iterator();

    // Remove all files from chord that are associated with the file name,
    // while also removing the file and its associated pages from metadata.
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
    // Update the metadata.
    FileStream stream = formatMetaData(metadata);
    writeMetaData(stream);
  }

  /**
   * Method to rename the file.
   * @param  String    oldName       The name of the file to be renamed.
   * @param  String    newName       What the file should be renamed to.
   * @throws Exception
   */
  public void mv(String oldName, String newName) throws Exception {
    // Get the file from metadata.
    Metadata metadata = readMetaData();
    List<File> files = metadata.getFiles();
    Iterator<File> ite = files.iterator();

    // Find the file and for each of its pages, rename the guid while
    // also changing the file's name in metadata.
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
    // Update metadata.
    FileStream stream = formatMetaData(metadata);
    writeMetaData(stream);
  }

  /**
   * Method to read a file page.
   * @param  String    fileName      The name of the file to be looked at.
   * @param  int       pageNumber    The page to be read.
   * @throws Exception [description]
   */
  public void read(String fileName, int pageNumber) throws Exception {
    // This is the guid of the page that we will be looking for.
    long guid = md5("" + fileName + "" + pageNumber);
    // Get the page and write it to a temporary file.
    ChordMessageInterface peer = chord.locateSuccessor(guid);
    InputStream inputStream = peer.get(guid);
    FileOutputStream outputStream = new FileOutputStream("./temp.txt");
    while (inputStream.available() > 0)
        outputStream.write(inputStream.read());
    outputStream.close();
    // Read the contents of that temporary file and print it to the console.
    java.io.File tempFile = new java.io.File("./temp.txt");
    Scanner scan = new Scanner(tempFile);
    while(scan.hasNext()) {
      System.out.println(scan.nextLine());
    }
  }

  /**
   * Method to read the last page of a file.
   * @param  String    fileName      The name of the file to be read from.
   * @throws Exception
   */
  public void tail(String fileName) throws Exception {
    // Get files from metadata.
    Metadata metadata = readMetaData();
    List<File> files = metadata.getFiles();
    int lastPage = -1;
    // Find the last page of the file.
    for(File file: files) {
      if(file.getName().equals(fileName)) {
        lastPage = Integer.parseInt(file.getNumberOfPages());
        break;
      }
    }
    // Call the read method above with the last page.
    read(fileName, lastPage);
  }

  /**
   * Method to read the first page of a file.
   * @param  String    fileName      The name of th efile to be read from.
   * @throws Exception
   */
  public void head(String fileName) throws Exception {
    // Call the read method above with the first page.
    read(fileName, 1);
  }

  /**
   * Method to append to a file.
   * @param  String    filename      Name of the file to be appended to.
   * @param  String    filepath      File path for the file to append with.
   * @throws Exception
   */
  public void append(String filename, String filepath) throws Exception {
    // Get files from metadata.
    Metadata metadata = readMetaData();
    List<File> files = metadata.getFiles();
    int pageNo = -1;
    long pageLength = new java.io.File(filepath).length();
    // Check whether or not there needs to be a new page added due to size.
    // If the size of the file appended to the page is too large, create a new page.
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
    // Get the last page of the file.
    long guid = md5("" + filename + "" + pageNo);
    ChordMessageInterface peer = chord.locateSuccessor(guid);
    InputStream inputStream = peer.get(guid);
    // Create a new temporary file
    FileOutputStream outputStream = new FileOutputStream("./temp.txt");
    // Write the existing page to this temporary page.
    while (inputStream.available() > 0)
        outputStream.write(inputStream.read());
    // Write the file to append with also to this temporary page.
    FileStream file = new FileStream(filepath);
    while(file.available() > 0)
      outputStream.write(file.read());
    outputStream.close();
    // Add the temporary file into the DFS.
    FileStream f = new FileStream("./temp.txt");
    peer.put(guid, f);
  }

  /**
   * Method to read metadata from the DFS.
   * @return The Metadata object.
   * @throws Exception
   */
  public Metadata readMetaData() throws Exception {
    // Guid for the metadata.
    long guid = md5("Metadata");
    // Get the metadata from Chord.
    ChordMessageInterface peer = chord.locateSuccessor(guid);
    InputStream metadataRaw = peer.get(guid);
    JsonParser parser = new JsonParser();
    JsonObject jsonObject = null;
    // Parse the metadata and return.
    try {
      jsonObject = parser.parse(new JsonReader(new InputStreamReader(metadataRaw, "UTF-8"))).getAsJsonObject();
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
    Result result = new Gson().fromJson(jsonObject, Result.class);
    return result.getMetadata();
  }

  /**
   * Method to write metadata
   * @param  InputStream stream        Metadata to be written.
   * @throws Exception
   */
  public void writeMetaData(InputStream stream) throws Exception {
    // Put the metadata into Chord using the correct guid.
    long guid = md5("Metadata");
    ChordMessageInterface peer = chord.locateSuccessor(guid);
    peer.put(guid, stream);
  }

  /**
   * Method to format the metadata.
   * @param  Metadata  metadata      Metadata to be formatted.
   * @return           Metadata formatted as a FileStream (extension of InputStream).
   * @throws Exception
   */
  public FileStream formatMetaData(Metadata metadata) throws Exception {
    // Write to a temporty file formatted version of the metadata.
    // This method formats using the Metadata, File, and Page classes' getters
    // and setters.
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
    // Return a FileStream object using the file path to the temporary file.
    return new FileStream("./temp.txt");
  }
}
