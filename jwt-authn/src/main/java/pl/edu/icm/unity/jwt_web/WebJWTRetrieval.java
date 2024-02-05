/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.jwt_web;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.hc.core5.net.URIBuilder;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.server.Resource;
import com.vaadin.ui.Label;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialRetrieval;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialRetrievalFactory;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.ResolvableError;
import pl.edu.icm.unity.engine.api.authn.AuthenticationStepContext;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorStepContext;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.RememberMeToken.LoginMachineDetails;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.remote.AuthenticationTriggeringContext;
import pl.edu.icm.unity.engine.api.authn.remote.RedirectedAuthnState;
import pl.edu.icm.unity.engine.api.authn.remote.SharedRemoteAuthenticationContextStore;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.engine.api.utils.URIBuilderFixer;
import pl.edu.icm.unity.oauth.client.OAuthContext;
import pl.edu.icm.unity.rest.jwt.authn.JWTExchange;
import pl.edu.icm.unity.types.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.webui.authn.LoginMachineDetailsExtractor;
import pl.edu.icm.unity.webui.authn.PreferredAuthenticationHelper;
import pl.edu.icm.unity.webui.authn.ProxyAuthenticationCapable;
import pl.edu.icm.unity.webui.authn.ProxyAuthenticationFilter;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.authn.remote.RemoteRedirectedAuthnResponseProcessingFilter;

@PrototypeComponent
public class WebJWTRetrieval extends AbstractCredentialRetrieval<JWTExchange> implements VaadinAuthentication, ProxyAuthenticationCapable
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_WEB, WebJWTRetrieval.class);
	public static final String NAME = "webjwt";
	public static final String DESC = "WebJWTRetrievalFactory.desc";
	
	private final SharedRemoteAuthenticationContextStore remoteAuthnContextStore;
	
	public WebJWTRetrieval(SharedRemoteAuthenticationContextStore remoteAuthnContextStore)
	{
		super(VaadinAuthentication.NAME);
		this.remoteAuthnContextStore = remoteAuthnContextStore;
	}
	
	@Component
	public static class Factory extends AbstractCredentialRetrievalFactory<WebJWTRetrieval>
	{
		@Autowired
		public Factory(ObjectFactory<WebJWTRetrieval> factory)
		{
			super(NAME, DESC, VaadinAuthentication.NAME, factory, JWTExchange.ID);
		}
	}

	@Override 
	public boolean triggerAutomatedAuthentication(HttpServletRequest httpRequest,
			HttpServletResponse httpResponse,
			String endpointPath,
			AuthenticatorStepContext authnContext) throws IOException
	{
		String token = getToken(httpRequest);
		if (token == null)
		{
			return false;
		} else
		{
			InvocationContext invocationContext = InvocationContext.getCurrent();
			String currentRelativeURI = ProxyAuthenticationFilter.getCurrentRelativeURL(httpRequest);
			LoginMachineDetails loginMachineDetails = LoginMachineDetailsExtractor.getLoginMachineDetailsFromCurrentRequest();
			AuthenticationOptionKey authnOptionId = getAuthnOptionId(getIdpConfigKey(httpRequest));
			AuthenticationStepContext authnStepContext = new AuthenticationStepContext(authnContext, authnOptionId);
			AuthenticationTriggeringContext authnTriggeringContext = AuthenticationTriggeringContext.authenticationTriggeredFirstFactor();
			
			RedirectedAuthnState baseAuthnContext = new RedirectedAuthnState(
					authnStepContext, 
					__ -> processResponse(token, invocationContext), 
					loginMachineDetails, 
					currentRelativeURI, 
					authnTriggeringContext);

			OAuthContext context = new OAuthContext(baseAuthnContext);
			context.setReturnUrl(currentRelativeURI);
			remoteAuthnContextStore.addAuthnContext(context);
			
			// TODO: is this needed?
			HttpSession session = httpRequest.getSession();
			session.setAttribute(ProxyAuthenticationFilter.AUTOMATED_LOGIN_FIRED, "true");

			httpResponse.sendRedirect(getRedirectWithContextIdParam(context.getReturnUrl(), context.getRelayState()));
			return true;
		}
	}

	private String getRedirectWithContextIdParam(String returnUrl,
			String relayState) throws IOException
	{
		try
		{
			URIBuilder uriBuilder = URIBuilderFixer.newInstance(returnUrl);
			uriBuilder.addParameter(RemoteRedirectedAuthnResponseProcessingFilter.CONTEXT_ID_HTTP_PARAMETER, relayState);
			return uriBuilder.build().toString();
		} catch (URISyntaxException e)
		{
			throw new IOException("Can't build return URL", e);
		}
	}

	private AuthenticationResult processResponse(String token, InvocationContext invocationContext)
	{
		try
		{
			// TODO: this is workaround for how jwt-simple works
			InvocationContext.setCurrent(invocationContext);
			return credentialExchange.checkJWT(token);
		} catch (Exception e)
		{
			LOG.error("Runtime error during JWT response processing or principal mapping", e);
			return RemoteAuthenticationResult.failed(null, e, new ResolvableError("WebJWTRetrieval.authnFailedError"));
		} finally
		{
			InvocationContext.setCurrent(null);
		}
	}
	
	private String getIdpConfigKey(HttpServletRequest httpRequest)
	{
		return httpRequest.getParameter(PreferredAuthenticationHelper.IDP_SELECT_PARAM);
	}
	
	private AuthenticationOptionKey getAuthnOptionId(String idpConfigKey)
	{
		return AuthenticationOptionKey.valueOf(idpConfigKey);
	}

	protected String getToken(HttpServletRequest httpRequest)
	{
		String aa = getTokenFromParam(httpRequest).or(() -> getTokenFromHeader(httpRequest)).orElse(null);
		if (aa == null)
			return null;
		int firstDot = aa.indexOf('.');
		if (firstDot == -1)
			return null;
		int secDot = aa.indexOf('.', firstDot+1);
		if (secDot == -1)
			return null;
		if (aa.indexOf('.', secDot+1) != -1)
			return null;
		return aa;
	}
	
	private Optional<String> getTokenFromParam(HttpServletRequest httpRequest)
	{
		return Optional.ofNullable(httpRequest.getParameter("token"));
	}
	
	private Optional<String> getTokenFromHeader(HttpServletRequest httpRequest)
	{
		String authHeader = httpRequest.getHeader("Authorization");
		if (authHeader == null || !authHeader.startsWith("Bearer "))
			return Optional.empty();
		return Optional.of(authHeader.substring(7));
	}

	@Override
	public void triggerAutomatedUIAuthentication(VaadinAuthenticationUI authenticatorUI)
	{
	}
	
	@Override
	public String getSerializedConfiguration()
	{
		return null;
	}

	@Override
	public void setSerializedConfiguration(String config)
	{
	}

	@Override
	public Collection<VaadinAuthenticationUI> createUIInstance(Context context,
			AuthenticatorStepContext authenticatorContext)
	{
		return List.of(new Retrievel());
	}

	@Override
	public boolean supportsGrid()
	{
		return false;
	}

	@Override
	public boolean isMultiOption()
	{
		return false;
	}
	
	private static class Retrievel implements VaadinAuthenticationUI
	{
		@Override
		public com.vaadin.ui.Component getComponent()
		{
			return new Label();
		}

		@Override
		public void setAuthenticationCallback(AuthenticationCallback callback)
		{
		}

		@Override
		public String getLabel()
		{
			return null;
		}

		@Override
		public Set<String> getTags()
		{
			return null;
		}

		@Override
		public Resource getImage()
		{
			return null;
		}

		@Override
		public void clear()
		{
		}

		@Override
		public String getId()
		{
			return "web-jwt";
		}

		@Override
		public void presetEntity(Entity authenticatedEntity)
		{
		}
	}
}
