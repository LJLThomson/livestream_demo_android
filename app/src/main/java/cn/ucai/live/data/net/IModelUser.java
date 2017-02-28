package cn.ucai.live.data.net;

import android.content.Context;

import com.hyphenate.chat.EMGroup;

import java.io.File;

import cn.ucai.live.data.model.Result;

/**
 * Created by Administrator on 2017/2/8 0008.
 */

public interface IModelUser {
    void RegisterEnter(Context context, String userName, String NickName, String password, OnCompleteListener<String> OnCompleteListener);

    void UnRegisterEnter(Context context, String userName, OnCompleteListener<String> onCompleteListener);

    void LoginEnter(Context context, String userName, String password, OnCompleteListener<String> listener);

    void getUserByName(Context context, String userName, OnCompleteListener<Result> listener);

    void updateNickName(Context context, String userName, String nickName, OnCompleteListener<Result> listener);

    void updateAvator(Context context, String user_name_or_hxid, File file, OnCompleteListener<Result> listener);

    void addcontact(Context context, String user_name, String cname, OnCompleteListener<Result> listener);

    void loginContact(Context context, String user_name, OnCompleteListener<Result> listener);

    void removeContact(Context context, String username, String cname, OnCompleteListener<Result> listener);

    void CreateAppGroup(Context context, EMGroup group, File file, OnCompleteListener<Result> listener);

    void addGroupMembers(Context context, String memberNames, String groupId, OnCompleteListener<Result> listener);
}
