package im.whoop.app;

public class PM {
	
	//private User user;
	
	//public PM(User user)
	//{
	//	this.user = user;
	//}
	
	private String username;
	
	public PM(String username)
	{
		this.username = username;
	}
	
	public void receiveMessage(String message)
	{
		
	}
	
	public void receiveMessageFailed(String message)
	{
		
	}
	
	public void sendMessage(String message)
	{
		
		
		// receive own messages
		receiveMessage(message);
	}
}
