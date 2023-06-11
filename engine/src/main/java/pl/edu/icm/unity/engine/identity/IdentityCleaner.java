/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.identity;

import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.identity.IdentityType;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;
import pl.edu.icm.unity.store.api.IdentityDAO;
import pl.edu.icm.unity.store.api.IdentityTypeDAO;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.store.types.StoredIdentity;

/**
 * Removes expired identities. 
 * @author K. Benedyczak
 */
@Component
public class IdentityCleaner
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CORE, IdentityCleaner.class);
	private IdentityDAO identityDAO;
	private IdentityTypeHelper idTypeHelper;
	private IdentityTypeDAO idTypeDAO;

	@Autowired
	public IdentityCleaner(IdentityDAO identityDAO, IdentityTypeHelper idTypeHelper,
			IdentityTypeDAO idTypeDAO)
	{
		this.identityDAO = identityDAO;
		this.idTypeHelper = idTypeHelper;
		this.idTypeDAO = idTypeDAO;
	}

	@Transactional
	public void removeExpiredIdentities()
	{
		Map<String, IdentityType> types = idTypeDAO.getAllAsMap();
		for (StoredIdentity sidentity: identityDAO.getAll())
		{
			Identity identity = sidentity.getIdentity();
			IdentityType identityType = types.get(identity.getTypeId());
			IdentityTypeDefinition typeDefinition = idTypeHelper.getTypeDefinition(identityType);
			if (typeDefinition.isExpired(identity))
			{
				log.info("Removing expired identity " + identity);
				identityDAO.delete(StoredIdentity.toInDBIdentityValue(identityType.getName(), 
						identity.getComparableValue()));
			}
		}
	}
}
