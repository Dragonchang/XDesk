package com.xd.xdmanager;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.xd.xdmanager.adapter.DeskManagerAdapter;
import com.xd.xdmanager.base.BaseActivity;
import com.xd.xdmanager.config.Desk;
import com.xd.xdmanager.config.DeskManager;
import com.xd.xdmanager.databinding.ActivityDeskManagerBinding;

import java.util.ArrayList;
import java.util.List;

/***
 * 动态配置座位行和列
 * 添加删除课桌
 */

public class DeskManagerActivity extends BaseActivity {

    private EditText rowInput;
    private EditText columnInput;
    private LinearLayout inputLayout;
    private ScrollView desk_layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @NonNull
    @Override
    public ViewBinding inflateBinding(@NonNull LayoutInflater layoutInflater) {
        return ActivityDeskManagerBinding.inflate(getLayoutInflater());
    }

    @Override
    public void loadData() {

    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        //1.获取控件
        inputLayout = findViewById(R.id.inputLayout);
        desk_layout = findViewById(R.id.desk_layout);
        rowInput = findViewById(R.id.rowInput);
        columnInput = findViewById(R.id.columnInput);
        int columns = DeskManager.INSTANCE(this).getmColumns();
        int rows = DeskManager.INSTANCE(this).getmRows();
        List<Desk> allDeskList  = DeskManager.INSTANCE(this).getAllDeskList();
        Log.i("DeskManagerActivity", "columns: " + columns + " rows: " + rows + " allDeskList: "+ allDeskList.size());
        if(allDeskList.isEmpty()) {
            inputLayout.setVisibility(View.VISIBLE);
            desk_layout.setVisibility(View.GONE);
        } else {
            inputLayout.setVisibility(View.GONE);
            desk_layout.setVisibility(View.VISIBLE);
            initDeskLayout(rows, columns, allDeskList);
        }

        setListeners();
    }
    private void setListeners() {
        // 创建课桌按钮点击事件
        findViewById(R.id.createButton).setOnClickListener(v -> saveRowAndColumn());
    }

    private void saveRowAndColumn() {
        String rowStr = rowInput.getText().toString();
        String columnStr = columnInput.getText().toString();

        if (rowStr.isEmpty() || columnStr.isEmpty()) {
            Toast.makeText(this, "请输入行数和列数", Toast.LENGTH_SHORT).show();
            return;
        }

        int rows = Integer.parseInt(rowStr);
        int columns = Integer.parseInt(columnStr);

        if (rows <= 0 || columns <= 0) {
            Toast.makeText(this, "行数和列数必须大于0", Toast.LENGTH_SHORT).show();
            return;
        }
        //保存课桌布局同时创建课桌
        DeskManager.INSTANCE(this).setmRows(rows);
        DeskManager.INSTANCE(this).setmColumns(columns);
        DeskManager.INSTANCE(this).loadAllDeskList();
        inputLayout.setVisibility(View.GONE);
        desk_layout.setVisibility(View.VISIBLE);
        List<Desk> allDeskList  = DeskManager.INSTANCE(this).getAllDeskList();
        initDeskLayout(rows, columns, allDeskList);
    }

    private void initDeskLayout(int rows, int columns, List<Desk> list) {
        LinearLayout boxView = findViewById(R.id.box);
        final List<RecyclerView> syncRecyclerViews = new ArrayList<>();
        Log.i("DeskManagerActivity", "list: " + list.size());
        for (int i = 1; i <= rows; i++) {
            int index_begin = (i - 1) * columns;
            int index_end = i * columns;
            List<Desk> rowList = list.subList(index_begin, index_end);
            RecyclerView syncRecyclerView = new RecyclerView(this);
            DeskManagerAdapter adapter = new DeskManagerAdapter(this, rowList);
            LinearLayoutManager ms = new LinearLayoutManager(this);
            ms.setOrientation(LinearLayoutManager.HORIZONTAL);
            syncRecyclerView.setLayoutManager(ms);
            syncRecyclerView.setAdapter(adapter);
            syncRecyclerViews.add(syncRecyclerView);
            syncRecyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);//取消边缘波浪样式
            boxView.addView(syncRecyclerView);
            //添加监听，使所有的RecyclerView保持同步
            RecyclerView.OnScrollListener listener = new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    for (RecyclerView item : syncRecyclerViews) {
                        if (recyclerView == item) {
                            continue;
                        }
                        item.clearOnScrollListeners();
                        item.scrollBy(dx, dy);
                        item.addOnScrollListener(this);
                    }
                }
            };
            syncRecyclerView.addOnScrollListener(listener);

        }
    }
}