package com.xd.xdmanager.config;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;


/***
 * 课桌管理类
 * 1.保存课桌的行列配置
 * 2.保存所有课桌信息数据
 * 3.添加课桌信息
 * 4.删除课桌信息
 */
public class DeskManager {
    private Context context;
    private int mRows = 0;    // 所有课桌行数
    private int mColumns = 0; // 所有课桌列数
    //所有课桌列表
    private final List<Desk> mDesks = new ArrayList<>();
    private final Gson gson = new Gson ();

    private static volatile DeskManager INSTANCE;

    public static DeskManager INSTANCE(Context c) {
        if (INSTANCE == null) {
            synchronized (DeskManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new DeskManager(c);
                }
            }
        }
        return INSTANCE;
    }

    private DeskManager(Context c) {
        context = c;
        mRows = WManager.Companion.instance(c).getXNum();
        mColumns = WManager.Companion.instance(c).getYNum();
        loadAllDeskList();
    }

    public int getmRows() {
        return mRows;
    }

    public int getmColumns() {
        return mColumns;
    }

    public void setmRows(int rows) {
        WManager.Companion.instance(context).setXNum(rows);
        mRows = WManager.Companion.instance(context).getXNum();
    }

    public void setmColumns(int columns) {
        WManager.Companion.instance(context).setYNum(columns);
        mColumns = WManager.Companion.instance(context).getYNum();
    }
    /**
     * 更新布局
     * @param r
     * @param c
     * @return
     */
    public int updateRowsAndColumns(int r, int c) {
        Log.i("DeskManager", "updateRowsAndColumns r: "+ r + " c: "+ c);
        int ret = -1;
        if(r <= 0 || c <= 0) {
            return ret;
        }
        //新的布局不能容纳目前的课桌
        if((r * c) < (mRows * mColumns)) {
            Log.i("DeskManager", "updateRowsAndColumns failed rows: "+ mRows + " columns: "+ mColumns);
            return ret;
        }
        mRows = r;
        mColumns = c;
        WManager.Companion.instance(context).setXNum(r);
        WManager.Companion.instance(context).setYNum(c);
        //需要更新所有课桌的行和列
        // 循环遍历修改每个课桌的行和列值
        for (int i = 0; i < mDesks.size(); i++) {
            Desk desk = mDesks.get(i);
            // 计算新的行号和列号（从0开始）
            int newRow = i / c;
            int newColumn = i % c;

            desk.setRow(newRow);
            desk.setColumn(newColumn);
        }
        saveAllDeskList();
        return 1;
    }

    /**
     * 重置布局
     */
    public void resetDesks() {

    }

    /**
     * 通过行列号来获取desk对象
     * @param row
     * @param column
     * @return
     */
    public Desk getDeskByRowAndColum(int row, int column) {
        for (Desk desk: mDesks) {
            if(desk.getRow() == row && desk.getColumn() == column) {
                Log.i("DeskManager", "getDeskByRowAndColum desk: " + desk.toString());
                return desk;
            }
        }
        Log.w("DeskManager", "didn't find desk with row: " + row + " column: "+ column);
        return null;
    }

    /**
     * 配置课桌
     * 包含mqtt id 和can subId
     * 设置是否可用
     * @param desk
     */
    public void updateDesk(Desk desk) {

    }

    public void loadAllDeskList() {
        List<Desk> desks = getAllDeskList();
        mDesks.clear();
        if(desks != null && !desks.isEmpty()) {
            mDesks.addAll(desks);
        } else {
            Log.w("DeskManager", "loadAllDeskList desks is null");
            if(mRows > 0 && mColumns >0 ) {
                for(int row = 1; row <= mRows; row ++) {
                    for (int column = 1; column <= mColumns; column ++) {
                        Desk desk = new Desk();
                        desk.setRow(row);
                        desk.setColumn(column);
                        mDesks.add(desk);
                    }
                }
                saveAllDeskList();
            } else {
                Log.w("DeskManager", "loadAllDeskList set the rows and columns");
            }
        }
    }

    /**
     保存 List<Desk>到 SharedPreferences
     */
    public void saveAllDeskList () {
        // 将 List 转换为 JSON 字符串
        if(mDesks.isEmpty()) {
            String json = "";
            Log.w("DeskManager", "saveAllDeskList json: " + json);
            WManager.Companion.instance(context).setAllDeskInfo(json);
        } else {
            String json = gson.toJson(mDesks);
            Log.w("DeskManager", "saveAllDeskList json: " + json);
            WManager.Companion.instance(context).setAllDeskInfo(json);
        }
    }

    /**
     从 SharedPreferences 读取 List<Desk>
     获取所有行列的课桌
     @return 读取到的桌台列表，若不存在则返回空列表
     */
    public List<Desk> getAllDeskList () {
        // 从 SharedPreferences 获取 JSON 字符串
        String json = WManager.Companion.instance(context).getAllDeskInfo();
        Log.w("DeskManager", "getAllDeskList json: " + json);
        // 如果没有数据，返回空列表
        if (json.isEmpty()) {
            return new ArrayList<>();
        }
        // 定义 Gson 解析的类型
        Type type = new TypeToken<List<Desk>>(){}.getType();
        // 将 JSON 字符串转换为 List<Desk>
        List<Desk> deskList = gson.fromJson(json, type);
        // 防止解析失败返回 null
        return deskList != null ? deskList : new ArrayList<>();
    }
}
