/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.form.action;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.mvel2.MVEL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.mvel.MVELExpressionContext;
import pl.edu.icm.unity.engine.api.translation.ActionValidationException;
import pl.edu.icm.unity.engine.api.translation.ExternalDataParser;
import pl.edu.icm.unity.engine.api.translation.form.DynamicGroupParam;
import pl.edu.icm.unity.engine.api.translation.form.GroupRestrictedFormValidationContext;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationContext;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationMVELContextKey;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationTranslationAction;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.registration.GroupSelection;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition.Type;
import pl.edu.icm.unity.types.translation.TranslationActionType;

/**
 * Allows for adding an additional attribute to for the requester
 * 
 * @author K. Benedyczak
 */
@Component
public class AddAttributeActionFactory extends AbstractRegistrationTranslationActionFactory
{
	public static final String NAME = "addAttribute";
	private AttributeTypeSupport attrsSupport;
	private ExternalDataParser externalDataParser;
	
	@Autowired
	public AddAttributeActionFactory(AttributeTypeSupport attrsSupport, ExternalDataParser externalDataParser)
	{
		super(NAME, new ActionParameterDefinition[] {
				new ActionParameterDefinition(
						"attributeName",
						"RegTranslationAction.addAttribute.paramDesc.attributeName",
						Type.UNITY_ATTRIBUTE, true),
				new ActionParameterDefinition(
						"group",
						"RegTranslationAction.addAttribute.paramDesc.group",
						Type.UNITY_DYNAMIC_GROUP, true),
				new ActionParameterDefinition("expression", "RegTranslationAction.addAttribute.paramDesc.expression",
						Type.EXPRESSION, true,
						MVELExpressionContext.builder().withTitleKey("RegTranslationAction.addAttribute.editor.title")
								.withEvalToKey("RegTranslationAction.addAttribute.editor.evalTo")
								.withVars(RegistrationMVELContextKey.toMap()).build()) });
		this.attrsSupport = attrsSupport;
		this.externalDataParser = externalDataParser;
	}

	@Override
	public RegistrationTranslationAction getInstance(String... parameters)
	{
		return new AddAttributeAction(getActionType(), parameters, attrsSupport, externalDataParser);
	}
	
	public static class AddAttributeAction extends RegistrationTranslationAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION,
				AddAttributeActionFactory.AddAttributeAction.class);
		private String unityAttribute;
		private String group;
		private Serializable expressionCompiled;
		private AttributeType at;
		private ExternalDataParser externalDataParser;
		
		public AddAttributeAction(TranslationActionType description, String[] parameters, 
				AttributeTypeSupport attrsSupport, ExternalDataParser externalDataParser)
		{
			super(description, parameters);
			this.externalDataParser = externalDataParser;
			setParameters(parameters);
			at = attrsSupport.getType(unityAttribute);
			if (at == null)
				throw new IllegalArgumentException(
						"Attribute type " + unityAttribute + " is not known");
		}

		@Override
		protected void invokeWrapped(TranslatedRegistrationRequest state, Object mvelCtx, RegistrationContext context,
				String currentProfile) throws EngineException
		{
			Object value = MVEL.executeExpression(expressionCompiled, mvelCtx, new HashMap<>());
			if (value == null)
			{
				log.debug("Attribute value evaluated to null, skipping");
				return;
			}
			
			List<?> aValues = value instanceof List ? (List<?>)value : Collections.singletonList(value);
			
			for (String group : getGroups(context))
			{
				Attribute attribute;
				try
				{
					attribute = externalDataParser.parseAsAttribute(at, group, aValues, null,
							currentProfile);
				} catch (IllegalAttributeValueException e)
				{
					log.info("Can't convert attribute values returned by the action's expression "
							+ "to the type of attribute " + unityAttribute
							+ " , skipping it", e);
					return;
				}
				log.debug("Mapped attribute: " + attribute);
				state.addAttribute(attribute);
			}
		}

		@Override
		public void validateGroupRestrictedForm(GroupRestrictedFormValidationContext context) throws ActionValidationException
		{
			if (group.equals("/"))
			{
				if (context.allowedRootGroupAttributes.contains(unityAttribute))
				{
					return;
				}
				
				throw new ActionValidationException("Attribute " + unityAttribute + " is forbidden in root group");
			}

			if (!Group.isChildOrSame(group, context.parentGroup))
			{
				throw new ActionValidationException("Attribute " + unityAttribute + " is forbidden in " + group);
			}
		}
		
		private void setParameters(String[] parameters)
		{
			unityAttribute = parameters[0];
			group = parameters[1];
			expressionCompiled = MVEL.compileExpression(parameters[2]);
		}
		
		private Set<String> getGroups(RegistrationContext context)
		{
			if (DynamicGroupParam.isDynamicGroup(group))
			{
				int groupParamIndex = new DynamicGroupParam(group).index;
				GroupSelection groupSelection = context.groupSelections.get(groupParamIndex);
				if (groupSelection == null)
				{
					return Collections.emptySet();
				}	
				return Sets.newHashSet(groupSelection.getSelectedGroups());
			} else
			{
				return Sets.newHashSet(group);
			}
		}
		
	}
}
