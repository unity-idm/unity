/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.webauthz;

import com.nimbusds.oauth2.sdk.client.ClientType;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.idp.ActiveValueClientHelper;
import pl.edu.icm.unity.engine.api.idp.CommonIdPProperties;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext;
import pl.edu.icm.unity.oauth.as.OAuthAuthzContext.Prompt;
import pl.edu.icm.unity.oauth.as.preferences.OAuthPreferences.OAuthClientSettings;
import pl.edu.icm.unity.types.basic.EntityParam;

import java.util.Arrays;

class ASConsentDecider
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, ASConsentDecider.class);

	private final EnquiryManagement enquiryManagement;
	private final PolicyAgreementManagement policyAgreementsMan;
	private final MessageSource msg;


	ASConsentDecider(EnquiryManagement enquiryManagement, PolicyAgreementManagement policyAgreementsMan,
			MessageSource msg)
	{
		this.enquiryManagement = enquiryManagement;
		this.policyAgreementsMan = policyAgreementsMan;
		this.msg = msg;
	}

	boolean isNonePrompt(OAuthAuthzContext oauthCtx)
	{
		return oauthCtx.getPrompts().contains(Prompt.NONE);	
	}
	
	boolean forceConsentIfConsentPrompt(OAuthAuthzContext oauthCtx)
	{
		return oauthCtx.getPrompts().contains(Prompt.CONSENT);
	}
	
	
	boolean isInteractiveUIRequired(OAuthClientSettings preferences, OAuthAuthzContext oauthCtx)
	{
		return isConsentRequired(preferences, oauthCtx) || isActiveValueSelectionRequired(oauthCtx)
				|| isEnquiryWaiting() || isPolicyAgreementWaiting(oauthCtx);
	}

	private boolean isActiveValueSelectionRequired(OAuthAuthzContext oauthCtx)
	{
		return ActiveValueClientHelper.isActiveValueSelectionConfiguredForClient(oauthCtx.getConfig().getActiveValueClients(),
				oauthCtx.getClientUsername());
	}

	/**
	 * According to native OAuth profile, public clients needs to have consent shown
	 * regardless of user's saved "trust" for the client. Still we honor admin
	 * setting disabling consent globally.
	 */
	private boolean isConsentRequired(OAuthClientSettings preferences, OAuthAuthzContext oauthCtx)
	{
		if (preferences.isDoNotAsk() && oauthCtx.getClientType() == ClientType.CONFIDENTIAL)
			return areScopesChanged(preferences, oauthCtx) || isAudienceChanged(preferences, oauthCtx);

		return areScopesChanged(preferences, oauthCtx) || isAudienceChanged(preferences, oauthCtx)
				|| !oauthCtx.getConfig().isSkipConsent();
	}
	
	private boolean areScopesChanged(OAuthClientSettings preferences, OAuthAuthzContext oauthCtx)
	{
		return !preferences.getEffectiveRequestedScopes()
				.containsAll(Arrays.asList(oauthCtx.getEffectiveRequestedScopesList()));
	}

	
	private boolean isAudienceChanged(OAuthClientSettings preferences, OAuthAuthzContext oauthCtx)
	{
		return !preferences.getAudience()
				.containsAll(oauthCtx.getAdditionalAudience());
	}

	private boolean isEnquiryWaiting()
	{
		LoginSession ae = InvocationContext.getCurrent().getLoginSession();
		EntityParam entity = new EntityParam(ae.getEntityId());
		try
		{
			return !enquiryManagement.getPendingEnquires(entity).isEmpty();
		} catch (EngineException e)
		{
			log.warn("Can't retrieve pending enquiries for user", e);
			return false;
		}
	}

	private boolean isPolicyAgreementWaiting(OAuthAuthzContext oauthCtx)
	{
		try
		{
			return !policyAgreementsMan
					.filterAgreementToPresent(
							new EntityParam(InvocationContext.getCurrent().getLoginSession().getEntityId()),
							CommonIdPProperties.getPolicyAgreementsConfig(msg, oauthCtx.getConfig()).agreements)
					.isEmpty();
		} catch (EngineException e)
		{
			log.error("Unable to determine policy agreements to accept");
		}
		return false;
	}

}
