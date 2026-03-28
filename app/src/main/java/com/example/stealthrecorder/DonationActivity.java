package com.example.stealthrecorder;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class DonationActivity extends Activity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donation);
        
        Button alipayBtn = findViewById(R.id.alipay_btn);
        Button wechatBtn = findViewById(R.id.wechat_btn);
        Button paypalBtn = findViewById(R.id.paypal_btn);
        Button websiteBtn = findViewById(R.id.website_btn);
        
        alipayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAlipay();
            }
        });
        
        wechatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openWechatPay();
            }
        });
        
        paypalBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openPaypal();
            }
        });
        
        websiteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openWebsite();
            }
        });
    }
    
    private void openAlipay() {
        try {
            // 支付宝红包码或收款码
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("alipayqr://platformapi/startapp?saId=10000007&qrcode=https://qr.alipay.com/你的收款码"));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "请安装支付宝", Toast.LENGTH_SHORT).show();
            // 备用方案：打开浏览器
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://qr.alipay.com/你的收款码"));
            startActivity(intent);
        }
    }
    
    private void openWechatPay() {
        try {
            // 微信支付
            Intent intent = new Intent();
            intent.setClassName("com.tencent.mm", "com.tencent.mm.plugin.remittance.ui.RemittanceUI");
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "请安装微信", Toast.LENGTH_SHORT).show();
            // 备用方案：显示二维码图片
            // 这里需要添加显示二维码图片的逻辑
        }
    }
    
    private void openPaypal() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://www.paypal.com/paypalme/你的PayPal账号"));
        startActivity(intent);
    }
    
    private void openWebsite() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://你的域名.com/donate"));
        startActivity(intent);
    }
}