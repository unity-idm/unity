/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.tactions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.authn.remote.translation.ActionParameterDesc;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationAction;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationActionFactory;

/**
 * Factory for {@link UpdateAttributesAction}
 *   
 * @author K. Benedyczak
 */
@Component
public class UpdateAttributesActionFactory implements TranslationActionFactory
{
	public static final String NAME = "updateAttributes";
	
	private static final ActionParameterDesc[] PARAMS = {
		new ActionParameterDesc(true, "pattern", 
				"Regular expression describing which attributes should be updated", 20),
		new ActionParameterDesc(true, "valuesOnly", 
				"If true, then only the values of already existing attributes will be updated.", 10)
	};
	
	private AttributesManagement attrsMan;

	@Autowired
	public UpdateAttributesActionFactory(@Qualifier("insecure") AttributesManagement attrsMan)
	{
		this.attrsMan = attrsMan;
	}
	
	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescription()
	{
		return "Updates selected attributes of the client. Only attributes that has been previously mapped " +
				"to local name and have assigned group scope can be updated (other are ignored)." +
				" Can work in two modes: either all attributes are added/updated or only the values " +
				"are updated for those attributes which are already present locally.";
	}

	@Override
	public ActionParameterDesc[] getParameters()
	{
		return PARAMS;
	}

	@Override
	public TranslationAction getInstance(String... parameters) throws EngineException
	{
		return new UpdateAttributesAction(parameters, attrsMan);
	}
}
