/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.credential;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.attributes.SystemAttributesProvider;
import pl.edu.icm.unity.engine.attribute.AttributeTypeHelper;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.basic.AttributeType;

/**
 * Provides system attribute for credential
 * @author P.Piernik
 *
 */
@Component
public class CredentialAttributeTypesProvider implements SystemAttributesProvider
{

	private SystemCredentialProvider sysProvider;
	private AttributeTypeHelper attrTypeHelper;
	
	public CredentialAttributeTypesProvider(SystemCredentialProvider sysProvider,
			AttributeTypeHelper attrTypeHelper)
	{
		
		this.sysProvider = sysProvider;
		this.attrTypeHelper = attrTypeHelper;
	}
	
	@Override
	public List<AttributeType> getSystemAttributes()
	{
		List<AttributeType> attrsType = new ArrayList<>();
		for (CredentialDefinition cr: sysProvider.getSystemCredentials())
		{
			AttributeType at = attrTypeHelper.getCredentialAT(cr.getName());
			attrsType.add(at);
		}
		return attrsType;
	}

	@Override
	public boolean requiresUpdate(AttributeType at)
	{
		return false;
	}
}
