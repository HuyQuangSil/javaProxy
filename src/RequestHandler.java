import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;

import javax.imageio.ImageIO;
import java.awt.Desktop;  

public class RequestHandler extends Thread{

	Socket clientSocket;
	BufferedReader proxyToClientBr; // đọc dữ liệu từ client cho proxy
	BufferedWriter proxyToClientWr; // ghi dữ liệu từ proxy qua cho client
	
	// constructor 
	public RequestHandler(Socket clientSocket) {
		this.clientSocket=clientSocket;
		try {
			this.clientSocket.setSoTimeout(2000);
			this.proxyToClientBr=new BufferedReader(new InputStreamReader( clientSocket.getInputStream())); // đọc request từ client
			this.proxyToClientWr=new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream())); // ghi dữ liệu trả lại cho client
			
		}catch(Exception e) {
			e.getStackTrace();
		}
	}
	
	// dùng để gửi du liệu về client
	private void sendNonCachedToClient(String urlString){

		try{
			
			// Compute a logical file name as per schema
			// This allows the files on stored on disk to resemble that of the URL it was taken from
			int fileExtensionIndex = urlString.lastIndexOf(".");
			String fileExtension;

			// Get the type of file
			fileExtension = urlString.substring(fileExtensionIndex, urlString.length());

			

			// Check if file is an image
			if((fileExtension.contains(".png")) || fileExtension.contains(".jpg") ||fileExtension.contains(".jpeg") || fileExtension.contains(".gif")){
				// Create the URL
				URL remoteURL = new URL(urlString);
				BufferedImage image = ImageIO.read(remoteURL); 

				if(image != null) { 
					String line = "HTTP/1.0 200 OK\n" +
							"Proxy-agent: ProxyServer/1.0\n" +
							"\r\n"; // 
					proxyToClientWr.write(line); 
					proxyToClientWr.flush();  

					// Send them the image data
					ImageIO.write(image, fileExtension.substring(1), clientSocket.getOutputStream());

				// No image received from remote server
				} else {
					
					String error = "HTTP/1.0 404 NOT FOUND\n" +
							"Proxy-agent: ProxyServer/1.0\n" +
							"\r\n";
					proxyToClientWr.write(error); // tráº£ vá» lá»—i
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
				

			
				String line = "HTTP/1.0 200 OK\n" +
						"Proxy-agent: ProxyServer/1.0\n" +
						"\r\n";
				proxyToClientWr.write(line);
				
				
				
				while((line = proxyToServerBR.readLine()) != null){
					
					proxyToClientWr.write(line); 
					
				}
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
			requestString=this.proxyToClientBr.readLine(); // đọc dữ liệu từ client gửi đến Proxy
		}
		catch(Exception e)
		{
			e.getStackTrace();
			System.out.println("Error request from client....");
			return ;
		}
		System.out.println("Request receiving "+requestString); // xem thử có nhận được request hay không
		
		// parse URL
		//String request=requestString.substring(0,requestString.indexOf(' ')); // GET hoăc POST
		String urlString=requestString.substring(requestString.indexOf(' ')+1); 
		urlString=urlString.substring(0,urlString.indexOf(' ')); // đường dẫn url
		
		
		if(!urlString.substring(0,4).equals("http")) // kiem tra xem url có là phương thức Http hay ko
		{
			urlString="http://"+urlString; //nếu ko thì thêm vào http:// để nó trở thành phương thức http
		}
		
		if(Proxy.isBlocked(urlString))  // check xem url có bị nằm trong danh sách blackList hay không 
		{
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
			System.out.println("-------------------------FORBIDEN PAGE-----------------------");
			BufferedWriter bufferedWriter=new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
			String line = "HTTP/1.0 403 Access Forbidden \n" +
					"User-Agent: ProxyServer/1.0\n" +
					"\r\n";
			bufferedWriter.write(line);
			StringBuilder contentBuilder = new StringBuilder();
			try {
			    BufferedReader in = new BufferedReader(new FileReader("src/ForbiddenPage/index.html"));
			    String str;
			    while ((str = in.readLine()) != null) {
			        contentBuilder.append(str);
			    }
			    in.close();
			} catch (IOException e) {
				e.getStackTrace();
			}
			String content = contentBuilder.toString();
			bufferedWriter.write(content); // trả về cho user biết là page đã bị blocked
			bufferedWriter.flush();
			bufferedWriter.close();
			
		}catch(Exception e)
		{
			e.getStackTrace();
		}
		
	}
}
