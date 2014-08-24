package im.whoop.app;

import org.json.JSONException;
import org.json.JSONObject;

public class User {
	
	private String name = "";
	private int role = 0;
	public User(JSONObject jsonObject)
	{
		try {
			name = jsonObject.getString("name");
			role = jsonObject.getInt("role");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public String getName()
	{
		return this.name;
	}
	
	public int getRole()
	{
		return this.role;
	}
	
	
}
