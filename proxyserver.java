package Proxy;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class proxyserver {

	//cache is a Map: the key is the URL and the value is the file name of the file that stores the cached content
	static Map<String, String> cache;
	
	ServerSocket proxySocket;
	String logFileName = "log.txt";
	File logFile = new File(logFileName);

	public void main(String[] args) {
		new proxyserver();
		proxyserver.startServer(Integer.parseInt(args[0]));
	}

	/**
	 * @param proxyPort
	 */
	static void startServer(int proxyPort) {

		cache = new ConcurrentHashMap<>();
		DataOutputStream os;
		DataInputStream is;
		Socket client;
		//List<Socket> clientRequestList= new List<Socket>();

		// create the directory to store cached files. 
		File cacheDir = new File("cached");
		if (!cacheDir.exists() || (cacheDir.exists() && !cacheDir.isDirectory())) {
			cacheDir.mkdirs();
		}

		ServerSocket server;
		try {
			server = new ServerSocket(proxyPort);
			client = server.accept();
			is = new DataInputStream(client.getInputStream());
			os = new DataOutputStream(client.getOutputStream());
		} catch (IOException e1) {
			e1.printStackTrace();
		}
 
		/**
				 * To do:
				 * create a serverSocket to listen on the port (proxyPort)
				 * Create a thread (RequestHandler) for each new client connection 
				 * remember to catch Exceptions!
				 *
			*/
	}



	public String getCache(String hashcode) {
		return cache.get(hashcode);
	}

	public void putCache(String hashcode, String fileName) {
		cache.put(hashcode, fileName);
	}

	public synchronized void writeLog(String info) throws IOException {
		
		FileOutputStream fileOs = new FileOutputStream(logFile);
		byte[] strToBytes = info.getBytes();
		fileOs.write(strToBytes);

		fileOs.close();
			/**
			 * To do
			 * write string (info) to the log file, and add the current time stamp 
			 * e.g. String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
			 *
			*/
	}

}