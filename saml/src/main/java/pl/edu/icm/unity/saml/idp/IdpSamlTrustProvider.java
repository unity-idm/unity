/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp;

import java.util.Collection;

import pl.edu.icm.unity.saml.SAMLEndpointDefinition;
import pl.edu.icm.unity.saml.metadata.cfg.RemoteMetaManager;
import pl.edu.icm.unity.saml.slo.SAMLLogoutProcessor.SamlTrustProvider;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import eu.unicore.samly2.trust.SamlTrustChecker;

/**
 * Used to obtain configuration information, which is changed at runtime.
 * @author K. Benedyczak
 */
public class IdpSamlTrustProvider implements SamlTrustProvider
{
	private RemoteMetaManager myMetadataManager;
	
	public IdpSamlTrustProvider(RemoteMetaManager myMetadataManager)
	{
		this.myMetadataManager = myMetadataManager;
	}

	@Override
	public SamlTrustChecker getTrustChecker()
	{
		SamlIdpProperties virtualConf = (SamlIdpProperties) 
				myMetadataManager.getVirtualConfiguration();
		return virtualConf.getAuthnTrustChecker();
	}

	@Override
	public Collection<SAMLEndpointDefinition> getSLOEndpoints(
			NameIDType samlEntity)
	{
		SamlIdpProperties virtualConf = (SamlIdpProperties) 
				myMetadataManager.getVirtualConfiguration();
		String entityKey = virtualConf.getSPConfigKey(samlEntity);
		if (entityKey == null)
			return null;
		return virtualConf.getLogoutEndpointsFromStructuredList(entityKey);
	}
}
