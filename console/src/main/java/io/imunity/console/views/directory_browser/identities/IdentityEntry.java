/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;

import io.imunity.console.views.directory_browser.EntityWithLabel;
import org.apache.logging.log4j.Logger;
import org.springframework.util.StringUtils;
import pl.edu.icm.unity.base.authn.CredentialPublicInformation;
import pl.edu.icm.unity.base.entity.EntityInformation;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class IdentityEntry
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_WEB, IdentityEntry.class);

	private final Map<BaseColumn, String> columnsToValues = new HashMap<>();
	private final EntityWithLabel sourceEntity;
	private final Identity sourceIdentity;
	private Map<String, String> attributes;

	IdentityEntry(EntityWithLabel entityWithLabel, Map<String, String> attributes, MessageSource msg)
	{
		this.sourceEntity = entityWithLabel;
		this.sourceIdentity = null;
		for (BaseColumn base: BaseColumn.values())
			columnsToValues.put(base, "");
		setShared(entityWithLabel, attributes, msg);
	}

	IdentityEntry(EntityWithLabel entityWithLabel, Map<String, String> attributes, Identity id,
			IdentityTypeDefinition typeDefinition, MessageSource msg)
	{
		this.sourceEntity = entityWithLabel;
		this.sourceIdentity = id;
		setShared(entityWithLabel, attributes, msg);
		columnsToValues.put(BaseColumn.type, id.getTypeId());
		columnsToValues.put(BaseColumn.identity, typeDefinition.toPrettyStringNoPrefix(id));
		columnsToValues.put(BaseColumn.local, String.valueOf(id.isLocal()));
		columnsToValues.put(BaseColumn.dynamic, String.valueOf(typeDefinition.isDynamic()));
		columnsToValues.put(BaseColumn.target, id.getTarget() == null ? "" : id.getTarget());
		columnsToValues.put(BaseColumn.realm, id.getRealm() == null ? "" : id.getRealm());
		columnsToValues.put(BaseColumn.remoteIdP, 
				id.getRemoteIdp() == null ? "" : id.getRemoteIdp());
		columnsToValues.put(BaseColumn.profile, 
				id.getTranslationProfile() == null ? "" : id.getTranslationProfile());
	}

	private void setShared(EntityWithLabel entityWithLabel, Map<String, String> attributes, 
			MessageSource msg)
	{
		columnsToValues.put(BaseColumn.entity, entityWithLabel.toString());
		columnsToValues.put(BaseColumn.credReq, 
				entityWithLabel.getEntity().getCredentialInfo().getCredentialRequirementId());
		columnsToValues.put(BaseColumn.status, 
				msg.getMessage("EntityState."+entityWithLabel.getEntity().getState().name()));
		EntityInformation entInfo = entityWithLabel.getEntity().getEntityInformation();
		String scheduledOperation = entInfo.getScheduledOperation() == null ? "" : 
			msg.getMessage("EntityScheduledOperationWithDateShort."+
				entInfo.getScheduledOperation().name(),
				entInfo.getScheduledOperationTime());
		columnsToValues.put(BaseColumn.scheduledOperation, scheduledOperation);
		this.attributes = new HashMap<>(attributes);
	}
	
	String getAttribute(String key)
	{
		return attributes.get(key);
	}

	String getBaseValue(BaseColumn key)
	{
		return columnsToValues.get(key);
	}

	String getCredentialStatus(String credential)
	{
		CredentialPublicInformation credInfo = sourceEntity.getEntity().getCredentialInfo()
				.getCredentialsState().get(credential);
		if (credInfo == null)
			return "";
		
		String status = credInfo.getState().toString();
		if (StringUtils.hasLength(credInfo.getStateDetail()))
			status = status + " - " + credInfo.getStateDetail();
		return status;
	}
	
	String getAnyValue(String key)
	{
		try
		{
			BaseColumn baseColumn = BaseColumn.valueOf(key);
			return getBaseValue(baseColumn);
		} catch (IllegalArgumentException e)
		{
			LOG.trace(e);
			return getAttribute(key);
		}
	}
	
	EntityWithLabel getSourceEntity()
	{
		return sourceEntity;
	}

	Identity getSourceIdentity()
	{
		return sourceIdentity;
	}

	boolean anyFieldContains(String text, Set<String> testedColumns)
	{
		String textLower = text.toLowerCase();
		for (Map.Entry<BaseColumn, String> value: columnsToValues.entrySet())
			if (testedColumns.contains(value.getKey().name()) &&
					value.getValue() != null && 
					value.getValue().toLowerCase().contains(textLower))
				return true;
		for (Map.Entry<String, String> value: attributes.entrySet())
			if (testedColumns.contains(value.getKey()) && 
					value.getValue() != null && 
					value.getValue().toLowerCase().contains(textLower))
				return true;
		return false;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
		result = prime * result + columnsToValues.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IdentityEntry other = (IdentityEntry) obj;
		if (attributes == null)
		{
			if (other.attributes != null)
				return false;
		} else if (!attributes.equals(other.attributes))
			return false;
		return columnsToValues.equals(other.columnsToValues);
	}
}
