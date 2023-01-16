import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class udpServer {
	
		/** CONSTANTS **/
		public static final int SERVER_PORT = 9500;
		
		/**	MAIN METHOD **/

		//	public main method
		public static void main(String[] args) throws Exception {
	        	
			//	Output to user
				System.out.println("Server initiated. Waiting for Client program.\nClient must use port 9500 as [1] argument.");
	    	
		    //	Create a new DatagramSocket using port
				DatagramSocket serverSideDS = new DatagramSocket(SERVER_PORT);
				
			//	Output instructions to the console to show server socket made
				System.out.println("Server running on port: " + SERVER_PORT);
		        
				runServer(serverSideDS);
			
			//  Close server side datagram
				serverSideDS.close();
	    }

	
	/*	runServer METHOD */
	
	//	Takes port as parameter
	private static void runServer(DatagramSocket serverSideDS) throws IOException{
		
	//	The Data parameter is accepted as a Byte array
		byte[] serverSideReceiveMsg = new byte[1024];
	//	Infinite loop using while(true)
		while (true) {

				/** Code section to receive/send data from/to client **/
			    
				//  Create new dp to receive data
				DatagramPacket dpServerSideReceive = new DatagramPacket(serverSideReceiveMsg, serverSideReceiveMsg.length);
			
				//	To receive a Datagram via this socket
				serverSideDS.receive(dpServerSideReceive);

				//	To then get the data from the dpServerReceive datagram, we use getData() and create a new string object raCclientMsg
				byte[] rawClientData = dpServerSideReceive.getData();

				//  copyOfRange takes the paylod excluding the sequence number
				byte[] rawMsg = Arrays.copyOfRange(rawClientData, 1, rawClientData.length-1);

				//  Code to remove excess/filler data from response
				List<Byte> msgResult = new ArrayList<>();
				for(byte b : rawMsg) {
					if(b != 0) {
						msgResult.add(b);
					}
					else {
						break;
					}
				}
				byte[] msgResultAsBytes = new byte[msgResult.size()];
				for(int n = 0; n<msgResultAsBytes.length; n++) {
					msgResultAsBytes[n] = msgResult.get(n);
				}

				//	rawSeq is the byte at index 0, the sequence number of received packet
				byte rawSeq = rawClientData[0];

			
			//  Output instructions to the console to show the packet was received
			//  Output sequence number of received packet as an int 			
				System.out.println("Packet received msg: " +  new String(msgResultAsBytes, StandardCharsets.UTF_8));
				System.out.println("Packet received seq number: " + (int) rawSeq);

			//  Get the incoming packet's IP and Port
				InetAddress ip = dpServerSideReceive.getAddress();
				int returnPort = dpServerSideReceive.getPort();
				
			//	The outcome of this manipulation is serverSideSendMsg
				byte[] serverSideSendMsg = ("ACK: " + (int)rawSeq).getBytes(StandardCharsets.UTF_8);
			
			//	Send the packet back to the client
				DatagramPacket serverSideResponse = new DatagramPacket(serverSideSendMsg, serverSideSendMsg.length, ip, returnPort);
			
			//	the packet is sent to the client side socket
				serverSideDS.send(serverSideResponse);
				
		}
	}
}
