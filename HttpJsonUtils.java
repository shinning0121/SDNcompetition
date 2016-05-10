package testjson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class HttpJsonUtils {
    public static void main(String[] args) throws JSONException {   
        List<NameValuePair> params = new ArrayList<NameValuePair>();  
        params.add(new BasicNameValuePair("test", "123"));   
        //要传递的参数.  
        @SuppressWarnings("deprecation")
		String posturl = "https://echo.getpostman.com/post";
        String geturl = "https://echo.getpostman.com/get?";
        //拼接路径字符串将参数包含进去   
        //System.out.println(get(url,params).toString());  
        post(posturl, params);
        System.out.println(get(geturl, params).toString());
          
    }  
	@SuppressWarnings("deprecation")
	public static JSONObject get(String url) {  
         
	        HttpClient client = new DefaultHttpClient();
	        HttpGet get = new HttpGet(url);  
	        JSONObject json = null;  
	        try {  
	            HttpResponse res = client.execute(get);  
	            if (res.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {  
	                HttpEntity entity = res.getEntity();  
	                json = new JSONObject(new JSONTokener(new InputStreamReader(entity.getContent(), HTTP.UTF_8)));  
	            }  
	        } catch (Exception e) {  
	            throw new RuntimeException(e);  
	              
	        } finally{  
	            //关闭连接 ,释放资源  
	            client.getConnectionManager().shutdown();  
	        }  
	        return json;  
	 }
	 public static JSONObject get(String baseurl, List<NameValuePair> params) {
		 JSONObject json = new JSONObject(); 
		 String url = baseurl + URLEncodedUtils.format(params, HTTP.UTF_8);  
		 json = get(url);
		 return json;
	 }
	 public static void post(String url, List<NameValuePair> params) {
		 HttpClient client = new DefaultHttpClient();
		 UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, Consts.UTF_8);
		 HttpPost httppost = new HttpPost(url);
		 httppost.setEntity(entity);
		 try {
			 HttpResponse response = client.execute(httppost);
			 if (response.getStatusLine().getStatusCode() == 200) {  
			     HttpEntity res_entity = response.getEntity();  
			  
			     InputStream in = res_entity.getContent();  
			     try {
					readResponse(in);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}  
			 } 
		 } catch (ClientProtocolException e) {
			 // TODO Auto-generated catch block
			 e.printStackTrace();
		 } catch (IOException e) {
			 // TODO Auto-generated catch block
			 e.printStackTrace();
		 }		 
	 }
	 public static void readResponse(InputStream in) throws Exception{  
		  
	    BufferedReader reader = new BufferedReader(new InputStreamReader(in));  
	    String line = null;  
	    while ((line = reader.readLine()) != null) {  
	        System.out.println(line);  
	    }  
	}
}
