import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;

import javax.imageio.ImageIO;


public class RequestHandler extends Thread{

	Socket clientSocket;
	BufferedReader proxyToClientBr;
	BufferedWriter proxyToClientWr;
	public RequestHandler(Socket clientSocket) {
		this.clientSocket=clientSocket;
		try {
			this.clientSocket.setSoTimeout(2000);
			this.proxyToClientBr=new BufferedReader(new InputStreamReader( clientSocket.getInputStream()));
			this.proxyToClientWr=new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
			
		}catch(Exception e) {
			e.getStackTrace();
		}
	}
	
	private void sendNonCachedToClient(String urlString){

		try{
			
			// Compute a logical file name as per schema
			// This allows the files on stored on disk to resemble that of the URL it was taken from
			int fileExtensionIndex = urlString.lastIndexOf(".");
			String fileExtension;

			// Get the type of file
			fileExtension = urlString.substring(fileExtensionIndex, urlString.length());

			// Get the initial file name
			String fileName = urlString.substring(0,fileExtensionIndex);


			// Trim off http://www. as no need for it in file name
			fileName = fileName.substring(fileName.indexOf('.')+1);

			// Remove any illegal characters from file name
			fileName = fileName.replace("/", "__");
			fileName = fileName.replace('.','_');
			
			// Trailing / result in index.html of that directory being fetched
			if(fileExtension.contains("/")){
				fileExtension = fileExtension.replace("/", "__");
				fileExtension = fileExtension.replace('.','_');
				fileExtension += ".html";
			}
		
			fileName = fileName + fileExtension;



			// Attempt to create File to cache to
			

			// Check if file is an image
			if((fileExtension.contains(".png")) || fileExtension.contains(".jpg") ||fileExtension.contains(".jpeg") || fileExtension.contains(".gif")){
				// Create the URL
				URL remoteURL = new URL(urlString);
				BufferedImage image = ImageIO.read(remoteURL);

				if(image != null) {
					// Send response code to client
					String line = "HTTP/1.0 200 OK\n" +
							"Proxy-agent: ProxyServer/1.0\n" +
							"\r\n";
					proxyToClientWr.write(line);
					proxyToClientWr.flush();

					// Send them the image data
					ImageIO.write(image, fileExtension.substring(1), clientSocket.getOutputStream());

				// No image received from remote server
				} else {
					System.out.println("Sending 404 to client as image wasn't received from server"
							+ fileName);
					String error = "HTTP/1.0 404 NOT FOUND\n" +
							"Proxy-agent: ProxyServer/1.0\n" +
							"\r\n";
					proxyToClientWr.write(error);
					proxyToClientWr.flush();
					return;
				}
			} 

			// File is a text file
			else {
								
				// Create the URL
				URL remoteURL = new URL(urlString);
				// Create a connection to remote server
				HttpURLConnection proxyToServerCon = (HttpURLConnection)remoteURL.openConnection();
				proxyToServerCon.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
				proxyToServerCon.setRequestProperty("Content-Language", "en-US");  
				//proxyToServerCon.setUseCaches(false);
				proxyToServerCon.setDoOutput(true);
			
				// Create Buffered Reader from remote Server
				BufferedReader proxyToServerBR = new BufferedReader(new InputStreamReader(proxyToServerCon.getInputStream()));
				

				// Send success code to client
				String line = "HTTP/1.0 200 OK\n" +
						"Proxy-agent: ProxyServer/1.0\n" +
						"\r\n";
				proxyToClientWr.write(line);
				
				
				// Read from input stream between proxy and remote server
				while((line = proxyToServerBR.readLine()) != null){
					// Send on data to client
					proxyToClientWr.write(line);
					
				}
				
				// Ensure all data is sent by this point
				proxyToClientWr.flush();

				// Close Down Resources
				if(proxyToServerBR != null){
					proxyToServerBR.close();
				}
			}
			// Close down resources
			
			if(proxyToClientWr != null){
				proxyToClientWr.close();
			}
		} 

		catch (Exception e){
			e.printStackTrace();
		}
	}

	@Override
	public void run()
	{
		String requestString;
		try {
			requestString=this.proxyToClientBr.readLine();
		}catch(Exception e)
		{
			e.getStackTrace();
			System.out.println("Error request from client....");
			return ;
		}
		System.out.println("Request receiving "+requestString);
		
		// parse URL
		String request=requestString.substring(0,requestString.indexOf(' '));
		String urlString=requestString.substring(requestString.indexOf(' ')+1);
		urlString=urlString.substring(0,urlString.indexOf(' '));
		
		
		if(!urlString.substring(0,4).equals("http"))
		{
			urlString="http://"+urlString;
		}
		if(Proxy.isBlocked(urlString)) {
			System.out.println("Block site request "+urlString);
			blackListRequest();
			return;
		}
		else{			
			System.out.println("HTTP GET for : " + urlString + "\n");
			sendNonCachedToClient(urlString);
		}
	}
	
	
	private void blackListRequest() {
		try {
			System.out.println("-------------------------ERROR PAGE-----------------------");
			BufferedWriter bufferedWriter=new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
			String line = "HTTP/1.0 403 Access Forbidden \n" +
					"User-Agent: ProxyServer/1.0\n" +
					"\r\n";
			bufferedWriter.write(line);
			bufferedWriter.flush();
			
		}catch(Exception e)
		{
			e.getStackTrace();
		}
		
	}
}
