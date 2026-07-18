package com.sirithree.shopops.admin.auth.service;

import com.sirithree.shopops.admin.auth.domain.LoginParam;
import com.sirithree.shopops.admin.auth.domain.LoginResult;

public interface AuthService {
    LoginResult login(LoginParam param);
}
