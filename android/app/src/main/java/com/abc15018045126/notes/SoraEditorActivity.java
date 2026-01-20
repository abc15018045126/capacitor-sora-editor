package com.abc15018045126.notes;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage;
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class SoraEditorActivity extends AppCompatActivity {

    private CodeEditor editor;
    private String filePath;
    private String originalContent;

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
        
        // 基本设置
        editor.setLineNumberEnabled(true);
        // editor.setEdgeEnabled(false); // Method not found in this version
        editor.setWordwrap(true);
        editor.setTextSize(18); // 默认 18sp
        
        Intent intent = getIntent();
        filePath = intent.getStringExtra("path");
        originalContent = intent.getStringExtra("content");
        String title = intent.getStringExtra("title");
        
        if (title != null) {
            getSupportActionBar().setTitle(title);
        }

        if (originalContent != null) {
            editor.setText(originalContent);
        }

        // 应用默认方案
        // editor.setColorScheme(new EditorColorScheme()); // Failing currently
        // SoraEditor 0.23+ 默认包含一些颜色配置，可以通过代码进一步调整
        
        // 如果需要 TextMate 语法高亮，可以在这里配置
        // 但为了简单，我们先保证基础功能
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "撤销").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(0, 2, 0, "重做").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(0, 3, 0, "保存").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == 1) {
            editor.undo();
            return true;
        } else if (item.getItemId() == 2) {
            editor.redo();
            return true;
        } else if (item.getItemId() == 3) {
            saveAndExit();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveAndExit() {
        String content = editor.getText().toString();
        if (filePath != null) {
            try (FileOutputStream fos = new FileOutputStream(new File(filePath))) {
                fos.write(content.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        Intent result = new Intent();
        result.putExtra("content", content);
        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    public void onBackPressed() {
        String content = editor.getText().toString();
        if (!content.equals(originalContent)) {
            // 这里可以弹窗提示保存，或者直接保存
            saveAndExit();
        } else {
            finish();
        }
    }
}
