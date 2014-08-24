package im.whoop.app;

import java.io.BufferedReader;
import java.io.BufferedWriter;
//import java.io.IOException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json_actual.JSONWriter;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
//import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.method.ScrollingMovementMethod;
//import android.util.JsonReader;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	public final static String CHAT_INPUT = "whoop.im.chat.input";
	
	private EditText input_field = null;
	private TextView chatroom_input = null;
	
	private WhoopService bound_service;
	
	private boolean service_is_bound = false;
	
	private Intent whoop_service_intent = null;
	
	public Messenger service_messenger = null; 	// Messenger for communicating with service.
	
	// closure scope
	public final MainActivity current_activity = this;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        
        //Toast toast = Toast.makeText(getApplicationContext(), "this is my toast!", Toast.LENGTH_LONG);
    	//toast.show();
    	
    	//this.clientParser = new ClientParser(this);
    	
    	// get text fields
    	input_field = (EditText) findViewById(R.id.edit_message);
    	chatroom_input = (TextView)findViewById(R.id.room);
    	
    	doBindService();
    	
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart()
    {
    	super.onStart();
        
    }
    
    @Override
    protected void onStop()
    {
    	super.onStop();
    	
    }
    
    public void shutdown()
    {
    	doUnbindService();
        finish();
        System.exit(0);
    }
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    /** Called when the user clicks the Send button */
    public void sendMessage(View view) {
        // Do something in response to button
    	//Intent intent = new Intent(this, DisplayMessageActivity.class);
    	//EditText editText = (EditText) findViewById(R.id.edit_message);
    	//String message = editText.getText().toString();
    	//intent.putExtra(CHAT_INPUT, message);
    	//startActivity(intent);
    	
    	Log.d("test", "before1");
    	
    	// check if this works
    	input_field = (EditText) findViewById(R.id.edit_message);
    	
    	//EditText editText = (EditText) findViewById(R.id.edit_message);
    	String message = input_field.getText().toString();
    	
    	Log.d("test", "after1");
    	Log.d("test", message);
    	
    	//TextView chatRoomText = (TextView)findViewById(R.id.room);
    	//chatroom_input.append('\n'+message);
    	sentChannelMessage("main", message);
    	
    	input_field.setText("");
    	
    	//scrollDownChatroomInput();
    	
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_close:
                shutdown();
                return true;
            case R.id.action_settings:
                //openSettings();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    
    public void appendMessage(String channel_name, String message) {
    	
    	// check if this works
    	chatroom_input = (TextView)findViewById(R.id.room);
    	
    	// debug: append when different channel
    	if(!channel_name.equals("main"))
    	{
    		message = "OC|"+channel_name+"| "+ message;
    	}
    	
    	chatroom_input.append('\n'+message);
    	
    	scrollDownChatroomInput();
    	
    }
    
    private void scrollDownChatroomInput()
    {
    	// do the scroll thing
    	chatroom_input.setMovementMethod(new ScrollingMovementMethod());
        final int scrollAmount = chatroom_input.getLayout().getLineTop(chatroom_input.getLineCount())
                -chatroom_input.getHeight();
        // if there is no need to scroll, scrollAmount will be <=0
        if(scrollAmount>0)
        	chatroom_input.scrollTo(0, scrollAmount);
        else
        	chatroom_input.scrollTo(0,0);
    }
    

    /**
     * Service binding
     */
    
    
    // Handler of incoming messages from service.
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
        	
        	Bundle bundle = msg.getData();
        	
        	Log.d("WHAT", "6");
        	
            switch (msg.what) {
                case WhoopService.MSG_SEND_LOGIN_POPUP:
                	
                	
                	Log.d("a", "5");
                	
            		final EditText input = new EditText(current_activity);
            		input.setText(bundle.getString("field_value"));
            		
            		//final ClientParser client_parser = this;
            		
            		new AlertDialog.Builder(current_activity)
            	    .setTitle(bundle.getString("title"))
            	    .setMessage(bundle.getString("message"))
            	    .setView(input)
            	    .setPositiveButton(current_activity.getString(R.string.login_password_popup_button_ok), new DialogInterface.OnClickListener() {
            	        public void onClick(DialogInterface dialog, int whichButton) {
            	        	
            	        	// We want to monitor the service for as long as we are
            	    	    // connected to it.
            	    	    try {
            	    	        Message msg = Message.obtain(null, WhoopService.MSG_SEND_LOGIN_POPUP);
            	    	        msg.replyTo = mMessenger;
            	    	        Bundle bundle = new Bundle(getClassLoader());
            	    	        bundle.putString("username", input.getText().toString());
            	    	        msg.setData(bundle);
            	    	        service_messenger.send(msg);
            	    	    } catch (RemoteException e) {
            	    	        // In this case the service has crashed before we could even
            	    	        // do anything with it; we can count on soon being
            	    	        // disconnected (and then reconnected if it can be restarted)
            	    	        // so there is no need to do anything here.
            	    	    }
            	        	
            	        	//client_parser._sendStart(true, password_value);

            	            //_joinChannel(channel_name, );
            	        }
            	    }).setNegativeButton(current_activity.getString(R.string.login_password_popup_button_cancel), new DialogInterface.OnClickListener() {
            	        public void onClick(DialogInterface dialog, int whichButton) {
            	            // Try again
            	        	//client_parser._showLoginPopup(title, message, client_parser.current_username, initial);
            	        }
            	    }).show();
            	break;
                case WhoopService.MSG_RECEIVED_CHANNEL_MESSAGE:
                	String channel_name = bundle.getString("title");
                	String message = bundle.getString("message");
                	
    		    	chatroom_input = (TextView)findViewById(R.id.room);
    		    	
    		    	// debug: append when different channel
    		    	if(!channel_name.equals("main"))
    		    	{
    		    		message = "OC|"+channel_name+"| "+ message;
    		    	}
    		    	
    		    	chatroom_input.append('\n'+message);
                	
            	break;
                case WhoopService.MSG_SEND_LOGINPASSWORD_POPUP:
                	
            	break;
                case WhoopService.MSG_SEND_PASSWORD_POPUP:
                	
            	break;
                case WhoopService.MSG_SEND_STORED_MESSAGES:
                	
                	Log.d("CLIENT", "A");
                	
                	if(bundle.containsKey("messages"))
                	{
                		Log.d("CLIENT", "B");
                		
                		@SuppressWarnings("unchecked")
                		HashMap<String, String[]> messages = (HashMap<String, String[]>)bundle.getSerializable("messages");
                		// loop trough channels, and tell each of them this user as changed status
                		for (Map.Entry<String, String[]> entry : messages.entrySet())
                		{
                			Log.d("CLIENT", "C");
                			
                			if(entry.getKey().equals("main"))
                			{
                				Log.d("CLIENT", "D");
                				
                				String[] channel_messages = entry.getValue();
                				for(int i = 0; i < channel_messages.length; i++)
                				{
                					Log.d("CLIENT", "E");
                					appendMessage("main", channel_messages[i]);
                				}
                				
                			}
                		}
                	} else {
                		Log.d("CLIENT" ,"keys not found: size: "+ bundle.keySet().size());
                		for (String s : bundle.keySet()) {
                			Log.d("CLIENT_KEY", s);
                		}
                	}
                	
                	
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
    
    
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
        	// MainActivity.this.bound_service = ((WhoopService.LocalBinder)service).getService();

            // Tell the user about this for our demo.
            Toast.makeText(MainActivity.this.getApplicationContext(), R.string.local_service_connected,
                    Toast.LENGTH_SHORT).show();
            
            
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            service_messenger = new Messenger(service);

            // notify the server of us
            try {
            	Log.d("b", "1");
                Message msg = Message.obtain(null, WhoopService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                service_messenger.send(msg);
            } catch (RemoteException e) {
            	Log.d("b", "1nee");
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
            }
            
            
            // As part of the sample, tell the user what happened.
            Toast.makeText(MainActivity.this.getApplicationContext(), R.string.local_service_connected,
                    Toast.LENGTH_SHORT).show();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
        	MainActivity.this.bound_service = null;
            Toast.makeText(MainActivity.this.getApplicationContext(), R.string.local_service_disconnected,
                    Toast.LENGTH_SHORT).show();
        }
    };


    
    private void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        //bindService(new Intent(MainActivity.this.getApplicationContext(), 
      //  		WhoopService.class), mConnection, Context.BIND_AUTO_CREATE);
    	
    	//whoop_service_intent = new Intent(getBaseContext(), WhoopService.class);
    	//getBaseContext().startService(whoop_service_intent);
    	Intent intent = new Intent(this, WhoopService.class);
    	startService(intent);
    	
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        service_is_bound = true;
    }

    private void doUnbindService() {
        if (service_is_bound) {
        	
            // If we have received the service, and hence registered with
            // it, then now is the time to unregister.
            if (service_messenger != null) {
                try {
                    Message msg = Message.obtain(null, WhoopService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    service_messenger.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service
                    // has crashed.
                }
            }

            // Detach our existing connection.
            unbindService(mConnection);
            service_is_bound = false;
    	
            // Detach our existing connection.
            //unbindService(mConnection);
        	//getBaseContext().stopService(whoop_service_intent); 
        	//service_is_bound = false;
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }
    
    /**
     * End service binding
     */
    
    // Service communication
    public void sentChannelMessage(String channel_name, String message)
    {
    	// We want to monitor the service for as long as we are
	    // connected to it.
	    try {
	        Message msg = Message.obtain(null, WhoopService.MSG_SEND_CHANNEL_MESSAGE);
	        msg.replyTo = mMessenger;
	        Bundle bundle = new Bundle(getClassLoader());
	        bundle.putString("channel_name", channel_name);
	        bundle.putString("message", message);
	        msg.setData(bundle);
	        service_messenger.send(msg);
	    } catch (RemoteException e) {
	        // In this case the service has crashed before we could even
	        // do anything with it; we can count on soon being
	        // disconnected (and then reconnected if it can be restarted)
	        // so there is no need to do anything here.
	    }
	
    }    
	/**
	 * 
	 * 
	 * 
	 * 
	 * 
	 * @author peter
	 *
	 */
	/*
    private class RunNetworkTask extends AsyncTask {

    	MainActivity main_activity;
    	ClientParser client_parser;
    	
    	
    	@Override
        protected Object doInBackground(Object... objects) {
    		main_activity = (MainActivity) objects[0];
            client_parser = (ClientParser)objects[1];
    		
            try {
                InetAddress serverAddr = InetAddress.getByName(server_IP);
                Log.d("ClientActivity", "C: Connecting...");
                
                Socket socket = new Socket(serverAddr, server_port);
                socket.setTcpNoDelay(true);
                
            	// show tha popup thes logins
                client_parser.showLoginPopup();
                
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                
                //connected = true;
                
                
                // check for incoming messages
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                
        		while( !main_activity.has_stopped ) //!do_stop)
        		{
        			Log.d("LOOP", "socket-line-loop");
        			
        			//Log.d("LOOP", line);
            	    // Instantiate a JSON object from the request response
        			String line = in.readLine();
        			
        			//Log.d("line", line);
        			
            	    JSONObject jsonObject = new JSONObject(line);
                	
                	client_parser.parseJSONMessage(jsonObject);
        		}

        		socket.close();
                Log.d("ClientActivity", "C: Closed.");
            } catch (Exception e) {
                Log.e("ClientActivity", "C: Error2", e);
                connected = false;
            }
			return Boolean.valueOf(true);
            
            
        }
		
		protected void onProgressUpdate(String action, HashMap<String, String> params)
    	{
			if(action.equals("appendMessage"))
			{
		    	// check if this works
		    	chatroom_input = (TextView)findViewById(R.id.room);
		    	
		    	
		    	String channel_name = params.get("channel_name");
		    	String message = params.get("message");
		    	
		    	// debug: append when different channel
		    	if(!channel_name.equals("main"))
		    	{
		    		message = "OC|"+channel_name+"| "+ message;
		    	}
		    	
		    	chatroom_input.append('\n'+message);
			} else if(action.equals("_showPasswordPopup"))
			{
				final EditText input = new EditText(main_activity);
				//final Channel channel = getChannel(channel_name);
				final String title = params.get("title");
				final String message = params.get("message");
				final String channel_name = params.get("channel_name");
				
				new AlertDialog.Builder(main_activity)
			    .setTitle(title)
			    .setMessage(message)
			    .setView(input)
			    .setPositiveButton(main_activity.getString(R.string.password_popup_button_ok), new DialogInterface.OnClickListener() {
			        public void onClick(DialogInterface dialog, int whichButton) {
			            
			            _sendJoinChannel(channel_name, input.getText().toString());
			        }
			    }).setNegativeButton(main_activity.getString(R.string.password_popup_button_cancel), new DialogInterface.OnClickListener() {
			        public void onClick(DialogInterface dialog, int whichButton) {
			            // Do nothing.
			        }
			    }).show();
			}
			
			
    	}
    }
	
		*/
    /*
	public class ClientRunnable implements Runnable {
    	 
		private volatile boolean do_stop = false;
		private ClientParser clientParser = null;
		private TextView chatRoomText = null;
		private MainActivity currentActivity = null;
		private boolean connected = false;
		private Socket socket;
		//private PrintWriter  out;
		
		public ClientRunnable(TextView chatRoomText, MainActivity currentActivity, ClientParser clientParser) {
			// store parameter for later use
			this.chatRoomText = chatRoomText;
			this.currentActivity = currentActivity;
			this.clientParser = clientParser;
			
			clientParser.setClientRunnable(this);
			
			// show tha popup thes logins
			this.clientParser.showLoginPopup();
		}
		
        public void run() {
        	 
            try {
                InetAddress serverAddr = InetAddress.getByName(server_IP);
                Log.d("ClientActivity", "C: Connecting...");
                this.socket = new Socket(serverAddr, server_port);
                this.socket.setTcpNoDelay(true);
                
                // out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                
                connected = true;
                
                
                // check for incoming messages
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                
                //String json_string;
                //StringBuilder sb;
                //while (!do_stop) {
                	
                	//sb = new StringBuilder();

                	//String line = null;
                	//while ((line = in.readLine()) != null)
                	//{
                		
                		
                		while(!do_stop)
                		{
                			Log.d("LOOP", "socket-line-loop");
                			
                			//Log.d("LOOP", line);
	                	    // Instantiate a JSON object from the request response
                			String line = in.readLine();
                			
                			//Log.d("line", line);
                			
	                	    JSONObject jsonObject = new JSONObject(line);
	                    	
	                    	clientParser.parseJSONMessage(jsonObject);
                		}
                	    //sb.append(line + "\n");
                		
                		//if(do_stop)
                		//{
                		//	
                		//}
                	//}
                	//json_string = sb.toString();


                	
                //}
                socket.close();
                Log.d("ClientActivity", "C: Closed.");
            } catch (Exception e) {
                Log.e("ClientActivity", "C: Error2", e);
                connected = false;
            }
        }
        
    	/*
        private void sendMessage(String message)
        {
            try {
                //Log.d("ClientActivity", "C: Sending command.");
               
                    // where you issue the commands
                    out.println(message);
                    //Log.d("ClientActivity", "C: Sent.");
            } catch (Exception e) {
                Log.e("ClientActivity", "S: Error", e);
            }
        }
        * /
        
        public void do_stop()
        {
        	do_stop = true;
        }
    }
*/
    
}
