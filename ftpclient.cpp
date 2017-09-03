#include <iostream>
#include <fstream>
#include <string>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <netinet/in.h>
#include <sys/types.h>
#include <unistd.h>
#include <netdb.h>

#define BUFFER_LENGTH 2048
#define WAITING_TIME 100000

int create_connection(std::string host, int port) {
    int s;
    struct sockaddr_in saddr;

    memset(&saddr,0, sizeof(saddr));
    s = socket(AF_INET,SOCK_STREAM,0);
    saddr.sin_family=AF_INET;
    saddr.sin_port= htons(port);

    int a1,a2,a3,a4;
    if (sscanf(host.c_str(), "%d.%d.%d.%d", &a1, &a2, &a3, &a4 ) == 4) {
        saddr.sin_addr.s_addr =  inet_addr(host.c_str());
    } else {
        hostent *record = gethostbyname(host.c_str());
        in_addr *addressptr = (in_addr *)record->h_addr;
        saddr.sin_addr = *addressptr;
    }
    if(connect(s,(struct sockaddr *)&saddr,sizeof(struct sockaddr))==-1) {
        perror("connection fail");
        exit(1);
        return -1;
    }
    return s;
}

int request(int sock, std::string message) {
    return send(sock, message.c_str(), message.size(), 0);
}

std::string reply(int s) {
    std::string strReply;
    int count;
    char buffer[BUFFER_LENGTH+1];

    usleep(WAITING_TIME);
    do {
        count = recv(s, buffer, BUFFER_LENGTH, 0);
        buffer[count] = '\0';
        strReply += buffer;
    }while (count ==  BUFFER_LENGTH);
    return strReply;
}

std::string request_reply(int s, std::string message) {
	if (request(s, message) > 0) {
    	return reply(s);
	}
	return "";
}

void quitsock(int sock) {
  request_reply(sock, "QUIT\r\n");
}

void quitprog(int sockpi, int sockdt) {
  quitsock(sockpi);
  quitsock(sockdt);
  std::cout << std::endl;
  std::cout << "*********************************************************************************";
  std::cout << std::endl << std::endl;
  exit(0);
}

void help() {
  std::cout << std::endl;
  std::cout << "**************************************Help***************************************";
  std::cout << std::endl << std::endl;
  std::cout << "\tHere are the commands you can enter:" << std::endl << std::endl;
  std::cout << "\t'list'\t\t - List all of the files on the server." << std::endl;
  std::cout << "\t'retr <file>'\t - Retrieve a file." << std::endl;
  std::cout << "\t'quit'\t\t - Close your connections and quit the program." << std::endl;
  std::cout << std::endl;
  std::cout << "*********************************************************************************";
  std::cout << std::endl << std::endl;
}

int pasv(int sockpi) {
  int sockdt;
  std::string strReply = request_reply(sockpi, "PASV\r\n");

  std::string delim  = "(";
  std::string ip = strReply.substr(strReply.find(delim) + 1);
  delim = ")";
  ip = ip.substr(0, ip.find(delim));

  int a1,a2,a3,a4,a5,a6;
  if(sscanf(ip.c_str(), "%d,%d,%d,%d,%d,%d", &a1, &a2, &a3, &a4, &a5, &a6) == 6) {
    ip = std::to_string(a1) + "." + std::to_string(a2) + "." + std::to_string(a3) + "." + std::to_string(a4);
    int port = (a5 << 8) | a6;
    sockdt = create_connection(ip, port);
  } else {
    std::cout << "Incorrect message was sent back from PASV command..." << std::endl << std::endl;
    quitprog(sockdt, sockpi);
  }
  return sockdt;
}

void list(int sockpi, int sockdt) {
  std::string strReply = request_reply(sockpi, "LIST\r\n");
  if(strReply.substr(0,3) == "150") {
    std::string strReply = reply(sockdt);
    std::cout << std::endl << strReply << std::endl;
  } else {
    std::cout << std::endl << "List not retreived..." << std::endl << std::endl;
  }
  quitsock(sockdt);
}

void retr(int sockpi, int sockdt, std::string file) {
  std::string query = "RETR " + file + "\r\n";
  std::string strReply = request_reply(sockpi, query);
  if(strReply.substr(0,3) == "150") {
    std::string strReply = reply(sockdt);
    std::ofstream filestream;
    filestream.open(file);
    filestream << strReply;
    filestream.close();
    std::cout << std::endl << file << " copied to your local directory." << std::endl << std::endl;
    quitsock(sockdt);
  } else {
    std::cout << std::endl << "File not retrieved... Probably not a file. Try something else." << std::endl << std::endl;
  }

}

void cmdline(int pi) {
  int sockpi = pi;
  std::string cmd;
  std::string file;
  int sockdt;

  std::cout << "Welcome to the command line! Enter 'help' if you're not sure what to do." << std::endl << std::endl;
  do {
    sockdt = pasv(sockpi);
    std::cout << ">> ";
    std::cin >> cmd;
    if(cmd == "QUIT" || cmd == "quit") {
      quitprog(sockpi, sockdt);
    } else if (cmd == "HELP" || cmd == "help"){
      help();
    } else if(cmd == "LIST" || cmd == "list") {
      list(sockpi, sockdt);
    } else if(cmd == "RETR" || cmd == "retr") {
      std::cin >> file;
      retr(sockpi, sockdt, file);
    } else {
      std::cout << std::endl << "Command '" << cmd << "' not found. Enter 'help' if you're not sure what to do." << std::endl << std::endl;
    }
  } while(cmd != "QUIT" && cmd != "quit");
}

int main(int argc , char *argv[])
{
    int sockpi;
    std::string strReply;

    std::cout << std::endl << "*********************Welcome to Mike and Derrick's FTP Client*********************" << std::endl << std::endl;
    std::cout << "Attempting to login..." << std::endl << std::endl;

    if (argc > 2)
        sockpi = create_connection(argv[1], atoi(argv[2]));
    if (argc == 2)
        sockpi = create_connection(argv[1], 21);
    else
        sockpi = create_connection("130.179.16.134", 21);
    strReply = reply(sockpi);
    std::cout << strReply  << std::endl;

    std::cout << "Attempting to log in using username anonymous..." << std::endl << std::endl;
    strReply = request_reply(sockpi, "USER anonymous\r\n");

    if(strReply.substr(0,3) == "331") {
      std::cout << "Username checks out." << std::endl << std::endl;
      std::cout << "Attempting to log in using password asa@asas.com..." << std::endl << std::endl;
      strReply = request_reply(sockpi, "PASS asa@asas.com\r\n");

      if(strReply.substr(0,3) == "230") {
        std::cout << "Password checks out." << std::endl << std::endl;
        cmdline(sockpi);
      }
    } else {
      std::cout << "Login failed." << std::endl << std::endl;
    }

    return 0;
}
