/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.ecp;

import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import eu.unicore.samly2.validators.ReplayAttackChecker;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.api.internal.IdentityResolver;
import pl.edu.icm.unity.server.api.internal.NetworkServer;
import pl.edu.icm.unity.server.endpoint.EndpointFactory;
import pl.edu.icm.unity.server.endpoint.EndpointInstance;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;

/**
 * Factory of {@link ECPEndpoint}s.
 * @author K. Benedyczak
 */
@Component
public class ECPEndpointFactory implements EndpointFactory
{
	public static final String SERVLET_PATH = "/saml2-ecp";
	public static final String METADATA_SERVLET_PATH = "/metadata";
	public static final String NAME = "SAML-ECP";
	
	private EndpointTypeDescription description;
	private PKIManagement pkiManagement;
	private URL baseAddress;
	private ECPContextManagement samlContextManagement;
	private ReplayAttackChecker replayAttackChecker;
	private IdentityResolver identityResolver;
	private TranslationProfileManagement profileManagement;
	private AttributesManagement attrMan;
	
	@Autowired
	public ECPEndpointFactory(PKIManagement pkiManagement, NetworkServer jettyServer,
			ECPContextManagement samlContextManagement,
			ReplayAttackChecker replayAttackChecker, IdentityResolver identityResolver,
			@Qualifier("insecure") TranslationProfileManagement profileManagement, 
			@Qualifier("insecure") AttributesManagement attrMan)
	{
		this.pkiManagement = pkiManagement;
		this.baseAddress = jettyServer.getAdvertisedAddress();
		this.samlContextManagement = samlContextManagement;
		this.replayAttackChecker = replayAttackChecker;
		this.identityResolver = identityResolver;
		this.profileManagement = profileManagement;
		this.attrMan = attrMan;
		Set<String> supportedAuthn = new HashSet<String>();
		Map<String,String> paths = new HashMap<String, String>();
		paths.put(SERVLET_PATH, "SAML 2 ECP authentication endpoint");
		paths.put(METADATA_SERVLET_PATH, "Metadata of the SAML ECP endpoint");
		description = new EndpointTypeDescription(NAME, 
				"SAML 2 ECP authentication endpoint", supportedAuthn, paths);
	}
	
	@Override
	public EndpointTypeDescription getDescription()
	{
		return description;
	}

	@Override
	public EndpointInstance newInstance()
	{
		return new ECPEndpoint(description, SERVLET_PATH, pkiManagement, samlContextManagement, baseAddress,
				replayAttackChecker, identityResolver, profileManagement, attrMan);
	}

}
