package com.sirithree.shopops.admin.auth.controller;

import com.sirithree.shopops.admin.auth.domain.CurrentUserDto;
import com.sirithree.shopops.admin.auth.domain.LoginParam;
import com.sirithree.shopops.admin.auth.domain.LoginResult;
import com.sirithree.shopops.admin.auth.service.AuthService;
import com.sirithree.shopops.admin.common.context.RequestContextHolder;
import com.sirithree.shopops.common.api.CommonResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {
    private final AuthService authService;

    public AdminAuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public CommonResult<LoginResult> login(@Valid @RequestBody LoginParam param) {
        return CommonResult.success(authService.login(param));
    }

    @GetMapping("/me")
    public CommonResult<CurrentUserDto> me() {
        return CommonResult.success(CurrentUserDto.from(RequestContextHolder.current()));
    }
}
