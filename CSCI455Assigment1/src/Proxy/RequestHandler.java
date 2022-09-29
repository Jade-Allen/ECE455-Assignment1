package Proxy;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.security.SecureRandom;

import javax.imageio.ImageIO;

// RequestHandler is thread that process requests of one client connection
public class RequestHandler implements Runnable{

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

	


	/**
	 * creates RequestHandler Object for use with GET requests from client
	 * 
	 */
	public RequestHandler(Socket clientSocket) {

		
		this.clientSocket = clientSocket;

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
			File file = ProxyServer.getCache(requestURL);
			if(file != null){
				System.out.println("Cached copy found\n");
				sendCachedInfoToClient(file);
			}else{
				System.out.println("HTTP GET request for " + requestURL + "\n");
				sendNoncachedInfoToClient(requestURL);
			}
		}


	}
	
	
	
	// Sends the cached content stored in the cache file to the client
	private void sendCachedInfoToClient(File cachedFile) {
		try{
			// If file is an image
			String fileExtension = cachedFile.getName().substring(cachedFile.getName().lastIndexOf('.'));
			
			String response;
			if((fileExtension.contains(".png")) || fileExtension.contains(".jpg") ||
					fileExtension.contains(".jpeg") || fileExtension.contains(".gif")){
				BufferedImage image = ImageIO.read(cachedFile);
				
				if(image == null ){
					System.out.println("Image " + cachedFile.getName() + " was null");
					response = "HTTP/1.0 404 NOT FOUND \n" +
							"Proxy-agent: ProxyServer/1.0\n" +
							"\r\n";
					outToClient.write(response.getBytes());
					outToClient.flush();
				} else {
					response = "HTTP/1.0 200 OK\n" +
							"Proxy-agent: ProxyServer/1.0\n" +
							"\r\n";
					outToClient.write(response.getBytes());
					outToClient.flush();
					ImageIO.write(image, fileExtension.substring(1), clientSocket.getOutputStream());
				}
			} 
			
			else {
				BufferedReader cachedFileBufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(cachedFile)));

				response = "HTTP/1.0 200 OK\n" +
						"Proxy-agent: ProxyServer/1.0\n" +
						"\r\n";
				outToClient.write(response.getBytes());
				outToClient.flush();

				String line;
				while((line = cachedFileBufferedReader.readLine()) != null){
					outToClient.write(line.getBytes());
				}
				outToClient.flush();
				
				// Close
				if(cachedFileBufferedReader != null){
					cachedFileBufferedReader.close();
				}	
			}


			// Close Down 
			if(outToClient != null){
				outToClient.close();
			}

		} catch (IOException e) {
			System.out.println("Error Sending Cached file to client");
			e.printStackTrace();
		}
	}

	private void sendNoncachedInfoToClient(String requestURL) {
		try{
			

			int fileExtensionIndex = requestURL.lastIndexOf(".");
			String fileExtension;

			// Get the type of file
			fileExtension = requestURL.substring(fileExtensionIndex, requestURL.length());

			// Get the initial file name
			String fileName = requestURL.substring(0,fileExtensionIndex);


			// Trim off http://www.
			fileName = fileName.substring(fileName.indexOf('.')+1);

			fileName = fileName.replace("/", "__");
			fileName = fileName.replace('.','_');
			
			if(fileExtension.contains("/")){
				fileExtension = fileExtension.replace("/", "__");
				fileExtension = fileExtension.replace('.','_');
				fileExtension += ".html";
			}
		
			fileName = fileName + fileExtension;



			boolean caching = true;
			File fileToCache = null;
			BufferedWriter fileToCacheBW = null;

			try{
				fileToCache = new File("cached/" + fileName);

				if(!fileToCache.exists()){
					fileToCache.createNewFile();
				}

				fileToCacheBW = new BufferedWriter(new FileWriter(fileToCache));
			}
			catch (IOException e){
				System.out.println("Couldn't cache: " + fileName);
				caching = false;
				e.printStackTrace();
			} catch (NullPointerException e) {
				System.out.println("NPE opening file");
			}


			// Check if file is an image
			if((fileExtension.contains(".png")) || fileExtension.contains(".jpg") ||
					fileExtension.contains(".jpeg") || fileExtension.contains(".gif")){
				// Create the URL
				URL remoteURL = new URL(requestURL);
				BufferedImage image = ImageIO.read(remoteURL);

				if(image != null) {
					ImageIO.write(image, fileExtension.substring(1), fileToCache);

					String line = "HTTP/1.0 200 OK\n" +
							"Proxy-agent: ProxyServer/1.0\n" +
							"\r\n";
					outToClient.write(line.getBytes());
					outToClient.flush();

					ImageIO.write(image, fileExtension.substring(1), clientSocket.getOutputStream());

				} else {
					System.out.println("Sending 404 to client as image wasn't received from server"
							+ fileName);
					String error = "HTTP/1.0 404 NOT FOUND\n" +
							"Proxy-agent: ProxyServer/1.0\n" +
							"\r\n";
					outToClient.write(error.getBytes());
					outToClient.flush();
					return;
				}
			} 

			// File is a text file
			else {
								
				// Create the URL
				URL remoteURL = new URL(requestURL);
				HttpURLConnection proxyToServerCon = (HttpURLConnection)remoteURL.openConnection();
				proxyToServerCon.setRequestProperty("Content-Type", 
						"application/x-www-form-urlencoded");
				proxyToServerCon.setRequestProperty("Content-Language", "en-US");  
				proxyToServerCon.setUseCaches(false);
				proxyToServerCon.setDoOutput(true);
			
				BufferedReader proxyToServerBR = new BufferedReader(new InputStreamReader(proxyToServerCon.getInputStream()));
				

				String line = "HTTP/1.0 200 OK\n" +
						"Proxy-agent: ProxyServer/1.0\n" +
						"\r\n";
				outToClient.write(line.getBytes());
				
				
				while((line = proxyToServerBR.readLine()) != null){
					outToClient.write(line.getBytes());

					if(caching){
						fileToCacheBW.write(line);
					}
				}
				
				outToClient.flush();

				// Close
				if(proxyToServerBR != null){
					proxyToServerBR.close();
				}
			}


			if(caching){
				fileToCacheBW.flush();
				ProxyServer.putCache(requestURL, fileToCache);
			}

			if(fileToCacheBW != null){
				fileToCacheBW.close();
			}

			if(outToClient != null){
				outToClient.close();
			}
		} 

		catch (Exception e){
			e.printStackTrace();
		}
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