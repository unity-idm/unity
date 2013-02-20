/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.AttributeValueTypesRegistry;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.types.Attribute;
import pl.edu.icm.unity.types.AttributeType;
import pl.edu.icm.unity.types.AttributeValueSyntax;
import pl.edu.icm.unity.types.AttributesClass;
import pl.edu.icm.unity.types.EntityParam;

/**
 * Implements attributes operations.
 * @author K. Benedyczak
 */
@Component
public class AttributesManagementImpl implements AttributesManagement
{
	private AttributeValueTypesRegistry attrValueTypesReg;
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String[] getSupportedAttributeValueTypes() throws EngineException
	{
		Collection<AttributeValueSyntax<?>> all = attrValueTypesReg.getAll();
		String[] ret = new String[all.size()];
		Iterator<AttributeValueSyntax<?>> it = all.iterator();
		for (int i=0; it.hasNext(); i++)
			ret[i] = it.next().getValueSyntaxId();
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T> void addAttributeType(AttributeType<T> at) throws EngineException
	{
		throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T> void updateAttributeType(AttributeType<T> at) throws EngineException
	{
		throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeAttributeType(String id, boolean deleteInstances) throws EngineException
	{
		throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<AttributeType<?>> getAttributeTypes() throws EngineException
	{
		throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addAttributeClass(AttributesClass clazz) throws EngineException
	{
		throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeAttributeClass(String id) throws EngineException
	{
		throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void assignAttributeClasses(EntityParam entity, String[] classes)
			throws EngineException
	{
		throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T> void setAttribute(EntityParam entity, Attribute<T> attribute, boolean update)
			throws EngineException
	{
		throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeAttribute(EntityParam entity, String groupPath, String attributeTypeId)
			throws EngineException
	{
		throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T> List<Attribute<T>> getAttributes(EntityParam entity, String groupPath,
			String attributeTypeId) throws EngineException
	{
		throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T> List<Attribute<T>> getHiddenAttributes(EntityParam entity, String groupPath,
			String attributeTypeId) throws EngineException
	{
		throw new RuntimeException("NOT implemented"); // TODO Auto-generated method stub
	}

}
