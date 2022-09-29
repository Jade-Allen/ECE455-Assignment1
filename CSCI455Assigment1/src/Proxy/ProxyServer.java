package Proxy;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Scanner;

public class ProxyServer implements Runnable {



	public static void main(String[] args) {
		ProxyServer thisProxy = new ProxyServer(80);
		thisProxy.listen();
		
	}

	private static ServerSocket proxySocket;

	//semaphore 
	private volatile static boolean running = true;

	//cache is a Map: the key is the URL and the value is the file name of the file that stores the cached content
	static HashMap<String, File> cache;
	
	//arraylist to hold all threads currently running
	static ArrayList<Thread> servicingThreads;

	

	
	String logFileName = "log.txt";
	File logFile = new File(logFileName);

	

	/**
	 * @param proxyPort
	 */
	public ProxyServer(int proxyPort) {
		
		//hashmap to hold cached site URLs
		cache = new HashMap<>();

		// holds threads
		servicingThreads = new ArrayList<>();

		new Thread().start(); //start run()

		try{
			// Load in cached sites from file
			File cachedSites = new File("cachedSites.txt");
			if(!cachedSites.exists()){
				System.out.println("No cached sites found - creating new file");
				cachedSites.createNewFile();
			} else {
				FileInputStream fileInputStream = new FileInputStream(cachedSites);
				ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
				cache = (HashMap<String,File>)objectInputStream.readObject();
				fileInputStream.close();
				objectInputStream.close();
			}

		} catch (IOException e) {
			System.out.println("Error loading previously cached sites file");
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			System.out.println("Class not found loading in preivously cached sites file");
			e.printStackTrace();
		}

		try {
			proxySocket = new ServerSocket(proxyPort);


			System.out.println("Waiting for client on port " + proxySocket.getLocalPort() + "..");
			running = true;
		} 

		// Catch exceptions associated with opening socket
		catch (SocketException se) {
			System.out.println("Socket Exception when connecting to client");
			se.printStackTrace();
		}
		catch (SocketTimeoutException ste) {
			System.out.println("Timeout occured while connecting to client");
		} 
		catch (IOException io) {
			System.out.println("IO exception when connecting to client");
		}

	}

	public void listen(){

		while(running){
			try {
				Socket socket = proxySocket.accept();
				
				Thread thread = new Thread(new RequestHandler(socket));
				
				servicingThreads.add(thread);
				
				thread.start();	
			} catch (SocketException e) {
				System.out.println("Server closed");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static File getCache(String url) {
		return cache.get(url);
	}

	/** puts file into a cache.
	 *  hashcode = URL
	 */
	public static void putCache(String url, File fileName) {
		cache.put(url, fileName);
	}

	private void closeServer(){
		System.out.println("\nClosing Server..");
		running = false;
		try{
			FileOutputStream fileOutputStream = new FileOutputStream("cachedSites.txt");
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);

			objectOutputStream.writeObject(cache);
			objectOutputStream.close();
			fileOutputStream.close();
			System.out.println("Cached Sites written");

			try{
				for(Thread thread : servicingThreads){
					if(thread.isAlive()){
						System.out.print("Waiting on "+  thread.getId()+" to close..");
						thread.join();
						System.out.println(" closed");
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			} catch (IOException e) {
				System.out.println("Error saving cache/blocked sites");
				e.printStackTrace();
			}

			try{
				System.out.println("Terminating Connection");
				proxySocket.close();
			} catch (Exception e) {
				System.out.println("Exception closing proxy's server socket");
				e.printStackTrace();
			}

		}

		@Override
		public void run() {
			Scanner scanner = new Scanner(System.in);

			String command;
			while(running){
				System.out.println("Enter \"cached\" to see cached sites, or \"close\" to close server.");
				command = scanner.nextLine();
				if(command.toLowerCase().equals("cached")){
					System.out.println("\nCurrently Cached Sites");
					for(String key : cache.keySet()){
						System.out.println(key);
					}
					System.out.println();
				}


				else if(command.equals("close")){
					running = false;
					closeServer();
				}

			}
			scanner.close();
		} 

		

	public synchronized void writeLog(String info) throws IOException {
		
		String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
		FileOutputStream fileOs = new FileOutputStream(logFile);
		logFile.createNewFile();

		fileOs.write(info.getBytes());
	}

}