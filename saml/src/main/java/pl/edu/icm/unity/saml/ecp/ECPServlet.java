/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.ecp;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import pl.edu.icm.unity.saml.metadata.cfg.RemoteMetaManager;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.api.internal.IdentityResolver;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.api.internal.TokensManagement;
import pl.edu.icm.unity.server.authn.remote.TranslationEngine;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import eu.unicore.samly2.validators.ReplayAttackChecker;

/**
 * ECP servlet which performs the actual ECP profile processing over PAOS binding.
 * <p>
 * The GET request is used to ask for SAML request. The POST request is used to provide SAML response 
 * and obtain a JWT token which can be subsequently used with other Unity endpoints.
 * 
 * @author K. Benedyczak
 */
public class ECPServlet extends HttpServlet
{
	private ECPStep1Handler step1Handler;
	private ECPStep2Handler step2Handler;

	public ECPServlet(SAMLECPProperties samlProperties, RemoteMetaManager metadataManager,
			ECPContextManagement samlContextManagement, 
			String myAddress, ReplayAttackChecker replayAttackChecker, IdentityResolver identityResolver,
			TranslationProfileManagement profileManagement, TranslationEngine trEngine,
			TokensManagement tokensMan, PKIManagement pkiManagement, IdentitiesManagement identitiesMan,
			SessionManagement sessionMan, AuthenticationRealm realm, String address)
	{
		step1Handler = new ECPStep1Handler(metadataManager, samlContextManagement, myAddress);
		step2Handler = new ECPStep2Handler(samlProperties, metadataManager, samlContextManagement, myAddress,
				replayAttackChecker, identityResolver, profileManagement, trEngine,
				tokensMan, pkiManagement, identitiesMan, sessionMan, realm, address);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
	{
		step1Handler.processECPGetRequest(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
	{
		step2Handler.processECPPostRequest(req, resp);
	}
}
