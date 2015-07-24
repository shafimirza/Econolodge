package androidserver;
import javax.swing.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author root
 */
public class AndroidServer {

  public static void main(String[] args) {
    //opens the window where the messages will be received and sent
    ServerBoard frame = new ServerBoard();
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.pack();
    frame.setVisible(true);
     
  }
}