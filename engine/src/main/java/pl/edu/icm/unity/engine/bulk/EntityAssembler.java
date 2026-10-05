/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.bulk;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Stopwatch;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.authn.CredentialInfo;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.IllegalCredentialException;
import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialsRegistry;
import pl.edu.icm.unity.engine.credential.CredentialRequirementsHolder;
import pl.edu.icm.unity.engine.credential.EntityCredentialsHelper;

/**
 * Assembles {@link Entity} objects (identities + credential status) out of the bulk-loaded
 * {@link EntitiesData}/{@link GlobalSystemData}. Shared between {@link BulkQueryServiceImpl} and
 * {@link DirectoryBrowserGroupQueryServiceImpl} so the two don't duplicate credential status resolution.
 */
@Component
class EntityAssembler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_BULK_OPS, EntityAssembler.class);

	private final EntityCredentialsHelper credentialsHelper;
	private final LocalCredentialsRegistry localCredReg;

	@Autowired
	EntityAssembler(EntityCredentialsHelper credentialsHelper, LocalCredentialsRegistry localCredReg)
	{
		this.credentialsHelper = credentialsHelper;
		this.localCredReg = localCredReg;
	}

	Map<Long, Entity> getGroupEntitiesNoContext(boolean includeTargeted, EntitiesData entitiesData,
			GlobalSystemData globalSystemData)
	{
		Stopwatch watch = Stopwatch.createStarted();
		Map<Long, Entity> ret = new HashMap<>();
		for (Long entityId : entitiesData.getEntityInfo().keySet())
			ret.put(entityId, assembleEntity(entityId, includeTargeted, entitiesData, globalSystemData));
		log.debug("Bulk entities assembly: {}", watch.toString());
		return ret;
	}

	Entity assembleEntity(long entityId, boolean includeTargeted, EntitiesData entitiesData,
			GlobalSystemData globalSystemData)
	{
		CredentialInfo credInfo = getCredentialInfo(entityId, entitiesData, globalSystemData);
		List<Identity> identitites = entitiesData.getIdentities().get(entityId);
		if (!includeTargeted)
			identitites = filterTargetedIdentitites(identitites);
		return new Entity(identitites, entitiesData.getEntityInfo().get(entityId), credInfo);
	}

	private List<Identity> filterTargetedIdentitites(List<Identity> all)
	{
		return all.stream().filter(id -> id.getTarget() == null).collect(Collectors.toList());
	}

	CredentialInfo getCredentialInfo(long entityId, EntitiesData entitiesData, GlobalSystemData globalSystemData)
	{
		Map<String, AttributeExt> attributes = entitiesData.getDirectAttributes().get(entityId).get("/");
		String credentialRequirementId = credentialsHelper.getCredentialReqFromAttribute(attributes);

		CredentialRequirementsHolder credReq;
		try
		{
			credReq = new CredentialRequirementsHolder(localCredReg,
					globalSystemData.getCredentialRequirements().get(credentialRequirementId),
					globalSystemData.getCredentials());
		} catch (IllegalCredentialException e)
		{
			throw new InternalException("Unknown credential assigned to entity", e);
		}
		return credentialsHelper.getCredentialInfoNoQuery(entityId, attributes, credReq, credentialRequirementId);
	}
}
