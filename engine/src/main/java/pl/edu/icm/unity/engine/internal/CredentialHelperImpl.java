/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.internal;

import java.util.Collections;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBAttributes;
import pl.edu.icm.unity.engine.transactions.SqlSessionTL;
import pl.edu.icm.unity.engine.transactions.Transactional;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.authn.CredentialHelper;
import pl.edu.icm.unity.server.authn.LocalCredentialVerificator;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.sysattrs.SystemAttributeTypes;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeVisibility;

/**
 * Default implementation of the credential helper. Immutable.
 * @author K. Benedyczak
 */
@Component
public class CredentialHelperImpl implements CredentialHelper
{
	private DBAttributes dbAttributes;
	
	
	@Autowired
	public CredentialHelperImpl(DBAttributes dbAttributes)
	{
		this.dbAttributes = dbAttributes;
	}

	@Override
	@Transactional
	public void updateCredential(long entityId, String credentialName, String value) throws EngineException 
	{
		updateCredentialInternal(entityId, credentialName, value, SqlSessionTL.get());
	}
	
	@Override
	@Transactional
	public void setCredential(long entityId, String credentialName, String value, 
			LocalCredentialVerificator handler) throws EngineException 
	{
		SqlSession sql = SqlSessionTL.get();
		String credentialAttributeName = SystemAttributeTypes.CREDENTIAL_PREFIX+credentialName;
		Map<String, AttributeExt<?>> attributes = dbAttributes.getAllAttributesAsMapOneGroup(
				entityId, "/", credentialAttributeName, sql);
		Attribute<?> currentCredentialA = attributes.get(credentialAttributeName);
		String currentCredential = currentCredentialA != null ? 
				(String)currentCredentialA.getValues().get(0) : null;
				String newValue = handler.prepareCredential(value, currentCredential);
				updateCredentialInternal(entityId, credentialName, newValue, sql);
	}
	

	private void updateCredentialInternal(long entityId, String credentialName, String value, SqlSession sql) 
			throws EngineException 
	{
		String credentialAttributeName = SystemAttributeTypes.CREDENTIAL_PREFIX+credentialName;
		StringAttribute newCredentialA = new StringAttribute(credentialAttributeName, 
				"/", AttributeVisibility.local, Collections.singletonList(value));
		dbAttributes.addAttribute(entityId, newCredentialA, true, sql);
	}
}
