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
										
				URL remoteURL = new URL(urlString); // khởi tạo 1 class URL để giữ đường dẫn cần truy cập đến
				// tạo 1 connection và cho phép client truy cập vào URL
				HttpURLConnection proxyToServerCon = (HttpURLConnection)remoteURL.openConnection();
				proxyToServerCon.setRequestMethod("GET");  // set Request để lấy dữ liệu là phương thức GET
				proxyToServerCon.setRequestProperty("User-Agent", "Mozilla/5.0");			
				// tạo biến để đọc dữ liệu từ Webserver đến proxy và gửi cho client				
				BufferedReader proxyToServerBR = new BufferedReader(new InputStreamReader(proxyToServerCon.getInputStream()));
				// Send success code to client
				String line = "HTTP/1.0 200 OK\n" +
						"Proxy-agent: ProxyServer/1.0\n" +
						"\r\n";
				proxyToClientWr.write(line);
				
				
				//  proxy đọc dữ liệu từ server 
				while((line = proxyToServerBR.readLine()) != null){
					
					proxyToClientWr.write(line); // proxy chuyển dữ liệu từ proxy đến client
					
				}
				proxyToClientWr.flush(); // xoa bộ nhớ đệm
		
				// đóng file đọc
				if(proxyToServerBR != null){
					proxyToServerBR.close();
				}
				// đóng file ghi cho client
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
			System.out.println("-------------------------ERROR PAGE-----------------------");
			BufferedWriter bufferedWriter=new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
			String line = "HTTP/1.0 403 Access Forbidden \n" +
					"User-Agent: ProxyServer/1.0\n" +
					"\r\n";
			bufferedWriter.write(line);
			StringBuilder contentBuilder = new StringBuilder();
			try {
			    BufferedReader in = new BufferedReader(new FileReader("src/ForbiddenPage/html/index.html"));
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
			
		}catch(Exception e)
		{
			e.getStackTrace();
		}
		
	}
}
