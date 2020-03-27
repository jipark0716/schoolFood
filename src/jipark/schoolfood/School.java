package jipark.schoolfood;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class School {
	protected String json = null;
	protected Map<String,String> menu = null;
	public School(String json) {
		this.json = json;
		this.menu = new HashMap<String, String>();
	}
	protected int getScode() {
		Matcher matcher = Pattern.compile("schulCrseScCode\":\"(?<scode>[0-9])\"").matcher(json);
		matcher.find();
		return Integer.parseInt(matcher.group("scode"));
	}
	protected String getOrgCode() {
		Matcher matcher = Pattern.compile("orgCode\":\"(?<orgcode>[A-Z]{1}[0-9]{8,12})\"").matcher(json);
		matcher.find();
		return matcher.group("orgcode");
	}
	protected void cacheMenu(int y,int m) throws Exception{
		String year = Integer.toString(y);
		String month = lpad(Integer.toString(m),2,'0');
		RequestBody reqbody = new FormBody.Builder()
	        .add("schulCode",this.getOrgCode())
	        .add("ay",year)
	        .add("mm",month)
	        .add("schulKndScCode",Integer.toString(this.getScode()))
	        .add("schulCrseScCode",Integer.toString(this.getScode()))
	        .build();
		OkHttpClient client = new OkHttpClient();
		Request req = new Request
			.Builder()
			.post(reqbody)
			.url("https://stu.sen.go.kr/sts_sci_md00_001.do")
			.header("Content-Type","application/x-www-form-urlencoded")
			.header("Content-Length","69")
			.header("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.149 Safari/537.36")
			.build();
		Response response = client.newCall(req).execute();
		String menu = response.body().string();
		menu = menu.replaceAll("\t","");
		menu = menu.replaceAll("(\\r\\n|\\r|\\n|\\n\\r)","");
		menu = menu.replaceAll(" ","");
		Matcher matcher = Pattern.compile("<tbody><tr><td><div>(?<menu>.*)<\\/div><\\/td><\\/tr><\\/tbody>").matcher(menu);
		matcher.find();
		menu = matcher.group("menu");
		menu = menu.replaceAll("</tr><tr>","");
		String[] menus = menu.split("</div></td><td><div>");
		for(int i = 0 ; i < menus.length ; i ++) {
			try {
				this.menu.put(year+month+lpad(Integer.toString(i+1),2,'0'),menus[i].substring(6));
			} catch (Exception e) {
				this.menu.put(year+month+lpad(Integer.toString(i+1),2,'0'),"");
			}
		}
	}
	public String[] getMenu(int y,int m,int d){
		String year = Integer.toString(y);
		String month = lpad(Integer.toString(m),2,'0');
		String date = lpad(Integer.toString(d),2,'0');
		String menu = this.menu.get(year+month+date);
		if(menu == null) {
			try {
				this.cacheMenu(y, m);
			} catch (Exception e) {
				e.printStackTrace();
			}
			menu = this.menu.get(year+month+date);
		}else if(menu.equals("")) {
			return null;
		}
		return menu.split("<br/>.");
	}
	public static String lpad(String str,int length,char con) {
		while(str.length() < length) {
			str = con+str;
		}
		return str;
	}
	public static School create(String sname) throws Exception{
		OkHttpClient client = new OkHttpClient();
		Response response = client.newCall(new Request
			.Builder()
			.url("https://par.sen.go.kr/spr_ccm_cm01_100.do?kraOrgNm="+sname)
			.build()
		).execute();
		return new School(response.body().string());
	}
}
