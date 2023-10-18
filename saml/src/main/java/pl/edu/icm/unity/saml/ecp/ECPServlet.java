/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.ecp;

import java.io.IOException;
import java.util.function.Supplier;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import eu.unicore.samly2.validators.ReplayAttackChecker;
import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthnResultTranslator;
import pl.edu.icm.unity.engine.api.session.SessionManagementEE8;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.rest.jwt.JWTAuthenticationConfig;
import pl.edu.icm.unity.saml.metadata.cfg.SPRemoteMetaManager;
import pl.edu.icm.unity.saml.sp.config.SAMLSPConfiguration;

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
	private final ECPStep1Handler step1Handler;
	private final ECPStep2Handler step2Handler;

	public ECPServlet(JWTAuthenticationConfig jwtConfig, 
			Supplier<SAMLSPConfiguration> configProvider,
			SPRemoteMetaManager metadataManager,
			ECPContextManagement samlContextManagement, 
			String myAddress, ReplayAttackChecker replayAttackChecker, 
			RemoteAuthnResultTranslator remoteAuthnProcessor,
			TokensManagement tokensMan, PKIManagement pkiManagement, EntityManagement identitiesMan,
			SessionManagementEE8 sessionMan, AuthenticationRealm realm, String address)
	{
		step1Handler = new ECPStep1Handler(configProvider, metadataManager, samlContextManagement, myAddress);
		step2Handler = new ECPStep2Handler(jwtConfig, configProvider,
				metadataManager, samlContextManagement, myAddress,
				replayAttackChecker, 
				tokensMan, pkiManagement, remoteAuthnProcessor, 
				identitiesMan, sessionMan, realm, address);
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
