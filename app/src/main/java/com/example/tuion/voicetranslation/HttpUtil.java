package com.example.tuion.voicetranslation;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by tuion on 2017/3/23.
 */

public class HttpUtil {

    private static final String TAG = HttpUtil.class.getSimpleName();

    private static HttpUtil mHttpUtil;

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

    public void translate(String path, String input,onResponseListener mOnResponseListener){
        //执行网络请求
        StringBuffer sb  = new StringBuffer();
        sb.append(path).append(input);
        String str = null;
        try {
             str = new String(sb.toString().getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        new http(mOnResponseListener).execute(str);

    }

    class http extends AsyncTask<String,Void,String>{

        public onResponseListener onResponseListener;
        public http(onResponseListener mOnResponseListener) {
            this.onResponseListener = mOnResponseListener;
        }

        @Override
        protected String doInBackground(String... params) {
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
            }
            Log.e(TAG,"网络请求的结果-----"+response.toString());
            return response.toString();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (s != null) {
                onResponseListener.onSuccess(s);
            }

        }
    }

    public interface onResponseListener{
        void onSuccess(String result);
        void onFail(String error);
    }
}
