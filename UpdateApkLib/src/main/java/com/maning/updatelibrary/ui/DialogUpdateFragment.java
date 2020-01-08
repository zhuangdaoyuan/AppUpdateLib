package com.maning.updatelibrary.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.maning.updatelibrary.InstallUtils;
import com.maning.updatelibrary.R;
import com.maning.updatelibrary.utils.DisplayUtil;
import com.tbruyelle.rxpermissions2.RxPermissions;

/**
 * @author :庄道园
 * Date :2020/1/7
 * 安静撸码，淡定做人
 */
public class DialogUpdateFragment extends DialogFragment implements View.OnClickListener {
    private TextView confirm;
    private TextView cancel;
    private TextView contentTv;
    private ProgressBar progressBar;
    private DialogUpdateListener dialogUpdateListener;

    //资源
    //更新内容
    private static final String CONTENT = "content";
    //是否强制更新
    private static final String IS_FORCE = "isforce";
    //下载地址
    private static final String DOWNLOAD_ADDRESS = "download_address";

    //安装包存储地址
    private String downloadUrl;

    private static DialogUpdateFragment newInstance(Builder builder) {
        DialogUpdateFragment customerDialogFragment = new DialogUpdateFragment();
        Bundle bundle = new Bundle();
        bundle.putString(CONTENT, builder.content);
        bundle.putBoolean(IS_FORCE, builder.isForce);
        bundle.putString(DOWNLOAD_ADDRESS, builder.downloadUrl);
        customerDialogFragment.setArguments(bundle);
        return customerDialogFragment;
    }

    public DialogUpdateFragment setDialogUpdateListener(DialogUpdateListener dialogUpdateListener) {
        this.dialogUpdateListener = dialogUpdateListener;
        return this;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.Dialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_update, container, false);
        confirm = view.findViewById(R.id.update_confirm);
        cancel = view.findViewById(R.id.update_cancel);
        contentTv = view.findViewById(R.id.update_content);
        confirm.setOnClickListener(this);
        cancel.setOnClickListener(this);
        progressBar = view.findViewById(R.id.update_progress);

        Bundle bundle = getArguments();
        try {
            String content = bundle.getString(CONTENT);
            contentTv.setText(content);
            boolean isForceUpdate = bundle.getBoolean(IS_FORCE);
            cancel.setVisibility(!isForceUpdate ? View.VISIBLE : View.GONE);
            downloadUrl = bundle.getString(DOWNLOAD_ADDRESS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        setCancelable(false);
        return view;
    }

    @Override
    public void onResume() {
        ViewGroup.LayoutParams params = getDialog().getWindow().getAttributes();
        params.width = DisplayUtil.getWindowWidth(getContext()) - DisplayUtil.dip2px(getContext(), 100);
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        getDialog().getWindow().setAttributes((WindowManager.LayoutParams) params);
        super.onResume();
    }

    @Override
    public void onClick(View v) {
//        if (dialogUpdateListener == null) {
//            dismiss();
//            return;
//        }
        if (v.getId() == R.id.update_cancel) {
            dismiss();
        } else if (v.getId() == R.id.update_confirm) {
            cancel.setVisibility(View.GONE);
            confirm.setVisibility(View.INVISIBLE);
            progressBar.setProgress(1);
            progressBar.setVisibility(View.VISIBLE);
//            dialogUpdateListener.onUpdate();
            startDownload();
        }
    }

    private void startDownload() {
        //申请SD卡权限
        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(granted -> {
                    if (granted) {
                        InstallUtils.with(getContext())
                                //必须-下载地址
                                .setApkUrl(downloadUrl)
                                //非必须-下载保存的文件的完整路径+name.apk
//                                .setApkPath(apkPath)
                                //非必须-下载回调
                                .setCallBack(new InstallUtils.DownloadCallBack() {
                                    @Override
                                    public void onStart() {
                                    }

                                    @Override
                                    public void onComplete(String path) {
                                        checkInstallPermission(path);
                                    }

                                    @Override
                                    public void onLoading(long total, long current) {
                                        //内部做了处理，onLoading 进度转回progress必须是+1，防止频率过快
                                        int progress = (int) (current * 100 / total);
                                        progressBar.setProgress(progress);
                                    }

                                    @Override
                                    public void onFail(Exception e) {
                                        Toast.makeText(getContext(), "下载失败", Toast.LENGTH_SHORT).show();
                                        cancel.setVisibility(View.VISIBLE);
                                        confirm.setVisibility(View.VISIBLE);
                                        progressBar.setVisibility(View.INVISIBLE);
                                    }

                                    @Override
                                    public void cancel() {
                                    }
                                })
                                .startDownload();//开始下载
                    } else {
                        Toast.makeText(getContext(), "请前往设置授权存储权限", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    //检测安装权限
    private void checkInstallPermission(String path) {
        InstallUtils.checkInstallPermission(getActivity(), new InstallUtils.InstallPermissionCallBack() {
            @Override
            public void onGranted() {
                installApk(path);
            }

            @Override
            public void onDenied() {
                //弹出弹框提醒用户
                AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                        .setTitle("温馨提示")
                        .setMessage("请授权安装APK权限-设置允许安装")
                        .setNegativeButton("取消", null)
                        .setPositiveButton("设置", (dialog, which) -> {
                            //打开设置页面
                            InstallUtils.openInstallPermissionSetting((Activity) getContext(), new InstallUtils.InstallPermissionCallBack() {
                                @Override
                                public void onGranted() {
                                    //去安装APK
                                    installApk(path);
                                }

                                @Override
                                public void onDenied() {
                                    Toast.makeText(getContext(), "请尽快升级最新版本，否则无法使用相关功能", Toast.LENGTH_SHORT).show();
                                }
                            });
                        })
                        .create();
                alertDialog.show();
            }
        });
    }

    //安装
    private void installApk(String path) {
        InstallUtils.installAPK(getActivity(), path, new InstallUtils.InstallCallBack() {
            @Override
            public void onSuccess() {
                //onSuccess：表示系统的安装界面被打开
                //防止用户取消安装，在这里可以关闭当前应用，以免出现安装被取消
                dismiss();
            }

            @Override
            public void onFail(Exception e) {
                Toast.makeText(getContext(), "安装失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public interface DialogUpdateListener {
        void onUpdate();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        DialogUpdateFragment dialogUpdateFragment;
        private String content;
        private boolean isForce;
        private String downloadUrl;

        public Builder setContent(String content) {
            this.content = content;
            return this;
        }

        public Builder setISForce(boolean isForce) {
            this.isForce = isForce;
            return this;
        }

        public Builder setDownloadUrl(String downloadUrl) {
            this.downloadUrl = downloadUrl;
            return this;
        }

        public DialogUpdateFragment build() {
            if (dialogUpdateFragment == null) {
                dialogUpdateFragment = DialogUpdateFragment.newInstance(this);
            }
            return dialogUpdateFragment;
        }
    }

}
