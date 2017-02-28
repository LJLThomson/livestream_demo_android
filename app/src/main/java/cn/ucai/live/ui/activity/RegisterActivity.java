package cn.ucai.live.ui.activity;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Toast;

import com.hyphenate.EMError;
import com.hyphenate.chat.EMClient;
import com.hyphenate.easeui.domain.User;
import com.hyphenate.exceptions.HyphenateException;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.ucai.live.LiveHelper;
import cn.ucai.live.R;
import cn.ucai.live.data.model.Result;
import cn.ucai.live.data.net.IModelUser;
import cn.ucai.live.data.net.ModelUser;
import cn.ucai.live.data.net.OnCompleteListener;
import cn.ucai.live.utils.CommonUtils;
import cn.ucai.live.utils.MD5;
import cn.ucai.live.utils.MFGT;
import cn.ucai.live.utils.ResultUtils;

public class RegisterActivity extends BaseActivity {

    @BindView(R.id.email)
    EditText userNameEditText;
    @BindView(R.id.password)
    EditText passwordEditText;
    @BindView(R.id.register)
    Button register;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.usernick)
    EditText usernick;
    @BindView(R.id.Comfirmpassword)
    EditText confirmPwdEditText;
    @BindView(R.id.login_form)
    ScrollView loginForm;
    IModelUser model;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        ButterKnife.bind(this);
        model = new ModelUser();
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        register.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                final String username = userNameEditText.getText().toString().trim();
                final String nick = usernick.getText().toString().trim();
                final String pwd = passwordEditText.getText().toString().trim();
                String confirm_pwd = confirmPwdEditText.getText().toString().trim();
                if (TextUtils.isEmpty(username)) {
                    Toast.makeText(RegisterActivity.this,getResources().getString(R.string.User_name_cannot_be_empty), Toast.LENGTH_SHORT).show();
                    userNameEditText.requestFocus();
                    return;
                } else if (username.matches("[a-zA-Z]\\w{5,15}]")) {//正則表達式，5到16位，開頭為字母
                    userNameEditText.setError(getResources().getString(R.string.illegal_user_name));
                    userNameEditText.requestFocus();
                    return;
                } else if (TextUtils.isEmpty(nick)) {
                    Toast.makeText(RegisterActivity.this, getResources().getString(R.string.User_nick_cannot_be_empty), Toast.LENGTH_SHORT).show();
                    usernick.requestFocus();
                    return;
                } else if (TextUtils.isEmpty(pwd)) {
                    Toast.makeText(RegisterActivity.this, getResources().getString(R.string.Password_cannot_be_empty), Toast.LENGTH_SHORT).show();
                    passwordEditText.requestFocus();
                    return;
                } else if (TextUtils.isEmpty(confirm_pwd)) {
                    Toast.makeText(RegisterActivity.this, getResources().getString(R.string.Confirm_password_cannot_be_empty), Toast.LENGTH_SHORT).show();
                    confirmPwdEditText.requestFocus();
                    return;
                } else if (!pwd.equals(confirm_pwd)) {
                    Toast.makeText(RegisterActivity.this, getResources().getString(R.string.Two_input_password), Toast.LENGTH_SHORT).show();
                    return;
                }
                registerbyArea(username, nick, pwd);
            }
        });
    }
    private void registerbyArea(final String username, String nick, final String pwd) {
        final ProgressDialog pd = new ProgressDialog(this);
        model.RegisterEnter(this, username, nick, pwd, new OnCompleteListener<String>() {
            @Override
            public void onSuccess(String s) {
                if (s != null) {
//                    将结果String转化为Result
                    Log.i("main","register_s"+s);
                    Result result = ResultUtils.getResultFromJson(s, User.class);
                    if (result.isRetMsg()) {
//                        说明注册成功
                        CommonUtils.showLongToast(R.string.Registered_successfully);
//                        跳到环信注册模块
                        registerEMServer(username, pwd);
                    } else {
//                        注册失败
                        CommonUtils.showLongToast(R.string.Registration_failed);
                    }
                } else {
                    CommonUtils.showLongToast(R.string.Registration_failed);
                }
                pd.dismiss();
            }

            @Override
            public void onError(String error) {

            }
        });
    }
    /**
     * 环信注册用户
     * 当其注册失败时，本地注册应该取消注册
     *
     * @param username 用户名
     * @param pwd      密码
     */
    private void registerEMServer(final String username, final String pwd) {
        if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(pwd)) {
            final ProgressDialog pd = new ProgressDialog(this);
            pd.setMessage(getResources().getString(R.string.Is_the_registered));
            pd.show();
            new Thread(new Runnable() {
                public void run() {
                    try {
                        // call method in SDK
                        EMClient.getInstance().createAccount(username, MD5.getMessageDigest(pwd));
                        runOnUiThread(new Runnable() {
                            public void run() {
                                if (!RegisterActivity.this.isFinishing())
                                    pd.dismiss();
                                // save current user
//                                设置用户名
                                LiveHelper.getInstance().setCurrentUserName(username);
                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.Registered_successfully), Toast.LENGTH_SHORT).show();
                                MFGT.gotoLoginActivity(RegisterActivity.this);
                                finish();
                            }
                        });
                    } catch (final HyphenateException e) {
//                        环信注册失败，则要删除本地上传的注册信息
                        unRegister(username);
                        runOnUiThread(new Runnable() {
                            public void run() {
                                if (!RegisterActivity.this.isFinishing())
                                    pd.dismiss();
                                int errorCode = e.getErrorCode();
                                if (errorCode == EMError.NETWORK_ERROR) {
                                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.network_anomalies), Toast.LENGTH_SHORT).show();
                                } else if (errorCode == EMError.USER_ALREADY_EXIST) {
                                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.User_already_exists), Toast.LENGTH_SHORT).show();
                                } else if (errorCode == EMError.USER_AUTHENTICATION_FAILED) {
                                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.registration_failed_without_permission), Toast.LENGTH_SHORT).show();
                                } else if (errorCode == EMError.USER_ILLEGAL_ARGUMENT) {
                                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.illegal_user_name), Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.Registration_failed), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                }
            }).start();

        }
    }
    /**
     * 取消注册
     * @param username
     */
    private void unRegister(String username) {
        model.UnRegisterEnter(this, username, new OnCompleteListener<String>() {
            @Override
            public void onSuccess(String s) {
                if (s != null) {
                    Result result = ResultUtils.getResultFromJson(s, User.class);
                    if (result != null) {
                        CommonUtils.showLongToast(R.string.Registration_failed);
                    }
                }
            }

            @Override
            public void onError(String error) {

            }
        });
    }

//    private void registerEMServer(final String username,) {
//        final ProgressDialog pd = new ProgressDialog(RegisterActivity.this);
//        pd.setMessage("正在注册...");
//        pd.setCanceledOnTouchOutside(false);
//        pd.show();
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    EMClient.getInstance().createAccount(username.getText().toString(), password.getText().toString());
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            pd.dismiss();
//                            showToast("注册成功");
//                            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
//                            finish();
//                        }
//                    });
//                } catch (final HyphenateException e) {
//                    e.printStackTrace();
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            pd.dismiss();
//                            showLongToast("注册失败：" + e.getMessage());
//                        }
//                    });
//                }
//            }
//        }).start();
//    }
}
