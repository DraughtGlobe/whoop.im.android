package im.whoop.app;

import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.Editable;
import android.util.JsonWriter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

public class ClientParser {
	
	private WhoopService current_service = null;
	//private MainActivity current_activity = null;
	//private ClientRunnable clientRunnable = null;
	//private RunNetworkTask run_network_task = null;
	private Socket socket;
	private boolean is_started = false;
	private HashMap<String, Channel> channels = 	new HashMap<String, Channel>();
	private HashMap<String, PM> pms = 	new HashMap<String, PM>();
	
	// "closure-scope"-d
	private String current_username = "";
	
	public ClientParser(WhoopService current_service, Socket socket)
	{
		this.current_service = current_service;
		this.socket = socket;
		//this.run_network_task = run_network_task;
	}
	
	
	
	//public void setClientRunnable(ClientRunnable client_runnable)
	//{
	//	this.clientRunnable = client_runnable;
	//}
	
	
	public void showLoginPopup()
	{
		Random rand = new Random();
		int random_number = rand.nextInt(9999) + 1;
		Log.d("a", "2");
		_showLoginPopup(
				current_service.getString(R.string.login_username_popup_initial_title),
				current_service.getString(R.string.login_username_popup_initial_message),
        		"WhoopWhoop"+random_number,
        		true
			);
	}
	
	public WhoopService getWhoopService()
	{
		return current_service;
	}
	
	public String getUsername()
	{
		return current_username;
	}
	
	public void sentChannelMessage(String channel_name, String message)
	{
		_getChannel(channel_name).sendMessage(message);
	}
	
	public void parseJSONMessage(JSONObject json_message)
	{
		//if(clientRunnable == null)
		//{
		//	return;
		//}
		
		Log.d("parseJSONMessage", "hHAAI");
		
		String action;
		try {
			action = json_message.getString("action");
		
			Log.d("parseJSONMessage", "action:");
			Log.d("parseJSONMessage", action);
			
			// action handling
			if(action.equals("nick_taken"))
			{
				nickTaken();
			// incoming: field
			}else if(action.equals("invalid_field"))
			{
				invalidField(json_message.getString("field"));
			// incoming: is_root
			} else if(action.equals("started"))
			{
				started(json_message.getBoolean("is_root"));
			// incoming: 
			} else if(action.equals("logged_in_elsewhere"))
			{
				loggedInElsewhere();
			// incoming: channel_name, user
			} else if(action.equals("user_join"))
			{
				userJoin(json_message.getString("channel_name"), json_message.getJSONObject("user"));
			// incoming: channel_name, user
			} else if(action.equals("user_leave"))
			{
				userLeave(json_message.getString("channel_name"), json_message.getJSONObject("user"));
			// incoming: name, topic, is_public, users
			} else if(action.equals("channel_joined"))
			{
				channelJoined(json_message.getString("name"), json_message.getString("topic"), json_message.getBoolean("is_public"), json_message.getJSONArray("users"));
			// incoming: name
			} else if(action.equals("channel_leave"))
			{
				channelLeave(json_message.getString("name"));
			// incoming: user_name, message
			} else if(action.equals("receive_pm"))
			{
				receivePM(json_message.getString("user_name"), json_message.getString("message"));
			// incoming: user_name, message
			} else if(action.equals("receive_pm_failed"))
			{
				receivePMFailed(json_message.getString("user_name"), json_message.getString("message"));
			// incoming: channel_name, user_name, message
			} else if(action.equals("receive_message"))
			{
				receiveMessage(json_message.getString("channel_name"), json_message.getString("user_name"), json_message.getString("message"));
			// incoming: success
			} else if(action.equals("registered"))
			{
				registered(json_message.getBoolean("registered"));
			// incoming: username
			} else if(action.equals("logged_in"))
			{
				loggedIn(json_message.getString("username"));
			// incoming: channel_name, user_name, role
			} else if(action.equals("user_change_status"))
			{
				userChangeStatus(json_message.getString("channel_name"), json_message.getString("user_name"), json_message.getInt("role"));
			// incoming: user_name
			} else if(action.equals("user_regged"))
			{
				userRegged(json_message.getString("user_name"));
			// incoming: channel_name, user_name
			} else if(action.equals("muted"))
			{
				muted(json_message.getString("channel_name"), json_message.getString("user_name"));
			// incoming: channel_name, user_name
			} else if(action.equals("unmuted"))
			{
				unmuted(json_message.getString("channel_name"), json_message.getString("user_name"));
			// incoming: channel_name
			} else if(action.equals("password_required"))
			{
				passwordRequired(json_message.getString("channel_name"));
			// incoming: channel_name
			} else if(action.equals("password_invalid"))
			{
				passwordInvalid(json_message.getString("channel_name"));
			// incoming: title, text
			} else if(action.equals("text_found"))
			{
				textFound(json_message.getString("title"), json_message.getString("text"));
			// incoming: name, users_no, topic, has_password
			} else if(action.equals("channel_list_add"))
			{
				channelListAdd(json_message.getString("name"), json_message.getInt("users_no"), json_message.getString("topic"), json_message.getBoolean("has_password"));
			// incoming: name
			} else if(action.equals("channel_list_remove"))
			{
				channelListRemove(json_message.getString("name"));
			// incoming: notice, user_name (optional), channel_name (optional)
			} else if(action.equals("notice"))
			{
				notice(json_message.getString("notice"), json_message.getString("user_name"), json_message.getString("channel_name"));
			// incoming: channel_name, statuscode, param
			} else if(action.equals("channel_list_update"))
			{
				channelListUpdate(json_message.getString("channel_name"), json_message.getString("statuscode"), json_message.getBoolean("param"));
	
			// incoming: channel_name, channel_topic
			} else if(action.equals("topic_set"))
			{
				topicSet(json_message.getString("channel_name"), json_message.getString("channel_topic"));
			// incoming: channel_name
			} else if(action.equals("public_set"))
			{
				publicSet(json_message.getString("channel_name"));
			// incoming: channel_name
			} else if(action.equals("private_set"))
			{
				privateSet(json_message.getString("channel_name"));
			// incoming: channel_name, unban_list
			} else if(action.equals("unban_list_found"))
			{
				unbanListFound(json_message.getString("channel_name"), json_message.getJSONArray("unban_list"));
			// incoming: 
			} else if(action.equals("logged_in_failed"))
			{
				loggedInFailed();
			} else {
				// ha ha ignore
				Log.d("COMMAND_NOT_FOUND", action);
				
				return;
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			// possible invalid data, let's ignore :D
			
			return;
		}
	}
	
	public void sendJSONMessage(String message, HashMap<String, String> arguments)
	{
		current_service.sendJSONMessage(message, arguments);
	}

	private void nickTaken() {
		_doToast(current_service.getString(R.string.nick_taken));
		
	}

	private void invalidField(String field)
	{
		if(field.equals("username"))
		{
			_doToast(current_service.getString(R.string.invalid_field_username));
		} else if(field.equals("password"))
		{
			_doToast(current_service.getString(R.string.invalid_field_password));
		} else if(field.equals("email"))
		{
			_doToast(current_service.getString(R.string.invalid_field_email));
		} else if(field.equals("channel_password"))
		{
			_doToast(current_service.getString(R.string.invalid_field_channel_password));
		} else {
			_doToast(current_service.getString(R.string.invalid_field_unknown) + field);
		}
	}

	private void started(boolean is_root) {
		
		if(!is_started)
		{
			is_started = true;
			
			// INIT STUFF AFTER LOGIN
			_sendJoinChannel("main", "");
			
			
		}  
		
	}

	private void loggedInElsewhere() {
		if(is_started)
		{
			_doToast(current_service.getString(R.string.logged_in_elsewhere));
		}
		
	}

	private void userJoin(String channel_name, JSONObject json_user) {
		if(is_started)
		{
			Channel join_channel = _getChannel(channel_name);
			User user = new User(json_user);
			
			join_channel.addUser(user);
					
		}
		
	}

	private void userLeave(String channel_name, JSONObject user) {
		if(is_started)
		{
			Channel leave_channel = _getChannel(channel_name);
			
			try {
				leave_channel.removeUser(user.getString("name"));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}

	private void channelJoined(String name, String topic, boolean is_public, JSONArray users) {
		if(is_started)
		{
			Channel channel_join = new Channel(this, socket, name, topic, is_public, users);
			_addChannel(name, channel_join);
		}
		
	}

	private void channelLeave(String name) {
		if(is_started)
		{
			_removeChannel(name);
		}
	}

	private void receivePM(String user_name, String message) {
		if(is_started)
		{
			_getPM(user_name).receiveMessage(message);
		}
	}

	private void receivePMFailed(String user_name, String message) {
		if(is_started)
		{
			_getPM(user_name).receiveMessage(message);
		}
	}

	private void receiveMessage(String channel_name, String user_name, String message) {
		if(is_started)
		{
			_getChannel(channel_name).receiveMessage(user_name, message);
		}
	}

	private void registered(boolean success) {
		if(is_started)
		{
			_doToast(current_service.getString(R.string.channel_registration_successfull));
		}
	}

	private void loggedIn(String username) {
		if(is_started)
		{
			if(username.equals(this.current_username))
			{
				_doToast(current_service.getString(R.string.login_successfull));
			}
			
			// loop trough channels, and tell each of them this user as changed status
			for (Map.Entry<String, Channel> entry : channels.entrySet())
			{
				// System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
				entry.getValue().setStatusLogin(username);
			}
		}
	}

	private void userChangeStatus(String channel_name, String user_name, int role) {
		if(is_started)
		{
			_getChannel(channel_name).changeUserRole(user_name, role);
		}
	}

	private void userRegged(String username) {
		if(is_started)
		{
			// loop trough channels, and tell each of them this user as registered
			for (Map.Entry<String, Channel> entry : channels.entrySet())
			{
				// System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
				entry.getValue().setUserRegged(username);
			}
		}
	}

	private void muted(String channel_name, String user_name) {
		if(is_started)
		{
			_getChannel(channel_name).mute(user_name);
		}
	}

	private void unmuted(String channel_name, String user_name) {
		if(is_started)
		{
			_getChannel(channel_name).unmute(user_name);
		}
	}

	private void passwordRequired(String channel_name) {
		if(is_started)
		{

			_showPasswordPopup(
					current_service.getString(R.string.password_popup_required_title), 
					current_service.getString(R.string.password_popup_required_message) + channel_name,
					channel_name);
		}
		
	}

	private void passwordInvalid(String channel_name) {
		if(is_started)
		{
			_showPasswordPopup(
					current_service.getString(R.string.password_popup_invalid_title), 
					current_service.getString(R.string.password_popup_invalid_message) + channel_name,
					channel_name);
		}
	}

	private void textFound(String title, String text) {
		if(is_started)
		{
			// TODO Auto-generated method stub
		}
	}

	private void channelListAdd(String name, int users_no, String topic, boolean has_password) {
		
		if(is_started)
		{
			// TODO Auto-generated method stub
		}
	}

	private void channelListRemove(String name) {
		if(is_started)
		{
			// TODO Auto-generated method stub
		}
	}

	private void notice(String notice, String user_name, String channel_name) {
       
		if(notice.equals("ninja"))
       {
    	   _doToast(current_service.getString(R.string.notice_ninja));
       } else if(notice.equals("ninja_caught"))
       {
    	   _doToast(current_service.getString(R.string.notice_ninja_caught) + user_name);
       } else if(notice.equals("banned"))
       {
    	   _doToast(current_service.getString(R.string.notice_banned_can_not_join) + channel_name + ". " + current_service.getString(R.string.notice_banned_explanation));
       } else if(notice.equals("message_cap"))
       {
    	   _doToast(current_service.getString(R.string.notice_too_many_messages));
       } else if(notice.equals("register_channel_succeeded"))
       {
    	   _doToast(current_service.getString(R.string.notice_channel_succesfully_registered));
       } else if(notice.equals("register_channel_failed"))
       {
    	   _doToast(current_service.getString(R.string.notice_register_channel_failed));
       }
	}

	private void channelListUpdate(String channel_name, String statuscode, boolean param) {
		if(is_started)
		{
			// TODO Auto-generated method stub
		}
	}

	private void topicSet(String channel_name, String channel_topic) {
		if(is_started)
		{
			_getChannel(channel_name).setTopic(channel_topic);
		}
	}

	private void publicSet(String channel_name) {
		if(is_started)
		{
			_getChannel(channel_name).setPublic();
		}
	}

	private void privateSet(String channel_name) {
		if(is_started)
		{
			_getChannel(channel_name).setPrivate();
		}
	}

	private void unbanListFound(String channel_name, JSONArray unban_list) {
		if(is_started)
		{
			// TODO Auto-generated method stub
		}
	}

	private void loggedInFailed() {
        _doToast(current_service.getString(R.string.login_failed));
        _showLoginPopup(
        		current_service.getString(R.string.login_username_popup_login_failed_title),
        		current_service.getString(R.string.login_username_popup_login_failed_message),
        		current_username,
        		true
        		);
		
	}

	//public Map<String, Object> getChannels() {
	//	return channels;
	//}
	
	public void _sendJoinChannel(String name, String password)
	{
		HashMap<String, String> params = new HashMap<String, String>();
	    	params.put("name", name);
	    	params.put("password", password);
    	
    	sendJSONMessage("join_channel", params);
		
	}

	public void _addChannel(String name, Channel channel) {
		
		this.channels.put(name, channel);
	}
	
	public void _removeChannel(String name)
	{	
		this.channels.remove(name);
	}
	
	private void _doToast(String message)
	{
		
        Toast toast = Toast.makeText(current_service.getApplicationContext(), message, Toast.LENGTH_LONG);
    	toast.show();
	}
	
	private Channel _getChannel(String name)
	{
		Channel channel = channels.get(name);
		if(channel == null)
		{
			Log.d("test", "HALP!");
		}
		return channels.get(name);
	}
	
	private PM _getPM(String username)
	{
		if(pms.containsKey(username))
		{
			return pms.get(username);
		} else {
			PM pm = new PM(username);
			pms.put(username, pm);
			return pm;
		}
		
	}
	
	public void _showLoginPopup(final String title, final String message, final String username_value, final boolean initial)
	{
		//loginView = new View(currentActivity);
		
		//
		//final Channel channel = getChannel(channel_name);
		
		//FrameLayout fl = (FrameLayout) currentActivity.findViewById(android.R.id.custom);
		//fl.addView(myView, new LayoutParams(MATCH_PARENT, WRAP_CONTENT));
		
        //LayoutInflater factory = LayoutInflater.from(currentActivity);
        //final View textEntryView = factory.inflate(R.layout.login_popup_view, null);
        
        //final EditText input1 = (EditText) textEntryView.findViewById(R.id.username);
		
		/*
		final EditText input = new EditText(current_service);
		input.setText(username_value);
		
		final ClientParser client_parser = this;
		
		new AlertDialog.Builder(current_service)
	    .setTitle(title)
	    .setMessage(message)
	    .setView(input)
	    .setPositiveButton(current_service.getString(R.string.login_popup_button_ok), new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
	        	
	        	client_parser.current_username = input.getText().toString();
	        	
	        	client_parser._sendStart(false, "");
	        }
	    }).setNegativeButton(current_service.getString(R.string.login_popup_button_cancel), new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
	            // Try again
	        	//_showLoginPopup(title, message, username_value, initial);
	        	current_service.finish();
	        }
	    }).show();
		*/
		
		Log.d("a", "3");
		current_service.showLoginPopup(title, message, username_value, initial);
	}
	
	public void _showLoginPasswordPopup(final String title, final String message, final String password_value, final boolean initial)
	{
		//loginView = new View(currentActivity);
		
		//
		//final Channel channel = getChannel(channel_name);
		
		//FrameLayout fl = (FrameLayout) currentActivity.findViewById(android.R.id.custom);
		//fl.addView(myView, new LayoutParams(MATCH_PARENT, WRAP_CONTENT));
		
        //LayoutInflater factory = LayoutInflater.from(currentActivity);
        //final View textEntryView = factory.inflate(R.layout.login_popup_view, null);
        
        //final EditText input1 = (EditText) textEntryView.findViewById(R.id.username);
		
		/*
		final EditText input = new EditText(current_service);
		input.setText(password_value);
		
		final ClientParser client_parser = this;
		
		new AlertDialog.Builder(current_service)
	    .setTitle(title)
	    .setMessage(message)
	    .setView(input)
	    .setPositiveButton(current_service.getString(R.string.login_password_popup_button_ok), new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
	        	
	        	client_parser._sendStart(true, password_value);

	            //_joinChannel(channel_name, );
	        }
	    }).setNegativeButton(current_service.getString(R.string.login_password_popup_button_cancel), new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
	            // Try again
	        	//client_parser._showLoginPopup(title, message, client_parser.current_username, initial);
	        }
	    }).show();
	    */
	}
	
	private void _showPasswordPopup( String title, String message, final String channel_name )
	{
		/*
		final EditText input = new EditText(current_service);
		//final Channel channel = getChannel(channel_name);
		
		new AlertDialog.Builder(current_service)
	    .setTitle(title)
	    .setMessage(message)
	    .setView(input)
	    .setPositiveButton(current_service.getString(R.string.password_popup_button_ok), new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
	            
	            _sendJoinChannel(channel_name, input.getText().toString());
	        }
	    }).setNegativeButton(current_service.getString(R.string.password_popup_button_cancel), new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
	            // Do nothing.
	        }
	    }).show();
	    */
	}
	
	public void setUsername(String username)
	{
		this.current_username = username;
	}
	
	public void _sendStart(boolean do_login, String password)
	{
		HashMap<String, String>params = new HashMap<String, String>(); 
		if(do_login)
		{
	    	params.put("login", "1");
	    	params.put("username", current_username);
	    	params.put("password", password);
		} else { 
	    	params.put("login", "");
	    	params.put("username", current_username);
	    	params.put("password", "");
		}
    	
    	sendJSONMessage("start", params);
	}
	
	public HashMap<String, String[]> getStoredMessagesPerChannel()
	{
		HashMap<String, String[]> channel_messages = new HashMap<String, String[]>();
		
		// loop trough channels, and tell each of them this user as changed status
		for (Map.Entry<String, Channel> entry : channels.entrySet())
		{
			channel_messages.put(entry.getValue().getName(), entry.getValue().getMessages());
		}
		
		return channel_messages;
	}
}
