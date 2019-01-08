/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.translation.out.action;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.translation.out.OutputTranslationAction;
import pl.edu.icm.unity.engine.api.translation.out.TranslationInput;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.engine.attribute.AttributeTypeHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.DynamicAttribute;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition.Type;
import pl.edu.icm.unity.types.translation.TranslationActionType;

/**
 * Map name of unity attribute to external name
 * 
 * @author P.Piernik
 *
 */
@Component
public class MapAttributeNameActionFactory extends AbstractOutputTranslationActionFactory
{
	public static final String NAME = "mapAttributeName";
	private AttributeTypeHelper atHelper;
	
	@Autowired
	public MapAttributeNameActionFactory(AttributeTypeHelper atHelper, UnityMessageSource msg)
	{
		super(NAME, new ActionParameterDefinition("unityAttribute",
				"TranslationAction.mapAttributeName.paramDesc.unityAttribute",
				Type.UNITY_ATTRIBUTE, true),
				new ActionParameterDefinition("attributeName",
						"TranslationAction.mapAttributeName.paramDesc.attributeName",
						Type.TEXT, true),
				new ActionParameterDefinition("mandatory",
						"TranslationAction.mapAttributeName.paramDesc.mandatory",
						Type.BOOLEAN, true),
				new ActionParameterDefinition("attributeDisplayName",
						"TranslationAction.mapAttributeName.paramDesc.attributeDisplayName",
						Type.TEXT, false),
				new ActionParameterDefinition("attributeDescription",
						"TranslationAction.mapAttributeName.paramDesc.attributeDescription",
						Type.TEXT, false));
		this.atHelper = atHelper;
	}

	@Override
	public OutputTranslationAction getInstance(String... parameters)
	{
		return new MapAttributeNameAction(parameters, getActionType(), atHelper);
	}

	public static class MapAttributeNameAction extends OutputTranslationAction

	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION,
				MapAttributeNameAction.class);

		private String unityAttribute;
		private String attrName;
		private String attrDisplayname;
		private String attrDescription;
		private boolean attrMandatory;
		private AttributeTypeHelper atHelper;

		public MapAttributeNameAction(String[] params, TranslationActionType desc, AttributeTypeHelper atHelper)
		{
			super(desc, params);
			setParameters(params);
			this.atHelper = atHelper;
		}

		@Override
		protected void invokeWrapped(TranslationInput input, Object mvelCtx,
				String currentProfile, TranslationResult result)
				throws EngineException
		{
			DynamicAttribute at = null;
			for (DynamicAttribute a : result.getAttributes())
			{
				if (a.getAttribute().getName().equals(unityAttribute))
				{
					at = a;
				}
			}
			if (at == null)
			{
				log.debug("Attribute " + unityAttribute
						+ " is not defined, skipping");
				return;
			}

			if (result.removeAttributesByName(unityAttribute))
			{
				// check if attribute is also in attribute to
				// persist and remove them.
				result.removeAttributesToPersistByName(unityAttribute);
			}			
			
			Attribute newAttr = new Attribute(attrName, at.getAttribute().getValueSyntax(), "/",
					at.getAttribute().getValues());
			
			
			AttributeType attrType = null;

			try
			{
				attrType = atHelper.getTypeForAttributeName(unityAttribute);
			} catch (Exception e)
			{
				log.debug("Cannot find attribute type for " + unityAttribute
						+ ", using default string attribute type");
				attrType = new AttributeType(attrName, StringAttributeSyntax.ID);
			}
					
			result.getAttributes().add(new DynamicAttribute(newAttr, attrType,
					attrDisplayname, attrDescription, attrMandatory));
			
			log.debug("Map attribute name " + unityAttribute + " -> " + attrName);
		}
		
		private void setParameters(String[] parameters)
		{
			unityAttribute = parameters[0];
			attrName = parameters[1];
			attrMandatory = Boolean.valueOf(parameters[2]);
			if (parameters.length > 3)
				attrDisplayname = parameters[3];
			if (parameters.length > 4) 
				attrDescription = parameters[4];
		}
	}
}
