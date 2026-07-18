package com.sirithree.shopops.admin.auth.controller;

import com.sirithree.shopops.admin.auth.domain.CurrentUserDto;
import com.sirithree.shopops.admin.common.context.RequestContextHolder;
import com.sirithree.shopops.common.api.CommonResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {
    @GetMapping("/me")
    public CommonResult<CurrentUserDto> me() {
        return CommonResult.success(CurrentUserDto.from(RequestContextHolder.current()));
    }
}
