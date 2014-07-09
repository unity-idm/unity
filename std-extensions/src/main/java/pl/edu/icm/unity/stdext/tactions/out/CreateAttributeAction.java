/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.tactions.out;

import java.io.Serializable;

import org.apache.log4j.Logger;
import org.mvel2.MVEL;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.server.translation.TranslationActionDescription;
import pl.edu.icm.unity.server.translation.out.AbstractOutputTranslationAction;
import pl.edu.icm.unity.server.translation.out.CreationMode;
import pl.edu.icm.unity.server.translation.out.TranslationInput;
import pl.edu.icm.unity.server.translation.out.TranslationResult;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityTypeDefinition;

/**
 * Creates new outgoing attributes.
 *   
 * @author K. Benedyczak
 */
public class CreateAttributeAction extends AbstractOutputTranslationAction
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, CreateAttributeAction.class);
	private IdentityTypeDefinition idType;
	private String attrNameString;
	private AttributeType attributeType;
	private Serializable valuesExpression;
	private AttributesManagement attrsMan;
	private CreationMode creationMode;

	public CreateAttributeAction(String[] params, TranslationActionDescription desc) 
			throws EngineException
	{
		super(desc, params);
		setParameters(params);
	}

	@Override
	protected void invokeWrapped(TranslationInput input, Object mvelCtx, String currentProfile,
			TranslationResult result) throws EngineException
	{
		Object value = MVEL.executeExpression(valuesExpression, mvelCtx);
		for (Attribute<?> existing: result.getAttributes())
		{
			if (existing.getName().equals(attrNameString))
			{
				log.trace("Attribute already exists, skipping");
				return;
			}
		}
		/* TODO
		Attribute<?> newAttr = new Attribute();
		newId.setValue(value);
		newId.setTypeId(idTypeString);
		result.getAttributes().add(newAttr);
		if (creationMode == CreationMode.PERSISTENT)
			result.getAttributesToPersist().add(newAttr);
		log.debug("Created a new " + creationMode.toString().toLowerCase() + " attribute: " + newAttr);
		*/
	}

	private void setParameters(String[] parameters)
	{
		if (parameters.length != 3)
			throw new IllegalArgumentException("Action requires exactly 3 parameters");
		attrNameString = parameters[0];
		valuesExpression = MVEL.compileExpression(parameters[1]);
		creationMode = CreationMode.valueOf(parameters[2]);

		try
		{
			attributeType = attrsMan.getAttributeTypesAsMap().get(attrNameString);
			if (attributeType == null && creationMode == CreationMode.PERSISTENT)
				throw new IllegalArgumentException("The attribute type " + parameters[0] + 
						" is not a valid Unity attribute type and therefore can not be persisted");
			if (!attributeType.isInstanceImmutable() && creationMode == CreationMode.PERSISTENT)
				throw new IllegalArgumentException("The attribute type " + parameters[0] + 
						" is managed internally only so it can not be persisted");
		} catch (EngineException e)
		{
			throw new IllegalStateException("Can not verify attribute type", e);
		}
	}

}
