import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class udpClient {

	/**
	 * CONSTANTS
	 **/

	// //	Select for quitting the infinite loop
	// public static final String QUIT = "q";

	//	Window size
	public static final int WINDOW_SIZE = 5;

	//  code to Concatenate array
	static <T> T[] concatWithArrayCopy(T[] array1, T[] array2) {
		T[] result = Arrays.copyOf(array1, array1.length + array2.length);
		System.arraycopy(array2, 0, result, array1.length, array2.length);
		return result;
	}

	/**
	 * main METHOD
	 **/

	//	Public main method calls the runClient method
	public static void main(String[] args) throws Exception {

		//  IP address as command line argument [0]
		InetAddress ip_address = InetAddress.getByName(args[0]);

		//	Port as command line argument [1]
		int serverPort = Integer.parseInt(args[1]);

		//	File path is in the same folder as the client.java program
		String filePath = "./Umbrella.txt";

		//	File path is passed as string parameter to the readTxt method
		String txtRead = readTxt(filePath);

		//	Assing datagram socket for both stopAndWait() and goBackN() operations
		DatagramSocket clientSideDS = new DatagramSocket(9501); //assigns random port -> mismatch 9501 != 84756348756

		//  Call both operations 
		stopAndWait(txtRead, clientSideDS, ip_address, serverPort);
		goBackN(txtRead, clientSideDS, ip_address, serverPort);

		//	DatagramSocket close
		clientSideDS.close();

		// Output to user
		System.out.println("\nClient side closed.");

	}

	/**
	 * stopAndWait METHOD
	 **/

	private static void stopAndWait(String msg, DatagramSocket ds, InetAddress ip, int port) throws Exception {

		//	BufferedReader object to take quit program input from user
		BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));

		//	A timeout is set for the Stop and Wait
		ds.setSoTimeout(3000);

		//  Sequence Number
		Integer sequenceNo = 0;

		//	Output to the user the Stop and wait is running
		System.out.println("\nStop and Wait running...");

		//	for loop to run for 5 times to demonstrate Stop and Wait
		for (int i = 0; i < 5; i++) {
			
			boolean timer = true;

			while (timer) {

				//	sequenceNo increments
				sequenceNo++;

				/** Code section to send and receive data to/from server **/

				//	The Data parameter is accepted as a Byte array 
				//  SequenceNo is returned as a byte value
				byte[] msgAsBytes = msg.getBytes(StandardCharsets.UTF_8);
				byte seqAsBytes = sequenceNo.byteValue();

				//  Creates a byte array size reflecting the message and sequence number
				byte[] result = new byte[msgAsBytes.length + 1];
				result[0] = seqAsBytes;

				//  Inserts the sequence number into the byte array, result
				for (int n = 1; n < result.length; n++) {
					result[n] = msgAsBytes[n - 1];
				}

				//  result is assigned to clientSideMessage for sending
				byte[] clientSideSendMsg = result;

				//  byte array created to receive response
				byte[] clientSideReceiveMsg = new byte[1024];

				//  Output to the user the sequence number being sent
				System.out.println("Sending packet number : " + sequenceNo);

				try {

					//	calls sendToServer(Socket, Data, IP, Port)
					sendToServer(ds, clientSideSendMsg, ip, port);

					//	calls serverResponse(Socket, Data)
					String serverMsg = serverResponse(ds, clientSideReceiveMsg);

					//  Output to user the received message
					System.out.println("ACK FROM SERVER: " + serverMsg);

					//  The while loop stops on receipt of an ACK from server
					timer = false;
					
				} 
				
				//  To implement the Stop and Wait we use a SocketTimeoutException
				catch (SocketTimeoutException exception) {

					// If an ACK is not received from server for a packet,  the program resends the relevant sequenceNo
					System.out.println("Timer timed out sequence number: " + sequenceNo);

					// SequenceNo decrements to run that sequence number again
					sequenceNo--;
				}
			}
		}
	}


	/**
	 * goBackN METHOD
	 *
	 * @throws Exception
	 **/

	//	In GBN the sender transmits multiple packets in a pipeline, 5 in our case, without waiting for an ACK from the server
	//	It cannot have more than N packets in the pipeline.

	//  Go-Back-N operates with cumulative Acks. When sender receives an Ack with sequence number N, it acknowledges all packets up to sequenceNo = N.
	//  When receiving an Ack of N the window is moved forward to begin at N + 1.
	//  For packet loss we have a single timer, for the oldest "in-flight" packet.
	//  When timeout occurs, we retransmit packet N and all higher sequenceNo packets in the window.

	//  To demonstrate GBN, the client will send 10 versions of umbrella with sequence number appended
	//  It will do this in windows of 5 packets

	private static void goBackN(String msg, DatagramSocket ds, InetAddress ip, int port) throws Exception {

		System.out.println("\ngoBackN running...");
		
		//  A datasocket timeout of 1 second is set for GBN
		ds.setSoTimeout(1000);

		//  sequenceNo of packet
		Integer sequenceNo = 0;

		//	End Of Transmission number - I have limited it to 10 packets
		int eot = 10;

		//	Send packets to the while sequence number is less than the end of transition figure
		while (sequenceNo < eot) {

			try {

				//  A for loop of window size 5, breaks if sequenceNo reaches the eot
				for (int i = 0; i < WINDOW_SIZE; i++) {
					if(sequenceNo >= eot) {
						break;
					}

					/** Code section to send data to server **/

					//	The Data parameter is accepted as a Byte array - ByteBuffer sequenceNo allocated at front
					byte[] msgAsBytes = msg.getBytes(StandardCharsets.UTF_8);
					byte seqAsBytes = sequenceNo.byteValue();

					byte[] clientSideSendMsg = new byte[msgAsBytes.length + 1];
					clientSideSendMsg[0] = seqAsBytes;

					for (int n = 1; n < clientSideSendMsg.length; n++) {
						clientSideSendMsg[n] = msgAsBytes[n - 1];
					}

					/**
					artificially create a package loss
					 **/

					//  Here is some code to artificially demonstrate packet loss and therefore require the GoBackN protocol to be demonstrated
					Random rand = new Random();
					int nextRandomNumber = rand.nextInt(10);

					if (nextRandomNumber != 3) {
						
						/**real code - required in a normal program run**/
						//  Output to the user the sequence number being sent
						System.out.println("Sending GBN packet number : " + sequenceNo);
						//	calls sendToServer(Socket, Data, IP, Port)
						sendToServer(ds, clientSideSendMsg, ip, port);
						/**real code**/
					
					} else {
						System.out.println("Skipping packet " + sequenceNo);
					}
					/**
					 artificially create a package loss
					 **/

					//  sequenceNo increments
					sequenceNo++;
				}

				/** Code to receive packets from the server and catch missing packets, or a timeout **/ 
				//  t holds the current time at run
				//  end is the the point of timeout in miliseconds
				//  acknowledgements is an ArrayList to hold received data from the server
				long t = System.currentTimeMillis();
				long end = t + 3000;
				List<String> acknowledgements = new ArrayList<>();
				while (System.currentTimeMillis() < end) {

					/** Code section to receive data from server **/
					byte[] clientSideReceiveMsg = new byte[1024];

					//  NOTE THAT: blocking makes the waiting time imprecise
					//	calls serverResponse(Socket, Data)
					try {
						String serverMsg = serverResponse(ds, clientSideReceiveMsg);
						System.out.println("ACK FROM SERVER: " + serverMsg);
						//  the recieved packet is added to acknowledgements
						acknowledgements.add(serverMsg);
					}
					catch (Exception ex) {
						System.out.println("Catching socket timeout but pls continue");
					}
					Thread.sleep(100);
				}

				//  Create a list of int seq (sequence numbers), stream() makes this sequential, sorted() sorts them
				List<Integer>
						seqs =
						acknowledgements.stream()
								.map((ack) -> Integer.valueOf(ack.split(" ")[1]))
								.sorted()
								.collect(Collectors.toList());

				//  Edge case, what happens if last window is smaller than 5
				if(acknowledgements.size() != 5) {
					for(int range = sequenceNo - 4; range<sequenceNo; range++) {
						if(!seqs.contains(range)) {
							sequenceNo = range;
							break;
						}
					}
				} else {
					//shift window : NOP
				}

				acknowledgements.clear();

			} catch (SocketTimeoutException exception) {
				System.out.println("Upsi");
				System.out.println(exception);
			}
		}
	}


	/**
	 * sendToServer METHOD
	 **/

	//	As required in the brief, this takes IP and Port as parameters and sends the packet to the server 
	private static void sendToServer(DatagramSocket clientSideSocket, byte[] msg, InetAddress ip, int port)
			throws IOException {

		//	Creates DP using arguments
		DatagramPacket dpClientSideSend = new DatagramPacket(msg, msg.length, ip, port);

		//	Send the packet to the socket
		clientSideSocket.send(dpClientSideSend);

		//  Output to the use
		System.out.println("Seq "
				+ (int) msg[0]
				+ " "
				+ new String(Arrays.copyOfRange(msg, 1, msg.length))
				+ " sent to server...");
	}


	/**
	 * serverResponse METHOD
	 **/

	//	This requires a new datagram packet - however, IP and Port parameters not required for receipt
	private static String serverResponse(DatagramSocket clientSideDS, byte[] serverResponse) throws Exception {

		//  Creates DP using arguments
		DatagramPacket dpClientSideReceive = new DatagramPacket(serverResponse, serverResponse.length);

		//	To receive a datagram from this socket
		clientSideDS.receive(dpClientSideReceive);

		//	To then get the data from the dpClientReceive datagram, we use getData() and create a new string object serverResponse
		return new String(dpClientSideReceive.getData(), 0, dpClientSideReceive.getLength());

	}


	/**
	 * readTxt METHOD
	 **/

	//	readTxt takes a filePath as a string and returns the contents of the txt file at that filePath as a String
	private static String readTxt(String filePath) throws FileNotFoundException {

		//	Define the string to be returned
		String clientData = null;

		//	Create a File instance using the file path, if it exists the program will call getName(), getAbsolutePath(), canRead(), and length()
		//	This gives the user useful information about their file.
		//	if clientObj.exists() returns false, the user is notified
		File clientObj = new File(filePath);
		if (clientObj.exists()) {
			System.out.println("\nFile information...");
			System.out.println("File name: " + clientObj.getName());
			System.out.println("Absolute path: " + clientObj.getAbsolutePath());
			System.out.println("File size in bytes: " + clientObj.length());
		} else {
			System.out.println("The file does not exist or is unreadable.");
		}

		//	Using Scanner, bytes from the file object are converted to characters
		Scanner clientReader = new Scanner(clientObj);

		//  The while loop runs whilst clientRead Scanner object has another line to process - dictated by line separators
		while (clientReader.hasNextLine()) {

			//	Stored in the clientData string object and printed out for the user
			clientData = clientReader.nextLine();
			System.out.println("\nClient data: " + clientData);
		}

		//	The client clientReader Scanner object is closed
		clientReader.close();

		//	The clientData String object is returned
		return clientData;
	}
}

