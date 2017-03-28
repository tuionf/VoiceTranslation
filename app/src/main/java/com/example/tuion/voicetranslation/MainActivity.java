package com.example.tuion.voicetranslation;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;
import com.iflytek.sunflower.FlowerCollector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.LinkedHashMap;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText result_tv;
    private EditText edit_query;
    private Button btn_query;
    private ImageButton voice;
    int ret = 0; // 函数调用返回值
    private SpeechRecognizer mIat;
    // 语音听写UI
    private RecognizerDialog mIatDialog;
    private static String TAG = MainActivity.class.getSimpleName();
    // 用HashMap存储听写结果
    private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();

    // 引擎类型
    private String mEngineType = SpeechConstant.TYPE_CLOUD;
    private final String path = "http://fanyi.youdao.com/openapi.do?keyfrom=VoiceInstants&key=1540376092&type=data&doctype=json&version=1.1&q=";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SpeechUtility.createUtility(MainActivity.this, "appid=" + "58d4d7e4");

        mIat = SpeechRecognizer.createRecognizer(MainActivity.this,mInitListener);

        result_tv = (EditText) findViewById(R.id.result);
        edit_query = (EditText) findViewById(R.id.edit_query);
        btn_query = (Button) findViewById(R.id.btn_query);
        voice = (ImageButton) findViewById(R.id.voice);
        // 使用UI听写功能，请根据sdk文件目录下的notice.txt,放置布局文件和图片资源
        mIatDialog = new RecognizerDialog(MainActivity.this, mInitListener);

        btn_query.setOnClickListener(this);
        voice.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_query:
                translateQry();
                break;
            case R.id.voice:
                if( null == mIat ){
                    // 创建单例失败，与 21001 错误为同样原因，参考 http://bbs.xfyun.cn/forum.php?mod=viewthread&tid=9688
                    Toast.makeText(this, "创建对象失败，请确认 libmsc.so 放置正确，且有调用 createUtility 进行初始化" , Toast.LENGTH_SHORT).show();
                    return;
                }
                voiceTranslateQry();
                break;

        }
    }

    private void voiceTranslateQry() {

        //2.设置听写参数，详见《科大讯飞MSC API手册(Android)》SpeechConstant类
        // 应用领域 服务器为不同的应用领域，定制了不同的听写匹配引擎，使用对应的领域能获取更 高的匹配率
        // iat 默认
        //1.创建SpeechRecognizer对象，第二个参数：本地听写时传InitListener

        // 移动数据分析，收集开始听写事件
        FlowerCollector.onEvent(MainActivity.this, "iat_recognize");

        result_tv.setText(null);// 清空显示内容
        mIatResults.clear();



        // 清空参数
        setParam();
        boolean isDialog = true;
        if (isDialog) {
            // 显示听写对话框
            mIatDialog.setListener(mRecognizerDialogListener);
            mIatDialog.show();
            isDialog = false;
        } else {
            // 不显示听写对话框
            ret =  mIat.startListening(mRecoListener);
            if (ret != ErrorCode.SUCCESS) {
                Toast.makeText(this, "听写失败,错误码：" + ret, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "开始说话。。。", Toast.LENGTH_SHORT).show();
            }
        }

        //3.开始听写
        mIat.startListening(mRecoListener);
    }

    private void translateQry() {
        HttpUtil.onResponseListener onResponseListener = new HttpUtil.onResponseListener() {
            @Override
            public void onSuccess(String result) {

                StringBuffer explainsSB = new StringBuffer();
                try {
                    JSONObject object = new JSONObject(result);
                    int errorCode =  object.optInt("errorCode");
                    if (errorCode == 0) {
                        JSONObject basic = null;
                        if (!object.isNull("basic")) {
                            basic = object.optJSONObject("basic");

                            if (!basic.isNull("explains")) {
                                JSONArray explainsJS = basic.optJSONArray("explains");
                                for (int i = 0; i < explainsJS.length(); i++) {
                                    Log.e("hhp", explainsJS.get(i).toString() + "");
                                    explainsSB.append(explainsJS.get(i).toString()).append("\n");
                                }
                            }

                            result_tv.setText(explainsSB.toString());
                        }else {
                            result_tv.setText("暂无结果");
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onFail(String error) {

            }
        };

        String input = null;
        try {
            input = new String(edit_query.getText().toString().getBytes(),"UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        HttpUtil.getInstance().translate(path, input, onResponseListener);
    }

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
        public void onResult(RecognizerResult recognizerResult, boolean b) {
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

    /**
     * 参数设置
     *
     * @param
     * @return
     */
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

    private void printResult(RecognizerResult results) {
        String text = JsonParser.parseIatResult(results.getResultString());

        String sn = null;
        // 读取json结果中的sn字段
        try {
            JSONObject resultJson = new JSONObject(results.getResultString());
            sn = resultJson.optString("sn");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mIatResults.put(sn, text);

        StringBuffer resultBuffer = new StringBuffer();
        for (String key : mIatResults.keySet()) {
            resultBuffer.append(mIatResults.get(key));
        }



        edit_query.setText(resultBuffer.toString());
        translateQry();
//        edit_query.setSelection(result_tv.length());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if( null != mIat ){
            // 退出时释放连接
            mIat.cancel();
            mIat.destroy();
        }
    }

    @Override
    protected void onResume() {
        // 开放统计 移动数据统计分析
        FlowerCollector.onResume(MainActivity.this);
        FlowerCollector.onPageStart(TAG);
        super.onResume();
    }

    @Override
    protected void onPause() {
        // 开放统计 移动数据统计分析
        FlowerCollector.onPageEnd(TAG);
        FlowerCollector.onPause(MainActivity.this);
        super.onPause();
    }
    /**
     * 听写UI监听器
     */
    private RecognizerDialogListener mRecognizerDialogListener = new RecognizerDialogListener() {
        public void onResult(RecognizerResult results, boolean isLast) {
            printResult(results);
        }

        /**
         * 识别回调错误.
         */
        public void onError(SpeechError error) {
            Toast.makeText(MainActivity.this, error.getPlainDescription(true), Toast.LENGTH_SHORT).show();
        }

    };
}
