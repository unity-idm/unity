/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp;

import pl.edu.icm.unity.saml.SAMLEndpointDefinition;
import pl.edu.icm.unity.saml.metadata.cfg.IdpRemoteMetaManager;
import pl.edu.icm.unity.saml.slo.SAMLLogoutProcessor.SamlTrustProvider;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;

import java.security.PublicKey;
import java.util.Collection;
import java.util.List;

/**
 * Used to obtain configuration information, which is changed at runtime.
 * @author K. Benedyczak
 */
public class IdpSamlTrustProvider implements SamlTrustProvider
{
	private IdpRemoteMetaManager myMetadataManager;
	
	public IdpSamlTrustProvider(IdpRemoteMetaManager myMetadataManager)
	{
		this.myMetadataManager = myMetadataManager;
	}

	@Override
	public Collection<SAMLEndpointDefinition> getSLOEndpoints(
			NameIDType samlEntity)
	{
		SAMLIdPConfiguration samlIdPConfiguration = myMetadataManager.getSAMLIdPConfiguration();
		TrustedServiceProvider configuration = samlIdPConfiguration.getSPConfig(samlEntity);
		if (configuration == null)
			return null;
		return configuration.getLogoutEndpoints();
	}

	@Override
	public List<PublicKey> getTrustedKeys(NameIDType samlEntity)
	{
		SAMLIdPConfiguration samlIdPConfiguration = myMetadataManager.getSAMLIdPConfiguration();
		return samlIdPConfiguration.getTrustedKeysForSamlEntity(samlEntity);
	}
}
