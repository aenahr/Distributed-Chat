
/**
 *  CECS327 Assignment 2: Distributed Chat
 *  @file Chat.java
 *  @author Dan Ho
    @author Aenah Ramones
 *  @brief This is a distributed chat without a central coordinator; communication
 *  is based on a flooding protocol where processors ultimately form an overlay topology
 *  similar to that of a ring.
*/
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.Semaphore;

import javax.json.*;


/**
 * @brief It implements a distributed chat. 
 * It creates a ring and delivers messages
 * using flooding 
 **/
public class Chat   {

	/** Semaphore to lock our 4 global variables*/
	public Semaphore rLock = new Semaphore(1);
	/** My info*/
	public String alias;
	public int myPort;
	/** Successor*/
	public String ipSuccessor;
	public int portSuccessor;
	/*! Predecessor */
	public String ipPredecessor;
	public int portPredecessor;

	/**
	 * @brief A Join message to process i+1 that asks to join the ring. It sends its information so that the 
	 * process can change its predecessor information to it's information.
	 * @param alias process i's alias
	 * @param myPort process i's port
	 * @return JsonObject join message
	 */
	private JsonObject createJOINmsg(String alias, int myPort)
	{
		JsonObject value = Json.createObjectBuilder()
				.add("type", "JOIN")
				.add("parameters", Json.createObjectBuilder()
						.add("myAlias", alias)
						.add("myPort", myPort)).build();


		return value;
	}

	/**
	 * An Accept message that tells the process listening that they can join. This 
	 * also sends the listening processing the sender's predecessor information.
	 * @param ip the ip address of the predecessor
	 * @param port of the predecessor
	 * @return accept message in form of JSonObject
	 */
	private JsonObject createACCEPTmsg(String ip, int port)
	{
		JsonObject value = Json.createObjectBuilder()
				.add("type", "ACCEPT")
				.add("parameters", Json.createObjectBuilder()
						.add("ipPred", ip)
						.add("portPred", port)).build();


		return value;
	}
	
	/**
	 * A NewSucessor message that tells the receiving process to change it's successor information
	 * to that of that ones in the message's parameters
	 * @param ip ip of the process's new successor
	 * @param port port of the process's new successor
	 * @return NewSucessor message in form of JSonObject
	 */
	private JsonObject createNEWSUCmsg(String ip, int port)
	{
		JsonObject value = Json.createObjectBuilder()
				.add("type", "NEWSUCCESSOR")
				.add("parameters", Json.createObjectBuilder()
						.add("ipSuccessor", ip)
						.add("portSuccessor", port)).build();


		return value;
	}

	/**
	 * Leave message that tells process i+1 that it will be leaving the ring. It leave it's predecessor information
	 * so that process i+1 can change it's pred information.
	 * @param ipPred process i's predecessor ip
	 * @param portPred process i's precessor port
	 * @return message in form of JSonObject
	 */
	private JsonObject createLEAVEmsg(String ipPred, int portPred)

	{
		JsonObject value = Json.createObjectBuilder()
				.add("type", "LEAVE")
				.add("parameters", Json.createObjectBuilder()
						.add("ipPred", ipPred)
						.add("portPredecessor", portPred)).build();

		return value;

	}


	/**
	 * A Put message that holds information about a message being sent. This includes the alias of the sender, alias of the 
	 * receiver, and the message being sent
	 * @param aliasSender The alias of the process sending the message
	 * @param aliasReceiver The alias of the process receiving the message 
	 * @param message The message that is being sent
	 * @return the message in the form of a JSonObject
	 */
	private JsonObject createPUTmsg(String aliasSender, String aliasReceiver, String message)

	{
		JsonObject value = Json.createObjectBuilder()
				.add("type", "PUT")
				.add("parameters", Json.createObjectBuilder()
						.add("aliasSender", aliasSender)
						.add("aliasReceiver", aliasReceiver)
						.add("message", message)).build();  
		return value;
	}
	

	/**
	 * @brief It implements the server. Server Class "Chat.java"
	 * @author Dan and Aenah
	 */
	private class Server implements Runnable 
	{
		public Server()
		{
		}
		/*****************************//**
		 * \brief It allows the system to interact with the participants. 
		 **********************************/   
		public void run() {
			try {
				ServerSocket servSock = new ServerSocket(myPort);
				while (true)
				{

					Socket clntSock = servSock.accept(); /*! Get client connections*/

					/*!The Streams through which the Server listens and writes through*/
					ObjectInputStream  ois = new ObjectInputStream(clntSock.getInputStream());
					ObjectOutputStream oos = new ObjectOutputStream(clntSock.getOutputStream());
					JsonReader jsonreader = Json.createReader(ois);

					JsonObject value = jsonreader.readObject();

					/*!How the Server should react if the JSonObject received is of a Join Type*/
					if (value.getString("type").equals("JOIN"))
					{
						/*!Prompt Join*/
						System.out.println("Receiving Joining ");

						/*!Keeps information of the parameters in the join message*/
						String alias = value.getJsonObject("parameters").getString("myAlias");
						Integer port = value.getJsonObject("parameters").getInt("myPort");

						/*!Prepare of accept and new successor message to be sent*/
						JsonObject accept = createACCEPTmsg(ipPredecessor, portPredecessor);
						JsonObject newSuccessor = createNEWSUCmsg("localhost", port);
						
						clntSock.close();

						/*!Write the accept message into the process trying to join the ring*/
						Socket socket = new Socket("localhost", port);
						oos = new ObjectOutputStream(socket.getOutputStream());
						JsonWriter jsonWriter = Json.createWriter(oos);
						jsonWriter.write(accept);
						jsonWriter.close();
						socket.close();

						/*!Write the new successor to the predecessor so that it can 
						 * update its new successor 
						 */
						socket = new Socket("localhost", portPredecessor);
						oos = new ObjectOutputStream(socket.getOutputStream());
						jsonWriter = Json.createWriter(oos);
						jsonWriter.write(newSuccessor);
						jsonWriter.close();
						socket.close();

						/*! Locks the two global variables when they're used and releases them when it's done using them*/
						rLock.acquire();
						ipPredecessor = "localhost";
						portPredecessor = port;
						rLock.release();
					}
					/*!Process i receives an accept, which contains information about
					 * the sender's predeceessor
					 */
					if (value.getString("type").equals("ACCEPT"))
					{

						System.out.println("Receiving Accept ");

						/*!Information about the send's predecessor.
						 * Allows this current process to change its predecessor information
						 */
						String ip = value.getJsonObject("parameters").getString("ipPred");
						Integer port = value.getJsonObject("parameters").getInt("portPred");

						clntSock.close();

						/*!Locks the two global variables when they're used and releases them when it's done using them*/
						rLock.acquire();
						ipPredecessor = ip;
						portPredecessor = port;
						rLock.release();
					}
					if (value.getString("type").equals("NEWSUCCESSOR"))
					{
						System.out.println("Receiving New Successor");

						/*!Information for the receiving process to update its successor*/
						String ip = value.getJsonObject("parameters").getString("ipSuccessor");
						Integer port = value.getJsonObject("parameters").getInt("portSuccessor");

						clntSock.close();

						/*! Locks the two global variables when they're used and releases them when it's done using them*/
						rLock.acquire();
						ipSuccessor = ip;
						portSuccessor = port;
						rLock.release();
					}
					/*! The receiving process checks whether or not the message is for itself and 
					 * reacts accordingly.
					 */
					if (value.getString("type").equals("PUT"))
					{
						System.out.println("Receiving PUT");
						/*! Information about who is sending the message, who the message is for,
						 *  and what the message will say
						 */
						String aliasRec = value.getJsonObject("parameters").getString("aliasReceiver");
						String aliasSend = value.getJsonObject("parameters").getString("aliasSender");
						String message = value.getJsonObject("parameters").getString("message");

						/*!If the alias that sent the message is equal to this alias,
						 * then we made a full circle and should print it.
						 */
						if(aliasSend.equals(alias))
						{
							System.out.println("We made a full circle. I guess that alias isn't here.");
						}
						/*! If the receiving process's alias is equal to the receiving alias,
						 * then we should output the message
						 */
						else if(aliasRec.equals(alias))
						{
							System.out.println(aliasSend + " to me: " + message);
						}
						/*! Otherwise, the message should just be passed on. It will pass
						 * The message on to it's successor, continuing along the ring.
						 */
						else
						{
							System.out.println("I'm not " + aliasRec + ", so I'll pass you down to my sucessor.");
							
							/*! Create JsonObject containing information the put message again */
							JsonObject put = createPUTmsg(aliasSend,aliasRec,message);
							
							/*! Makes the socket to pass information to its successor*/
							Socket socket = new Socket("localhost", portSuccessor);
							oos = new ObjectOutputStream(socket.getOutputStream());
							JsonWriter jsonWriter = Json.createWriter(oos);
							
							/*! Writes the object into the stream, into the socket*/
							jsonWriter.write(put);
							jsonWriter.close();
							socket.close();
						}
					}
					/*! The process receiving this message is process i+1 for the 
					 * process trying to leave, process i. It will allow process i
					 * to leave the ring. 
					 */
					if (value.getString("type").equals("LEAVE"))
					{
						System.out.println("Receiving LEAVE");
						/*! Contain's information about process i's predecessor.
						 * Allows process i+1 to update it's predecessor information 
						 */
						String ipPred = value.getJsonObject("parameters").getString("ipPred");
						int portPred = value.getJsonObject("parameters").getInt("portPredecessor");

						
						/*! Locks the global variables when they are used and releases them when we're done with them */
						rLock.acquire();
						ipPredecessor = ipPred;
						portPredecessor = portPred;
						rLock.release();
						
						/*! Sends newSucessor to process i's predecessor to update it's successor 
						 * information to process i+1's. Nothing from this point will be pointing 
						 * to process i.
						 */
						JsonObject newSuccessor = createNEWSUCmsg("localhost", myPort);
						Socket socket = new Socket("localhost", portPred);
						oos = new ObjectOutputStream(socket.getOutputStream());
						JsonWriter jsonWriter = Json.createWriter(oos);
						jsonWriter.write(newSuccessor);
						jsonWriter.close();
						socket.close();
						clntSock.close();
					}

				}
			} catch (IOException e)
			{
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}



	/**
	 * @brief It implements the client
	 * @author Dan and Aenah
	 */
	private class Client implements Runnable 
	{       

		public Client()
		{
		}
 
		/**
		 * It allows the user to interact with the system. 
		 */
		public void run()
		{
			while (true)
			{
				Scanner s = new Scanner(System.in);
				/*! Displays a menu for this client */
				
				System.out.println("\n-------------------------------------");
				System.out.println("               Menu");
				System.out.println("-------------------------------------");
				System.out.println("1. Join A Port");
				System.out.println("2. Print Predecessor/Successor Ports");
				System.out.println("3. Send Message");
				System.out.println("4. Leave Ring");
				System.out.println("0. Exit");
				
				/*! Prompts input from user to decide what to do*/
				int option = s.nextInt();

				if (option == 1) //JOIN
				{
					try
					{
						/*!Prompt user for which port they would want to change to*/
						System.out.println("Which port would you like to join?");
						int port = s.nextInt();
						
						/*!Sends a join message to the port that it wants to join*/
						Socket socket = new Socket("localhost", port);
						ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
						JsonObject join = createJOINmsg(alias, myPort);
						JsonWriter jsonWriter = Json.createWriter(oos);
						
						/*! Writes the message into the listening process*/
						jsonWriter.write(join);
						jsonWriter.close();
						socket.close();
						
						/*! Update this process's information to that */
						portSuccessor = port;
						ipSuccessor = "localhost";
					}
					catch(IOException e)
					{
						e.printStackTrace();

					}
				}
				/*! prints information about this process, including its alias, successor and predecessor*/
				else if (option == 2) 
				{
					System.out.println("Alias: " + alias);
					System.out.println("Successor "+portSuccessor);
					System.out.println("Predecessor "+portPredecessor);

				}
				//
				else if (option == 3) 
				{
					System.out.println("What is the Alias/Name of the process you are messaging?");
					s.nextLine();
					String receiver = s.nextLine();
					System.out.println("What do you want to say to it?");
					String message = s.nextLine();
					Socket socket;
					try {
						socket = new Socket("localhost", portSuccessor);
						ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
						JsonObject put = createPUTmsg(alias,receiver,message);

						JsonWriter jsonWriter = Json.createWriter(oos);
						jsonWriter.write(put);
						jsonWriter.close();
						socket.close();


					} catch (UnknownHostException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				/*! Allows the process to leave the ring by sending a message to its successor */
				else if (option == 4)
				{

					try
					{
						/*! Creates a socket for the successor*/
						Socket socket = new Socket("localhost", portSuccessor);
						ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
						
						/*! Sends a leave message to the successor with the information
						 * of its predecessor. The successor can change its predecessor 
						 * information to the process i's predecessor.
						 */
						JsonObject leave = createLEAVEmsg(ipPredecessor, portPredecessor);
						JsonWriter jsonWriter = Json.createWriter(oos);
						jsonWriter.write(leave);
						jsonWriter.close();
						socket.close();
						System.out.println("I'm outta here!");
					}
					catch(IOException e)
					{
						e.printStackTrace();

					}

				}
				/*! Allows the user to exit the program */
				else if (option == 0)
				{
					System.exit(0);
				}
			}
		}
	}

	/*****************************//**
	 * Starts the threads with the client and server:
	 * @param Id unique identifier of the process
	 * @param port where the server will listen
	 **********************************/  
	public Chat(String alias, int myPort) {

		this.alias = alias;
		this.myPort = myPort;

		ipSuccessor = "localhost";
		portSuccessor = myPort;
		ipPredecessor= "localhost";
		portPredecessor = myPort;


		/*! Initialization of the peer*/
		Thread server = new Thread(new Server());
		Thread client = new Thread(new Client());
		server.start();
		client.start();
		try {
			client.join();
			server.join();
		} catch (InterruptedException e)
		{
			/*! Handle Exception*/
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {

		if (args.length < 2 ) {  
			throw new IllegalArgumentException("Parameter: <alias> <myPort>");
		}
		Chat chat = new Chat(args[0], Integer.parseInt(args[1]));
	}
}
