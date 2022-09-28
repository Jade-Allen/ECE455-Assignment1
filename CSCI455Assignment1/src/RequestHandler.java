package Proxy;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.SecureRandom;

//Temp comment
// RequestHandler is thread that process requests of one client connection
public class RequestHandler extends Thread{

	//socket for communication between client and proxy
	//passed by proxy
	Socket clientSocket;

	//date from client to proxy
	InputStream inFromClient;

	//data from proxy to client
	OutputStream outToClient;
	
	//request from client to proxy server.
	//client creates request. proxy reads and forwards it to web site
	byte[] request = new byte[1024];

	
	private ProxyServer server;


	/**
	 * creates RequestHandler Object for use with GET requests from client
	 * 
	 */
	public RequestHandler(Socket clientSocket, ProxyServer proxyServer) {

		
		this.clientSocket = clientSocket;

		this.server = proxyServer;

		try {
			clientSocket.setSoTimeout(2000);
			inFromClient = clientSocket.getInputStream();
			outToClient = clientSocket.getOutputStream();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	
	@Override
	
	public void run() {

		String requestString = request.toString();
		try{
			inFromClient.read(request);
		}catch(IOException e){
			e.printStackTrace();
			System.out.println("Error reading client request\n");
			return;
		}

		//prints request
		System.out.println("Request: " + requestString + "\n");

		//request type
		String requestType = requestString.substring(0, requestString.indexOf(' '));

		//URL string
		String requestURL = requestString.substring(requestString.indexOf(' ') + 1);

		//remove after last space
		requestURL = requestURL.substring(0, requestURL.indexOf(' '));

		//put HTTP if not present
		if(!requestURL.substring(0, 4).equals("http")){
			String t = "http://";
			requestURL = t + requestURL;
		}

		//check request type is GET. Ignore others
		if(requestType.equals("GET")){
			File file = proxyserver.getCache();
			if(file != null){
				System.out.println("Cached copy found\n");
				sendCachedInfoToClient(file);
			}else{
				System.out.println("HTTP GET request for " + requestURL + "\n");
				sendNoncachedInfoToClient(requestURL);
			}
		}
		/**
			 * To do
			 * Process the requests from a client. In particular, 
			 * (1) Check the request type, only process GET request and ignore others
                         * (2) Write log.
			 * (3) If the url of GET request has been cached, respond with cached content
			 * (4) Otherwise, call method proxyServertoClient to process the GET request
			 *
		*/

	}

	
	private void proxyServertoClient(byte[] clientRequest) {

		FileOutputStream fileWriter = null;
		Socket toWebServerSocket = null;
		InputStream inFromServer;
		OutputStream outToServer;
		
		// Create Buffered output stream to write to cached copy of file
		String fileName = "cached/" + generateRandomFileName() + ".dat";
		
		// to handle binary content, byte is used
		byte[] serverReply = new byte[4096];

		/**
		 * To do
		 * (1) Create a socket to connect to the web server (default port 80)
		 * (2) Send client's request (clientRequest) to the web server, you may want to use flush() after writing.
		 * (3) Use a while loop to read all responses from web server and send back to client
		 * (4) Write the web server's response to a cache file, put the request URL and cache file name to the cache Map
		 * (5) close file, and sockets.
		*/
		String clientSentence;
		String capitalizedSentence;

		ServerSocket proxySocket = new ServerSocket(80);

		// while(true){		// wait for contact by server
		// 	Socket connectionSocket = proxySocket.accept();

		// 	BufferedReader inFromClient = new BufferedReader(new InputStreamReader(proxySocket.getInputStream()));

		// 	DataOutputStream outToClient = new DataOutputStream(proxySocket.getOutputStream());

		// 	clientSentence = inFromClient.readLine();
		// 	capitalizedSentence = clientSentence.toUpperCase() + "\n";
		// 	outToClient.writeBytes(capitalizedSentence);
		// }

	}
	
	
	
	// Sends the cached content stored in the cache file to the client
	private void sendCachedInfoToClient(String fileName) {

		try {

			byte[] bytes = Files.readAllBytes(Paths.get(fileName));

			outToClient.write(bytes);
			outToClient.flush();

		} catch (Exception e) {
			e.printStackTrace();
		}

		try {

			if (clientSocket != null) {
				clientSocket.close();
			}

		} catch (Exception e) {
			e.printStackTrace();

		}
	}
	private void sendNoncachedInfoToClient(String requestURL) {
		//TODO
	}
	
	// Generates a random file name  
	public String generateRandomFileName() {

		String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_";
		SecureRandom RANDOM = new SecureRandom();
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < 10; ++i) {
			sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
		}
		return sb.toString();
	}

}