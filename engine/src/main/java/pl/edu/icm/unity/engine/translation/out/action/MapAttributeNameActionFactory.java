/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.translation.out.action;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.translation.out.OutputTranslationAction;
import pl.edu.icm.unity.engine.api.translation.out.TranslationInput;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.types.basic.Attribute;
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

	public MapAttributeNameActionFactory()
	{
		super(NAME, new ActionParameterDefinition("unityAttribute",
				"TranslationAction.mapAttributeName.paramDesc.unityAttribute",
				Type.UNITY_ATTRIBUTE),
				new ActionParameterDefinition("attributeName",
						"TranslationAction.mapAttributeName.paramDesc.attributeName",
						Type.EXPRESSION));
	}

	@Override
	public OutputTranslationAction getInstance(String... parameters)
	{
		return new MapAttributeNameAction(parameters, getActionType());
	}

	public static class MapAttributeNameAction extends OutputTranslationAction

	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION,
				MapAttributeNameAction.class);

		private String unityAttribute;
		private String attrName;

		public MapAttributeNameAction(String[] params, TranslationActionType desc)
		{
			super(desc, params);
			setParameters(params);
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

			Attribute newAttr = new Attribute(attrName, StringAttributeSyntax.ID, "/",
					at.getAttribute().getValues());
			result.getAttributes().add(new DynamicAttribute(newAttr));
			log.debug("Map attribute name " + unityAttribute + " -> " + attrName);
		}

		private void setParameters(String[] parameters)
		{
			if (parameters.length != 2)
				throw new IllegalArgumentException(
						"Action requires exactly 2 parameters");
			unityAttribute = parameters[0];
			attrName = parameters[1];
		}

	}
}
