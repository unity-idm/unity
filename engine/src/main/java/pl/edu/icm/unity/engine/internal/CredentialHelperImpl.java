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
import pl.edu.icm.unity.db.DBSessionManager;
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
	private DBSessionManager db;
	private DBAttributes dbAttributes;
	
	
	@Autowired
	public CredentialHelperImpl(DBSessionManager db, DBAttributes dbAttributes)
	{
		this.db = db;
		this.dbAttributes = dbAttributes;
	}

	@Override
	public void updateCredential(long entityId, String credentialName, String value) throws EngineException 
	{
		SqlSession sql = db.getSqlSession(true);
		try
		{
			updateCredentialInternal(entityId, credentialName, value, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}
	
	@Override
	public void setCredential(long entityId, String credentialName, String value, 
			LocalCredentialVerificator handler) throws EngineException 
	{
		SqlSession sql = db.getSqlSession(true);
		try
		{
			String credentialAttributeName = SystemAttributeTypes.CREDENTIAL_PREFIX+credentialName;
			Map<String, AttributeExt<?>> attributes = dbAttributes.getAllAttributesAsMapOneGroup(
					entityId, "/", credentialAttributeName, sql);
			Attribute<?> currentCredentialA = attributes.get(credentialAttributeName);
			String currentCredential = currentCredentialA != null ? 
					(String)currentCredentialA.getValues().get(0) : null;
			String newValue = handler.prepareCredential(value, currentCredential);
			updateCredentialInternal(entityId, credentialName, newValue, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
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
