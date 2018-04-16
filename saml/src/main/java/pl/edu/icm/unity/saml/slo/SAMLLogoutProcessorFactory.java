/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.slo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.samly2.validators.ReplayAttackChecker;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.identity.IdentityResolver;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.engine.api.session.SessionParticipantTypesRegistry;
import pl.edu.icm.unity.engine.api.utils.FreemarkerAppHandler;
import pl.edu.icm.unity.saml.idp.IdentityTypeMapper;
import pl.edu.icm.unity.saml.slo.SAMLLogoutProcessor.SamlTrustProvider;

/**
 * Simplifies creation of {@link SAMLLogoutProcessor}.
 * 
 * @author K. Benedyczak
 */
@Component
public class SAMLLogoutProcessorFactory
{
	private SessionManagement sessionManagement;
	private IdentityResolver idResolver;
	private LogoutContextsStore contextsStore;
	private ReplayAttackChecker replayChecker;
	private PKIManagement pkiManagement;
	private SLOAsyncResponseHandler responseHandler;
	private SessionParticipantTypesRegistry registry;
	private UnityServerConfiguration serverConfig;
	
	@Autowired
	public SAMLLogoutProcessorFactory(SessionManagement sessionManagement, PKIManagement pkiManagement,
			IdentityResolver idResolver, LogoutContextsStore contextsStore,
			ReplayAttackChecker replayChecker, FreemarkerAppHandler freemarker, 
			SessionParticipantTypesRegistry registry, UnityServerConfiguration serverConfig)
	{
		super();
		this.sessionManagement = sessionManagement;
		this.idResolver = idResolver;
		this.contextsStore = contextsStore;
		this.replayChecker = replayChecker;
		this.responseHandler = new SLOAsyncResponseHandler(freemarker);
		this.pkiManagement = pkiManagement;
		this.registry = registry;
		this.serverConfig = serverConfig;
	}


	public SAMLLogoutProcessor getInstance(IdentityTypeMapper identityTypeMapper, String consumerEndpointUri,
			long requestValidity, String localSamlId,
			X509Credential localSamlCredential, SamlTrustProvider samlTrustProvider,
			String realm)
	{
		InternalLogoutProcessor internalProcessor = getInternalProcessorInstance(consumerEndpointUri);
		return new SAMLLogoutProcessor(sessionManagement, registry, idResolver, contextsStore, replayChecker, 
				responseHandler, internalProcessor, identityTypeMapper, consumerEndpointUri, 
				requestValidity, localSamlId, localSamlCredential, samlTrustProvider, realm, 
				serverConfig);
	}
	
	public InternalLogoutProcessor getInternalProcessorInstance(String consumerEndpointUri)
	{
		return new InternalLogoutProcessor(pkiManagement, contextsStore, responseHandler, consumerEndpointUri);
	}
}
