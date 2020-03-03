package com.example.tflite_zf;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.LinkedHashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    ImageView image;
    CodeDetector codeDetector;
    TextView txtCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        image = findViewById(R.id.image);
        txtCode = findViewById(R.id.txtCode);
        codeDetector = new CodeDetector(this);
        loadImage();
    }

    void loadImage() {
        String url = "http://jwxt.njupt.edu.cn/CheckCode.aspx";
        Log.i(TAG, "loadImage: from: " + url);
        txtCode.setText("loading");
        RequestQueue queue = Volley.newRequestQueue(this);
        ImageRequest request = new ImageRequest(url,
                new Response.Listener<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap bitmap) {
                        Log.i(TAG, "got bitmap");
                        image.setImageBitmap(bitmap);
                        codeDetector.sliceImage(bitmap);
                        showSubImage(bitmap);
                        detect();
                    }
                },
                0,0, ImageView.ScaleType.CENTER_CROP,null,
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(MainActivity.this, "获取验证码失败，" + error, Toast.LENGTH_SHORT).show();
                    }
                }) {
            // 覆盖获取请求头方法，设置User-Agent，实现桌面端请求
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new LinkedHashMap<>();
                headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:53.0) Gecko/20100101 Firefox/53.0");
                return headers;
            }
        };
        queue.add(request);
    }

    public void showSubImage(Bitmap bitmap) {
        Bitmap.Config config = bitmap.getConfig();
        int[] imgIds = {R.id.image1, R.id.image2, R.id.image3, R.id.image4};
        for (int i=0; i<imgIds.length; i++) {
            Bitmap subBitmap = Bitmap.createBitmap(codeDetector.pixels[i],
                    codeDetector.width, codeDetector.height, config);
            ImageView iw = findViewById(imgIds[i]);
            iw.setImageBitmap(subBitmap);
        }
    }

    void detect() {
        Log.d(TAG, "detect: before");
        String code = codeDetector.detect();
        TextView txtCode = findViewById(R.id.txtCode);
        txtCode.setText(code);
        Log.i(TAG, "detect: " + code);
    }


    public void refresh(View view) {
        loadImage();
    }


}
