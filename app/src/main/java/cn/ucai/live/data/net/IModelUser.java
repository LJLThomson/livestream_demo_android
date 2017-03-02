package cn.ucai.live.data.net;

import android.content.Context;

import com.hyphenate.easeui.domain.User;

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

    void updateAvator(Context context, String user_name_or_hxid, File file, OnCompleteListener<Result> listener);

    void loginContact(Context context, String user_name, OnCompleteListener<Result> listener);

    void updateNickName(Context context, String userName, String nickName, OnCompleteListener<Result> listener);

    void createChatRoom(Context context, User user, OnCompleteListener<String> listener);

    void deleteChatRoom(Context context,String ChatRoomId,OnCompleteListener<String> listener);
}
