/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.ac;

import java.util.Set;

import pl.edu.icm.unity.store.DBTypeTestBase;

public class DBAttributesClassTest extends DBTypeTestBase<DBAttributesClass>
{

	@Override
	protected String getJson()
	{

		return "{\"name\":\"name\",\"description\":\"desc\",\"allowed\":[\"allowed\",\"mandatory\"],"
				+ "\"mandatory\":[\"mandatory\"],\"allowArbitrary\":false,\"parentClasses\":[\"parent\"]}\n";
	}

	@Override
	protected DBAttributesClass getObject()
	{
		return DBAttributesClass.builder()
				.withName("name")
				.withAllowArbitrary(false)
				.withDescription("desc")
				.withParentClasses(Set.of("parent"))
				.withAllowed(Set.of("allowed", "mandatory"))
				.withMandatory(Set.of("mandatory"))
				.withParentClasses(Set.of("parent"))
				.build();
	}

}
