/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.generic;

import org.apache.ibatis.session.SqlSession;

import pl.edu.icm.unity.db.model.GenericObjectBean;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Provides support for the object stored in the generic objects table.
 * Implementation must not be stateful and therefore must be thread safe: single instance
 * is used for handling all objects of the type.
 * @author K. Benedyczak
 */
public interface GenericEntityHandler<T>
{
	public String getType();
	public Class<T> getModelClass();
	
	public byte[] updateBeforeImport(String name, JsonNode node) throws JsonProcessingException;

	public GenericObjectBean toBlob(T value, SqlSession sql);
	public T fromBlob(GenericObjectBean blob, SqlSession sql);
}
