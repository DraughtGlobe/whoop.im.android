package im.whoop.app;

//import im.whoop.app.MainActivity.ClientRunnable;

import java.net.Socket;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class Channel {
	
	private String name;
	private String topic;
	private boolean is_public;
	
	private ClientParser client_parser;
	//private ClientRunnable clientRunnable = null;
	private Socket socket;
	
	private String[] messages = new String[WhoopService.MAX_MESSAGES_PER_CHANNEL];
	private int message_overflow_iterator = 0;
	private int message_iterator = 0;
	
	private HashMap<String, User> users = 	new HashMap<String, User>();
	
	public Channel(ClientParser client_parser, Socket socket, String name, String topic, boolean is_public, JSONArray json_users)
	{
		this.name = name;
		this.topic = topic;
		this.is_public = is_public;
		this.client_parser = client_parser;
		//this.clientRunnable = clientRunnable;
		this.socket = socket;
		
	    try {
			// Pulling items from the array
			for (int i=0; i < json_users.length(); i++)
			{
				User user = new User(json_users.getJSONObject(i));
				users.put(user.getName(), user);
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public String getName()
	{
		return name;
	}
	
	public void addUser(User user)
	{
		users.put(user.getName(), user);
	}
	
	public void removeUser(String user_name)
	{
		users.remove(user_name);
	}
	
	public void receiveMessage(String user_name, String message)
	{
		String return_value = user_name+": "+message;
		
		this.storeMessage(return_value);
		
		this.client_parser.getWhoopService().displayMessage(this.name, return_value);
	}
	
	public void sendMessage(String message)
	{
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("channel_name", 	this.name);
		params.put("message",		message);

		this.storeMessage(this.client_parser.getUsername() + ": " + message);
		
		this.client_parser.getWhoopService().sendJSONMessage("send_message", params);
	}
	
	public void printSystemMessage(String message)
	{
		// TODO
	}
	
	// wat doet dit?
	public void setStatusLogin(String username)
	{
		// TODO
	}
	
	public void changeUserRole(String username, int role)
	{
		// TODO
	}
	
	public void setUserRegged(String username)
	{
		// TODO
	}
	
	public void mute(String username)
	{
		// TODO
	}
	
	public void unmute(String username)
	{
		
	}
	
	public void setTopic(String topic)
	{
		this.topic = topic;
		if(topic.equals(""))
		{
			printSystemMessage("Channel topic has been unset");
		} else
		{
			printSystemMessage("Channel topic has been set to \""+topic+"\"");
		}
	}
	
	public void setPublic()
	{
		is_public = true;
		
		printSystemMessage("Channel set public");
	}
	
	public void setPrivate()
	{
		is_public = false;
		
		printSystemMessage("Channel set private");
	}
	
	private void storeMessage(String message)
	{
		Log.d("Store shit", message);
		// if max messages exceeded, start overwriting old values
		if(this.message_iterator == WhoopService.MAX_MESSAGES_PER_CHANNEL)
		{
			this.messages[this.message_overflow_iterator] = message;
			
			if(this.message_overflow_iterator == WhoopService.MAX_MESSAGES_PER_CHANNEL)
			{
				this.message_overflow_iterator = 0;
			} else {
				this.message_overflow_iterator++;
			}
		} else {
			// fill the not maxxed out array
			this.messages[this.message_iterator] = message;
			this.message_iterator++;
		}
	}
	
	/**
	 * Return the array of messages in the correct order
	 */
	public String[] getMessages()
	{
		String[] messages_ordered = new String[this.message_iterator];
		Log.d("getMessages:A", ""+message_iterator);
		Log.d("getMessages:B", ""+messages_ordered.length);
		for(int i = 0; i < this.message_iterator; i++)
		{
			int iterator = i+this.message_overflow_iterator;
			if(iterator > WhoopService.MAX_MESSAGES_PER_CHANNEL)
			{
				iterator -= WhoopService.MAX_MESSAGES_PER_CHANNEL;
			}
			messages_ordered[i] = this.messages[iterator];
		}
		
		return messages_ordered;
	}
	
	
	
}
