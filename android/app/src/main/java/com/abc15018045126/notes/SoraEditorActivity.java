package com.abc15018045126.notes;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.widget.EditorSearcher;
import io.github.rosemoe.sora.widget.EditorSearcher.SearchOptions;
import io.github.rosemoe.sora.event.SelectionChangeEvent;
import io.github.rosemoe.sora.event.PublishSearchResultEvent;
import io.github.rosemoe.sora.event.ContentChangeEvent;
import io.github.rosemoe.sora.text.Cursor;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.text.ContentIO;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SoraEditorActivity extends AppCompatActivity {

    private CodeEditor editor;
    private String filePath;
    private boolean isDirty = false;
    private LinearLayout searchPanel;
    private EditText searchInput;
    private EditText replaceInput;
    private TextView positionDisplay;
    private SearchOptions searchOptions = new SearchOptions(SearchOptions.TYPE_NORMAL, true);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sora_editor);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        editor = findViewById(R.id.editor);
        searchPanel = findViewById(R.id.search_panel);
        searchInput = findViewById(R.id.search_input);
        replaceInput = findViewById(R.id.replace_input);
        positionDisplay = findViewById(R.id.position_display);

        // 基本设置
        editor.setLineNumberEnabled(true);
        editor.setWordwrap(true, true);
        editor.setTextSize(18);

        Intent intent = getIntent();
        filePath = intent.getStringExtra("path");
        String contentFromIntent = intent.getStringExtra("content");
        String title = intent.getStringExtra("title");

        if (title != null && !title.isEmpty()) {
            getSupportActionBar().setTitle(title);
        } else {
            getSupportActionBar().setTitle("文本编辑器");
        }

        loadContent(contentFromIntent);

        // 监听事件以更新状态栏
        editor.subscribeAlways(SelectionChangeEvent.class, (event) -> updatePositionText());
        editor.subscribeAlways(PublishSearchResultEvent.class, (event) -> updatePositionText());
        
        // 监听修改状态
        editor.subscribeAlways(ContentChangeEvent.class, (event) -> {
            isDirty = true;
        });

        // 搜索逻辑
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                tryCommitSearch();
            }
        });

        findViewById(R.id.btn_prev).setOnClickListener(v -> editor.getSearcher().gotoPrevious());
        findViewById(R.id.btn_next).setOnClickListener(v -> editor.getSearcher().gotoNext());
        findViewById(R.id.btn_replace).setOnClickListener(v -> {
            try {
                editor.getSearcher().replaceCurrentMatch(replaceInput.getText().toString());
            } catch (Exception e) {
                Toast.makeText(this, "替换失败", Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.btn_replace_all).setOnClickListener(v -> {
            try {
                editor.getSearcher().replaceAll(replaceInput.getText().toString());
            } catch (Exception e) {
                Toast.makeText(this, "全量替换失败", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.search_more).setOnClickListener(this::showSearchOptionsMenu);

        updatePositionText();
    }

    private void loadContent(String fallbackContent) {
        if (filePath != null) {
            File file = new File(filePath);
            if (file.exists()) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    Content content = ContentIO.createFrom(fis);
                    editor.setText(content);
                    isDirty = false;
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "读取文件失败，尝试使用备用内容", Toast.LENGTH_SHORT).show();
                }
            }
        }
        
        if (fallbackContent != null) {
            editor.setText(fallbackContent);
            isDirty = false;
        }
    }

    private void tryCommitSearch() {
        String query = searchInput.getText().toString();
        if (query.length() > 0) {
            editor.getSearcher().search(query, searchOptions);
        } else {
            editor.getSearcher().stopSearch();
        }
    }

    private void showSearchOptionsMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenu().add(0, 1, 0, "正则表达式").setCheckable(true).setChecked(searchOptions.type == SearchOptions.TYPE_REGULAR_EXPRESSION);
        popup.getMenu().add(0, 2, 0, "区分大小写").setCheckable(true).setChecked(!searchOptions.caseInsensitive);
        popup.getMenu().add(0, 3, 0, "全词匹配").setCheckable(true).setChecked(searchOptions.type == SearchOptions.TYPE_WHOLE_WORD);
        
        popup.setOnMenuItemClickListener(item -> {
            item.setChecked(!item.isChecked());
            boolean caseInsensitive = searchOptions.caseInsensitive;
            int type = searchOptions.type;
            
            if (item.getItemId() == 1) {
                type = item.isChecked() ? SearchOptions.TYPE_REGULAR_EXPRESSION : SearchOptions.TYPE_NORMAL;
            } else if (item.getItemId() == 2) {
                caseInsensitive = !item.isChecked();
            } else if (item.getItemId() == 3) {
                type = item.isChecked() ? SearchOptions.TYPE_WHOLE_WORD : SearchOptions.TYPE_NORMAL;
            }
            
            searchOptions = new SearchOptions(type, caseInsensitive);
            tryCommitSearch();
            return true;
        });
        popup.show();
    }

    private void updatePositionText() {
        Cursor cursor = editor.getCursor();
        String text = String.format("行: %d, 列: %d | 位置: %d", 
            cursor.getLeftLine() + 1, 
            cursor.getLeftColumn(), 
            cursor.getLeft());

        EditorSearcher searcher = editor.getSearcher();
        if (searcher.hasQuery()) {
            int count = searcher.getMatchedPositionCount();
            int index = searcher.getCurrentMatchedPositionIndex();
            if (count > 0) {
                text += String.format(" | 匹配: %d/%d", index + 1, count);
            } else {
                text += " | 无匹配";
            }
        }
        positionDisplay.setText(text);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "撤销").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(0, 2, 0, "重做").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(0, 3, 0, "查找").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(0, 4, 0, "目录").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(0, 5, 0, "属性").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(0, 6, 0, "保存").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == 1) {
            editor.undo();
            return true;
        } else if (id == 2) {
            editor.redo();
            return true;
        } else if (id == 3) {
            toggleSearchPanel();
            return true;
        } else if (id == 4) {
            showTOC();
            return true;
        } else if (id == 5) {
            showStats();
            return true;
        } else if (id == 6) {
            saveAndExit();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleSearchPanel() {
        if (searchPanel.getVisibility() == View.GONE) {
            searchPanel.setVisibility(View.VISIBLE);
        } else {
            searchPanel.setVisibility(View.GONE);
            editor.getSearcher().stopSearch();
        }
    }

    private void saveAndExit() {
        if (filePath != null) {
            try {
                ContentIO.writeTo(editor.getText(), new FileOutputStream(new File(filePath)), true);
                isDirty = false;
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }
        }
        
        Intent result = new Intent();
        setResult(RESULT_OK, result);
        finish();
    }

    private void showTOC() {
        String text = editor.getText().toString();
        List<String> chapters = new ArrayList<>();
        List<Integer> positions = new ArrayList<>();
        
        String[] lines = text.split("\n");
        int currentPos = 0;
        for (String line : lines) {
            if (line.trim().startsWith("#")) {
                chapters.add(line.trim());
                positions.add(currentPos);
            }
            currentPos += line.length() + 1;
        }

        if (chapters.isEmpty()) {
            Toast.makeText(this, "未发现标题 (#)", Toast.LENGTH_SHORT).show();
            return;
        }
        
        new AlertDialog.Builder(this)
            .setTitle("目录")
            .setItems(chapters.toArray(new String[0]), (dialog, which) -> {
                editor.setSelection(positions.get(which), positions.get(which));
            })
            .show();
    }

    private void showStats() {
        int chars = editor.getText().length();
        int lines = editor.getLineCount();
        String stats = String.format("字数: %d\n行数: %d", chars, lines);
        new AlertDialog.Builder(this)
            .setTitle("属性")
            .setMessage(stats)
            .setPositiveButton("确定", null)
            .show();
    }

    @Override
    public void onBackPressed() {
        if (isDirty) {
            new AlertDialog.Builder(this)
                .setTitle("提示")
                .setMessage("保存并退出吗？")
                .setPositiveButton("保存", (dialog, which) -> saveAndExit())
                .setNegativeButton("不保存", (dialog, which) -> finish())
                .setNeutralButton("取消", null)
                .show();
        } else {
            finish();
        }
    }
}
