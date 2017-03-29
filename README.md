# VoiceTranslation
实现英文和语音翻译
调用讯飞语音api和有道api实现

Github的地址—[VoiceTranslation项目][1]
# 功能 

- 英文翻译成中文 
- 英文语音翻译
- 翻译时没有网络的提示


# 实现 

## 思路 

### 整体思路 

1. 使用  AsyncTask 发起网络请求 
	- .doInBackground 做真正的后台网络请求 
	- onPostExecute 对返回的结果作处理 
2. 利用接口将请求的数据（翻译的结果）返回给 UI 
 

### 翻译模块 

翻译使用**有道翻译** 的 API，参考 http://fanyi.youdao.com/openapi?path=data-mode 

1. 通过网址拼接的形式来获取翻译的数据
2. http://fanyi.youdao.com/openapi.do?keyfrom=<keyfrom>&key=<key>&type=data&doctype=<doctype>&version=1.1&q=要翻译的文本 
3. 需要修改的地方 
	1. key需要替换为自己申请的key  ； 
	2. doctype - 返回结果的数据格式，xml或json或jsonp，此处是json
4. 返回码errorCode是0 的时候表示 正常


### 讯飞语音接口

1. 下载相应的SDK，集成到项目中
2. 根据文档 http://www.xfyun.cn/doccenter/awd 调用接口


# 关键点 

## 网络请求 

### AsyncTask 
AsyncTask的三个参数，
- Params 启动任务执行的输入参数，比如HTTP请求的URL————**决定了doInBackground方法、execute方法的参数类型**
- Progress 后台任务执行的百分比————**决定了publishProgress方法、onProgressUpdate方法的参数类型**
- Result 后台执行任务最终返回的结果，比如String,Integer等————**决定了doInBackground方法的返回值类型、onPostExecute方法的参数类型**

不需要参数可以传 void，此处 doInBackground方法 执行后台翻译的网络请求，所以参数类型是 String ，返回的结果也是 String 类型，在onPostExecute中处理返回的结果，第二个参数百分比目前不需要，设置为void

###  HttpURLConnection

``` java
//执行网络请求
            URL url = null;
            HttpURLConnection httpURLConnection = null;
            BufferedReader reader = null;
            StringBuilder response = new StringBuilder();

            try {
                url = new URL(params[0]);

                Log.e(TAG,"网络请求链接-----"+url.toString());

                httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setReadTimeout(5000);

                InputStream is = httpURLConnection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(is));

                Log.e(TAG,"网络请求-----"+httpURLConnection.getResponseCode());

                String line ;
                while ((line = reader.readLine()) != null){
                    response.append(line);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
```
### 请求结果与UI交互 

onPostExecute中来处理返回的结果 
与UI交互，利用接口

``` java
@Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (s != null) {
                onResponseListener.onSuccess(s);
            }
        }
```

**回调接口**

``` java
public interface onResponseListener{
        void onSuccess(String result);
        void onFail(String error);
    }
```


### 网络请求实例写成单例

``` java
public static HttpUtil getInstance() {
        if (mHttpUtil == null) {
            synchronized (HttpUtil.class){
                if (mHttpUtil == null) {
                    mHttpUtil = new HttpUtil();
                }
            }
        }
        return mHttpUtil;
    }
```

## 语音识别 
### 初始化  
 1. 在OnCreate方法中初始化 

``` java
SpeechUtility.createUtility(context, SpeechConstant.APPID +"=APPID");   
```
2. 创建SpeechRecognizer对象

``` java
SpeechRecognizer mIat = SpeechRecognizer.createRecognizer(MainActivity.this,mInitListener);

	/**
     * 初始化监听器。
     */
    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int code) {

            if (code != ErrorCode.SUCCESS) {
                Toast.makeText(MainActivity.this, "初始化失败，错误码：" + code, Toast.LENGTH_SHORT).show();
            }
        }
    };
```

3. 设置参数 

``` java
public void setParam() {
        // 清空参数
        mIat.setParameter(SpeechConstant.PARAMS, null);

        // 设置听写引擎
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
        // 设置返回结果格式
        mIat.setParameter(SpeechConstant.RESULT_TYPE, "json");
        // 设置语言
        mIat.setParameter(SpeechConstant.LANGUAGE, "en_us");
        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mIat.setParameter(SpeechConstant.AUDIO_FORMAT,"wav");
        mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/iat.wav");
    }
```


4.开始听写  

``` stylus
mIat.startListening(mRecoListener);  
```
### 听写回调 

- 听写结果回调接口(返回Json格式结果  
- 一般情况下会通过onResults接口多次返回结果，完整的识别内容是多次结果的累加；    
- 关于解析Json的代码可参见MscDemo中JsonParser类；    
- isLast等于true时会话结束 

``` stylus
private RecognizerListener mRecoListener = new RecognizerListener() {
        //音量值0~30
        @Override
        public void onVolumeChanged(int i, byte[] bytes) {
//            Toast.makeText(MainActivity.this, "音量：" + i, Toast.LENGTH_SHORT).show();
        }

        //开始录音
        @Override
        public void onBeginOfSpeech() {
//            Toast.makeText(MainActivity.this, "开始录音" , Toast.LENGTH_SHORT).show();
        }

        //结束录音
        @Override
        public void onEndOfSpeech() {
//            Toast.makeText(MainActivity.this, "结束录音" , Toast.LENGTH_SHORT).show();
            mIatDialog.dismiss();
        }

        //一般情况下会通过onResults接口多次返回结果，完整的识别内容是多次结果的累加
        @Override
        public void onResult(RecognizerResult recognizerResult, boolean isLast) {
            printResult(recognizerResult);
            Log.d("Result:",recognizerResult.getResultString ());
        }

        //会话发生错误回调接口
        @Override
        public void onError(SpeechError speechError) {
        }

        //扩展用接口
        @Override
        public void onEvent(int i, int i1, int i2, Bundle bundle) {

        }
    };

```

### 语音返回数据处理

数据格式-json

![enter description here][2]


![enter description here][3]


1. 分析数据结构 

需要的数据在 ws 这个array下，先去这个array 

``` stylus
JSONArray words = joResult.getJSONArray("ws");
```


![enter description here][4]

2. ws的目录结构 
取ws这个array 里面的第一个值 

![enter description here][5]

``` stylus
words.getJSONObject(i)
```

3.ws下第一个jsonobject的结构 

![enter description here][6]

取 cw这个数组

``` stylus
JSONArray items = words.getJSONObject(i).getJSONArray("cw");
```
4. cw 这个数组的结构
![enter description here][7]
取第一个值

``` stylus
JSONObject obj = items.getJSONObject(0);
```

![enter description here][8]

取 key为 w 的值

``` stylus
obj.getString("w")
```


# 坑。。。

## 导包 jar 和 so文件 

### 导包 jar


![enter description here][9]
1. 按照上图创建libs目录
2. 拷贝jar包到libs下
3. 选中jar包，右键 

![enter description here][10]

4. ![enter description here][11]

### 添加so文件

![enter description here][12]
1. 建立目录jniLibs
2. 拷贝so文件进去
3. 在该目录![enter description here][13] 中的android添加 
  sourceSets { main { jni.srcDirs = ['src/main/jni', 'src/main/jniLibs'] }
``` stylus
android {
    compileSdkVersion 25
    buildToolsVersion "25.0.2"
	 .....
    sourceSets { main { jni.srcDirs = ['src/main/jni', 'src/main/jniLibs'] } }
}
```


## 网络权限

开发过程中，打包完成之后讯飞在录音时报错，进行排查
1. 检查需要的录音权限是否申请——已申请
2. 检查是否有其他程序占用麦克风——无
3. 重启手机——重启之后还是不行 

最后发现应用安装在手机上之后**手机没有给应用权限**

# 小技巧

快捷键  

Ctrl+F3  跳转到下一个相同变量——向下搜索
Shift+F3  跳转到上一个相同变量——向上搜索
Ctrl+F12 查看类中所有的参数和方法


每次设置参数或者给Edittext内容之前都应该先清空内容 

第三方引入的库中对象的初始化在使用前应该首先判断是否为空，并弹框提示，方便定位问题。

``` java
if( null == mIat )
{ // 创建单例失败
          Toast.makeText(this, "创建对象失败，请确认 libmsc.so 放置正确，且有调用 createUtility 进行初始化" , Toast.LENGTH_SHORT).show();
         return;
 }
```


  [1]: https://github.com/tuionf/VoiceTranslation
  [2]: http://wx4.sinaimg.cn/large/534fc2d6ly1fe3ikmjx8mj208q09q748.jpg "1490755473918.jpg"
  [3]: http://wx1.sinaimg.cn/large/534fc2d6ly1fe3ilu2ucdj208u071q30.jpg "1490755543475.jpg"
  [4]: http://wx4.sinaimg.cn/large/534fc2d6ly1fe3in7jwhaj206003l3yb.jpg "1490755622927.jpg"
  [5]: http://wx2.sinaimg.cn/large/534fc2d6ly1fe3iqnj6i2j205601jjr5.jpg "1490755821481.jpg"
  [6]: http://wx4.sinaimg.cn/large/534fc2d6ly1fe3isg4swpj206702k3yb.jpg "1490755924911.jpg"
  [7]: http://wx1.sinaimg.cn/large/534fc2d6ly1fe3itrfsh2j205l01agld.jpg "1490756000744.jpg"
  [8]: http://wx4.sinaimg.cn/large/534fc2d6ly1fe3ivlfoi5j20im05zaa5.jpg "1490756106401.jpg"
  [9]: http://wx2.sinaimg.cn/large/534fc2d6ly1fe3ibhvy6hj208w0a5wet.jpg "1490754947400.jpg"
  [10]: http://wx3.sinaimg.cn/large/534fc2d6ly1fe3igq3u85j209z01iglf.jpg "1490755248967.jpg"
  [11]: http://wx1.sinaimg.cn/large/534fc2d6ly1fe3iitihgxj20kr086mxr.jpg "1490755369401.jpg"
  [12]: http://wx4.sinaimg.cn/large/534fc2d6ly1fe3j09f12sj20a20bsaac.jpg "1490756375379.jpg"
  [13]: http://wx1.sinaimg.cn/large/534fc2d6ly1fe3j1vu93dj207m00sgle.jpg "1490756469127.jpg"
