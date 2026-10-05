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
	private final Map<String, String> attributes = new HashMap<>();

	IdentityEntry(EntityWithLabel entityWithLabel, MessageSource msg)
	{
		this.sourceEntity = entityWithLabel;
		this.sourceIdentity = null;
		for (BaseColumn base: BaseColumn.values())
			columnsToValues.put(base, "");
		setShared(entityWithLabel, msg);
	}

	IdentityEntry(EntityWithLabel entityWithLabel, Identity id, IdentityTypeDefinition typeDefinition,
			MessageSource msg)
	{
		this.sourceEntity = entityWithLabel;
		this.sourceIdentity = id;
		setShared(entityWithLabel, msg);
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

	private void setShared(EntityWithLabel entityWithLabel, MessageSource msg)
	{
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
	}

	String getAttribute(String key)
	{
		return attributes.get(key);
	}

	void putAttributeValue(String key, String value)
	{
		attributes.put(key, value);
	}

	String getBaseValue(BaseColumn key)
	{
		// entity's displayed label is resolved lazily and mutated in place on sourceEntity - see
		// IdentitiesTreeGrid.resolveEntityLabel (UY-1483)
		if (key == BaseColumn.entity)
			return sourceEntity.toString();
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
		for (BaseColumn column : BaseColumn.values())
		{
			String value = getBaseValue(column);
			if (testedColumns.contains(column.name()) &&
					value != null &&
					value.toLowerCase().contains(textLower))
				return true;
		}
		for (Map.Entry<String, String> value: attributes.entrySet())
			if (testedColumns.contains(value.getKey()) &&
					value.getValue() != null &&
					value.getValue().toLowerCase().contains(textLower))
				return true;
		return false;
	}

	/*
	 * Deliberately based on the stable sourceEntity/sourceIdentity references rather than on
	 * columnsToValues/attributes - the latter are mutated in place (attributes especially, as columns
	 * are lazily resolved on render, see IdentitiesTreeGrid - UY-1483) after this entry may already be
	 * held in TreeData/selection sets, and a changing hashCode there would corrupt those collections.
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + sourceEntity.hashCode();
		result = prime * result + ((sourceIdentity == null) ? 0 : sourceIdentity.hashCode());
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
		if (!sourceEntity.equals(other.sourceEntity))
			return false;
		return sourceIdentity == null ? other.sourceIdentity == null : sourceIdentity.equals(other.sourceIdentity);
	}
}
