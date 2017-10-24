import java.rmi.*;
import java.net.*;
import java.util.*;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.nio.file.*;


public class Client {
  public static DFS dfs;
  public static Scanner in;
  public Client(int p) throws Exception {
    dfs = new DFS(p);
  }

  static public void main(String args[]) throws Exception {
    if (args.length < 1 ) {
      throw new IllegalArgumentException("Parameter: <port>");
    }
    Client client=new Client( Integer.parseInt(args[0]));
    in = new Scanner(System.in);
    ui();
  }

  public static void ui() throws Exception {
    System.out.print(">> ");
    String line = in.nextLine();
    String[] args = line.split(" ");
    String cmd = args[0];

    switch(cmd) {
      case "join":
        dfs.join(args[1], Integer.parseInt(args[2]));
        break;
        case "ls":
        dfs.ls();
        break;
      case "touch":
        dfs.touch(args[1]);
        break;
      case "rm":
        dfs.rm(args[1]);
        break;
      case "mv":
        dfs.mv(args[1], args[2]);
        break;
      case "append":
        dfs.append(args[1], args[2]);
        break;
      case "read":
        dfs.read(args[1], Integer.parseInt(args[2]));
        break;
      case "head":
        dfs.head(args[1]);
        break;
      case "tail":
        dfs.head(args[1]);
        break;
      case "quit":
        System.exit(0);
      default:
        System.out.println(cmd + " is invalid.");
    }
    ui();
  }
}
