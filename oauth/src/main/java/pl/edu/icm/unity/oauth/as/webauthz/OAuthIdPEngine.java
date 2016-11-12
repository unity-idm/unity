/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import org.apache.log4j.Logger;

import com.nimbusds.oauth2.sdk.AuthorizationErrorResponse;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext;
import pl.edu.icm.unity.oauth.as.OAuthErrorResponseException;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.server.api.internal.CommonIdPProperties;
import pl.edu.icm.unity.server.api.internal.IdPEngine;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.translation.ExecutionFailException;
import pl.edu.icm.unity.server.translation.out.TranslationResult;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.IdentityParam;

/**
 * Wraps {@link IdPEngine} with code used by OAuth AS. In the first place provides standard error handling.
 * 
 * @author K. Benedyczak
 */
public class OAuthIdPEngine
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OAuthIdPEngine.class);
	
	private IdPEngine idpEngine;
	
	public OAuthIdPEngine(IdPEngine idpEngine)
	{
		this.idpEngine = idpEngine;
	}

	public TranslationResult getUserInfo(OAuthAuthzContext ctx) throws OAuthErrorResponseException
	{
		try
		{
			return getUserInfoUnsafe(ctx);
		} catch (ExecutionFailException e)
		{
			log.debug("Authentication failed due to profile's decision, returning error");
			ErrorObject eo = new ErrorObject("access_denied", 
					e.getMessage(), HTTPResponse.SC_FORBIDDEN);
			AuthorizationErrorResponse oauthResponse = new AuthorizationErrorResponse(ctx.getReturnURI(), 
					eo, ctx.getRequest().getState(), ctx.getRequest().impliedResponseMode());
			throw new OAuthErrorResponseException(oauthResponse, true);
		} catch (IllegalGroupValueException igve)
		{
			log.debug("Entity trying to access OAuth resource is not a member of required group");
			ErrorObject eo = new ErrorObject("access_denied", 
					"Not a member of required group " + ctx.getUsersGroup(), 
					HTTPResponse.SC_FORBIDDEN);
			AuthorizationErrorResponse oauthResponse = new AuthorizationErrorResponse(ctx.getReturnURI(), 
					eo, ctx.getRequest().getState(), ctx.getRequest().impliedResponseMode());
			throw new OAuthErrorResponseException(oauthResponse, true);
		} catch (Exception e)
		{
			log.error("Engine problem when handling client request", e);
			//we kill the session as the user may want to log as different user if has access to several entities.
			AuthorizationErrorResponse oauthResponse = new AuthorizationErrorResponse(ctx.getReturnURI(), 
					OAuth2Error.SERVER_ERROR, ctx.getRequest().getState(),
					ctx.getRequest().impliedResponseMode());
			throw new OAuthErrorResponseException(oauthResponse, true);
		}
	}
	
	public IdentityParam getIdentity(TranslationResult userInfo, String subjectIdentityType) 
			throws EngineException
	{
		for (IdentityParam id: userInfo.getIdentities())
			if (subjectIdentityType.equals(id.getTypeId()))
				return id;
		throw new IllegalStateException("There is no " + subjectIdentityType + " identity "
				+ "for the authenticated user, sub claim can not be created. "
				+ "Probably the endpoint is misconfigured.");
	}
	
	private TranslationResult getUserInfoUnsafe(OAuthAuthzContext ctx) 
			throws EngineException
	{
		LoginSession ae = InvocationContext.getCurrent().getLoginSession();
		String flow = ctx.getRequest().getResponseType().impliesCodeFlow() ? 
				GrantFlow.authorizationCode.toString() : GrantFlow.implicit.toString();
		Boolean skipImport = ctx.getConfig().getBooleanValue(CommonIdPProperties.SKIP_USERIMPORT);
		TranslationResult translationResult = idpEngine.obtainUserInformation(new EntityParam(ae.getEntityId()), 
				ctx.getUsersGroup(), 
				ctx.getTranslationProfile(), 
				ctx.getRequest().getClientID().getValue(),
				"OAuth2", 
				flow,
				true,
				!skipImport);
		return translationResult;
	}
}
