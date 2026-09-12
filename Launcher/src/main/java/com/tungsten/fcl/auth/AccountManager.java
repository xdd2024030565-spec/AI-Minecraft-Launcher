package com.tungsten.fcl.auth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 账户管理器
 *
 * 管理 Minecraft 账户 (微软账户 / 离线账户)。
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
     * 创建离线账户 (正版登录可实现前先用)
     */
    public Account createOfflineAccount(String username) {
        String uuid = java.util.UUID.nameUUIDFromBytes(
            ("OfflinePlayer:" + username).getBytes()
        ).toString();
        return new Account(username, uuid, "offline", "offline");
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
