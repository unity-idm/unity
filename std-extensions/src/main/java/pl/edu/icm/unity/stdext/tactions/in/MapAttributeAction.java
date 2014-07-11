/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.tactions.in;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.mvel2.MVEL;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.translation.TranslationActionDescription;
import pl.edu.icm.unity.server.translation.in.AbstractInputTranslationAction;
import pl.edu.icm.unity.server.translation.in.AttributeEffectMode;
import pl.edu.icm.unity.server.translation.in.MappedAttribute;
import pl.edu.icm.unity.server.translation.in.MappingResult;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;

/**
 * Maps attributes using MVEL expressions.
 *   
 * @author K. Benedyczak
 */
public class MapAttributeAction extends AbstractInputTranslationAction
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, MapAttributeAction.class);
	private final AttributesManagement attrMan;
	private String unityAttribute;
	private String group;
	private AttributeVisibility visibility;
	private Serializable expressionCompiled;
	private AttributeEffectMode mode;
	private AttributeType at;

	public MapAttributeAction(String[] params, TranslationActionDescription desc, AttributesManagement attrsMan) 
			throws EngineException
	{
		super(desc, params);
		setParameters(params);
		attrMan = attrsMan;
		at = attrMan.getAttributeTypesAsMap().get(unityAttribute);
		if (at == null)
			throw new WrongArgumentException("Attribute type " + unityAttribute + " is not known");
	}
	
	@Override
	protected MappingResult invokeWrapped(RemotelyAuthenticatedInput input, Object mvelCtx, 
			String currentProfile) throws EngineException
	{
		MappingResult ret = new MappingResult();
		Object value = MVEL.executeExpression(expressionCompiled, mvelCtx);
		if (value == null)
		{
			log.debug("Attribute value evaluated to null, skipping");
			return ret;
		}
		List<?> aValues = value instanceof List ? (List<?>)value : Collections.singletonList(value.toString());
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		Attribute<?> attribute = new Attribute(unityAttribute, at.getValueType(), group, 
				visibility, aValues, input.getIdpName(), currentProfile);
		MappedAttribute ma = new MappedAttribute(mode, attribute);
		log.debug("Mapped attribute: " + attribute);
		ret.addAttribute(ma);
		return ret;
	}

	private void setParameters(String[] parameters)
	{
		if (parameters.length != 5)
			throw new IllegalArgumentException("Action requires exactly 5 parameters");
		unityAttribute = parameters[0];
		group = parameters[1];
		expressionCompiled = MVEL.compileExpression(parameters[2]);
		visibility = AttributeVisibility.valueOf(parameters[3]);
		mode = AttributeEffectMode.valueOf(parameters[4]);
	}
}
