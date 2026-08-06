package com.sirithree.shopops.admin.model.controller;

import com.sirithree.shopops.admin.auth.annotation.RequireRole;
import com.sirithree.shopops.admin.auth.domain.AuthRole;
import com.sirithree.shopops.admin.common.context.RequestContext;
import com.sirithree.shopops.admin.common.context.RequestContextHolder;
import com.sirithree.shopops.admin.model.domain.PromptEnableParam;
import com.sirithree.shopops.admin.model.domain.PromptRenderResult;
import com.sirithree.shopops.admin.model.domain.PromptRenderTestParam;
import com.sirithree.shopops.admin.model.domain.PromptTemplateDto;
import com.sirithree.shopops.admin.model.domain.PromptTemplateQueryParam;
import com.sirithree.shopops.admin.model.domain.PromptVersionParam;
import com.sirithree.shopops.admin.model.service.PromptTemplateService;
import com.sirithree.shopops.common.api.CommonPage;
import com.sirithree.shopops.common.api.CommonResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/prompts")
public class AdminPromptTemplateController {
    private final PromptTemplateService promptTemplateService;

    public AdminPromptTemplateController(PromptTemplateService promptTemplateService) {
        this.promptTemplateService = promptTemplateService;
    }

    @GetMapping
    @RequireRole(AuthRole.ADMIN)
    public CommonResult<CommonPage<PromptTemplateDto>> list(PromptTemplateQueryParam query) {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(promptTemplateService.list(context.getTenantId(), query));
    }

    @GetMapping("/{promptCode}")
    @RequireRole(AuthRole.ADMIN)
    public CommonResult<PromptTemplateDto> get(@PathVariable String promptCode,
                                               @RequestParam(required = false) String version) {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(promptTemplateService.get(context.getTenantId(), promptCode, version));
    }

    @PostMapping("/{promptCode}/versions")
    @RequireRole(AuthRole.ADMIN)
    public CommonResult<PromptTemplateDto> createVersion(@PathVariable String promptCode,
                                                         @Valid @RequestBody PromptVersionParam param) {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(promptTemplateService.createVersion(
                context.getTenantId(), context.getUserId(), promptCode, param));
    }

    @PostMapping("/{promptCode}/enable")
    @RequireRole(AuthRole.ADMIN)
    public CommonResult<PromptTemplateDto> enable(@PathVariable String promptCode,
                                                  @Valid @RequestBody PromptEnableParam param) {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(promptTemplateService.enableVersion(context.getTenantId(), promptCode, param));
    }

    @PostMapping("/{promptCode}/rollback")
    @RequireRole(AuthRole.ADMIN)
    public CommonResult<PromptTemplateDto> rollback(@PathVariable String promptCode,
                                                    @Valid @RequestBody PromptEnableParam param) {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(promptTemplateService.enableVersion(context.getTenantId(), promptCode, param));
    }

    @PostMapping("/{promptCode}/render-test")
    @RequireRole(AuthRole.ADMIN)
    public CommonResult<PromptRenderResult> renderTest(@PathVariable String promptCode,
                                                       @RequestBody PromptRenderTestParam param) {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(promptTemplateService.renderTest(context.getTenantId(), promptCode, param));
    }
}
