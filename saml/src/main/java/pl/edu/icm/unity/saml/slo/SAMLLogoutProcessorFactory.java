/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.slo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.saml.idp.FreemarkerHandler;
import pl.edu.icm.unity.saml.idp.IdentityTypeMapper;
import pl.edu.icm.unity.saml.slo.SAMLLogoutProcessor.SamlTrustProvider;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.api.internal.IdentityResolver;
import pl.edu.icm.unity.server.api.internal.SessionManagement;
import pl.edu.icm.unity.server.registries.SessionParticipantTypesRegistry;
import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.samly2.validators.ReplayAttackChecker;

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
	
	@Autowired
	public SAMLLogoutProcessorFactory(SessionManagement sessionManagement, PKIManagement pkiManagement,
			IdentityResolver idResolver, LogoutContextsStore contextsStore,
			ReplayAttackChecker replayChecker, FreemarkerHandler freemarker, 
			SessionParticipantTypesRegistry registry)
	{
		super();
		this.sessionManagement = sessionManagement;
		this.idResolver = idResolver;
		this.contextsStore = contextsStore;
		this.replayChecker = replayChecker;
		this.responseHandler = new SLOAsyncResponseHandler(freemarker);
		this.pkiManagement = pkiManagement;
		this.registry = registry;
	}


	public SAMLLogoutProcessor getInstance(IdentityTypeMapper identityTypeMapper, String consumerEndpointUri,
			long requestValidity, String localSamlId,
			X509Credential localSamlCredential, SamlTrustProvider samlTrustProvider,
			String realm)
	{
		InternalLogoutProcessor internalProcessor = getInternalProcessorInstance(consumerEndpointUri);
		return new SAMLLogoutProcessor(sessionManagement, registry, idResolver, contextsStore, replayChecker, 
				responseHandler, internalProcessor, identityTypeMapper, consumerEndpointUri, 
				requestValidity, localSamlId, localSamlCredential, samlTrustProvider, realm);
	}
	
	public InternalLogoutProcessor getInternalProcessorInstance(String consumerEndpointUri)
	{
		return new InternalLogoutProcessor(pkiManagement, contextsStore, responseHandler, consumerEndpointUri);
	}
}
