/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.identity;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.engine.authn.UserMFAOptInAttributeTypeProvider;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.store.types.StoredAttribute;


/**
 * Internal engine code to handle 2nd factor opt in. No authorization.
 * Required also to break dependency cycles (so can't live e.g. in {@link EntityCredentialsManagementImpl}
 */
@Component
@Transactional
public class SecondFactorOptInService
{
	private final AttributeDAO attributeDAO;
	
	@Autowired
	public SecondFactorOptInService(AttributeDAO attributeDAO)
	{
		this.attributeDAO = attributeDAO;
	}

	public boolean getUserOptin(long entityId) throws EngineException
	{
		return getUserOptinWithPresence(entityId).orElse(false);
	}
	
	private Optional<Boolean> getUserOptinWithPresence(long entityId) throws EngineException
	{
		Collection<StoredAttribute> userOptin;

		userOptin = attributeDAO.getAttributes(
				UserMFAOptInAttributeTypeProvider.USER_MFA_OPT_IN, entityId, "/");

		if (userOptin.size() == 0)
			return Optional.empty();

		StoredAttribute attr = userOptin.iterator().next();
		List<?> values = attr.getAttribute().getValues();
		return values.size() > 0 ? Optional.of(Boolean.valueOf((String) values.get(0)))
				: Optional.of(false);
	}

	public void setUserMFAOptIn(long entityId, boolean value) throws EngineException
	{
		if (value == false)
		{
			if (getUserOptinWithPresence(entityId).isPresent())
			{
				attributeDAO.deleteAttribute(
						UserMFAOptInAttributeTypeProvider.USER_MFA_OPT_IN,
						entityId, "/");
			}
		} else
		{
			Attribute sa = StringAttribute.of(
					UserMFAOptInAttributeTypeProvider.USER_MFA_OPT_IN, "/",
					String.valueOf(value));
			AttributeExt atExt = new AttributeExt(sa, true, new Date(), new Date());
			if (getUserOptinWithPresence(entityId).isPresent())
				attributeDAO.updateAttribute(new StoredAttribute(atExt, entityId));
			else
				attributeDAO.create(new StoredAttribute(atExt, entityId));
		}
	}
}
