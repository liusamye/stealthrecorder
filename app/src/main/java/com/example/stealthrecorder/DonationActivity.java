package com.example.stealthrecorder;

import android.app.Activity;
import android.content.Context;
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
            // 支付宝向 liusamye@163.com 支付
            // 方案1：直接打开支付宝转账页面
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("alipayqr://platformapi/startapp?saId=10000007&clientVersion=3.7.0.0718&qrcode=https://qr.alipay.com/fkx16888gq8qk9kfvfptxad"));
            startActivity(intent);
        } catch (Exception e) {
            // 方案2：打开支付宝网页版
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("alipays://platformapi/startapp?appId=09999988&actionType=toAccount&goBack=NO&amount=0.01&userId=2088102181274321&memo=支持StealthRecorder开发"));
                startActivity(intent);
            } catch (Exception e2) {
                // 方案3：打开浏览器到支付宝转账页面
                Toast.makeText(this, "正在打开支付宝...", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://qr.alipay.com/fkx16888gq8qk9kfvfptxad"));
                startActivity(intent);
            }
        }
    }
    
    private void openWechatPay() {
        try {
            // 微信向 81755825 账户支付
            // 方案1：尝试打开微信转账页面
            Intent intent = new Intent();
            intent.setClassName("com.tencent.mm", "com.tencent.mm.plugin.remittance.ui.RemittanceUI");
            // 添加转账参数
            intent.putExtra("scene", 1);
            intent.putExtra("receiver_name", "liusamye");
            intent.putExtra("receiver_user_name", "81755825");
            startActivity(intent);
        } catch (Exception e) {
            // 方案2：使用微信通用URL Scheme
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("weixin://dl/transfer"));
                startActivity(intent);
            } catch (Exception e2) {
                // 方案3：显示提示信息
                Toast.makeText(this, "请在微信中向账号 81755825 转账", Toast.LENGTH_LONG).show();
                // 复制账号到剪贴板
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("微信账号", "81755825");
                clipboard.setPrimaryClip(clip);
                
                // 尝试打开微信
                try {
                    Intent intent = getPackageManager().getLaunchIntentForPackage("com.tencent.mm");
                    if (intent != null) {
                        startActivity(intent);
                    }
                } catch (Exception e3) {
                    Toast.makeText(this, "无法打开微信，请手动打开", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    
    private void openPaypal() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://www.paypal.com/paypalme/你的PayPal账号"));
        startActivity(intent);
    }
    
    private void openWebsite() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://你的域名.com"));
        startActivity(intent);
    }
}