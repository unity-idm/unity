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
import pl.edu.icm.unity.store.export.DumpHeader;
import pl.edu.icm.unity.types.basic.IdentityType;

/**
 * Handles import/export of identity types.
 * @author K. Benedyczak
 */
@Component
public class IdentityTypeIE extends AbstractIEBase<IdentityType>
{
	@Autowired
	private IdentityTypeDAO dbIdTypes;
	
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
	protected IdentityType fromJsonSingle(ObjectNode src, DumpHeader header)
	{
		if (header.getVersionMajor() < DumpHeader.V_INITIAL2)
		{
			src.put("identityTypeProvider", src.get("name").asText());
		}
		
		return new IdentityType(src);
	}
}








