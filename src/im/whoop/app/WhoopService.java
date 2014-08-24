package im.whoop.app;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.json.JSONObject;
import org.json_actual.JSONWriter;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
//import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class WhoopService extends Service {

    /**
     * Command to the service to register a client, receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client where callbacks should be sent.
     */
    static final int MSG_REGISTER_CLIENT = 1;

    /**
     * Command to the service to unregister a client, to stop receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client as previously given with MSG_REGISTER_CLIENT.
     */
    static final int MSG_UNREGISTER_CLIENT = 2;

    /**
     * Command to service to set a new value.  This can be sent to the
     * service to supply a new value, and will be sent by the service to
     * any registered clients with the new value.
     */
    static final int MSG_SET_VALUE = 3;
    
    
    static final int MSG_SEND_CHANNEL_MESSAGE = 4;
    static final int MSG_RECEIVED_CHANNEL_MESSAGE = 8;
    static final int MSG_SEND_LOGIN_POPUP = 5;
    static final int MSG_SEND_LOGINPASSWORD_POPUP = 6;
    static final int MSG_SEND_PASSWORD_POPUP = 7;
    static final int MSG_SEND_STORED_MESSAGES = 9;
    
    static final int MAX_MESSAGES_PER_CHANNEL = 5000;
    
    static final int NOTIFICATIOON_ID = 1;

	
    //private NotificationManager mNM;
    
    /** Holds last value set by a client. */
    int mValue = 0;
    
    /** Keeps track of all current registered clients. */
    ArrayList<Messenger> mClients = new ArrayList<Messenger>();

    // Unique Identification Number for the Notification.
    // We use it on Notification start, and to cancel it.
    private int NOTIFICATION = R.string.local_service_started;
    
    
    
    // my own stuff
	private String server_IP = "whoop.im";
	private int server_port = 8124;
	
	//private Thread cThread = null;
	//private Socket socket = null;
	
	private volatile boolean connected = false;
	
	private ClientRunnable client_runnable;
	//private StartSocketTask run_network_task;
	
	private ClientParser client_parser = null;
	
	private boolean service_active = false;
	public volatile boolean has_stopped = false;
	private volatile boolean do_stop = false;
	public volatile Socket socket;
	
	public PrintWriter out;
	
	public Notification notification = null;
    

    /**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
        	
        	Bundle bundle = msg.getData();
        	
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                	Log.d("b", "2");
                    mClients.add(msg.replyTo);
                   
                    try {
            	        Message stored_messages_msg = Message.obtain(null, MSG_SEND_STORED_MESSAGES);
            	        Bundle stored_messages_bundle = new Bundle(getClassLoader());
            	        if(client_parser != null)
            	        {
            	        	stored_messages_bundle.putSerializable("messages", client_parser.getStoredMessagesPerChannel());
            	        	Log.d("b", "HAPPY SMILEY");
            	        } else {
            	        	Log.d("b", "SAD SMILEY");
            	        }
            	        msg.setData(stored_messages_bundle);
            	        msg.replyTo.send(stored_messages_msg);
            	        Log.d("WhoopService", "STORED MESSAGES SENT TO CLIENT");
            	        
                    } catch (RemoteException e) {
                    	Log.d("WhoopService", "Dead Client");
                    	
                    	 mClients.remove(msg.replyTo);
                    }
                	
                    
                	if(!service_active)
                	{
                		service_active = true;
                		connect();
                	}
                	

                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    
                    if(mClients.isEmpty() && !connected)
                    {
                    	Log.d("WhoopService", "Stopping service: not connected and no clients");
                    	stopSelf();
                    }
                    break;
                case MSG_SET_VALUE:
                    //mValue = msg.arg1;
                    
                    break;
                case MSG_SEND_CHANNEL_MESSAGE:
                	HashMap<String, String> message = new HashMap<String, String>();
                	message.put("channel_name", bundle.getString("channel_name"));
                	message.put("message", bundle.getString("message"));
                	sendJSONMessage("send_message", message);
                	break;
                case MSG_SEND_LOGIN_POPUP:
                	String username = bundle.getString("username");
                	client_parser.setUsername(username);
                	// TODO: fix user login feature
                	client_parser._sendStart(false, "");
            	break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    public void sentToClients(int message_type, String title, String message, String field_value, boolean initial)
    {
    	Log.d("WhoopService", "Number of clients: " + mClients.size());
    	for (int i=mClients.size()-1; i>=0; i--) {
    		if(!sendToClient(mClients.get(i), message_type, title, message, field_value, initial))
    		{
    			// The client is dead.  Remove it from the list;
                // we are going through the list from back to front
                // so this is safe to do inside the loop.
                mClients.remove(i);
    		}
        }	
    }
    
    public boolean sendToClient(Messenger client, int message_type, String title, String message, String field_value, boolean initial)
    {
        try {
	        Message msg = Message.obtain(null, message_type);
	        Bundle bundle = new Bundle(getClassLoader());
	        bundle.putString("title", title);
	        bundle.putString("message", message);
	        bundle.putString("field_value", field_value);
	        bundle.putBoolean("initial", initial);
	        msg.setData(bundle);
	        client.send(msg);
	        Log.d("WhoopService", "MESSAGE SENT TO CLIENT");
	        
	        return true;
        } catch (RemoteException e) {
        	Log.d("WhoopService", "Dead Client");
            
            
            return false;
        }
    }
    
    
    @Override
    public void onCreate() {
       // mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        
        // Display a notification about us starting.  We put an icon in the status bar.
        //showNotification();
    }

	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        

        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
        

    }

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
        //mNM.cancel(NOTIFICATION);

    	has_stopped = true;
    	
    	Log.e("WhoopService", "stopping thread");
    	//client_runnable.do_stop();
    	Log.e("WhoopService", "thread stopped");
        
        // Tell the user we stopped.
        Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT).show();
        
        destroyNotification();
        
        try {
        	socket.shutdownInput(); 
			socket.close();
		} catch (IOException e) {
			// s
		}
    }

    @Override
    public IBinder onBind(Intent intent) {
    	return mMessenger.getBinder();
    }
    
    // Close all opened clients and stop this service
    public void finish()
    {
    	// TODO: make sure all connected clients stop themselves
    	
    	
    	// Close this service
    	stopSelf();
    	
    }

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    //private final IBinder mBinder = new LocalBinder();

    /**
     * Show a notification while this service is running.
     */
    private void createNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
       // CharSequence text = getText(R.string.local_service_started);

        // Set the icon, scrolling text and timestamp
        //Notification notification = new Notification(R.drawable.ic_launcher, text,
        //        System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        // Set the info for the views that show in the notification panel.
        //notification.setLatestEventInfo(this, getText(R.string.local_service_label),
       //                text, contentIntent);

        // Send the notification.
        //mNM.notify(NOTIFICATION, notification);
        
        notification = new NotificationCompat.Builder(getBaseContext())
        .setContentTitle("Whoop.im")
        .setContentText("LELELELELELE")
        .setSmallIcon(R.drawable.ic_launcher)
        //.setLargeIcon(R.drawable.ic_launcher)
        .setContentIntent(contentIntent)
        .build();
        
        notification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
        
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATIOON_ID, notification);
        
        // TODO FOR NEXT: DELETE NOTIFICATION AFTER SERVICE CLOSE
    }
    
    private void destroyNotification()
    {
    	NotificationManager manager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
    	manager.cancel(NOTIFICATIOON_ID);
    }
    
    private void connect()
    {
    	/*
        Notification notification = new Notification(R.drawable.ic_launcher,
                "Rolling text on statusbar", System.currentTimeMillis());

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        notification.setLatestEventInfo(this,
                "Notification title", "Notification description", contentIntent);
        notification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
        
        startForeground(1, notification);
        */
    	
    	createNotification();
	                
		Log.d("a", "1");
		client_parser = new ClientParser(this, null);
		client_parser.showLoginPopup();
		
		client_runnable = new ClientRunnable();
		Thread thr = new Thread(client_runnable);
		thr.start();
        
		//runOnUiThread(client_runnable)
		Log.d("Flow", "starting main thread");
		//run_network_task = new StartSocketTask();
		//run_network_task.execute(this);
		
		//cThread = new Thread(client_runnable);
		//cThread.start();

    }
    

    
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void sendJSONMessage(String message, HashMap<String, String> arguments)
	{	 
		Log.d("sendJSONMessage", message);
		//if(this.connected)
		//{
		//HashMap<String, String> message_map = new HashMap<String, String>();
		//message_map.put("message", message);
		//SendMessagesTask send_messages_task = new SendMessagesTask();
		//send_messages_task.execute(message_map, arguments);
		
		//SendMessagesTask send_messages_task = new SendMessagesTask();
		//if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
		//	send_messages_task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, message_map, arguments);
		//else
		//	send_messages_task.execute(message_map, arguments);
		
        //try {
            //Log.d("ClientActivity", "C: Sending command.");
           
                // where you issue the commands
                //out.println(message);
                //Log.d("ClientActivity", "C: Sent.");
        //} catch (Exception e) {
        //   Log.e("ClientActivity", "S: Error", e);
        //}
		//}
		//run_network_task.doPublishProgress("send_message", socket, message, arguments);
                
		Log.d("sendJSONMessage", "gotaskgo");
		//String message = objects[0].get("message");
		//HashMap<String, String> arguments = objects[1];
		
		Log.d("sendJSONMessage", "YES");
		//synchronized (socket) {
			try {
				PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);					
				
    			// Log.d("ClientActivity", "sendJSONMessage action: "+ message);
    			JSONWriter param_object = new JSONWriter(out).object().key("action").value(message);
	    	    //JSONWriter param_object = writer;
	    	    for (Entry<String, String> entry : arguments.entrySet()) {
	    	    	param_object.key(entry.getKey()).value(entry.getValue());
	    	    	Log.d("senJSONMessapk", "key: "+entry.getKey());
	    	    	Log.d("senJSONMessapv", "value: "+entry.getValue());
	    	    }
	    	    param_object.endObject();
	    	    out.append('\n');
	    	    
	    	    out.flush();
            } catch (Exception e) {
                Log.e("ClientActivity", "sendJSONMessage: Error", e);
            }
		//}

	}
	
	public void showLoginPopup(final String title, final String message, final String username_value, final boolean initial)
	{
		Log.d("a", "4");
		sentToClients(MSG_SEND_LOGIN_POPUP, title, message, username_value, initial);
	}
	
	public void displayMessage(String channel_name, String message)
	{
		sentToClients(MSG_RECEIVED_CHANNEL_MESSAGE, channel_name, message, "", false);
	}
	
	public class ClientRunnable implements Runnable {
		
		//private TextView chatRoomText = null;
		

		//private PrintWriter  out;
		
		public ClientRunnable() {
			// store parameter for later use
			//this.chatRoomText = chatRoomText;
			//this.currentActivity = currentActivity;
			//this.clientParser = clientParser;
			
			// show tha popup thes logins
			//client_parser.showLoginPopup();
		}
		
        public void run() {
        	 
            try {
            	// connect to awsum network of awsum
                ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                if (networkInfo != null && networkInfo.isConnected()) {
                    // fetch data, connect to server
                	if (!connected) {
                		if(!server_IP.equals(""))
                		{
        	    			try {
        						socket = new Socket(server_IP, server_port);
        						socket.setTcpNoDelay(true);
        					} catch (UnknownHostException e) {
        						// TODO Auto-generated catch block
        						e.printStackTrace();
        					} catch (IOException e) {
        						// TODO Auto-generated catch block
        						e.printStackTrace();
        					}
			                //InetAddress serverAddr = InetAddress.getByName(server_IP);
			                //Log.d("ClientActivity", "C: Connecting...");
			                
			                // out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
			                
			                connected = true;
			
			        		Log.d("Flow", "started receive thread");
			    			try {
			    				Log.d("Flow", "started receive thread:trying");
			                    //connected = true;
			                    
			    				//synchronized (socket) {
			    					// check for incoming messages
			    					BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			    				//}
			    				
			            		while( !has_stopped ) //!main_activity.has_stopped ) //!do_stop)
			            		{
			            			Log.d("LOOP", "socket-line-loop");
			            			
			            			//Log.d("LOOP", line);
			                	    // Instantiate a JSON object from the request response
			            			String line = in.readLine();
			            			
			            			if(!has_stopped)
			            			{
			    	        			Log.d("line", line);
			    	        			
			    	            	    JSONObject jsonObject = new JSONObject(line);
			    	                	
			    	            	    client_parser.parseJSONMessage(jsonObject);
			            			}
			            		}
			            		Log.d("HAS_STOPPED", has_stopped?"Yes":"No");
			
			            		socket.close();
			                    Log.d("ClientActivity", "C: Closed.");
			                } catch (Exception e) {
			                	Log.d("Flow", "started receive thread: caught");
			                    Log.e("ClientActivity", "C: Error2", e);
			                    connected = false;
			                }
			                		
			                socket.close();
			                Log.d("ClientActivity", "C: Closed.");
                		}
                	}
                	
                	
                } else {
                    // display error
                	Context context = getApplicationContext();
                	CharSequence text = getString(R.string.could_not_connect);
                	int duration = Toast.LENGTH_LONG;

                	Toast toast = Toast.makeText(context, text, duration);
                	toast.show();
                	
                	//finish();
                }
            } catch (Exception e) {
                Log.e("ClientActivity", "C: Error2", e);
                connected = false;
            }
        }
        
        public void do_stop()
        {
        	do_stop = true;
        }
    }
	/*
	public class SenderRunnable implements Runnable {
		
		//private TextView chatRoomText = null;
		
		private String message_to_sent;

		//private PrintWriter  out;
		
		public SenderRunnable(String message_to_sent) {
			// Constructor
			this.message_to_sent = message_to_sent;
		}
		
        public void run() {
        	 
            try {
                //InetAddress serverAddr = InetAddress.getByName(server_IP);
                //Log.d("ClientActivity", "C: Connecting...");
                
        		Log.d("Whoopsent", "started send thread");
				out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
				
        		if(!has_stopped && this.message_to_sent != "")
        		{
        			out.println(this.message_to_sent);
        		} else {
        			Log.d("Whoopsent", "Not able to sent message");
        		}
                
            } catch (Exception e) {
                Log.e("ClientActivity", "C: Error2", e);
                connected = false;
            }
        }
    }
	*/
	/*
	
	public class StartSocketTask extends AsyncTask<Object, Void, Boolean> {

		@Override
	    protected Boolean doInBackground(Object... objects) {
			boolean success = false;
			Log.d("Flow", "started main thread");
            try {
            	Log.d("Flow", "started main thread: trying");
                InetAddress serverAddr = InetAddress.getByName(server_IP);
                Log.d("ClientActivity", "C: Connecting...");
                socket = new Socket(serverAddr, server_port);
                socket.setTcpNoDelay(true);
                success = true;
            } catch (Exception e) {
            	Log.d("Flow", "started main thread: caught");
                Log.e("ClientActivity", "C: Error2", e);
                connected = false;
                success = false;
            }
	        
			if(success)
			{
				Log.d("Flow", "SI");
				return true;
			} else {
				Log.d("Flow", "NO");
				return false;
			}
	    }
		
		//protected void onProgressUpdate(String action, HashMap<String, String> params)
		//{
		//	
		//	
		//}
		
		protected void onPostExecute(Boolean success)
		{
			Log.d("Flow", "started main thread: onPosExecute");
			if(success)
			{
				Log.d("Flow", "started main thread: success");
				ReceiveMessagesTask receive_messages_task = new ReceiveMessagesTask();
				receive_messages_task.execute();
				
            	// show tha popup thes logins
                client_parser.showLoginPopup();
			} else {
				Log.d("Flow", "started main thread: phail");
                // display error
            	Context context = getApplicationContext();
            	CharSequence text = getString(R.string.could_not_connect);
            	int duration = Toast.LENGTH_LONG;

            	Toast toast = Toast.makeText(context, text, duration);
            	toast.show();
			}
		}
		
	}
	
	private class ReceiveMessagesTask extends AsyncTask<Object, JSONObject, Boolean> {
		
		protected Boolean doInBackground(Object... objects){
            
			//Socket socket 				= (Socket)objects[0];
			//MainActivity main_activity 	= (MainActivity)objects[1];
			
			Log.d("Flow", "started receive thread");
			try {
				Log.d("Flow", "started receive thread:trying");
                //connected = true;
                
				//synchronized (socket) {
					// check for incoming messages
					BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				//}
				
        		while( !has_stopped ) //!main_activity.has_stopped ) //!do_stop)
        		{
        			Log.d("LOOP", "socket-line-loop");
        			
        			//Log.d("LOOP", line);
            	    // Instantiate a JSON object from the request response
        			String line = in.readLine();
        			
        			if(!has_stopped)
        			{
	        			Log.d("line", line);
	        			
	            	    JSONObject jsonObject = new JSONObject(line);
	                	
	            	    publishProgress(jsonObject);
        			}
        		}
        		Log.d("HAS_STOPPED", has_stopped?"Yes":"No");

        		socket.close();
                Log.d("ClientActivity", "C: Closed.");
            } catch (Exception e) {
            	Log.d("Flow", "started receive thread: caught");
                Log.e("ClientActivity", "C: Error2", e);
                connected = false;
            }
			return true;
		}
		@Override
		protected void onProgressUpdate(JSONObject... jsonObjects)
		{
			Log.d("Flow", "started receive thread: progress");
			client_parser.parseJSONMessage(jsonObjects[0]);
		}
		
		protected void onPostExecute(Boolean return_value)
		{
			Log.d("Flow", "started receive thread: done");
			// do on exit
		}
		
		
	}
	
	private class SendMessagesTask extends AsyncTask<HashMap<String, String>, Void, Boolean> {
		
		protected Boolean doInBackground(HashMap<String, String>... objects){
			
			Log.d("sendJSONMessage", "gotaskgo");
			String message = objects[0].get("message");
			HashMap<String, String> arguments = objects[1];
			
			Log.d("sendJSONMessage", "YES");
    		//synchronized (socket) {
    			try {
    				PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);					
    				
        			// Log.d("ClientActivity", "sendJSONMessage action: "+ message);
        			JSONWriter param_object = new JSONWriter(out).object().key("action").value(message);
    	    	    //JSONWriter param_object = writer;
    	    	    for (Entry<String, String> entry : arguments.entrySet()) {
    	    	    	param_object.key(entry.getKey()).value(entry.getValue());
    	    	    	Log.d("senJSONMessapk", "key: "+entry.getKey());
    	    	    	Log.d("senJSONMessapv", "value: "+entry.getValue());
    	    	    }
    	    	    param_object.endObject();
    	    	    out.append('\n');
    	    	    
    	    	    out.flush();
                } catch (Exception e) {
                    Log.e("ClientActivity", "sendJSONMessage: Error", e);
                }
			//}

    		
    		return true;
		}
		
		public void onPostExecute(Boolean success)
		{
			Log.d("sendJSONMessage", "Send message thread ended: success = "+(success?"true":"false"));
		}
		
		
		
	}
    
    
    */
}
