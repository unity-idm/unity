/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.registries;

import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.attrstmnt.CopyParentAttributeStatement;
import pl.edu.icm.unity.types.basic.attrstmnt.CopySubgroupAttributeStatement;
import pl.edu.icm.unity.types.basic.attrstmnt.EverybodyStatement;
import pl.edu.icm.unity.types.basic.attrstmnt.HasParentAttributeStatement;
import pl.edu.icm.unity.types.basic.attrstmnt.HasSubgroupAttributeStatement;
import pl.edu.icm.unity.types.basic.attrstmnt.MemberOfStatement;

/**
 * Pseudo registry of {@link AttributeStatement}s. AttributeStatements are tightly bound to the rest of the system
 * and are not pluggable. Therefore instantiation is fixed.
 * @author K. Benedyczak
 */
public class AttributeStatementsRegistry
{
	public static AttributeStatement getInstance(String name) throws WrongArgumentException
	{
		if (name.equals(HasParentAttributeStatement.NAME))
			return new HasParentAttributeStatement();
		if (name.equals(HasSubgroupAttributeStatement.NAME))
			return new HasSubgroupAttributeStatement();
		if (name.equals(EverybodyStatement.NAME))
			return new EverybodyStatement();
		if (name.equals(MemberOfStatement.NAME))
			return new MemberOfStatement();
		if (name.equals(CopyParentAttributeStatement.NAME))
			return new CopyParentAttributeStatement();
		if (name.equals(CopySubgroupAttributeStatement.NAME))
			return new CopySubgroupAttributeStatement();
		throw new WrongArgumentException("There is no attribute statement type with name " + name);
	}
}
