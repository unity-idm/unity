/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.identity;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.store.api.IdentityTypeDAO;
import pl.edu.icm.unity.types.basic.IdentityType;

/**
 * Router of {@link IdentityTypeDAO}.
 * @author K. Benedyczak
 */
@Component
@Primary
public class IdentityTypeDAOImpl implements IdentityTypeDAO
{
	@Autowired
	private IdentityTypeHzStore hzDAO;
	@Autowired
	private IdentityTypeRDBMSStore rdbmsDAO;
	

	@Override
	public IdentityType getIdentityType(String idType)
	{
		return hzDAO.getIdentityType(idType);
	}

	@Override
	public Map<String, IdentityType> getIdentityTypes()
	{
		return hzDAO.getIdentityTypes();
	}

	@Override
	public void updateIdentityType(IdentityType idType)
	{
		rdbmsDAO.updateIdentityType(idType);
		hzDAO.updateIdentityType(idType);
	}

	@Override
	public void createIdentityType(IdentityType idType)
	{
		rdbmsDAO.createIdentityType(idType);
		hzDAO.createIdentityType(idType);
	}

	@Override
	public void deleteIdentityType(String idType)
	{
		rdbmsDAO.deleteIdentityType(idType);
		hzDAO.deleteIdentityType(idType);
	}
}
