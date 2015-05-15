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
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.stdext.utils.Escaper;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.IdentityRepresentation;

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
			throws IllegalIdentityValueException
	{
		if (realm == null || target == null || inDbValue == null)
			throw new IllegalIdentityValueException("Insufficient arguments");
		return inDbValue;
	}

	@Override
	public IdentityRepresentation createNewIdentity(String realm, String target, String inDbValue)
			throws IllegalTypeException
	{
		if (realm == null || target == null)
			throw new IllegalTypeException("Identity can be created only when target is defined");
		if (inDbValue == null)
			inDbValue = UUID.randomUUID().toString();
		String cmpVal = Escaper.encode(realm, target, inDbValue);
		return new IdentityRepresentation(cmpVal, inDbValue);
	}

	@Override
	public boolean isTargeted()
	{
		return true;
	}

	@Override
	public String getComparableValue(String from, String realm, String target)
			throws IllegalIdentityValueException
	{
		return Escaper.encode(realm, target, from);
	}

	@Override
	public String toExternalFormNoContext(String inDbValue)
	{
		return inDbValue;
	}

	@Override
	public boolean isExpired(IdentityRepresentation representation)
	{
		return false;
	}
	
	@Override
	public String getHumanFriendlyDescription(MessageSource msg)
	{
		return msg.getMessage("TargetedPersistentIdentity.description");
	}
	

	@Override
	public String toHumanFriendlyString(MessageSource msg, String from)
	{
		return msg.getMessage("TargetedPersistentIdentity.fullyAnonymous");
	}
	
	@Override
	public boolean isVerifiable()
	{
		return false;
	}

	@Override
	public String getHumanFriendlyName(MessageSource msg)
	{
		return msg.getMessage("TargetedPersistentIdentity.name");
	}
}






