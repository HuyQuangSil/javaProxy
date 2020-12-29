import java.io.*;
import java.util.Scanner;
public class Menu {
	
	
	Scanner scanner=new Scanner(System.in);
	Menu()
	{
		// default constructor
	}
	
	public boolean isInteger(String in) // ham kiem tra xem du lieu nguoi dung nhap vao co la so nguyen hay khong
	{
		boolean tcin=false;
		try
		{
			Integer.parseInt(in);
			tcin=true;
		}
		catch(Exception e)
		{
			e.getStackTrace();
		}
		return tcin;
	}
	
	public int selectMenu(int soluachon)
	{
		String luachon;
		int res=-1;
		boolean tcin;
		do
		{
			System.out.println("--> Enter your choice : ");
			luachon=scanner.nextLine();
			tcin=isInteger(luachon);
			if(tcin==true)
			{
				res=Integer.parseInt(luachon);
			}
			
		}while(tcin==false||(res<0||res>soluachon));
		
		return res;
	}
	
	public int mainMenu()
	{
		System.out.println("==============MENU===========");
		System.out.println("1.Show the black list");
		System.out.println("2.Insert a website to the black list");
		System.out.println("3.Delete a website from the black list");
		System.out.println("4.Restore a web blocked from the bin");
		System.out.println("5.clear the black list");
		System.out.println("6.Delete the Bin");
		System.out.println("7.close the server");
		System.out.println("=============================");
		return 7;
	}
	

}
