/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.attribute.cache;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.tx.Transactional;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.AttributeSearchService;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;
import pl.edu.icm.unity.store.api.AttributesCacheDAO;

@Component
class AttributeSearchServiceImpl implements AttributeSearchService
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CORE, AttributeSearchServiceImpl.class);

	private final AttributesCacheDAO attributesCacheDAO;
	private final InternalAuthorizationManager authz;

	@Autowired
	AttributeSearchServiceImpl(AttributesCacheDAO attributesCacheDAO, InternalAuthorizationManager authz)
	{
		this.attributesCacheDAO = attributesCacheDAO;
		this.authz = authz;
	}

	@Override
	@Transactional
	public Set<Long> searchEntities(String group, String searchTerm)
	{
		authz.checkAuthorizationRT(group, AuthzCapability.readHidden, AuthzCapability.read);
		if (searchTerm == null || searchTerm.isBlank())
			return Set.of();
		List<Long> found = attributesCacheDAO.findEntitiesWithValueContaining(group, searchTerm);
		log.trace("Attribute search in group {} for '{}': {} matches", group, searchTerm, found.size());
		return new HashSet<>(found);
	}
}
