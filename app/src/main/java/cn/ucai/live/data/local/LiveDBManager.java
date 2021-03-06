package cn.ucai.live.data.local;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.domain.User;
import com.hyphenate.easeui.utils.EaseCommonUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import cn.ucai.live.LiveApplication;
import cn.ucai.live.LiveConstants;

public class LiveDBManager {
    static private LiveDBManager dbMgr = new LiveDBManager();
    private DbOpenHelper dbHelper;

    private LiveDBManager() {
//        创建数据库
        dbHelper = DbOpenHelper.getInstance(LiveApplication.getInstance().getApplicationContext());
    }

    public static synchronized LiveDBManager getInstance() {
        if (dbMgr == null) {
            dbMgr = new LiveDBManager();
        }
        return dbMgr;
    }

    /**
     * save contact list
     *
     * @param contactList
     */
    synchronized public void saveContactList(List<EaseUser> contactList) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (db.isOpen()) {
//            每次存储User时，将原来表单数据全部删除，这样就可以保证只有一个用户，其实这是保存在内存中的
            db.delete(UserDao.TABLE_NAME, null, null);
            for (EaseUser user : contactList) {
                ContentValues values = new ContentValues();
                values.put(UserDao.COLUMN_NAME_ID, user.getUsername());
                if (user.getNick() != null)
                    values.put(UserDao.COLUMN_NAME_NICK, user.getNick());
                if (user.getAvatar() != null)
                    values.put(UserDao.COLUMN_NAME_AVATAR, user.getAvatar());
                db.replace(UserDao.TABLE_NAME, null, values);
            }
        }
    }

    /**
     * get contact list
     *
     * @return
     */
    synchronized public Map<String, EaseUser> getContactList() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Map<String, EaseUser> users = new Hashtable<String, EaseUser>();
        if (db.isOpen()) {
            Cursor cursor = db.rawQuery("select * from " + UserDao.TABLE_NAME /* + " desc" */, null);
            while (cursor.moveToNext()) {
                String username = cursor.getString(cursor.getColumnIndex(UserDao.COLUMN_NAME_ID));
                String nick = cursor.getString(cursor.getColumnIndex(UserDao.COLUMN_NAME_NICK));
                String avatar = cursor.getString(cursor.getColumnIndex(UserDao.COLUMN_NAME_AVATAR));
                EaseUser user = new EaseUser(username);
                user.setNick(nick);
                user.setAvatar(avatar);
                if (username.equals(LiveConstants.NEW_FRIENDS_USERNAME) || username.equals(LiveConstants.GROUP_USERNAME)
                        || username.equals(LiveConstants.CHAT_ROOM) || username.equals(LiveConstants.CHAT_ROBOT)) {
                    user.setInitialLetter("");
                } else {
                    EaseCommonUtils.setUserInitialLetter(user);
                }
                users.put(username, user);
            }
            cursor.close();
        }
        return users;
    }

    /**
     * delete a contact
     *
     * @param username
     */
    synchronized public void deleteContact(String username) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (db.isOpen()) {
            db.delete(UserDao.TABLE_NAME, UserDao.COLUMN_NAME_ID + " = ?", new String[]{username});
        }
    }

    /**
     * save a contact
     *
     * @param user
     */
    synchronized public void saveContact(EaseUser user) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(UserDao.COLUMN_NAME_ID, user.getUsername());
        if (user.getNick() != null)
            values.put(UserDao.COLUMN_NAME_NICK, user.getNick());
        if (user.getAvatar() != null)
            values.put(UserDao.COLUMN_NAME_AVATAR, user.getAvatar());
        if (db.isOpen()) {
            db.replace(UserDao.TABLE_NAME, null, values);
        }
    }


    public void setDisabledGroups(List<String> groups) {
        setList(UserDao.COLUMN_NAME_DISABLED_GROUPS, groups);
    }

    public List<String> getDisabledGroups() {
        return getList(UserDao.COLUMN_NAME_DISABLED_GROUPS);
    }

    public void setDisabledIds(List<String> ids) {
        setList(UserDao.COLUMN_NAME_DISABLED_IDS, ids);
    }

    public List<String> getDisabledIds() {
        return getList(UserDao.COLUMN_NAME_DISABLED_IDS);
    }

    synchronized private void setList(String column, List<String> strList) {
        StringBuilder strBuilder = new StringBuilder();

        for (String hxid : strList) {
            strBuilder.append(hxid).append("$");
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (db.isOpen()) {
            ContentValues values = new ContentValues();
            values.put(column, strBuilder.toString());

            db.update(UserDao.PREF_TABLE_NAME, values, null, null);
        }
    }

    synchronized private List<String> getList(String column) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select " + column + " from " + UserDao.PREF_TABLE_NAME, null);
        if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }

        String strVal = cursor.getString(0);
        if (strVal == null || strVal.equals("")) {
            return null;
        }

        cursor.close();

        String[] array = strVal.split("$");

        if (array.length > 0) {
            List<String> list = new ArrayList<String>();
            Collections.addAll(list, array);
            return list;
        }

        return null;
    }



    synchronized public void closeDB() {
        if (dbHelper != null) {
            dbHelper.closeDB();
        }
        dbMgr = null;
    }


    /**
     * save contact list
     *
     * @param contactList
     */
    synchronized public void saveAppContactList(List<User> contactList) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (db.isOpen()) {
//            清空数据库
            db.delete(UserDao.USER_TABLE_NAME, null, null);
            for (User user : contactList) {
                ContentValues values = new ContentValues();
                values.put(UserDao.USER_COLUMN_NAME, user.getMUserName());
                if (user.getMUserNick() != null)
                    values.put(UserDao.USER_COLUMN_NICK, user.getMUserNick());
                if (user.getMAvatarId() != null)
                    values.put(UserDao.USER_COLUMN_AVATAR, user.getMAvatarId());
                if (user.getMAvatarPath() != null)
                    values.put(UserDao.USER_COLUMN_AVATAR_PATH, user.getMAvatarPath());
                if (user.getMAvatarSuffix() != null)
                    values.put(UserDao.USER_COLUMN_AVATAR_SUFFIX, user.getMAvatarSuffix());
                if (user.getMAvatarType() != null)
                    values.put(UserDao.USER_COLUMN_AVATAR_TYPE, user.getMAvatarType());
                if (user.getMAvatarLastUpdateTime() != null)
                    values.put(UserDao.USER_COLUMN_AVATAR_UPDATA_TIME, user.getMAvatarLastUpdateTime());
                db.replace(UserDao.USER_TABLE_NAME, null, values);
            }
        }
    }

    /**
     * get contact list
     *
     * @return
     */
    synchronized public Map<String, User> getAppContactList() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Map<String, User> users = new Hashtable<String, User>();
        if (db.isOpen()) {
            Cursor cursor = db.rawQuery("select * from " + UserDao.USER_TABLE_NAME /* + " desc" */, null);
            while (cursor.moveToNext()) {

                String username = cursor.getString(cursor.getColumnIndex(UserDao.USER_COLUMN_NAME));
                String nick = cursor.getString(cursor.getColumnIndex(UserDao.USER_COLUMN_NICK));
                int avaterId = cursor.getInt(cursor.getColumnIndex(UserDao.USER_COLUMN_AVATAR));
                String avatarPath = cursor.getString(cursor.getColumnIndex(UserDao.USER_COLUMN_AVATAR_PATH));
                String suffix = cursor.getString(cursor.getColumnIndex(UserDao.USER_COLUMN_AVATAR_SUFFIX));
                int type = cursor.getInt(cursor.getColumnIndex(UserDao.USER_COLUMN_AVATAR_TYPE));
                String lastTime = cursor.getString(cursor.getColumnIndex(UserDao.USER_COLUMN_AVATAR_UPDATA_TIME));
                User user = new User();
                Log.e("SuperWechatDBManaget","getAppContactList"+user);
                user.setMUserName(username);
                user.setMUserNick(nick);
                user.setMAvatarId(avaterId);
                user.setMAvatarPath(avatarPath);
                user.setMAvatarSuffix(suffix);
                user.setMAvatarType(type);
                user.setMAvatarLastUpdateTime(lastTime);
//username.equals(Constant.NEW_FRIENDS_USERNAME)用户名等于新朋友，群组名等，就将初始化
//                if (username.equals(Constant.NEW_FRIENDS_USERNAME) || username.equals(Constant.GROUP_USERNAME)
//                        || username.equals(Constant.CHAT_ROOM) || username.equals(Constant.CHAT_ROBOT)) {
//                    user.setInitialLetter("");
//                } else {
//                    EaseCommonUtils.setUserInitialLetter(user);
//                }
                EaseCommonUtils.setAppUserInitialLetter(user);
                users.put(username, user);//以用户名来得到user
            }
            cursor.close();
        }
        return users;
    }

    /**
     * delete a contact
     *
     * @param username
     */
    synchronized public void deleteAppContact(String username) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (db.isOpen()) {
            db.delete(UserDao.USER_TABLE_NAME, UserDao.USER_COLUMN_NAME + " = ?", new String[]{username});
        }
    }

    /**
     * save a contact
     *
     * @param user
     */
    synchronized public void saveAppContact(User user) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(UserDao.USER_COLUMN_NAME, user.getMUserName());
        if (user.getMUserNick() != null)
            values.put(UserDao.USER_COLUMN_NICK, user.getMUserNick());
        if (user.getMAvatarId() != null)
            values.put(UserDao.USER_COLUMN_AVATAR, user.getMAvatarId());
        if (user.getMAvatarPath() != null)
            values.put(UserDao.USER_COLUMN_AVATAR_PATH, user.getMAvatarPath());
        if (user.getMAvatarSuffix() != null)
            values.put(UserDao.USER_COLUMN_AVATAR_SUFFIX, user.getMAvatarSuffix());
        if (user.getMAvatarType() != null)
            values.put(UserDao.USER_COLUMN_AVATAR_TYPE, user.getMAvatarType());
        if (user.getMAvatarLastUpdateTime() != null)
            values.put(UserDao.USER_COLUMN_AVATAR_UPDATA_TIME, user.getMAvatarLastUpdateTime());
        if (db.isOpen()) {
//            用户名相同，则进行覆盖操作，与insert不同之处
            db.replace(UserDao.USER_TABLE_NAME, null, values);
        }
    }
}
