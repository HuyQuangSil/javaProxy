import java.io.*;
import java.net.*;
import java.util.*;
public class Proxy extends Thread {

	public static void main(String[] args) {
		Proxy myProxy=new Proxy(8888);
		myProxy.listen();

	}
	
	private ServerSocket serverSocket;
	private volatile boolean running=true;
	static HashMap<String,String>blackList;
	static ArrayList<Thread>receivingThread; 
	
	public Proxy(int port) {
		blackList=new HashMap<String,String>();
		receivingThread=new ArrayList<Thread>();
		
		new Thread(this).start(); // call the run method at the bottom
		
		try {
			
			File blackListFile =new File("blackList.txt");
			if(!blackListFile.exists()) {
				System.out.println("Can not find any File like this!!!");
				blackListFile.createNewFile();
			}
			else {
				ObjectInputStream objectInputStream=new ObjectInputStream(new FileInputStream(blackListFile));
				blackList=(HashMap<String,String>)objectInputStream.readObject(); 
				objectInputStream.close();
			}
		}
		catch(Exception e)
		{
			System.out.println(e);
		}
		
		try {
			serverSocket=new ServerSocket(port);
			System.out.println("Waiting for client on port "+serverSocket.getLocalPort()+" ...");
			this.running=true;
		}
		catch(Exception e) {
			System.out.println(e);
		}
	}
	
	public void listen() {
		while(running) {
			try {
				Socket socket=serverSocket.accept();
				
				Thread thread=new Thread(new RequestHandler(socket));
				thread.start();
				receivingThread.add(thread);
			}
			catch(Exception e) {
				System.out.println(e);
			}
		}
	}
	
	private void closeServer() {
		System.out.println("\nClosing server.......");
		try
		{
			FileOutputStream fileOutputStream1 = new FileOutputStream("cachedSites.txt");
			ObjectOutputStream objectOutputStream1 = new ObjectOutputStream(fileOutputStream1);

			
			objectOutputStream1.close();
			fileOutputStream1.close();
			System.out.println("Cached Sites written");
			
			
			FileOutputStream fileOut=new FileOutputStream("blackList.txt");
			ObjectOutputStream obj=new ObjectOutputStream(fileOut);
			obj.writeObject(blackList);
			obj.close();
			fileOut.close();
			System.out.println("Black list is saved!!");
		}
		catch(Exception e) {
			System.out.println(e);
			e.printStackTrace();
		}
		
		try {
			for(Thread thread :receivingThread) {
				if(thread.isAlive())
				{
					System.out.println("waiting for "+thread.getId()+" to close");
					thread.join();
					System.out.println("closed");
				}
			}
		}catch(Exception e) {
			e.printStackTrace();
			System.out.println(e);
		}
		
		try{
			System.out.println("Terminating Connection");
			serverSocket.close();
		} catch (Exception e) {
			System.out.println("Exception closing proxy's server socket");
			e.printStackTrace();
		}
	}
	
	
	public static boolean isBlocked(String url)
	{
		if(blackList.get(url)!=null)
			return true;
		return false;
	}
	
	public void run()
	{
		Scanner scanner=new Scanner(System.in);
		String command;
		while(running)
		{
			System.out.println("Enter new Site to block");
			System.out.println("or Enter \"blocked\" to see blackList");
			System.out.println("or Enter \"close\" to close the server");
			command=scanner.nextLine();
			if(command.toLowerCase().equals("blocked")) {
				System.out.println("The current blackList is :");
				for(String key : blackList.keySet()) {
					System.out.println(key);
				}
				System.out.println();
			}
			
			else if(command.toLowerCase().equals("close")) {
				running=false;
				closeServer();
			}
			else
			{
				blackList.put(command, command);
				System.out.println("Saved Successfully");
			}
		}
		scanner.close();
	}
}
