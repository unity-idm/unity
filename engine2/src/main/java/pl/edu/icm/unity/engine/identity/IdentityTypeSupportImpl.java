/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.identity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.store.api.tx.Transactional;


/**
 * Implementation of {@link IdentityTypeSupport}, delegating to {@link IdentityHelper},
 * decorating it with transactions.
 * 
 * @author K. Benedyczak
 */
@Component
@Transactional
public class IdentityTypeSupportImpl implements IdentityTypeSupport
{
	@Autowired
	private IdentityTypeHelper helper;
	
	/**
	 * @param typeDef
	 * @return {@link IdentityTypeDefinition} for the given IdentityType.
	 */
	@Override
	public IdentityTypeDefinition getTypeDefinition(String idType)
	{
		return helper.getTypeDefinition(idType);
	}
}
