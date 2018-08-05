package com.example.interstitialtest;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import java.util.Timer;
import java.util.TimerTask;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

public class MainActivity extends AppCompatActivity {

    // 全面広告のインスタンス
    private InterstitialAd interstitialAd;

    // ロードチェックの上限
    // 読み込みの完了チェックの回数がこの数を超えた場合、失敗と判断する
    public static int AD_LOAD_TIMEOUT = 30;

    // ロードチェックの回数
    private int adLoadCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 全面広告のインスタンスを生成しロードを開始する
        interstitialAd = newInterstitialAd();
        loadInterstitial();

        // レイアウトに配置したView(広告のロード完了時にテキストを設定する)
        final TextView textInterstitialTest = findViewById(R.id.textInterstitialTest);

        // ローディング中はプログレスダイアログを表示して操作不可にする
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(getString(R.string.app_name));
        progressDialog.setMessage("Loading...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);
        progressDialog.show();

        final SharedPreferences preferences = getSharedPreferences("preference", MODE_PRIVATE);

        // 読み込み完了時にViewを更新するため、handlerを使用する
        final Handler handler = new Handler();

        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        // 読み込みが完了(isLoaded)したか、0.5秒×30回読み込みに失敗したら終了と判断
                        if((interstitialAd != null && interstitialAd.isLoaded()) || adLoadCount >= AD_LOAD_TIMEOUT) {
                            // タイマーを終了
                            cancel();

                            // ダイアログを終了
                            progressDialog.dismiss();

                            if(interstitialAd != null) { // 読み込みOK
                                // 読み込みOKのときはshowInterstitialを呼び出して全面広告を表示します
                                // アプリの起動毎に表示するとユーザーに良い印象を与えないのである程度制限します
                                // count % 3 == 1 の条件をつけることで、初回起動時は表示せず、2回目、5回目、8回目･･･、といった具合で表示しています
                                int count = preferences.getInt("count", 0);
                                textInterstitialTest.setText("load completed!\ncount:" + count);
                                if (count % 3 == 1) {
                                    showInterstitial();
                                }

                                // アプリの起動回数をSharedPreferencesに記録する
                                SharedPreferences.Editor editor = preferences.edit();
                                count++;
                                editor.putInt("count", count);
                                editor.commit();
                            } else{ // 読み込み失敗
                                textInterstitialTest.setText("load failed!");
                            }
                        } else{
                            // ロード中
                            adLoadCount++;
                            Log.d(getClass().getName(), "ad_load:" + adLoadCount);
                            if(adLoadCount >= AD_LOAD_TIMEOUT){
                                interstitialAd = null;
                            }
                        }
                    }
                });
            }
        },0,500);
    }

    /**
     * 全面広告の表示
     */
    private void showInterstitial() {
        if(interstitialAd != null && interstitialAd.isLoaded()) {
            interstitialAd.show();
        } else{
            goToNextLevel();
        }
    }

    /**
     * 全面広告の読み込み
     */
    private void loadInterstitial() {
        AdRequest adRequest = new AdRequest.Builder().setRequestAgent("android_studio:ad_template").build();
        interstitialAd.loadAd(adRequest);
    }

    /**
     * 続けて表示する場合に新たにインスタンスを生成
     */
    private void goToNextLevel() {
        interstitialAd = newInterstitialAd();
        loadInterstitial();
    }

    /**
     * 全面広告のインスタンスを取得
     * @return InterstitialAd
     */
    private InterstitialAd newInterstitialAd() {
        InterstitialAd interstitialAd = new InterstitialAd(this);

        interstitialAd.setAdUnitId(getString(R.string.interstitial_ad_unit_id));
        interstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                Log.d(getClass().getName(), "onAdLoaded()");
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                Log.d(getClass().getName(), "onAdFailedToLoad(" + errorCode + ")");
            }

            @Override
            public void onAdClosed() {
                Log.d(getClass().getName(), "onAdClosed()");
                goToNextLevel();
            }
        });

        return interstitialAd;
    }
}
