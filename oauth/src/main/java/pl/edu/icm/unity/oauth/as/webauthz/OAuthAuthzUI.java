/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.nimbusds.oauth2.sdk.AuthorizationErrorResponse;
import com.nimbusds.oauth2.sdk.AuthorizationSuccessResponse;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.vaadin.annotations.Theme;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.idp.CommonIdPProperties;
import pl.edu.icm.unity.engine.api.idp.CommonIdPProperties.ActiveValueSelectionConfig;
import pl.edu.icm.unity.engine.api.idp.IdPEngine;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext;
import pl.edu.icm.unity.oauth.as.OAuthErrorResponseException;
import pl.edu.icm.unity.oauth.as.OAuthProcessor;
import pl.edu.icm.unity.types.basic.DynamicAttribute;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.webui.UnityEndpointUIBase;
import pl.edu.icm.unity.webui.authn.StandardWebAuthenticationProcessor;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.forms.enquiry.EnquiresDialogLauncher;
import pl.edu.icm.unity.webui.idpcommon.EopException;
import pl.edu.icm.unity.webui.idpcommon.activesel.ActiveValueSelectionScreen;

/**
 * UI of the authorization endpoint. Presents active value selection for attributes if configured. 
 * When attributes are obtained then consent screen is presented.
 *  
 * @author K. Benedyczak
 */
@Component("OAuthAuthzUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Theme("unityThemeValo")
public class OAuthAuthzUI extends UnityEndpointUIBase 
{
	private static Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OAuthAuthzUI.class);

	private final UnityMessageSource msg;
	private final TokensManagement tokensMan;
	private final OAuthIdPEngine idpEngine;
	private final AttributeHandlerRegistry handlersRegistry;
	private final PreferencesManagement preferencesMan;
	private final StandardWebAuthenticationProcessor authnProcessor;
	private final IdentityTypeSupport idTypeSupport;
	private final AttributeTypeSupport aTypeSupport; 
	private final SessionManagement sessionMan;

	private OAuthResponseHandler oauthResponseHandler;

	private IdentityParam identity; 

	@Autowired
	public OAuthAuthzUI(UnityMessageSource msg, TokensManagement tokensMan,
			AttributeHandlerRegistry handlersRegistry,
			PreferencesManagement preferencesMan,
			StandardWebAuthenticationProcessor authnProcessor, IdPEngine idpEngine,
			EnquiresDialogLauncher enquiryDialogLauncher,
			IdentityTypeSupport idTypeSupport, AttributeTypeSupport aTypeSupport,
			SessionManagement sessionMan)
	{
		super(msg, enquiryDialogLauncher);
		this.msg = msg;
		this.handlersRegistry = handlersRegistry;
		this.preferencesMan = preferencesMan;
		this.authnProcessor = authnProcessor;
		this.sessionMan = sessionMan;
		this.idpEngine = new OAuthIdPEngine(idpEngine);
		this.tokensMan = tokensMan;
		this.idTypeSupport = idTypeSupport;
		this.aTypeSupport = aTypeSupport;
	}

	@Override
	protected void enter(VaadinRequest request)
	{
		OAuthAuthzContext ctx = OAuthContextUtils.getContext();
		OAuthASProperties config = ctx.getConfig();
		
		TranslationResult translationResult;
		try
		{
			translationResult = getTranslationResult(ctx);
			handleRedirectIfNeeded(translationResult);
		} catch (EopException e)
		{
			return;
		}
		identity = idpEngine.getIdentity(translationResult, ctx.getConfig().getSubjectIdentityType());

		OAuthProcessor oauthProcessor = new OAuthProcessor();
		Set<DynamicAttribute> allAttributes = oauthProcessor.filterAttributes(translationResult, 
				ctx.getEffectiveRequestedAttrs());
		
		Optional<ActiveValueSelectionConfig> activeValueSelectionConfig = 
				CommonIdPProperties.getActiveValueSelectionConfig(config, ctx.getClientUsername(),
						allAttributes);
		
		if (activeValueSelectionConfig.isPresent())
			showActiveValueSelectionScreen(activeValueSelectionConfig.get());
		else
			gotoConsentStage(allAttributes);
	}
	
	private void gotoConsentStage(Collection<DynamicAttribute> attributes)
	{
		if (OAuthContextUtils.getContext().getConfig().isSkipConsent())
		{
			onFinalConfirm(identity, attributes);
			return;
		}
		OAuthConsentScreen consentScreen = new OAuthConsentScreen(msg, handlersRegistry, 
				preferencesMan, authnProcessor, 
				idTypeSupport, 
				aTypeSupport, sessionMan, 
				identity, attributes,
				this::onDecline,
				this::onFinalConfirm);
		setContent(consentScreen);
	}

	private void showActiveValueSelectionScreen(ActiveValueSelectionConfig config)
	{
		ActiveValueSelectionScreen valueSelectionScreen = new ActiveValueSelectionScreen(msg, 
				handlersRegistry, authnProcessor, 
				config.singleSelectableAttributes, config.multiSelectableAttributes,
				config.remainingAttributes,
				this::onDecline,
				this::gotoConsentStage);
		setContent(valueSelectionScreen);
	}

	private TranslationResult getTranslationResult(OAuthAuthzContext ctx) throws EopException
	{
		oauthResponseHandler = new OAuthResponseHandler(sessionMan);
		try
		{
			return idpEngine.getUserInfo(ctx);
		} catch (OAuthErrorResponseException e)
		{
			oauthResponseHandler.returnOauthResponse(e.getOauthResponse(), e.isInvalidateSession());
		} catch (Exception e)
		{
			log.error("Engine problem when handling client request", e);
			//we kill the session as the user may want to log as different user 
			//if has access to several entities.
			AuthorizationErrorResponse oauthResponse = new AuthorizationErrorResponse(ctx.getReturnURI(), 
					OAuth2Error.SERVER_ERROR, ctx.getRequest().getState(),
					ctx.getRequest().impliedResponseMode());
			oauthResponseHandler.returnOauthResponse(oauthResponse, true);
		}
		return null; //not reachable
	}
	
	private void handleRedirectIfNeeded(TranslationResult userInfo) throws EopException 
	{
		String redirectURL = userInfo.getRedirectURL();
		if (redirectURL != null)
		{
			Page.getCurrent().open(redirectURL, null);
			throw new EopException();
		}
	}
	
	private void onDecline()
	{
		OAuthAuthzContext ctx = OAuthContextUtils.getContext();
		AuthorizationErrorResponse oauthResponse = new AuthorizationErrorResponse(ctx.getReturnURI(), 
				OAuth2Error.ACCESS_DENIED, ctx.getRequest().getState(),
				ctx.getRequest().impliedResponseMode());
		oauthResponseHandler.returnOauthResponseNotThrowing(oauthResponse, false);
	}
	
	private void onFinalConfirm(IdentityParam identity, Collection<DynamicAttribute> attributes)
	{
		OAuthAuthzContext ctx = OAuthContextUtils.getContext();
		try
		{
			AuthorizationSuccessResponse oauthResponse = new OAuthProcessor().
					prepareAuthzResponseAndRecordInternalState(attributes, identity, ctx, tokensMan);
			
			oauthResponseHandler.returnOauthResponseNotThrowing(oauthResponse, false);
		} catch (Exception e)
		{
			log.error("Error during OAuth processing", e);
			AuthorizationErrorResponse oauthResponse = new AuthorizationErrorResponse(ctx.getReturnURI(), 
					OAuth2Error.SERVER_ERROR, ctx.getRequest().getState(),
					ctx.getRequest().impliedResponseMode());
			oauthResponseHandler.returnOauthResponseNotThrowing(oauthResponse, false);
		}
	}
}
