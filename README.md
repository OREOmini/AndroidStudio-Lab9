## 实验九 网络访问&Web 服务开发 



### 实验目的

1. 熟练使用 HttpURLConnection 访问 WebService 
2. 熟悉使用多线程以及 Handler 更新 UI
3. 熟悉使用 XmlPullParser 解析 xml 文档数据
4. 了解 RecyclerView 控件的使用
5. (可选)使用 Ksoap2 来访问 WebService
6. (可选)了解并掌握 SOAP 和 WSDL 相关基础



### 实验内容

#### 设置网络访问

使用网络,应该有相应使用允许。文件AndroidManifest添加

```
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
```

> android.permission.INTERNET——允许应用程序打开网络套接字。
> android.permission.ACCESS_NETWORK_STATE——允许应用程序访问网络连接信息。	

在实验pdf里面有说

> 注:Android4.0 之后,http 请求需要开启子线程,然后由子线程执行请求,所以我们之前所写代码都是在子线程中完成的,并且使用 XmlPullParser 进行解析从而得到我们想要的数据		

所以这里**用一个新的线程networkTask来处理网络访问**

```java
@Override
public void onClick(View v) {
    new Thread(networkTask).start();
}
```

在新的线程中执行以下部分

* 使用 HttpURLConnection 新建一个 http 连接
* 将我们需要请求的字段以流的形式写入 connection 之中
* 网页获取 xml 转化为字符串:

```java
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
                String request = city.getText().toString();
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
            Toast.makeText(MainActivity.this, "当前没有可用网络！", Toast.LENGTH_SHORT).show();
        }
    }
};
```

但是这里会出错，因为不能在内部改UI，所以用一个handler处理没有网络的提示

```java
else {
    Message message = new Message();
    Bundle bundle = new Bundle();
    bundle.putString("status", "noNet");
    message.setData(bundle);
    handleErrors.sendMessage(message);
}
```



#### 处理response数据

**XmlPullParser 解析 xml 文档**

因为还有可能出错的情况，所以大致判断一个长度，小于10就是没有得到想要的返回数据，就归入到错误处理里面

```java
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
```

![屏幕快照 2016-11-26 下午1.40.27](ScreenShoot/屏幕快照 2016-11-26 下午1.40.27.png)



再对**信息进行提取，得到想放在UI里面的信息**

用subString(), split()等方法提取出想要的信息，放在一个bundle里面，传给更新UI界面的handler处理

```java
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
```

![屏幕快照 2016-11-26 下午1.42.18](ScreenShoot/屏幕快照 2016-11-26 下午1.42.18.png)



#### 更新界面

**Handler更新UI界面**

从传入的message里的bundle中提取出来信息，并放在对应的界面元素里面；

用simpleAdapter更改list的界面

```java
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
```





**对于错误信息的处理**

一般错误都为一行，**两次点击太快**：![屏幕快照 2016-11-26 下午1.18.28](ScreenShoot/屏幕快照 2016-11-26 下午1.18.28.png)

就用一个`strList.get(0).substring(0, 6).equals("不能")`来判断

**用户访问超过数量**：![屏幕快照 2016-11-26 下午1.47.59](ScreenShoot/屏幕快照 2016-11-26 下午1.47.59.png)

用`strList.get(0).substring(9, 11).equals("24")`来判断

**输入名称有问题**：![屏幕快照 2016-11-26 下午1.50.03](ScreenShoot/屏幕快照 2016-11-26 下午1.50.03.png)

用`strList.get(0).substring(0, 6).equals("查询结果为空")`来判断

```java
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
```

再将这些错误交给handler来反馈给用户（包括没有可用网络）

```java
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
```



### 实验总结

* java中的finally语句：

  > finally 块确保 close 方法总被执行，而不管 try 块内是否发出异常。因此，可以确保在退出该方法之前总会调用 close 方法。这样您就可以确信套接字被关闭并且您没有泄漏资源。在此方法中不需要再有一个 catch 块。在第一个示例中提供 catch 块只是为了关闭套接字，现在这是通过 finally 关闭的。如果您确实提供了一个 catch 块，则 finally 块中的代码在 catch 块完成以后执行。

  > finally 块必须与 try 或 try/catch 块配合使用。此外，不可能退出 try 块而不执行其 finally 块。如果 finally 块存在，则它总会执行。

* ```java
   HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
     try {
       urlConnection.setDoOutput(true);
       urlConnection.setChunkedStreamingMode(0);

       OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
       writeStream(out);

       InputStream in = new BufferedInputStream(urlConnection.getInputStream());
       readStream(in);
     } finally {
       urlConnection.disconnect();
     }
  ```

* 这次用到的字符串处理函数：

  * split():

    ```
    string s=abcdeabcdeabcde;
    string[] sArray=s.Split('c') ;
    foreach(string i in sArray)
    Console.WriteLine(i.ToString());
    ```

    输出下面的结果:

    > ab
    > deab
    > deab
    > de

  * subString():

    > public String substring(int beginIndex, int endIndex)
    > 第一个int为开始的索引，对应String数字中的开始位置，
    > 第二个是截止的索引位置，对应String中的结束位置

    1. 取得的字符串长度为：endIndex - beginIndex;
       2. 从beginIndex开始取，到endIndex结束，从0开始数，其中不包括endIndex位置的字符

* 在没有用runnable和handler处理之前会抛出NetworkOnMainThreadException异常，

  > 从Honeycomb SDK（3.0）开始，google不再允许网络请求（HTTP、Socket）等相关操作直接在Main Thread类中，其实本来就不应该这样做，直接在UI线程进行网络操作，会阻塞UI、用户体验相当bad！
  > 也就是说，在Honeycomb SDK（3.0）以下的版本，你还可以继续在Main Thread里这样做，在3.0以上，就不行了

  解决方法就是使用handler：和network有关比较耗时的操作放到一个子线程里,然后用Handler消息机制与主线程通信

  ```java
  public void onCreate(Bundle savedInstanceState) {  
      super.onCreate(savedInstanceState);  
      this.setContentView(R.layout.test);  
      // 开启一个子线程，进行网络操作，等待有返回结果，使用handler通知UI  
      new Thread(networkTask).start();  
  }  
    
  Handler handler = new Handler() {  
      @Override  
      public void handleMessage(Message msg) {  
          super.handleMessage(msg);  
          Bundle data = msg.getData();  
          String val = data.getString("value");  
          Log.i("mylog", "请求结果为-->" + val);  
          // TODO  
          // UI界面的更新等相关操作  
      }  
  };  
    
  /** 
   * 网络操作相关的子线程 
   */  
  Runnable networkTask = new Runnable() {  
    
      @Override  
      public void run() {  
          // TODO  
          // 在这里进行 http request.网络请求相关操作  
          Message msg = new Message();  
          Bundle data = new Bundle();  
          data.putString("value", "请求结果");  
          msg.setData(data);  
          handler.sendMessage(msg);  
      }  
  };  
  ```

  另外还有老师在课件里面使用的asynctask异步机制

  加载网络图片的例子

  ```java
  class DownImage extends AsyncTask {  
    
      private ImageView imageView;  
    
      public DownImage(ImageView imageView) {  
          this.imageView = imageView;  
      }  
    
      @Override  
      protected Bitmap doInBackground(String... params) {  
          String url = params[0];  
          Bitmap bitmap = null;  
          try {  
              //加载一个网络图片  
              InputStream is = new URL(url).openStream();  
              bitmap = BitmapFactory.decodeStream(is);  
          } catch (Exception e) {  
              e.printStackTrace();  
          }  
          return bitmap;  
      }  
    
      @Override  
      protected void onPostExecute(Bitmap result) {  
          imageView.setImageBitmap(result);  
      }  
  }  
  ```

  ​

  ​
