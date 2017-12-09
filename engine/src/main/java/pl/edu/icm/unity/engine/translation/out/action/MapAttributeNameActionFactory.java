/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
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
	private UnityMessageSource msg;
	
	@Autowired
	public MapAttributeNameActionFactory(AttributeTypeHelper atHelper, UnityMessageSource msg)
	{
		super(NAME, new ActionParameterDefinition("unityAttribute",
				"TranslationAction.mapAttributeName.paramDesc.unityAttribute",
				Type.UNITY_ATTRIBUTE),
				new ActionParameterDefinition("attributeName",
						"TranslationAction.mapAttributeName.paramDesc.attributeName",
						Type.EXPRESSION),
				new ActionParameterDefinition("mandatory",
						"TranslationAction.mapAttributeName.paramDesc.mandatory",
						Type.BOOLEAN),
				new ActionParameterDefinition("attributeDisplayName",
						"TranslationAction.mapAttributeName.paramDesc.attributeDisplayName",
						Type.TEXT),
				new ActionParameterDefinition("attributeDescription",
						"TranslationAction.mapAttributeName.paramDesc.attributeDescription",
						Type.TEXT));
		this.atHelper = atHelper;
		this.msg = msg;
	}

	@Override
	public OutputTranslationAction getInstance(String... parameters)
	{
		return new MapAttributeNameAction(parameters, getActionType(), atHelper, msg);
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
		private UnityMessageSource msg;

		public MapAttributeNameAction(String[] params, TranslationActionType desc, AttributeTypeHelper atHelper, UnityMessageSource msg)
		{
			super(desc, params);
			setParameters(params);
			this.atHelper = atHelper;
			this.msg = msg;
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
				//ok
			}
					
			result.getAttributes().add(new DynamicAttribute(newAttr, attrType,
					getAttrName(attrDisplayname, attrName, attrType),
					getAttrDescription(attrDescription, attrName, attrType),
					attrMandatory));
			
			log.debug("Map attribute name " + unityAttribute + " -> " + attrName);
		}

		private String getAttrName(String attrDisplayname, String attrName,
				AttributeType type)
		{
			if (attrDisplayname != null && !attrDisplayname.isEmpty())
				return attrDisplayname;

			if (type != null && type.getDisplayedName() != null)
				return type.getDisplayedName().getValue(msg);

			return attrName;

		}

		private String getAttrDescription(String attrDescription, String attrName,
				AttributeType type)
		{
			if (attrDescription != null && !attrDescription.isEmpty())
				return attrDescription;

			if (type != null && type.getDescription() != null)
				return type.getDescription().getValue(msg);

			return attrName;
		}
		
		private void setParameters(String[] parameters)
		{
			if (parameters.length < 3)
				throw new IllegalArgumentException(
						"Action requires minimum 3 parameters");
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
