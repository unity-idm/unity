/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.tactions;

import java.util.Map;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.authn.remote.RemoteAttribute;
import pl.edu.icm.unity.server.authn.remote.RemoteInformationBase;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.authn.remote.translation.AbstractTranslationAction;
import pl.edu.icm.unity.server.utils.Log;

/**
 * Add an attribute to the user.
 *   
 * @author K. Benedyczak
 */
public class AddAttributeAction extends AbstractTranslationAction
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, AddAttributeAction.class);
	private String attribute;
	private String group;
	private String value;

	public AddAttributeAction(String[] params)
	{
		setParameters(params);
	}
	
	@Override
	public String getName()
	{
		return AddAttributeActionFactory.NAME;
	}

	@Override
	protected void invokeWrapped(RemotelyAuthenticatedInput input) throws EngineException
	{
		log.debug("Adding an attribute " + attribute);
		Map<String, RemoteAttribute> attrs = input.getAttributes();
		RemoteAttribute newAt = value == null ? 
				new RemoteAttribute(attribute) :
				new RemoteAttribute(attribute, value); 
		newAt.getMetadata().put(RemoteInformationBase.UNITY_ATTRIBUTE, attribute);
		newAt.getMetadata().put(RemoteInformationBase.UNITY_GROUP, group);
		attrs.put(attribute, newAt);
	}

	@Override
	public String[] getParameters()
	{
		return new String[] {group};
	}

	private void setParameters(String[] parameters)
	{
		if (parameters.length != 3)
			throw new IllegalArgumentException("Action requires exactely 3 parameters");
		attribute = parameters[0];
		group = parameters[1];
		value = parameters[3];
	}
}
