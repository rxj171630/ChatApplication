//Rahul Javalagi
//rxj171630
//CS4390.0W2

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;
import java.util.StringTokenizer;

public class Client {
    final static int port = 5000;   //constant port number
    public static void main(String args[]){
        try {
            Scanner in = new Scanner(System.in);        //get keyboard input for client
            InetAddress ip = InetAddress.getLocalHost();    //use localhost for local machine testing
            Socket s = new Socket(ip, port);        //connect to server with the ip and port (using TCP)
            DataInputStream input = new DataInputStream(s.getInputStream());    //client i/o
            DataOutputStream output = new DataOutputStream(s.getOutputStream());
            final boolean[] available = {true}; //flag for if the client should run

            //Used for parsing the header
            final String[] msgType = new String[1]; //0 = Req, 1 = Ack, 2 = Rej, 3 = Data
            final String[] msgLength = new String[1];
            final String[] data = new String[1];

            System.out.println("Client started!");  //output when client is ready
            String msg = "Requesting connection to server";
            System.out.println(msg);
            output.writeUTF("0\\" + msg.length() + "\\" + msg);

            //Create thread for writing messages
            Thread write = new Thread(new Runnable() {
                public void run() {
                    while (available[0]) {   //loop while this client is available for messaging
                        String msg = in.nextLine(); //get the input from the keyboard
                        msgLength[0] = String.valueOf(msg.length());
                        data[0] = msg;
                        if (data[0].equals("quit")){
                            msgType[0] = "2";
                        }
                        else{
                            msgType[0] = "3";
                        }
                        String out = msgType[0] +"\\" + msgLength[0] + "\\" + data[0];   //Data message

                        try {
                            output.writeUTF(out);   //send the message to the server to be broadcasted
                        } catch (IOException e) {
                            System.out.println("Error: Disconnected from server! Is it still running?");
                            available[0] = false;
                        }
                        if (msgType[0].equals("2")) {    //if the message is quit
                            available[0] = false;   //mark as unavailable and disconnect
                        }
                    }
                }
            });

            //Create thread for reading messages
            Thread read = new Thread(new Runnable() {
                public void run() {
                    while (available[0]) {   //Loop while this client is available for messaging
                        try {
                            String msg = input.readUTF();   //Get the message from the server
                            StringTokenizer st = new StringTokenizer(msg, "\\");
                            msgType[0] = st.nextToken();
                            msgLength[0] = st.nextToken();
                            data[0] = st.nextToken();
                            System.out.println(data[0]);    //Print the message
                        } catch (IOException e) {
                        }
                    }
                }
            });

            //start reading and writing threads
            write.start();
            read.start();
        } catch (IOException e) {
            System.out.println("Error: Server not found! Is it running on port " +port +"?");
        }
    }
}
