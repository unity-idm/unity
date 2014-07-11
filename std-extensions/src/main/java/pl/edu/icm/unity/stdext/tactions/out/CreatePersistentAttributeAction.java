/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.tactions.out;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.mvel2.MVEL;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.translation.TranslationActionDescription;
import pl.edu.icm.unity.server.translation.out.AbstractOutputTranslationAction;
import pl.edu.icm.unity.server.translation.out.TranslationInput;
import pl.edu.icm.unity.server.translation.out.TranslationResult;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;

/**
 * Creates new outgoing attributes.
 *   
 * @author K. Benedyczak
 */
public class CreatePersistentAttributeAction extends AbstractOutputTranslationAction
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, CreatePersistentAttributeAction.class);
	private String attrNameString;
	private AttributeType attributeType;
	private Serializable valuesExpression;
	private String group;

	public CreatePersistentAttributeAction(String[] params, TranslationActionDescription desc, 
			AttributesManagement attrsMan) throws EngineException
	{
		super(desc, params);
		setParameters(params, attrsMan);
	}

	@Override
	protected void invokeWrapped(TranslationInput input, Object mvelCtx, String currentProfile,
			TranslationResult result) throws EngineException
	{
		Object value = MVEL.executeExpression(valuesExpression, mvelCtx);
		if (value == null)
		{
			log.debug("Attribute value evaluated to null, skipping");
			return;
		}
		for (Attribute<?> existing: result.getAttributes())
		{
			if (existing.getName().equals(attrNameString))
			{
				log.trace("Attribute already exists, skipping");
				return;
			}
		}
		List<?> values;		
		if (value instanceof List)
			values = (List<?>) value;
		else
			values = Collections.singletonList(value.toString());
		
		@SuppressWarnings({ "unchecked", "rawtypes"})
		Attribute<?> newAttr = new Attribute(attrNameString, attributeType.getValueType(), group, 
				AttributeVisibility.full, values, null, currentProfile);
		result.getAttributes().add(newAttr);
		result.getAttributesToPersist().add(newAttr);
		log.debug("Created a new persisted attribute: " + newAttr);
	}

	private void setParameters(String[] parameters, AttributesManagement attrsMan)
	{
		if (parameters.length != 3)
			throw new IllegalArgumentException("Action requires exactly 3 parameters");
		attrNameString = parameters[0];
		valuesExpression = MVEL.compileExpression(parameters[1]);
		group = parameters[2];

		try
		{
			attributeType = attrsMan.getAttributeTypesAsMap().get(attrNameString);
			if (attributeType == null)
				throw new IllegalArgumentException("The attribute type " + parameters[0] + 
						" is not a valid Unity attribute type and therefore can not be persisted");
			if (attributeType.isInstanceImmutable())
				throw new IllegalArgumentException("The attribute type " + parameters[0] + 
						" is managed internally only so it can not be persisted");
		} catch (EngineException e)
		{
			throw new IllegalStateException("Can not verify attribute type", e);
		}
	}

}
