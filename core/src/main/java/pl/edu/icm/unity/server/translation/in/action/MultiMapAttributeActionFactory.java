/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.in.action;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.authn.remote.RemoteAttribute;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.translation.in.AttributeEffectMode;
import pl.edu.icm.unity.server.translation.in.InputTranslationAction;
import pl.edu.icm.unity.server.translation.in.MappedAttribute;
import pl.edu.icm.unity.server.translation.in.MappingResult;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition.Type;
import pl.edu.icm.unity.types.translation.TranslationActionType;

/**
 * Maps multiple attributes only by providing new names, values are unchanged.
 *   
 * @author K. Benedyczak
 */
@Component
public class MultiMapAttributeActionFactory extends AbstractInputTranslationActionFactory
{
	public static final String NAME = "multiMapAttribute";
	private AttributesManagement attrsMan;
	
	@Autowired
	public MultiMapAttributeActionFactory(@Qualifier("insecure") AttributesManagement attrsMan)
	{
		super(NAME, new ActionParameterDefinition[] {
				new ActionParameterDefinition(
						"mapping",
						"TranslationAction.multiMapAttribute.paramDesc.mapping",
						Type.LARGE_TEXT),
				new ActionParameterDefinition(
						"visibility",
						"TranslationAction.mapAttribute.paramDesc.visibility",
						AttributeVisibility.class),
				new ActionParameterDefinition(
						"effect",
						"TranslationAction.mapAttribute.paramDesc.effect",
						AttributeEffectMode.class)});
		this.attrsMan = attrsMan;
	}

	@Override
	public InputTranslationAction getInstance(String... parameters)
	{
		return new MultiMapAttributeAction(parameters, getActionType(), attrsMan);
	}
	
	public static class MultiMapAttributeAction extends InputTranslationAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, MultiMapAttributeAction.class);
		private final AttributesManagement attrMan;
		private String mapping;
		private AttributeVisibility visibility;
		private AttributeEffectMode mode;
		private List<Mapping> mappings;

		public MultiMapAttributeAction(String[] params, TranslationActionType desc, AttributesManagement attrsMan) 
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
				typedValues = MapAttributeActionFactory.MapAttributeAction.convertValues(
						ra.getValues(), at.getValueType());
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
		
		private void parseMapping()
		{
			mappings = new ArrayList<MultiMapAttributeAction.Mapping>();
			Map<String, AttributeType> attributeTypes;
			try
			{
				attributeTypes = attrMan.getAttributeTypesAsMap();
			} catch (EngineException e)
			{
				throw new InternalException("Can't get attribute types", e);
			}
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
					throw new IllegalArgumentException("Attribute type " + tokens[1] + " is not known");
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

}
