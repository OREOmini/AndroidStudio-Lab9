package com.example.wangyunwen.ex09_v3;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String urlString = "http://ws.webxml.com.cn/WebServices/WeatherWS.asmx/getWeather";
//    private final List<Map<String, String>> data = new ArrayList<>();
    Button search;
    EditText cityName;
    TextView temNum, humidity, airCon, wind, name, date, range;
    LinearLayout view;
    private ArrayList<String> list = new ArrayList<>();

    private void getCom() {
        search = (Button) findViewById(R.id.search);
        cityName = (EditText) findViewById(R.id.cityName);
        temNum = (TextView) findViewById(R.id.temNum);
        humidity = (TextView) findViewById(R.id.humidity);
        airCon = (TextView) findViewById(R.id.airCon);
        wind = (TextView) findViewById(R.id.wind);
        name = (TextView) findViewById(R.id.name);
        date = (TextView) findViewById(R.id.date);
        range = (TextView) findViewById(R.id.range);
        view = (LinearLayout) findViewById(R.id.view);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getCom();
        view.setVisibility(View.INVISIBLE);

        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(networkTask).start();
            }

            Runnable networkTask = new Runnable() {
                @Override
                public void run() {
                    ConnectivityManager connMgr = (ConnectivityManager)
                            getSystemService(MainActivity.this.CONNECTIVITY_SERVICE);
                    NetworkInfo networkinfo = connMgr.getActiveNetworkInfo();
                    if (networkinfo != null && networkinfo.isConnected()) {
                        HttpURLConnection connection = null;
                        try {
                            // 使用 HttpURLConnection 新建一个 http 连接,新建一个 URL
                            // 对象,打开连接即可, 并且设置访问方法以及时间设置:
                            Log.i("key", "Begin the connection");
                            URL url = new URL(urlString);
                            connection = (HttpURLConnection) url.openConnection();
                            connection.setRequestMethod("POST");
                            connection.setReadTimeout(8000);
                            connection.setConnectTimeout(8000);

                            // 将我们需要请求的字段以流的形式写入 connection 之中,这一步相当于将需要的参
                            // 数提交到网络连接,并且请求网络数据(类似于 html 中的表单操作,将 post 数据提交到 服务器)
                            DataOutputStream out = new DataOutputStream(connection.getOutputStream());
                            String request = cityName.getText().toString();
                            request = URLEncoder.encode(request, "utf-8");
                            out.writeBytes("theCityCode=" + request + "&theUserID=");

                            // 网页获取 xml 转化为字符串:
                            InputStream in = connection.getInputStream();
                            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                            StringBuilder response = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                Log.i("line", line);
                                response.append(line);
                            }
                            // 处理xml字符串
                            parseXML(response.toString());

                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        catch (XmlPullParserException e) {
                            e.printStackTrace();
                        }
                        // 关闭 connection
                        finally {
                            if (connection != null)
                                connection.disconnect();
                        }
                    } else {
                        Message message = new Message();
                        Bundle bundle = new Bundle();
                        bundle.putString("status", "noNet");
                        message.setData(bundle);
                        handleErrors.sendMessage(message);
                    }
                }
            };
            private void parseXML(String xml) throws XmlPullParserException, IOException{
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                XmlPullParser parser = factory.newPullParser();
                parser.setInput(new StringReader(xml));

                int eventType = parser.getEventType();
                list.clear();
                while(eventType != XmlPullParser.END_DOCUMENT) {
                    switch (eventType) {
                        case XmlPullParser.START_TAG:
                            if("string".equals(parser.getName())) {
                                String str = parser.nextText();
                                list.add(str);
                                Log.i("-------", str);
                            }
                            break;
                        case XmlPullParser.END_TAG:
                            break;
                        default:
                            break;
                    }
                    eventType = parser.next();
                }
                if(list.size() > 10) parseList(list);
                else parseError(list);
            }

            private void parseList(ArrayList<String> strList) {
                Bundle data = new Bundle();

                // top
                String name = strList.get(0);
                data.putString("name", name);
                String date = strList.get(3);
                data.putString("date", date);
                // 方框内
                // 今日天气实况：气温：6℃；风向/风力：西北风 1级；湿度：62%
                String temp = strList.get(4);
                String frameDetails = temp.substring(7, temp.length());
                String range = strList.get(8);
                data.putString("range", range);
                String[] details = frameDetails.split("；");
                Log.i("framework", frameDetails);
                ArrayList<String> d = new ArrayList<>();
                for (int i = 0; i < details.length; i++) {
                    String[] sen = details[i].split("：");
                    Log.i("details", details[i]);
                    d.add(sen[1]);
                    Log.i(sen[0], sen[1]);
                }
                data.putString("temNum", d.get(0));
                data.putString("wind", d.get(1));
                data.putString("humidity", details[2]);

                temp = strList.get(5);
                String[] a = temp.split("。");
                String aircon = a[1];
                Log.i("aircon", aircon);
                data.putString("airCon", aircon);

                // list
                // 紫外线指数：最弱，辐射弱，涂擦SPF8-12防晒护肤品。
                // 感冒指数：较易发，天较凉，增加衣服，注意防护。
                // 穿衣指数：较冷，建议着厚外套加毛衣等服装。
                // 洗车指数：不宜，有雨，雨水和泥水会弄脏爱车。
                // 运动指数：较不宜，有降水，推荐您在室内进行休闲运动。
                // 空气污染指数：良，气象条件有利于空气污染物扩散。
                temp = strList.get(6);
                String[] keys = temp.split("。");
                for (int i = 0; i < keys.length; i++) {
                    String[] vals = keys[i].split("：");
                    Log.i(vals[0], vals[1]);
                    data.putString(vals[0], vals[1]);
                }

                Message message = new Message();
                message.setData(data);
                handler.sendMessage(message);
            }

            Handler handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    Bundle bundleData = msg.getData();
                    view.setVisibility(View.VISIBLE);

                    name.setText(bundleData.getString("name"));
                    date.setText(bundleData.getString("date"));
                    temNum.setText(bundleData.getString("temNum"));
                    range.setText(bundleData.getString("range"));
                    humidity.setText(bundleData.getString("humidity"));
                    airCon.setText(bundleData.getString("airCon"));
                    wind.setText(bundleData.getString("wind"));

                    List<Map<String, String>> data = new ArrayList<>();
                    Map<String, String> temp = new LinkedHashMap<>();
                    temp.put("key", "紫外线指数");
                    temp.put("value", bundleData.getString("紫外线指数"));
                    data.add(temp);
                    Map<String, String> temp1 = new LinkedHashMap<>();
                    temp1.put("key", "感冒指数");
                    temp1.put("value", bundleData.getString("感冒指数"));
                    data.add(temp1);
                    Map<String, String> temp2 = new LinkedHashMap<>();
                    temp2.put("key", "穿衣指数");
                    temp2.put("value", bundleData.getString("穿衣指数"));
                    data.add(temp2);
                    Map<String, String> temp3 = new LinkedHashMap<>();
                    temp3.put("key", "洗车指数");
                    temp3.put("value", bundleData.getString("洗车指数"));
                    data.add(temp3);
                    Map<String, String> temp4 = new LinkedHashMap<>();
                    temp4.put("key", "运动指数");
                    temp4.put("value", bundleData.getString("运动指数"));
                    data.add(temp4);
                    Map<String, String> temp5 = new LinkedHashMap<>();
                    temp5.put("key", "空气污染指数");
                    temp5.put("value", bundleData.getString("空气污染指数"));
                    data.add(temp5);

                    ListView list = (ListView) findViewById(R.id.list);
                    SimpleAdapter simpleAdapter = new SimpleAdapter(MainActivity.this, data, R.layout.item,
                            new String[] {"key", "value"}, new int[] {R.id.key, R.id.value});
                    list.setAdapter(simpleAdapter);
                }
            };

            private void parseError(ArrayList<String> strList) {
                // 查询结果为空。http://www.webxml.com.cn/
                Message message = new Message();
                Bundle bundle = new Bundle();
                String status = "";
                Log.i("status", strList.get(0).substring(9, 11));
                if (strList.get(0).substring(0, 6).equals("查询结果为空")) {
                    status = "null";
                }
                else if(strList.get(0).substring(9, 11).equals("不能")) {
                    status = "highSpeed";
                }
                else if (strList.get(0).substring(9, 11).equals("24")) {
                    status = "24";
                }
                bundle.putString("status", status);
                message.setData(bundle);
                handleErrors.sendMessage(message);
            }

            Handler handleErrors = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    // view.setVisibility(View.INVISIBLE);
                    super.handleMessage(msg);
                    Bundle bundleData = msg.getData();
                    if (bundleData.getString("status").equals("null")) {
                        Toast.makeText(MainActivity.this, "当前城市不存在，请重新输入", Toast.LENGTH_SHORT).show();
                    }
                    if (bundleData.getString("status").equals("highSpeed")) {
                        Toast.makeText(MainActivity.this, "点击速度过快，两次点击间隔<600ms", Toast.LENGTH_SHORT).show();
                    }
                    if (bundleData.getString("status").equals("24")) {
                        Toast.makeText(MainActivity.this, "免费用户24h之内访问超过50次", Toast.LENGTH_SHORT).show();
                    }
                    if (bundleData.getString("status").equals("noNet")) {
                        Toast.makeText(MainActivity.this, "当前没有可用网络", Toast.LENGTH_SHORT).show();
                    }
                }
            } ;
        });
    }


}


