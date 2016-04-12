/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.identity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.internal.TransactionalRunner;
import pl.edu.icm.unity.store.GenericCompositeDAOImpl;
import pl.edu.icm.unity.store.api.IdentityTypeDAO;
import pl.edu.icm.unity.types.basic.IdentityType;

/**
 * Router of {@link IdentityTypeDAO}.
 * @author K. Benedyczak
 */
@Component
@Primary
public class IdentityTypeDAOImpl extends GenericCompositeDAOImpl<IdentityType> implements IdentityTypeDAO
{
	@Autowired
	public IdentityTypeDAOImpl(IdentityTypeHzStore hzDAO, IdentityTypeRDBMSStore rdbmsDAO,
			TransactionalRunner tx)
	{
		super(hzDAO, rdbmsDAO, tx);
	}
}
