package com.example;

import com.stormpath.sdk.application.Application;
import com.stormpath.sdk.impl.error.DefaultError;
import com.stormpath.sdk.impl.http.MediaType;
import com.stormpath.sdk.lang.Assert;
import com.stormpath.sdk.lang.Strings;
import com.stormpath.sdk.oauth.*;
import com.stormpath.sdk.resource.ResourceException;
import com.stormpath.sdk.servlet.filter.oauth.OAuthErrorCode;
import com.stormpath.sdk.servlet.filter.oauth.OAuthException;
import com.stormpath.sdk.servlet.mvc.AbstractController;
import com.stormpath.sdk.servlet.mvc.ViewModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.stormpath.sdk.servlet.filter.oauth.DefaultAccessTokenRequestAuthorizer.FORM_MEDIA_TYPE;

//@RestController
public class RevokeTokenController extends AbstractController {

    private static final Logger log = LoggerFactory.getLogger(RevokeTokenController.class);

    private final static String TOKEN = "token";

    private final static String TOKEN_TYPE_HINT = "token_type_hint";

    @Override
    //@PostMapping("/oauth/revoke")
    protected ViewModel doPost(HttpServletRequest request, HttpServletResponse response) throws Exception {

        OAuthRevocationRequestBuilder builder = OAuthRequests.OAUTH_TOKEN_REVOCATION_REQUEST.builder();

        response.setHeader("Cache-Control", "no-store, no-cache");
        response.setHeader("Pragma", "no-cache");

        try {
            //Form media type is required: https://tools.ietf.org/html/rfc6749#section-4.3.2
            String contentType = Strings.clean(request.getContentType());
            if (contentType == null || !contentType.startsWith(FORM_MEDIA_TYPE)) {
                String msg = "Content-Type must be " + FORM_MEDIA_TYPE;
                throw new OAuthException(OAuthErrorCode.INVALID_REQUEST, msg, null);
            }

            String tokenTypeHint = request.getParameter(TOKEN_TYPE_HINT);

            if (Strings.hasText(tokenTypeHint)) {
                builder.setTokenTypeHint(TokenTypeHint.fromValue(tokenTypeHint));
            }

            String token = request.getParameter(TOKEN);

            if (!Strings.hasText(token)) {
                throw new OAuthException(OAuthErrorCode.INVALID_REQUEST);
            }

            this.revoke(getApplication(request), builder.setToken(token).build());

            response.setStatus(HttpServletResponse.SC_OK);
            response.setHeader("Content-Length", "0");

        } catch (OAuthException e) {

            log.debug("Error occurred revoking token: {}", e.getMessage());

            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

            String json = e.toJson();

            response.setHeader("Content-Length", String.valueOf(json.length()));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().print(json);
            response.getWriter().flush();
        }
        return null;
    }

    private void revoke(Application application, OAuthRevocationRequest request) throws OAuthException {
        try {
            OAuthTokenRevocators.OAUTH_TOKEN_REVOCATOR.forApplication(application).revoke(request);
        } catch (ResourceException e) {
            com.stormpath.sdk.error.Error error = e.getStormpathError();
            String message = error.getMessage();

            OAuthErrorCode oauthError = OAuthErrorCode.INVALID_REQUEST;
            if (error instanceof DefaultError) {
                Object errorObject = ((DefaultError) error).getProperty("error");
                oauthError = errorObject == null ? oauthError : new OAuthErrorCode(errorObject.toString());
            }

            throw new OAuthException(oauthError, message);
        }
    }

    @Override
    public boolean isNotAllowedIfAuthenticated() {
        return false;
    }

    protected Application getApplication(HttpServletRequest request) {
        Application application = (Application) request.getAttribute(Application.class.getName());
        Assert.notNull(application, "request must have an application attribute.");
        return application;
    }
}