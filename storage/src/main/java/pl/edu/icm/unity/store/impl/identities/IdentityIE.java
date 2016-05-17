/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.identities;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.store.api.IdentityDAO;
import pl.edu.icm.unity.store.export.AbstractIEBase;
import pl.edu.icm.unity.types.basic.Identity;

/**
 * Handles import/export of identities.
 * @author K. Benedyczak
 */
@Component
public class IdentityIE extends AbstractIEBase<Identity>
{
	@Autowired
	private IdentityDAO dbIds;
	
	@Override
	protected List<Identity> getAllToExport()
	{
		return dbIds.getAll();
	}

	@Override
	protected ObjectNode toJsonSingle(Identity exportedObj)
	{
		return exportedObj.toJson();
	}

	@Override
	protected void createSingle(Identity toCreate)
	{
		dbIds.create(toCreate);
	}

	@Override
	protected Identity fromJsonSingle(ObjectNode src)
	{
		return new Identity(src);
	}
}



