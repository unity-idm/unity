/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.internal;

import java.util.Collection;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBAttributes;
import pl.edu.icm.unity.db.DBIdentities;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.generic.credreq.CredentialRequirementDB;
import pl.edu.icm.unity.db.resolvers.IdentitiesResolver;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.server.api.internal.IdentityResolver;
import pl.edu.icm.unity.server.authn.EntityWithCredential;
import pl.edu.icm.unity.sysattrs.SystemAttributeTypes;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;

/**
 * Default implementation of the identity resolver. Immutable.
 * @author K. Benedyczak
 */
@Component
public class IdentityResolverImpl implements IdentityResolver
{
	private DBSessionManager db;
	private DBAttributes dbAttributes;
	private DBIdentities dbIdentities;
	private IdentitiesResolver dbResolver;
	private CredentialRequirementDB dbCredReq;
	
	
	@Autowired
	public IdentityResolverImpl(DBSessionManager db, DBAttributes dbAttributes, CredentialRequirementDB dbCredReq,
			DBIdentities dbIdentities, IdentitiesResolver dbResolver)
	{
		this.db = db;
		this.dbAttributes = dbAttributes;
		this.dbIdentities = dbIdentities;
		this.dbResolver = dbResolver;
		this.dbCredReq = dbCredReq;
	}

	@Override
	public EntityWithCredential resolveIdentity(String identity, String[] identityTypes,
			String credentialName) throws EngineException
	{
		SqlSession sql = db.getSqlSession(true);
		try
		{
			long entityId = getEntity(identity, identityTypes, sql);
			EntityState entityState = dbIdentities.getEntityStatus(entityId, sql);
			if (entityState == EntityState.authenticationDisabled || entityState == EntityState.disabled)
				throw new IllegalIdentityValueException("Authentication is disabled for this entity");
			EntityWithCredential ret = new EntityWithCredential();
			if (credentialName != null)
			{
				CredentialRequirements credentialRequirements = resolveCredentialRequirements(
						entityId, sql);
				if (credentialRequirements.getRequiredCredentials().contains(credentialName))
				{
					Collection<AttributeExt<?>> credAttributes = dbAttributes.getAllAttributes(
						entityId, "/", true, 
						SystemAttributeTypes.CREDENTIAL_PREFIX+credentialName, sql);
					if (credAttributes.size() > 0)
					{
						Attribute<?> a = credAttributes.iterator().next();
						ret.setCredentialValue((String)a.getValues().get(0));
					}
				}
				ret.setCredentialName(credentialName);
			}
			ret.setEntityId(entityId);
			sql.commit();
			return ret;
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}
	
	private CredentialRequirements resolveCredentialRequirements(long entityId, SqlSession sql) 
			throws EngineException
	{
		Collection<AttributeExt<?>> credReqAttrs = dbAttributes.getAllAttributes(
				entityId, "/", true,
				SystemAttributeTypes.CREDENTIAL_REQUIREMENTS, sql);
		Attribute<?> cra = credReqAttrs.iterator().next();
		String cr = (String) cra.getValues().get(0);
		return dbCredReq.get(cr, sql);
	}
	
	@Override
	public long resolveIdentity(String identity, String[] identityTypes) throws IllegalIdentityValueException
	{
		SqlSession sql = db.getSqlSession(true);
		try
		{
			long entityId = getEntity(identity, identityTypes, sql);
			sql.commit();
			return entityId;
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}
	
	private long getEntity(String identity, String[] identityTypes, SqlSession sqlMap) 
			throws IllegalIdentityValueException
	{
		for (String identityType: identityTypes)
		{
			EntityParam entityParam = new EntityParam(new IdentityTaV(identityType, identity));
			try
			{
				return dbResolver.getEntityId(entityParam, sqlMap);
			} catch (EngineException e)
			{
				//ignored
			}
		}
		throw new IllegalIdentityValueException("No identity with value " + identity);
	}

}
