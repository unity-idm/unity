/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identities;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.util.StringUtils;

import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.authn.CredentialPublicInformation;
import pl.edu.icm.unity.types.basic.EntityInformation;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.webadmin.identities.IdentitiesGrid.BaseColumn;
import pl.edu.icm.unity.webui.common.EntityWithLabel;

/**
 * Data object behind a row in {@link IdentitiesGrid}. Stores either an entity information
 * or identity. 
 * @author K. Benedyczak
 */
class IdentityEntry
{
	private Map<BaseColumn, String> columnsToValues = new HashMap<>();
	private Map<String, String> attributes;
	private EntityWithLabel sourceEntity;
	private Identity sourceIdentity;

	IdentityEntry(EntityWithLabel entityWithLabel, Map<String, String> attributes, UnityMessageSource msg)
	{
		this.sourceEntity = entityWithLabel;
		this.sourceIdentity = null;
		for (BaseColumn base: IdentitiesGrid.BaseColumn.values())
			columnsToValues.put(base, "");
		setShared(entityWithLabel, attributes, msg);
	}

	IdentityEntry(EntityWithLabel entityWithLabel, Map<String, String> attributes, Identity id, 
			IdentityTypeDefinition typeDefinition, UnityMessageSource msg)
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
			UnityMessageSource msg)
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
	
	public String getAttribute(String key)
	{
		return attributes.get(key);
	}

	public String getBaseValue(BaseColumn key)
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
		if (!StringUtils.isEmpty(credInfo.getStateDetail()))
			status = status + " - " + credInfo.getStateDetail();
		return status;
	}
	
	public String getAnyValue(String key)
	{
		try
		{
			BaseColumn baseColumn = BaseColumn.valueOf(key);
			return getBaseValue(baseColumn);
		} catch (IllegalArgumentException e)
		{
			return getAttribute(key);
		}
	}
	
	public EntityWithLabel getSourceEntity()
	{
		return sourceEntity;
	}

	public Identity getSourceIdentity()
	{
		return sourceIdentity;
	}

	public boolean anyFieldContains(String text, Set<String> testedColumns)
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
		result = prime * result
				+ ((columnsToValues == null) ? 0 : columnsToValues.hashCode());
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
		if (columnsToValues == null)
		{
			if (other.columnsToValues != null)
				return false;
		} else if (!columnsToValues.equals(other.columnsToValues))
			return false;
		return true;
	}
}
