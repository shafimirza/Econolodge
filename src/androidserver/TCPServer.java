package androidserver;

import com.econolodge.econolodgeapp3.Message;

import javax.swing.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
* The class extends the Thread class so we can receive and send messages at the same time
*/
public class TCPServer extends Thread {
  public static final int SERVERPORT = 5551;
  private boolean running = false;
  private PrintWriter mOut;
  private OnMessageReceived messageListener;
  private Database db=new Database();
  private String lastinsertid="";
  private ObjectOutputStream out;

  public static void main(String[] args) {
    //opens the window where the messages will be received and sent
    ServerBoard frame = new ServerBoard();
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.pack();
    frame.setVisible(true);

  }

  /**
   * Constructor of the class
   * @param messageListener listens for the messages
   */
  public TCPServer(OnMessageReceived messageListener) {
    this.messageListener = messageListener;
  }

  /**
    * Method to send the messages from server to client
    * @param message the message sent by the server
   */
  public void sendMessage(String message){
    if (mOut != null && !mOut.checkError()) {
      mOut.println(message);
      mOut.flush();
    }
  }

  @Override
  public void run() {
    super.run();

    running = true;

    try {
      System.out.println("S: Connecting...");
      //create a server socket. A server socket waits for requests to come in over the network.
      ServerSocket serverSocket = new ServerSocket(SERVERPORT);
      //create client socket... the method accept() listens for a connection to be made to this socket and accepts it.
      Socket client = serverSocket.accept();
      System.out.println("S: Receiving...");
      try {
       //sends the message to the client
       //mOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream())), true);
       out = new ObjectOutputStream(client.getOutputStream());
        //read the message received from client
       //BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
       ObjectInputStream in = new ObjectInputStream(client.getInputStream());
       //in this while we wait to receive messages from client (it's an infinite loop)
       //this while it's like a listener for messages
       while (running) {
         Object message = in.readObject();
         System.out.println("MESSAGE RECEIVED");
         if (message != null && messageListener != null) {
             System.out.println("MESSAGE IS NOT NULL");
           //call the method messageReceived from ServerBoard class
           if(message instanceof Message.AndroidToServer && ((Message.AndroidToServer)message).getRequest()!=Message.AndroidToServer.dataToBeRequested.NULL) {
              Message.AndroidToServer.dataToBeRequested data = ((Message.AndroidToServer)message).getRequest();
              if(data.equals(Message.AndroidToServer.dataToBeRequested.TASKDATA)) {
                 System.out.println("received request for TASKDATA");
                 out.writeObject((Message.ServerToAndroid.returnTaskData)this.getTaskData());
                 System.out.println("FINISHED SENDING BACK TASKDATA");
              }else if(data.equals(Message.AndroidToServer.dataToBeRequested.TASKIMAGEDATA)) {
                 System.out.println("received request for TASKIMAGEDATA");
                 out.writeObject((Message.ServerToAndroid.returnTaskImageData)this.getTaskImageData());
                 System.out.println("FINISHED SENDING BACK TASKIMAGEDATA");
              }
              /**********************************************
              //Message.AndroidToServer AS = new Message.AndroidToServer();
              //AS.setRequest(Message.AndroidToServer.dataToBeRequest.TASKDATA);
              //out.writeObject(AS);
              */////////////////////////////////////////////              
           }//if(message instanceof Message.AndroidToServer && ((Message.AndroidToServer)message).getRequest()!=Message.AndroidToServer.dataToBeRequested.NULL) {
           else if(message instanceof Message.AndroidToServer.LogInMessage) {
             System.out.println("CHECKING USER");
             System.out.println(this.LoginUser((Message.AndroidToServer.LogInMessage)message));
           } //else if(message instanceof Message.AndroidToServer.LogInMessage) {
           else if(message instanceof Message.AndroidToServer.RegisterEmployee) {
             System.out.println("RECEIVED CLASS REGISTEREMPLOYEE");
             this.RegisterEmployee((Message.AndroidToServer.RegisterEmployee)message);
//           }else if(message instanceof Message.LoginDetailsTask) {
//             System.out.println("RECEIVED CLASS logindetailstask");
//             this.LoginDetailsTask((Message.LoginDetailsTask)message);
           } else if(message instanceof Message.AndroidToServer.CreateTaskTask) {
             System.out.println("RECEIVED CLASS CreateTaskTask");
             this.CreateTaskTask((Message.AndroidToServer.CreateTaskTask)message);
           } else if(message instanceof Message.AndroidToServer.PictureMessage) {
             System.out.println("RECIEVED CLASS PictureMessage");
             this.PictureMessageTask((Message.AndroidToServer.PictureMessage)message);
           }else {
               System.out.println("MESSAGE IS NOT INSTANCE OF LOGINMESSAGE");
           }
           
         }else {//if (message != null && messageListener != null) {
           System.err.println("MESSAGE WAS NULL");
         }
       }//while loop

      } catch (Exception e) {
        System.out.println("S: Error");
        e.printStackTrace();
      } finally {
        client.close();
        System.out.println("S: Done.");
      }
    } catch (Exception e) {
      System.out.println("S: Error");
      e.printStackTrace();
    }

  }  

private boolean LoginUser(Message.AndroidToServer.LogInMessage message) {
      String EMPID = db.selectString("Select login_id from login where userid='"+message.getUsername()+
                                      "' and password='"+message.getPassword()+"'");
      System.out.println("THIS IS EMPID:"+EMPID);
      if(EMPID!=null && !EMPID.equals(""))
          return true;
      else
          return false;
  }  
 private void RegisterEmployee(Message.AndroidToServer.RegisterEmployee message) {
  
      String query = "INSERT INTO employee(last_name, first_name, email, phone, address, city, zip, ssn, position, active)"
              + "values('"+message.getLastName()+"', '"+message.getFirstName()+"', '"+message.getEmail()+"',"
              + " '"+message.getAddress()+"', '"+message.getCity()+"', '"+message.getCity()+"', '"+message.getZip()+"',"
              + " '"+message.getSsn()+"', '"+message.getActive()+"', '"+message.getPosition()+"')";
      System.out.println(query);
      db.executeUpdate(query);
      lastinsertid = db.selectString("Select LAST_INSERT_ID()");
       query = "INSERT INTO login(employee_id, userid, password, role )"
             +"values("+lastinsertid+",'"+message.getUser()+"', '"+message.getPassword()+"','"+message.getActive()+"')";
      System.out.println(query);           
      db.executeUpdate(query);
      
      System.out.println("ended RegisterEmployee");
      
  }
  //Declare the interface. The method messageReceived(String message) will must be implemented in the ServerBoard
  //class at on startServer button click
  public interface OnMessageReceived {
    public void messageReceived(String message);
  }
private void CreateTaskTask(Message.AndroidToServer.CreateTaskTask message)
{
    
     String query = "INSERT INTO task_manager(title, description, schedule)"
             +"values('"+message.gettTitle()+"', '"+message.gettDesc()+"', '"+message.gettSpin()+"')";
      db.executeUpdate(query);
      System.out.println(query); 
      
}
public interface OnReceived {
    public void Received(String message);
  }

private void PictureMessageTask(Message.AndroidToServer.PictureMessage message) {
     String image= "/usr/local/images/"+message.getId() + ".jpg";         
    try{
      FileOutputStream fos = new FileOutputStream(image);
       System.out.println("Writting file: /usr/local/images/" + message.getId() + ".jpg");
         String query = "INSERT INTO task_pictures(task_id, picture_id)"
            +"values('"+message.getId()+"','"+image+"')";     
      db.executeUpdate(query);
      System.out.println(query);
     fos.write(message.getPicture());
     fos.close();
    } catch (Exception e) {
        e.printStackTrace();
    }
}
public interface OnImageReceived {
    public void Received(String message);
  }

private Message.ServerToAndroid.returnTaskData getTaskData() {
  Message.ServerToAndroid.returnTaskData rtd = new Message.ServerToAndroid.returnTaskData();
  try {
      ResultSet rs= db.execute("Select task_id, title, description from task_manager");
      Message.ServerToAndroid.TaskDataClass tdc;
      while(rs.next()){
        tdc = new Message.ServerToAndroid.TaskDataClass();
        tdc.SetTaskID(Integer.parseInt(rs.getString(1)));
        tdc.setTitle(rs.getString(2));
        tdc.setDescription(rs.getString(3));
        rtd.addTask(tdc);
      }
  }catch(Exception E) {
      System.out.println(E.getMessage());
  }
  return rtd;
}

private Message.ServerToAndroid.returnTaskImageData getTaskImageData() {
  Message.ServerToAndroid.returnTaskImageData rtidc = new Message.ServerToAndroid.returnTaskImageData();
  try {
      ResultSet rs= db.execute("Select task_id, picture_id from task_pictures");
      Message.ServerToAndroid.TaskImageClass tidc;
      while(rs.next()){
        tidc = new Message.ServerToAndroid.TaskImageClass();
        tidc.setFilename(rs.getString(1));
        tidc.setFile(rs.getString(2));
        rtidc.addTaskImage(tidc);
      }
}catch(Exception E) {
      System.out.println(E.getMessage());
  }
  return rtidc;
}




}



