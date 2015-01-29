/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.tactions.in;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.authn.remote.RemoteAttribute;
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
 * Maps multiple attributes only by providing new names, values are unchanged.
 *   
 * @author K. Benedyczak
 */
public class MultiMapAttributeAction extends AbstractInputTranslationAction
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, MultiMapAttributeAction.class);
	private final AttributesManagement attrMan;
	private String mapping;
	private AttributeVisibility visibility;
	private AttributeEffectMode mode;
	private List<Mapping> mappings;

	public MultiMapAttributeAction(String[] params, TranslationActionDescription desc, AttributesManagement attrsMan) 
			throws EngineException
	{
		super(desc, params);
		setParameters(params);
		attrMan = attrsMan;
		parseMapping();
	}
	
	@Override
	protected MappingResult invokeWrapped(RemotelyAuthenticatedInput input, Object mvelCtx, 
			String currentProfile)
	{
		MappingResult ret = new MappingResult();

		Map<String, RemoteAttribute> remoteAttrs = input.getAttributes();
		for (Mapping mapping: mappings)
		{
			RemoteAttribute ra = remoteAttrs.get(mapping.external);
			if (ra != null)
				mapSingle(ret, ra, mapping, input.getIdpName(), currentProfile);
		}
		return ret;
	}

	private void mapSingle(MappingResult ret, RemoteAttribute ra, Mapping mapping, String idp, String profile)
	{
		AttributeType at = mapping.local;
		List<Object> typedValues;
		try
		{
			typedValues = MapAttributeAction.convertValues(ra.getValues(), at.getValueType());
		} catch (IllegalAttributeValueException e)
		{
			log.debug("Can't convert attribute values returned by the action's expression "
					+ "to the type of attribute " + at.getName() + " , skipping it", e);
			return;
		}
		@SuppressWarnings({ "unchecked", "rawtypes" })
		Attribute<?> attribute = new Attribute(at.getName(), at.getValueType(), mapping.group, 
				visibility, typedValues, idp, profile);
		MappedAttribute ma = new MappedAttribute(mode, attribute);
		log.debug("Mapped attribute: " + attribute);
		ret.addAttribute(ma);
	}
	
	private void parseMapping() throws EngineException
	{
		mappings = new ArrayList<MultiMapAttributeAction.Mapping>();
		Map<String, AttributeType> attributeTypes = attrMan.getAttributeTypesAsMap();
		String lines[] = mapping.split("\n");
		int num = 0;
		for (String line: lines)
		{
			num++;
			line = line.trim();
			String tokens[] = line.split("[ ]+");
			if (tokens.length != 3)
				throw new IllegalArgumentException("Line " + num + " is invalid: must have 3 tokens");
			
			AttributeType at = attributeTypes.get(tokens[1]);
			if (at == null)
				throw new WrongArgumentException("Attribute type " + tokens[1] + " is not known");
			Mapping map = new Mapping(tokens[0], at, tokens[2]);
			mappings.add(map);
		}
	}
	
	
	private void setParameters(String[] parameters)
	{
		if (parameters.length != 3)
			throw new IllegalArgumentException("Action requires exactly 3 parameters");
		mapping = parameters[0];
		visibility = AttributeVisibility.valueOf(parameters[1]);
		mode = AttributeEffectMode.valueOf(parameters[2]);
	}
	
	private static class Mapping
	{
		private String external;
		private AttributeType local;
		private String group;

		public Mapping(String external, AttributeType local, String group)
		{
			this.external = external;
			this.local = local;
			this.group = group;
		}
	}
}
