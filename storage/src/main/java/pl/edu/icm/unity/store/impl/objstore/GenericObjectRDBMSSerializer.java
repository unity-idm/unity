/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.objstore;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.store.rdbms.RDBMSObjectSerializer;

/**
 * Noop serializer - in this case we use the in-DB bean as-is (is used in API) so no serialization is performed.
 * Actual serialziation is done in object-type specific way in higher level code.
 * @author K. Benedyczak
 */
@Component
public class GenericObjectRDBMSSerializer implements RDBMSObjectSerializer<GenericObjectBean, GenericObjectBean>
{
	@Override
	public GenericObjectBean toDB(GenericObjectBean object)
	{
		return object;
	}

	@Override
	public GenericObjectBean fromDB(GenericObjectBean bean)
	{
		return bean;
	}
}
