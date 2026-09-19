package com.tungsten.fcl.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.tungsten.fcl.auth.AccountManager;

/**
 * 账户管理页面 — 仿 FCL Accounts
 *
 * 支持离线账户和微软账户管理。
 */
public class AccountActivity extends AppCompatActivity {

    private AccountManager accountManager;

    private EditText etUsername;
    private TextView tvCurrentAccount;
    private LinearLayout accountListContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        accountManager = AccountManager.getInstance();
        accountManager.loadFromPreferences(this);
        setupUI();
    }

    private void setupUI() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 48, 48, 48);

        // 标题
        TextView title = new TextView(this);
        title.setText("账户管理");
        title.setTextSize(22);
        title.setPadding(0, 16, 0, 24);
        root.addView(title);

        // 当前账户
        tvCurrentAccount = new TextView(this);
        tvCurrentAccount.setTextSize(14);
        tvCurrentAccount.setPadding(0, 8, 0, 16);
        root.addView(tvCurrentAccount);

        // 离线账户创建
        TextView offlineTitle = new TextView(this);
        offlineTitle.setText("创建离线账户");
        offlineTitle.setTextSize(16);
        offlineTitle.setTypeface(offlineTitle.getTypeface(), android.graphics.Typeface.BOLD);
        offlineTitle.setPadding(0, 16, 0, 8);
        root.addView(offlineTitle);

        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);

        etUsername = new EditText(this);
        etUsername.setHint("游戏ID");
        etUsername.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        inputRow.addView(etUsername);

        Button btnAdd = new MaterialButton(this);
        btnAdd.setText("添加");
        btnAdd.setOnClickListener(v -> addOfflineAccount());
        inputRow.addView(btnAdd);

        root.addView(inputRow);

        // 账户列表
        TextView listTitle = new TextView(this);
        listTitle.setText("已保存的账户");
        listTitle.setTextSize(16);
        listTitle.setTypeface(listTitle.getTypeface(), android.graphics.Typeface.BOLD);
        listTitle.setPadding(0, 24, 0, 8);
        root.addView(listTitle);

        accountListContainer = new LinearLayout(this);
        accountListContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(accountListContainer);

        // 微软登录 (TODO)
        TextView msTitle = new TextView(this);
        msTitle.setText("微软账户");
        msTitle.setTextSize(16);
        msTitle.setTypeface(msTitle.getTypeface(), android.graphics.Typeface.BOLD);
        msTitle.setPadding(0, 24, 0, 8);
        root.addView(msTitle);

        Button btnMSLogin = new MaterialButton(this);
        btnMSLogin.setText("微软账户登录 (开发中)");
        btnMSLogin.setEnabled(false);
        root.addView(btnMSLogin);

        // 说明
        TextView note = new TextView(this);
        note.setText("\n说明:\n• 离线账户可直接使用，不需要微软账户\n• 微软账户登录功能开发中");
        note.setTextSize(12);
        note.setTextColor(0xFF888888);
        note.setPadding(0, 16, 0, 0);
        root.addView(note);

        scrollView.addView(root);
        setContentView(scrollView);
        refreshAccountList();
    }

    private void addOfflineAccount() {
        String username = etUsername.getText().toString().trim();
        if (username.isEmpty()) {
            Toast.makeText(this, "请输入用户名", Toast.LENGTH_SHORT).show();
            return;
        }

        AccountManager.Account account = accountManager.createOfflineAccount(username);
        accountManager.addAccount(account);
        accountManager.setCurrentAccount(account);
        accountManager.saveToPreferences(this);

        etUsername.setText("");
        Toast.makeText(this, "账户已添加: " + username, Toast.LENGTH_SHORT).show();
        refreshAccountList();
    }

    private void refreshAccountList() {
        accountListContainer.removeAllViews();

        AccountManager.Account current = accountManager.getCurrentAccount();
        if (current != null) {
            tvCurrentAccount.setText("当前账户: " + current.username + " (" + current.accountType + ")");
        } else {
            tvCurrentAccount.setText("当前账户: 无");
        }

        for (AccountManager.Account account : accountManager.getAccounts()) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setGravity(Gravity.CENTER_VERTICAL);
            item.setPadding(16, 12, 16, 12);

            TextView name = new TextView(this);
            name.setText(account.username + " (" + account.accountType + ")");
            name.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            item.addView(name);

            if (current != null && current.username.equals(account.username)) {
                TextView currentMark = new TextView(this);
                currentMark.setText("✓");
                item.addView(currentMark);
            } else {
                Button btnSelect = new MaterialButton(this);
                btnSelect.setText("选择");
                btnSelect.setOnClickListener(v -> {
                    accountManager.setCurrentAccount(account);
                    accountManager.saveToPreferences(this);
                    refreshAccountList();
                });
                item.addView(btnSelect);
            }

            Button btnRemove = new MaterialButton(this);
            btnRemove.setText("删除");
            btnRemove.setOnClickListener(v -> {
                accountManager.removeAccount(account);
                accountManager.saveToPreferences(this);
                refreshAccountList();
            });
            item.addView(btnRemove);

            accountListContainer.addView(item);
        }

        if (accountManager.getAccounts().isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("尚未添加任何账户");
            empty.setPadding(0, 16, 0, 16);
            accountListContainer.addView(empty);
        }
    }
}
