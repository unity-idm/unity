/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.in.action;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.attribute.IllegalAttributeValueException;
import pl.edu.icm.unity.base.translation.ActionParameterDefinition;
import pl.edu.icm.unity.base.translation.TranslationActionType;
import pl.edu.icm.unity.base.translation.ActionParameterDefinition.Type;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAttribute;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.engine.api.translation.ExternalDataParser;
import pl.edu.icm.unity.engine.api.translation.in.AttributeEffectMode;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationAction;
import pl.edu.icm.unity.engine.api.translation.in.MappedAttribute;
import pl.edu.icm.unity.engine.api.translation.in.MappingResult;

/**
 * Maps multiple attributes only by providing new names, values are unchanged.
 *   
 * @author K. Benedyczak
 */
@Component
public class MultiMapAttributeActionFactory extends AbstractInputTranslationActionFactory
{
	public static final String NAME = "multiMapAttribute";
	private AttributeTypeSupport attrsMan;
	private ExternalDataParser externalDataParser;
	
	@Autowired
	public MultiMapAttributeActionFactory(AttributeTypeSupport attrsMan, ExternalDataParser externalDataParser)
	{
		super(NAME, new ActionParameterDefinition[] {
				new ActionParameterDefinition(
						"mapping",
						"TranslationAction.multiMapAttribute.paramDesc.mapping",
						Type.LARGE_TEXT, true),
				new ActionParameterDefinition(
						"effect",
						"TranslationAction.mapAttribute.paramDesc.effect",
						AttributeEffectMode.class, true)});
		this.attrsMan = attrsMan;
		this.externalDataParser = externalDataParser;
	}

	@Override
	public InputTranslationAction getInstance(String... parameters)
	{
		return new MultiMapAttributeAction(parameters, getActionType(), attrsMan, externalDataParser);
	}
	
	public static class MultiMapAttributeAction extends InputTranslationAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, 
				MultiMapAttributeAction.class);
		private final AttributeTypeSupport attrMan;
		private String mapping;
		private AttributeEffectMode mode;
		private List<Mapping> mappings;
		private ExternalDataParser externalDataParser;

		public MultiMapAttributeAction(String[] params, TranslationActionType desc, 
				AttributeTypeSupport attrsMan, ExternalDataParser externalDataParser) 
		{
			super(desc, params);
			this.externalDataParser = externalDataParser;
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

		private void mapSingle(MappingResult ret, RemoteAttribute ra, Mapping mapping, String idp, 
				String profile)
		{
			AttributeType at = mapping.local;
			Attribute attribute;
			try
			{
				attribute = externalDataParser.parseAsAttribute(at, mapping.group, ra.getValues(), 
						idp, profile);
			} catch (IllegalAttributeValueException e)
			{
				log.debug("Can't convert attribute values returned by the action's expression "
						+ "to the type of attribute " + at.getName() + " , skipping it", e);
				return;
			}
			log.debug("Mapped attribute: " + attribute);
			ret.addAttribute(new MappedAttribute(mode, attribute));
		}
		
		private void parseMapping()
		{
			mappings = new ArrayList<MultiMapAttributeAction.Mapping>();
			Map<String, AttributeType> attributeTypes = attrMan.getAttributeTypes()
					.stream()
					.collect(Collectors.toMap(at -> at.getName(), at -> at));
			String lines[] = mapping.split("\n");
			int num = 0;
			for (String line: lines)
			{
				num++;
				line = line.trim();
				String tokens[] = line.split("[ ]+");
				if (tokens.length != 3)
					throw new IllegalArgumentException("Line " + num + 
							" is invalid: must have 3 tokens");
				
				AttributeType at = attributeTypes.get(tokens[1]);
				if (at == null)
					throw new IllegalArgumentException("Attribute type " + tokens[1] + 
							" is not known");
				Mapping map = new Mapping(tokens[0], at, tokens[2]);
				mappings.add(map);
			}
		}
		
		
		private void setParameters(String[] parameters)
		{
			mapping = parameters[0];
			mode = AttributeEffectMode.valueOf(parameters[1]);
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
