//Rahul Javalagi
//rxj171630
//CS4390.0W2

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class Server {
    static ArrayList<ClientHandler> clients = new ArrayList<>();    //Hold all instances of clients
    static int clientID = 0;    //used for numbering the clients
    static final int port = 5000;   //const port number

    public static void main(String[] args) throws IOException {
        ServerSocket s = new ServerSocket(port);    //Create new server socket (using TCP)

        System.out.println("Server started! Awaiting clients...");  //Output when server is ready

        while(true){    //loop forever
            Socket client = s.accept(); //Accept any incoming connections (using TCP)
            DataInputStream input = new DataInputStream(client.getInputStream());   //set up i/o
            DataOutputStream output = new DataOutputStream(client.getOutputStream());
            ClientHandler c = new ClientHandler(client, "client" +clientID, input, output); //create new client handler object for client
            Thread t = new Thread(c);   //create new thread for client
            clients.add(c); //add to arraylist with all clients
            t.start();  //start thread
            clientID++; //increment for naming next client
        }
    }
}

//helper class for handling each individual client
class ClientHandler implements Runnable{
    private String name;    //name of the client in "Client#" format
    final DataInputStream input;    //client i/o
    final DataOutputStream output;
    Socket s;   //connection from client to server
    boolean available;  //is this client available for chat?
    String clientList;  //Used for outputting all connected clients

    //constructor
    public ClientHandler(Socket s, String name, DataInputStream input, DataOutputStream output){
        this.input = input;
        this.output = output;
        this.name = name;
        this.s = s;
        this.available = true;  //set this client to be available by default
        clientList = "";    //set the list of connected clients to be empty by default
    }

    public void run(){
        //output as soon as connection is established with server
        try {
            String data = "Connected to server " +s;
            this.output.writeUTF("1\\" +data.length() +"\\" +data);   //send Ack message to client
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (ClientHandler ch: Server.clients){ //for each client connected to the server
            clientList += ", " + ch.name;   //format the list with commas
            //broadcast to all connected clients that this client joined the chat
            try {
                String data = this.name +" joined the chat.";
                ch.output.writeUTF("1\\" +data.length() +"\\" + data);    //send Ack message to client
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String msgType; //0 = Req, 1 = Ack, 2 = Rej, 3 = Data
        String msgLength;
        String data;

        String rec; //received message
        while (this.available){ //while this client is available, loop
            try {
                rec = input.readUTF();  //get the input
                StringTokenizer st = new StringTokenizer(rec, "\\");
                msgType = st.nextToken();
                msgLength = st.nextToken();
                data = st.nextToken();
                if (!msgType.equals("2")) {    //if it is not the quit message
                    System.out.println(this.name + ": " + data);   //output formatted to "Client#: message" serverside only
                    if (msgType.equals("0")){   //if it is a Req message, print Ack
                        System.out.println(this.name +" connected.");
                        //output the available clients to server
                        System.out.print("Available clients: ");
                        clientList = clientList.substring(2);   //remove first comma for formatting
                        System.out.println(clientList); //output list of connected clients
                        clientList = "";    //reset connected client list to empty string
                    }
                }
                else{   //if it is the quit message

                    //broadcast this client has left.
                    for (ClientHandler ch: Server.clients){
                        String msg = this.name +" has left the chat.";
                        ch.output.writeUTF("2\\" +msg.length() +"\\" +msg);   //Send Rej message to client
                    }

                    this.available = false; //set this client to unavailable
                    for (int i = 0; i < Server.clients.size(); i++){    //iterate through all connected clients
                        //remove from list of clients
                        if (Server.clients.get(i).name.equals(this.name)){
                            Server.clients.remove(i);
                        }
                    }

                    this.s.close(); //close this connection to server
                    System.out.println("Connection to " +this.name +" closed.");    //output to server that this connection is closed

                    //output available clients again
                    for (ClientHandler ch: Server.clients) {
                        clientList += ", " + ch.name;
                    }
                    System.out.print("Available clients: ");
                    if (clientList.length() > 0)
                        clientList = clientList.substring(2);
                    System.out.println(clientList);

                    //close i/o streams
                    this.input.close();
                    this.output.close();
                    break;
                }
                //broadcasting messages
                for (ClientHandler ch : Server.clients){    //for each client in the list
                    if (this.name.equals(ch.name) && msgType.equals("3")){         //if this is the client's own message, write "You" instead of client name
                        String msg = "You>> " +data;
                        ch.output.writeUTF("1\\" +msg.length() +"\\" +msg);   //Send Ack message back to client
                    }
                    if (!this.name.equals(ch.name) && ch.available && msgType.equals("3")){    //otherwise, if the client is available
                        String msg = this.name+">> " +data;
                        ch.output.writeUTF("3\\" +msg.length() +"\\" +msg);   //Send Data message to client
                    }
                }
            } catch (IOException e) {
                try {
                    this.available = false;
                    System.out.println("Connection to " +this.name +" closed.");    //output to server that this connection is closed

                    for (int i = 0; i < Server.clients.size(); i++){    //iterate through all connected clients
                        //remove from list of clients
                        if (Server.clients.get(i).name.equals(this.name)){
                            Server.clients.remove(i);
                        }
                    }

                    //output available clients again
                    for (ClientHandler ch: Server.clients) {
                        clientList += ", " + ch.name;
                    }
                    System.out.print("Available clients: ");
                    if (clientList.length() > 0)
                        clientList = clientList.substring(2);
                    System.out.println(clientList);

                    //broadcast this client has left.
                    for (ClientHandler ch: Server.clients){
                        String msg = this.name +" has left the chat.";
                        ch.output.writeUTF("2\\" +msg.length() +"\\" +msg);   //Send Rej message to client
                    }

                    //close i/o
                    this.input.close();
                    this.output.close();
                    this.s.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        }
    }

}