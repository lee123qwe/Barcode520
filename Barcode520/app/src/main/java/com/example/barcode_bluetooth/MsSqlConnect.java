package com.example.barcode_bluetooth;

import android.util.Log;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.crypto.Mac;

public class MsSqlConnect {

    // 資料庫定義
    String mysql_ip = "210.240.163.28";
    int mysql_port = 3306; // Port 預設為 3306
    String db_name = "PropertyCheck";
    String url = "jdbc:mysql://" + mysql_ip + ":" + mysql_port + "/" + db_name;
    String db_user = "Reamer10811115";
    String db_password = "backspace20220301";

    public void run() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            Log.v("DB", "加載驅動成功");
        } catch (ClassNotFoundException e) {
            Log.e("DB", "加載驅動失敗");
            return;
        }

        // 連接資料庫
        try {
            Connection con = DriverManager.getConnection(url, db_user, db_password);
            Log.v("DB", "遠端連接成功");
        } catch (SQLException e) {
            Log.e("DB", "遠端連接失敗");
            Log.e("DB", e.toString());
        }
    }

    public String getData_a_table() {//下載資料部分
        String data = "";
        try {
            Connection con = DriverManager.getConnection(url, db_user, db_password);//進行一個sql的連線
            String sql = "SELECT * FROM `a_table_item_information`";//指定要執行的sql指令(重點修改此處)
            Statement st = con.createStatement();//建立一個Statement 類似郵差的概念
            st.executeQuery(sql);//獲取資料用這個**注意與上傳不同
            ResultSet rs = st.executeQuery(sql);//把剛拿到的資料使用ResultSet接起來
            while (rs.next())//將得到的資料逐筆讀取
            {
                String ss = rs.getString("barcode");
                String aa = rs.getString("item_name");
                data += "條碼號碼: " + ss + "項目名稱: "+aa+"\n";//此例是把她都串起來
            }
            st.close();//關閉郵差
        } catch (SQLException e) {//如果有狀況
            e.printStackTrace();//印出例外原因
        }
        return data;//回傳給呼叫的人
    }
    public String getData_b_table() {//下載資料部分
        String data = "";
        try {
            Connection con = DriverManager.getConnection(url, db_user, db_password);//進行一個sql的連線
            String sql = "SELECT * FROM `b_table_ record`";//指定要執行的sql指令(重點修改此處)
            Statement st = con.createStatement();//建立一個Statement 類似郵差的概念
            st.executeQuery(sql);//獲取資料用這個**注意與上傳不同
            ResultSet rs = st.executeQuery(sql);//把剛拿到的資料使用ResultSet接起來
            while (rs.next())//將得到的資料逐筆讀取
            {
                String ss = rs.getString("id");
                String aa = rs.getString("time");
                String dd = rs.getString("item_name");
                String ee = rs.getString("positon");
                String gg=rs.getString("bacode");
                data += "時間: " + aa +" 項目名稱: "+dd+" 位置: "+ee+" 條碼號碼: "+gg+"\n\n";//此例是把她都串起來
            }
            st.close();//關閉郵差
        } catch (SQLException e) {//如果有狀況
            e.printStackTrace();//印出例外原因
        }
        return data;//回傳給呼叫的人
    }
    public String getData_c_table() {//下載資料部分
        String data = "";
        try {
            Connection con = DriverManager.getConnection(url, db_user, db_password);//進行一個sql的連線
            String sql = "SELECT * FROM `c_table_beacon_set_position`";//指定要執行的sql指令(重點修改此處)
            Statement st = con.createStatement();//建立一個Statement 類似郵差的概念
            st.executeQuery(sql);//獲取資料用這個**注意與上傳不同
            ResultSet rs = st.executeQuery(sql);//把剛拿到的資料使用ResultSet接起來
            while (rs.next())//將得到的資料逐筆讀取
            {
                String ss = rs.getString("id");
                String aa = rs.getString("beacon_mac");
                String dd = rs.getString("position");
                data += "id: " + ss +" beacon實體地址: "+aa+" 位置: "+dd+"\n\n";//此例是把她都串起來
            }
            st.close();//關閉郵差
        } catch (SQLException e) {//如果有狀況
            e.printStackTrace();//印出例外原因
        }
        return data;//回傳給呼叫的人
    }

    public String pushData_a_table(String barcode, String item_name) {//上傳資料到a資料表
        try {
            Connection con = DriverManager.getConnection(url, db_user, db_password);//進行一個sql的連線
            String sql = "INSERT INTO `a_table_item_information` (`id`, `barcode`, `item_name`) VALUES (NULL, '"+barcode+"', '"+item_name+"')";//指定要執行的sql指令(重點修改此處)
            Log.v("DB", sql);
            Statement st = con.createStatement();//建立一個Statement 類似郵差的概念
            st.executeUpdate(sql);//新增資料用這個(郵差把指令遞送)**注意與下載不同
            st.close();//關閉郵差
        } catch (SQLException e) {//如果有狀況
            e.printStackTrace();//印出例外原因
        }
        return "OK";
    }
    public String pushData_b_table(String item_name, String position, String barcode) {//上傳資料到b資料表
        try {
            Connection con = DriverManager.getConnection(url, db_user, db_password);//進行一個sql的連線
            String sql = "INSERT INTO `b_table_ record` (`id`, `time`, `item_name`, `positon`, `bacode`) VALUES (NULL, CURRENT_TIMESTAMP, '"+item_name+"', '"+position+"', '"+barcode+"')";//指定要執行的sql指令(重點修改此處)
            Log.v("DB", sql);
            Statement st = con.createStatement();//建立一個Statement 類似郵差的概念
            st.executeUpdate(sql);//新增資料用這個(郵差把指令遞送)**注意與下載不同
            st.close();//關閉郵差
        } catch (SQLException e) {//如果有狀況
            e.printStackTrace();//印出例外原因
        }
        return "OK";
    }
    public String pushData_c_table(String Beacon_Mac, String position) {//上傳資料到c資料表
        try {
            Connection con = DriverManager.getConnection(url, db_user, db_password);//進行一個sql的連線
            String sql = "INSERT INTO `c_table_beacon_set_position` (`id`, `beacon_mac`, `position`) VALUES (NULL, '"+Beacon_Mac+"', '"+position+"')";//指定要執行的sql指令(重點修改此處)
            Log.v("DB", sql);
            Statement st = con.createStatement();//建立一個Statement 類似郵差的概念
            st.executeUpdate(sql);//新增資料用這個(郵差把指令遞送)**注意與下載不同
            st.close();//關閉郵差
        } catch (SQLException e) {//如果有狀況
            e.printStackTrace();//印出例外原因
        }
        return "OK";
    }
    public String getspecific_table_c(String Beacon_Mac) {//上傳beacon_mac取得資料庫上對應的位置
        String data = "";
        try {
            Connection con = DriverManager.getConnection(url, db_user, db_password);//進行一個sql的連線
            String sql = "SELECT * FROM `c_table_beacon_set_position` WHERE `beacon_mac` LIKE '"+Beacon_Mac+"'";
            Statement st = con.createStatement();//建立一個Statement 類似郵差的概念
            ResultSet rs = st.executeQuery(sql);//把剛拿到的資料使用ResultSet接起來
            while (rs.next())//將得到的資料逐筆讀取
            {
                String dd = rs.getString("position");
                data = dd;
            }
            st.close();//關閉郵差
        } catch (SQLException e) {//如果有狀況
            data="NULL";
            e.printStackTrace();//印出例外原因
        }
        return data;//回傳給呼叫的人
    }

    public String update(int rssi, int dis, int number) {//更新資料的部分，想要修改的number,
        try {
            Connection con = DriverManager.getConnection(url, db_user, db_password);//進行一個sql的連線
            String sql = "UPDATE RSSI\n" +
                    "SET rssi=1125\n" +
                    "WHERE RSSI=25;";//指定要執行的sql指令(重點修改此處)
            Log.v("DB", sql);
            Statement st = con.createStatement();//建立一個Statement 類似郵差的概念
            st.executeUpdate(sql);//新增資料用這個(郵差把指令遞送)**注意與下載不同
            st.close();//關閉郵差
        } catch (SQLException e) {//如果有狀況
            e.printStackTrace();//印出例外原因
        }
        return "OK";
    }

    public String getspecificdata(String barccd) {//下載資料部分
        String data = "";
        try {
            Connection con = DriverManager.getConnection(url, db_user, db_password);//進行一個sql的連線
            String sql = "SELECT * FROM `a_table_item_information` WHERE `barcode`="+barccd;
            Statement st = con.createStatement();//建立一個Statement 類似郵差的概念
            ResultSet rs = st.executeQuery(sql);//把剛拿到的資料使用ResultSet接起來
            while (rs.next())//將得到的資料逐筆讀取
            {
                //String ss = rs.getString("id");
                String dd = rs.getString("item_name");
                data = dd;//此例是把她都串起來
            }
            st.close();//關閉郵差
        } catch (SQLException e) {//如果有狀況
            data="NULL";
            e.printStackTrace();//印出例外原因
        }
        return data;//回傳給呼叫的人
    }
}