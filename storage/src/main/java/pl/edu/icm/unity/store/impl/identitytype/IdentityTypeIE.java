/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.identitytype;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.store.api.IdentityTypeDAO;
import pl.edu.icm.unity.store.export.AbstractIEBase;
import pl.edu.icm.unity.types.basic.IdentityType;

/**
 * Handles import/export of identity types.
 * @author K. Benedyczak
 */
@Component
public class IdentityTypeIE extends AbstractIEBase<IdentityType>
{
	public static final String IDENTITY_TYPE_OBJECT_TYPE = "identityTypes";

	private final IdentityTypeDAO dbIdTypes;
	
	@Autowired
	public IdentityTypeIE(IdentityTypeDAO dbIdTypes)
	{
		super(1, IDENTITY_TYPE_OBJECT_TYPE);
		this.dbIdTypes = dbIdTypes;
	}

	@Override
	protected List<IdentityType> getAllToExport()
	{
		return dbIdTypes.getAll();
	}

	@Override
	protected ObjectNode toJsonSingle(IdentityType exportedObj)
	{
		return exportedObj.toJson();
	}

	@Override
	protected void createSingle(IdentityType toCreate)
	{
		dbIdTypes.create(toCreate);
	}

	@Override
	protected IdentityType fromJsonSingle(ObjectNode src)
	{
		return new IdentityType(src);
	}
}








