/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.identity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;

/**
 * Identity type which creates a different but persistent identifier for each target.
 *  
 * @author K. Benedyczak
 */
@Component
public class TargetedPersistentIdentity extends AbstractIdentityTypeProvider
{
	static final Logger log = Log.getLogger(Log.U_SERVER, TargetedPersistentIdentity.class);
	public static final String ID = "targetedPersistent";
	private static final List<Attribute<?>> empty = Collections.unmodifiableList(new ArrayList<Attribute<?>>(0));
	private ObjectMapper mapper;
	
	@Autowired
	public TargetedPersistentIdentity(ObjectMapper mapper)
	{
		this.mapper = mapper;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getId()
	{
		return ID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDefaultDescription()
	{
		return "Targeted persistent id";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<AttributeType> getAttributesSupportedForExtraction()
	{
		return Collections.emptySet();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void validate(String value) throws IllegalIdentityValueException
	{
		if (value != null)
			throw new IllegalIdentityValueException("Only null identity value is allowed "
					+ "for dynamic identity type");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getComparableValue(String from)
	{
		return from;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Attribute<?>> extractAttributes(String from, Map<String, String> toExtract)
	{
		return empty; 
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toPrettyStringNoPrefix(String from)
	{
		return from;
	}

	@Override
	public boolean isDynamic()
	{
		return true;
	}
	
	@Override
	public String toExternalForm(String realm, String target, String inDbValue)
	{
		if (realm == null || target == null || inDbValue == null)
			return null;
		StringTargetedIdsModel db = new StringTargetedIdsModel(mapper, inDbValue);
		return db.getIdentity(realm, target);
	}

	@Override
	public String createNewIdentity(String realm, String target, String inDbValue)
			throws IllegalTypeException
	{
		if (realm == null || target == null)
			throw new IllegalTypeException("Identity can be created only when target is defined");
		StringTargetedIdsModel db = new StringTargetedIdsModel(mapper, inDbValue);
		db.addIdentity(realm, target, UUID.randomUUID().toString());
		return db.serialize();
	}

	@Override
	public String resetIdentity(String realm, String target, String inDbValue)
			throws IllegalTypeException
	{
		if (inDbValue == null || (realm == null && target == null))
			return null;

		StringTargetedIdsModel db = new StringTargetedIdsModel(mapper, inDbValue);
		db.resetIdentities(realm, target);
		return db.serialize();
	}
}






