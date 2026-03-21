package com.cdd.auth.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cdd.auth")
public class AuthServiceProperties {

    private List<BootstrapAccount> bootstrapAccounts = new ArrayList<>();

    public List<BootstrapAccount> getBootstrapAccounts() {
        return bootstrapAccounts;
    }

    public void setBootstrapAccounts(List<BootstrapAccount> bootstrapAccounts) {
        this.bootstrapAccounts = bootstrapAccounts;
    }

    public static class BootstrapAccount {

        private String userId;
        private String accountType;
        private String accountName;
        private String password;
        private String displayName;
        private String merchantId;
        private String storeId;
        private String miniProgramId;
        private List<String> roleCodes = new ArrayList<>();

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getAccountType() {
            return accountType;
        }

        public void setAccountType(String accountType) {
            this.accountType = accountType;
        }

        public String getAccountName() {
            return accountName;
        }

        public void setAccountName(String accountName) {
            this.accountName = accountName;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getMerchantId() {
            return merchantId;
        }

        public void setMerchantId(String merchantId) {
            this.merchantId = merchantId;
        }

        public String getStoreId() {
            return storeId;
        }

        public void setStoreId(String storeId) {
            this.storeId = storeId;
        }

        public String getMiniProgramId() {
            return miniProgramId;
        }

        public void setMiniProgramId(String miniProgramId) {
            this.miniProgramId = miniProgramId;
        }

        public List<String> getRoleCodes() {
            return roleCodes;
        }

        public void setRoleCodes(List<String> roleCodes) {
            this.roleCodes = roleCodes;
        }
    }
}
