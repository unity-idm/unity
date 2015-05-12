/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.unicore.samlidp.web;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.saml.idp.FreemarkerHandler;
import pl.edu.icm.unity.saml.idp.ctx.SAMLAuthnContext;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences.SPSettings;
import pl.edu.icm.unity.saml.idp.web.filter.IdpConsentDeciderServlet;
import pl.edu.icm.unity.server.api.PreferencesManagement;
import pl.edu.icm.unity.server.api.internal.IdPEngine;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.registries.AttributeSyntaxFactoriesRegistry;
import pl.edu.icm.unity.unicore.samlidp.preferences.SamlPreferencesWithETD;

/**
 * Trivial extension of {@link IdpConsentDeciderServlet}, which uses UNICORE preferences instead of SAML preferences.
 * 
 * @author K. Benedyczak
 */
public class UnicoreIdpConsentDeciderServlet extends IdpConsentDeciderServlet
{
	public UnicoreIdpConsentDeciderServlet(PreferencesManagement preferencesMan, 
			AttributeSyntaxFactoriesRegistry attributeSyntaxFactoriesRegistry,
			IdPEngine idpEngine,
			FreemarkerHandler freemarker,
			SessionManagement sessionMan, String samlUiServletPath)
	{
		super(preferencesMan, attributeSyntaxFactoriesRegistry, idpEngine, 
				freemarker, sessionMan, samlUiServletPath);
	}
	
	@Override
	protected SPSettings loadPreferences(SAMLAuthnContext samlCtx) throws EngineException
	{
		SamlPreferencesWithETD preferences = SamlPreferencesWithETD.getPreferences(preferencesMan, 
				attributeSyntaxFactoriesRegistry);
		return preferences.getSPSettings(samlCtx.getRequest().getIssuer());
	}
}
