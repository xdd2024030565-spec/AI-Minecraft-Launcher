package com.tungsten.fcl.auth;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 账户管理器 — 仿 FCL Accounts
 *
 * 管理 Minecraft 账户 (微软账户 / 离线账户)。
 * 支持持久化存储到 SharedPreferences。
 * TODO: 实现微软 OAuth 登录流程和 Token 刷新
 */
public class AccountManager {

    private static AccountManager instance;
    private final List<Account> accounts = new ArrayList<>();
    private Account currentAccount;

    private AccountManager() {}

    public static AccountManager getInstance() {
        if (instance == null) {
            synchronized (AccountManager.class) {
                if (instance == null) {
                    instance = new AccountManager();
                }
            }
        }
        return instance;
    }

    public List<Account> getAccounts() {
        return Collections.unmodifiableList(accounts);
    }

    public Account getCurrentAccount() { return currentAccount; }

    public void setCurrentAccount(Account account) { this.currentAccount = account; }

    /**
     * 添加账户
     */
    public void addAccount(Account account) {
        accounts.add(account);
    }

    /**
     * 移除账户
     */
    public void removeAccount(Account account) {
        accounts.remove(account);
        if (currentAccount == account) {
            currentAccount = accounts.isEmpty() ? null : accounts.get(0);
        }
    }

    /**
     * 创建离线账户
     */
    public Account createOfflineAccount(String username) {
        String uuid = java.util.UUID.nameUUIDFromBytes(
            ("OfflinePlayer:" + username).getBytes()
        ).toString();
        return new Account(username, uuid, "offline", "offline");
    }

    // === 持久化 ===

    /**
     * 从 SharedPreferences 加载账户
     */
    public void loadFromPreferences(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("fcl_accounts", Context.MODE_PRIVATE);
        String json = prefs.getString("accounts", "");
        if (!json.isEmpty()) {
            try {
                List<Account> saved = new Gson().fromJson(json, new TypeToken<List<Account>>(){}.getType());
                if (saved != null) {
                    accounts.clear();
                    accounts.addAll(saved);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        String currentUuid = prefs.getString("current_account_uuid", "");
        if (!currentUuid.isEmpty()) {
            for (Account a : accounts) {
                if (a.uuid.equals(currentUuid)) {
                    currentAccount = a;
                    break;
                }
            }
        }
        // 默认创建一个离线账户
        if (accounts.isEmpty()) {
            Account offline = createOfflineAccount("Player");
            accounts.add(offline);
            currentAccount = offline;
            saveToPreferences(context);
        }
    }

    /**
     * 保存账户到 SharedPreferences
     */
    public void saveToPreferences(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("fcl_accounts", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("accounts", new Gson().toJson(accounts));
        editor.putString("current_account_uuid", currentAccount != null ? currentAccount.uuid : "");
        editor.apply();
    }

    /**
     * 账户数据类
     */
    public static class Account {
        public String username;
        public String uuid;
        public String accessToken;
        public String accountType; // "microsoft" or "offline"

        public Account(String username, String uuid, String accessToken, String accountType) {
            this.username = username;
            this.uuid = uuid;
            this.accessToken = accessToken;
            this.accountType = accountType;
        }
    }
}
