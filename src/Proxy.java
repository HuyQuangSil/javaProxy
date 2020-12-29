import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;




public class Proxy extends Thread {

	public static void main(String[] args) {
		Proxy myProxy=new Proxy(8888); // lắng nghe client ở cổng 8888
		
		myProxy.listen();  // gọi method listen() ở dưới

	}
	Scanner scanner=new Scanner(System.in);
	private ServerSocket serverSocket;  // tạo ra 1 cái socket 
	
	private volatile boolean running=true; // điều kiện để tiếp tục chạy hàm run() ở phía dưới
	
	// vì HashMap là 1 collection nên nó được serialize (tuần tự) nên có thể đọc ghi dữ liệu theo kiểu ObjectOut/InputStream
	static HashMap<String,String>blackList;  //danh sách các website sẽ bị block
	static HashMap<String,String>bin;
	static ArrayList<Thread>receivingThread;  // danh sách các luồng dữ liệu trong chương trình 
	
	public Proxy(int port) { // constructor khi khởi tạo 1 object proxy với tham số cần truyền vào là cổng để kết nối với client
		blackList=new HashMap<String,String>(); // khởi tạo blackList là 1 collection framework kiểu HashMap (key-value)
		receivingThread=new ArrayList<Thread>(); // Khởi tạo mảng các luồng dữ liệu trong chương trình
		bin=new HashMap<String,String>();  // khoi tao thung rac chua cac web bi xoa khoi blacklist
		new Thread(this).start(); // gọi start() thì hàm start() sẽ gọi đến hàm run(), đây là 1 hàm đã được định nghĩa sẵn trong Thead class
		
		try {
			
			File blackListFile =new File("blackList.conf"); // cố để mở file "backList.conf" 
			
			if(!blackListFile.exists()) { // nếu file không tồn tại
				System.out.println("Can not find any File like this!!!");
				blackListFile.createNewFile(); // tạo ra 1 file mới có tên "blackList.conf" trong folder
			}
			else {
				// nếu file đã tồn tại trong folder
				ObjectInputStream objectInputStream=new ObjectInputStream(new FileInputStream(blackListFile)); // đọc từ file backListFile theo kiểu object
				blackList=(HashMap<String,String>)objectInputStream.readObject(); // ép kiểu dữ liệu về HashMap
				objectInputStream.close();
				// kiểu ObjectInputStream là ta sẽ đọc dữ liệu theo kiểu mà ta đã lưu dữ liệu vào file
			}
		}
		catch(Exception e) // nếu xảy ra lỗi thì sẽ chạy vào exception
		{
			System.out.println(e);
		}
		
		File file=new File("bin.txt");
		try {
			ObjectInputStream obj=new ObjectInputStream(new FileInputStream(file)); // đọc từ file backListFile theo kiểu object
			bin=(HashMap<String,String>)obj.readObject(); // ép kiểu dữ liệu về HashMap
			
			obj.close();
			
			
			
		}catch(Exception e)
		{
			e.getStackTrace();
		}
		// kết nối với client 
		
		try {
			serverSocket=new ServerSocket(port); // tạo ra 1 socket tại cổng port (8888) - đây là cổng mà proxy lắng nghe clienrt
			System.out.println("Waiting for client on port "+serverSocket.getLocalPort()+" ...");
			this.running=true; // gán cho running=true để chương trình tiếp tục chạy
		}
		catch(Exception e) {
			System.out.println(e);
		}
		
	}
	
	// hàm lắng nghe và thực hiện lệnh được gọi trong hàm main
	public void listen() {
		while(running) { // chạy khi running =true,khi false thì nó sẽ dừng
			try {
				
				Socket socket=serverSocket.accept(); // tạo ra 1 socket để kết nối vs serverSocket(proxy) tại cổng 8888
				// nói sau hàm này
				Thread thread=new Thread(new RequestHandler(socket)); // đây là class RequestHandler.java
				thread.start(); // thread sẽ chạy hàm run() trong class RequestHandler.java
				
				receivingThread.add(thread); // thêm thread vào mảng các thread array của chương trình
			}
			catch(Exception e) {
				System.out.println(e);
			}
		}
	}
	private void saveBlackList()
	{
		try
		{
			FileOutputStream fileOut=new FileOutputStream("blackList.conf");
			ObjectOutputStream obj=new ObjectOutputStream(fileOut);
			obj.writeObject(blackList);// ghi nguyên 1 object vào với kiểu dữ liệu HashMap<String,String>
			obj.close();
			fileOut.close();
			System.out.println("******Black list is saved!!********");
		}
		catch(Exception e)
		{
			e.getStackTrace();
		}
	}
	private void closeServer() {
		System.out.println("\nClosing server.......");
		saveBlackList();
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
	
	
	
	private int showBlackList()
	{
		if(blackList.isEmpty())
		{
			System.out.println();
			System.out.println("*******THE BLACK LIST IS EMPTY********");
			System.out.println();
			return 0;
		}
		int count=1;
		System.out.println("=========BLACK LIST=======");
		for(String key :blackList.keySet())
		{
			System.out.println(count+"."+key);
			count++;
		}
		System.out.println("==========================");
		return count;
	}
	
	
	private String deleteBlackList(int order,int blist)
	{
		
		if(order<0||order>blist)
			return null;
		String temp="";
		int count=1;
		for(String key:blackList.keySet())
		{
			if(count==order)
			{
				temp=key;
				
				break;
			}
			count++;
		}
		blackList.remove(temp);
		saveBlackList();
		System.out.println("=========delete successfully==========");
		return temp;
	}
	
	public void showBin()
	{
		if(bin.isEmpty())
		{
			
			System.out.println("\n==========BIN IS EMPTY==========\n");
			return ;
		}
		System.out.println("\t\t===========BIN===========");
		int count=1;
		for(String key:bin.keySet())
		{
			System.out.println(count+"."+key);
			count++;
		}
		System.out.println("\t\t========================");
	}
	
	private boolean isSatisfiedInsertBin(String web)
	{
		if(bin.isEmpty())
			return true;
		for(String key :bin.keySet())
		{
			if(web.equals(key))
				return false;
		}
		return true;
	}
	
	public void addToBin(String web)
	{
		if(isSatisfiedInsertBin(web))
		{
			bin.put(web, web);
		}
	}
	
	public void saveBin()
	{
		try
		{
			FileOutputStream fileOut=new FileOutputStream("bin.txt");
			ObjectOutputStream obj=new ObjectOutputStream(fileOut);
			obj.writeObject(bin);// ghi nguyên 1 object vào với kiểu dữ liệu HashMap<String,String>
			obj.close();
			fileOut.close();
		}
		catch(Exception e)
		{
			e.getStackTrace();
		}
	}
	
	public String restoreBlackList(int order)
	{
		if(order<0||order>bin.size())
			return null;
		String temp="";
		int count=1;
		for(String key:bin.keySet())
		{
			if(count==order)
			{
				temp=key;
				break;
			}
			count++;
		}
		if(checkInsertBlackList(temp)) // check xem co thoa man de them vao blackList hay ko
		{
			blackList.put(temp, temp); // them lai vao black List
		}
		bin.remove(temp); // bin xoa temp khoi va save
		saveBin();
		return temp;
	}
	
	
	
	
	
	@SuppressWarnings("unused")
	private boolean checkInsertBlackList(String name)
	{
		for(String key :blackList.keySet())
		{
			if(key.equals(name)==true)
			{
				return false;
			}
		}
		return true;
	}
	
	public String deleteBin(int order)
	{
		if(order<=0||order>bin.size())
			return null;
		String temp="";
		int count=1;
		for(String key:bin.keySet())
		{
			if(order==count)
			{
				temp=key;
				break;
			}
			count++;
		}
		bin.remove(temp);
		saveBin();
		return temp;
	}
	
	
	@Override
	public void run()
	{
		Menu menu=new Menu();
		int choice;
		while(running)
		{
			choice=menu.selectMenu(menu.mainMenu());
			if(choice==1)
			{
				showBlackList();
				System.out.println();
			}
			else if (choice==2)
			{
				// insert a website into the blackList
				System.out.println("Enter the website you want to insert to the blacklist : ");
				String website;
				website=scanner.nextLine();
				boolean check=checkInsertBlackList(website);
				if(check==false)
				{
					System.out.println("!!!---YOUR WEBSITE HAD ALREADY EXIST IN THE BLACK LIST---");
					System.out.println("Do you wanna try to add another website to blackList (yes/no) : ");
					String command;
					boolean tcin=true;
					do
					{
						if(tcin)
						{
							System.out.println("Please enter your choose (yes/no)");
						}
						else
						{
							System.out.println("Please press 'yes' or 'no' to continue");
						}
						
						command=scanner.nextLine();
						tcin=(command.equals("no")||command.equals("yes"))?true:false;
					}while(tcin==false);
					if(command.equals("yes"))
					{
						do
						{
							System.out.println("Enter the website you want to block : ");
							website=scanner.nextLine();
						}while(!checkInsertBlackList(website));
						blackList.put(website, website);
						saveBlackList();
						System.out.println("******INSERT SUCCESSFULLY*******");
					}
					else
					{
						return ;
					}
					
				}
				else
				{
					blackList.put(website, website);
					saveBlackList();
					System.out.println("******INSERT SUCCESSFULLY*******");
				}
				
			}
			else if(choice==3) // ham xoa
			{
				if(blackList.size()!=0)
				{
					String luachon;
					boolean check;
					int result=-1;
					int number=showBlackList();
					System.out.println("Enter the number order of the website you want to delete (0:back) : ");
					do
					{
						luachon=scanner.nextLine();
						check=menu.isInteger(luachon);
						if(check==true)
						{
							result=Integer.parseInt(luachon);
						}
					}while(check==false||(result<0||result>blackList.size()));
					if(result==0)
					{
						System.out.println("----------EXIT--------");
						
					}
					else
					{
						String temp=deleteBlackList(result,number); // xoa khoi danh sach
						addToBin(temp); // luu vao thung rac
					}
					
				}
				else
					System.out.println("*********THE BLACK LIST IS EMPTY***********");
			}
			else if(choice ==4)
			{
				
				String command="";
				int res=-1;
				boolean check=false;
				String select="";
				do
				{
					do
					{
						showBin();
						System.out.println("enter the order number you want to restore (0 :back) : ");
						command=scanner.nextLine();
						check=menu.isInteger(command);
						if(check==true)
						{
							res=Integer.parseInt(command);
							restoreBlackList(res);
							System.out.println("==========RESTORE SUCCESSFULLY========");
						}
						else
						{
							System.out.println("!!!-------Invalid input, please try again---------!!!");
						}
						
					}while(res==-1&&check==false);
					do
					{
						System.out.println("Do you want to continue (yes/no)? : ");
						select=scanner.nextLine();
					}while(!select.equals("yes")&&!select.equals("no"));
					
					
				}while(select.equals("yes"));
				
				saveBin();
			}
			else if(choice ==5)
			{
				// clear all the black list
				String temp="";
				ArrayList<String>a=new ArrayList<String>();
				for(String key:blackList.keySet())
				{
					temp=key;
					addToBin(temp);
					a.add(temp);
				}
				for(String element:a)
				{
					blackList.remove(element);
				}
				saveBlackList();
				saveBin();
				System.out.println("*****THE BLACK LIST WAS DETELED COMPLETELY*********");
				System.out.println();
			}
			else if(choice==6)
			{
				showBin();
				String command="";
				String confirm="";
				int res=-1;
				boolean check=false;
				do
				{
					do
					{
						System.out.println("Enter the order number of the website you want to delete (0: back) : ");
						command=scanner.nextLine();
						check=menu.isInteger(command);
						if(check==false)
						{
							System.out.println("!!! your input is invalid,Please input the number!!!");
						}
						else
						{
							res=Integer.parseInt(command);
						}
					}while(check==false||res==-1);
					if(res!=0)
					{
						System.out.println("Are you sure,It will be deleted completely? (yes/no)");
						confirm=scanner.nextLine();
					}
					else
					{
						confirm="no";
					}
					
				}while(!confirm.equals("yes")&&!confirm.equals("no"));
				if(confirm.equals("yes")) {
					deleteBin(res);
					saveBin();
				}
				
			}
			else if(choice==7)
			{
				running=false;
				closeServer();
			}
			else
			{
				
			}
			
		}
		scanner.close();
	}
		
}
