package com.sirithree.shopops.admin.auth.service;

import com.sirithree.shopops.admin.auth.domain.TokenPrincipal;
import com.sirithree.shopops.admin.auth.domain.TokenSessionCreateCommand;

public interface TokenSessionService {
    void createSession(TokenSessionCreateCommand command);

    boolean validateAndTouch(TokenPrincipal principal);

    boolean revoke(String tokenId);
}
