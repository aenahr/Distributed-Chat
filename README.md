# Distributed Chat
This is a Distributed Chat program that contains no central coordinators; the processors will form an overlay topology known as a ring. Communication regarding all processes is done through a flooding protocol.

### Project Objective and Descrciption -- Important Notes
This program is done through a UNIX command line.

The task is to develop a distributed chat without a central coordinators. The processors will form an overlay topology known as a ring. In other words, every processor i will keep only the successor succ(i) and pred(i). When a user i wants to talk with a friend j it sends a PUT message to the successor. PUT consists of the source alias, the destination alias as well as the message. We assume that each user knows the alias of the friends.
A user that wants to join the system sends a JOIN message to a current participant (ex. i). Let j be the process that send the JOIN to i and let i + 1 be the successor of i. When i receives the request, it updates its routing table by replacing its predecessor with j and response with an ACCEPT message that contains the ip and port of the previous predecessor node in the ring.
When a user wants to leave the room, it sends a LEAV E message to the successor with the ip and port of the predecessor so that the nodes can reconstruct the ring.

### Important Functions to Note
* **PUT** - A Put message that holds information about a message being sent. This includes the alias of the sender, alias of the receiver, and the message being sent.
* **JOIN** - A Join message to process i+1 that asks to join the ring. It sends its information so that the process can change its predecessor information to it's information.
* **ACCEPT** - An Accept message that tells the process listening that they can join. This also sends the listening processing the sender's predecessor information.
* **NEW SUCCESSOR** - A NewSucessor message that tells the receiving process to change it's successor information to that of that ones in the message's parameters.
* **LEAVE** - Leave message that tells process i+1 that it will be leaving the ring. It leave it's predecessor information so that process i+1 can change it's pred information.

## Preparations for Running
Before running the program on UNIX, you must create the class files and import the packages needed for JSON to function.

### Creating the classpath files and importing the JSON jar files
```
javac -cp javax.json-1.0.4.jar:javax.json-api-1.0.jar Chat.java
```
### Running a process
Run Chat.java file with arg[0] being an alias and arg[1] being the port number
```
java -cp .:javax.json-1.0.4.jar Chat [alias] [portNum]
```
There will be three processes running simultaneously, each with a different alias and port number. 


