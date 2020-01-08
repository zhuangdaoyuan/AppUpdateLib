package com.maning.mnupdateapk;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.maning.updatelibrary.InstallUtils;
import com.maning.updatelibrary.ui.DialogUpdateFragment;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "InstallUtils";


    private Activity context;

    private TextView tv_progress;
    private TextView tv_info;
    private Button btnDownload;
    private Button btnCancle;
    private Button btnDownloadBrowser;
    private Button btnOther;
    private InstallUtils.DownloadCallBack downloadCallBack;
    private String apkDownloadPath;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        initViews();
        initCallBack();
    }


    private void initViews() {
        tv_progress = (TextView) findViewById(R.id.tv_progress);
        tv_info = (TextView) findViewById(R.id.tv_info);
        btnDownload = (Button) findViewById(R.id.btnDownload);
        btnCancle = (Button) findViewById(R.id.btnCancle);
        btnDownloadBrowser = (Button) findViewById(R.id.btnDownloadBrowser);
        btnOther = (Button) findViewById(R.id.btnOther);

        btnDownload.setOnClickListener(this);
        btnCancle.setOnClickListener(this);
        btnDownloadBrowser.setOnClickListener(this);
        btnOther.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //设置监听,防止其他页面设置回调后当前页面回调失效
        if (InstallUtils.isDownloading()) {
            InstallUtils.setDownloadCallBack(downloadCallBack);
        }
    }

    private void initCallBack() {
        downloadCallBack = new InstallUtils.DownloadCallBack() {
            @Override
            public void onStart() {
                Log.i(TAG, "InstallUtils---onStart");
                tv_progress.setText("0%");
                tv_info.setText("正在下载...");
                btnDownload.setClickable(false);
                btnDownload.setBackgroundResource(R.color.colorGray);
            }

            @Override
            public void onComplete(String path) {
                Log.i(TAG, "InstallUtils---onComplete:" + path);
                apkDownloadPath = path;
                tv_progress.setText("100%");
                tv_info.setText("下载成功");
                btnDownload.setClickable(true);
                btnDownload.setBackgroundResource(R.color.colorPrimary);

                //先判断有没有安装权限
                InstallUtils.checkInstallPermission(context, new InstallUtils.InstallPermissionCallBack() {
                    @Override
                    public void onGranted() {
                        //去安装APK
                        installApk(apkDownloadPath);
                    }

                    @Override
                    public void onDenied() {
                        //弹出弹框提醒用户
                        AlertDialog alertDialog = new AlertDialog.Builder(context)
                                .setTitle("温馨提示")
                                .setMessage("必须授权才能安装APK，请设置允许安装")
                                .setNegativeButton("取消", null)
                                .setPositiveButton("设置", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        //打开设置页面
                                        InstallUtils.openInstallPermissionSetting(context, new InstallUtils.InstallPermissionCallBack() {
                                            @Override
                                            public void onGranted() {
                                                //去安装APK
                                                installApk(apkDownloadPath);
                                            }

                                            @Override
                                            public void onDenied() {
                                                //还是不允许咋搞？
                                                Toast.makeText(context, "不允许安装咋搞？强制更新就退出应用程序吧！", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                })
                                .create();
                        alertDialog.show();
                    }
                });
            }

            @Override
            public void onLoading(long total, long current) {
                //内部做了处理，onLoading 进度转回progress必须是+1，防止频率过快
                Log.i(TAG, "InstallUtils----onLoading:-----total:" + total + ",current:" + current);
                int progress = (int) (current * 100 / total);
                tv_progress.setText(progress + "%");
            }

            @Override
            public void onFail(Exception e) {
                Log.i(TAG, "InstallUtils---onFail:" + e.getMessage());
                tv_info.setText("下载失败:" + e.toString());
                btnDownload.setClickable(true);
                btnDownload.setBackgroundResource(R.color.colorPrimary);
            }

            @Override
            public void cancel() {
                Log.i(TAG, "InstallUtils---cancle");
                tv_info.setText("下载取消");
                btnDownload.setClickable(true);
                btnDownload.setBackgroundResource(R.color.colorPrimary);
            }
        };
    }

    private void installApk(String path) {
        InstallUtils.installAPK(context, path, new InstallUtils.InstallCallBack() {
            @Override
            public void onSuccess() {
                //onSuccess：表示系统的安装界面被打开
                //防止用户取消安装，在这里可以关闭当前应用，以免出现安装被取消
                Toast.makeText(context, "正在安装程序", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFail(Exception e) {
                tv_info.setText("安装失败:" + e.toString());
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnCancle:
                //取消下载
                InstallUtils.cancleDownload();
                btnDownload.setClickable(true);
                btnDownload.setBackgroundResource(R.color.colorPrimary);
                break;
            case R.id.btnOther:
                startActivity(new Intent(this, OtherActivity.class));
                break;
            case R.id.btnDownloadBrowser:
                //通过浏览器去下载APK
                InstallUtils.installAPKWithBrower(this, Constants.APK_URL_02);
                break;
            case R.id.btnDownload:
//                //申请SD卡权限
//                if (!PermissionUtils.isGrantSDCardReadPermission(this)) {
//                    PermissionUtils.requestSDCardReadPermission(this, 100);
//                } else {
//                    InstallUtils.with(this)
//                            //必须-下载地址
//                            .setApkUrl(Constants.APK_URL_01)
//                            //非必须-下载保存的文件的完整路径+name.apk
//                            .setApkPath(Constants.APK_SAVE_PATH)
//                            //非必须-下载回调
//                            .setCallBack(downloadCallBack)
//                            //开始下载
//                            .startDownload();
//                }
                DialogUpdateFragment
                        .newBuilder()
                        .setContent("1、页面原生化\n2、修复bug")
                        .setISForce(false)
                        .setDownloadUrl("https://app.znds.com/data/ykq200.apk")
                        .build()
                        .show(getSupportFragmentManager(),"dialog");
                break;
            default:
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
