/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console_utils.tprofile;

import com.google.common.base.Supplier;
import io.imunity.vaadin.elements.CssClassNames;
import io.imunity.vaadin.endpoint.common.api.HtmlTooltipFactory;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.authn.CredentialRequirements;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.IdentityType;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.msg_template.UserNotificationTemplateDef;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.translation.ActionParameterDefinition;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.engine.api.*;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.translation.form.DynamicGroupParam;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Responsible for creating {@link ActionParameterComponent}s.
 * @implNote prototype scope to allow to create a reusable provider per user.
 */
@PrototypeComponent
public class ActionParameterComponentProvider
{
	private MessageSource msg;
	
	private List<String> groups;
	private Collection<String> credReqs;
	private Collection<String> idTypes;
	private List<AttributeType> atTypes;
	private List<String> inputProfiles;
	private List<String> outputProfiles;
	private List<String> userMessageTemplates;
	private List<String> registrationForm;

	private final AttributeTypeManagement attrsMan;
	private final IdentityTypeSupport idTypeSupport;
	private final CredentialRequirementManagement credReqMan;
	private final GroupsManagement groupsMan;
	private final TranslationProfileManagement profileMan;
	private final MessageTemplateManagement msgTemplateMan;
	private final RegistrationsManagement registrationMan;
	private final HtmlTooltipFactory htmlTooltipFactory;
	private final List<Supplier<List<DynamicGroupParamWithLabel>>> dynamicGroupProviders;
	
	public ActionParameterComponentProvider(MessageSource msg,
			AttributeTypeManagement attrsMan, IdentityTypeSupport idTypeSupport,
			CredentialRequirementManagement credReqMan, GroupsManagement groupsMan,
			TranslationProfileManagement profileMan,
			MessageTemplateManagement msgTemplateMan,
			HtmlTooltipFactory htmlTooltipFactory,
			RegistrationsManagement registrationMan)
	{
		this.msg = msg;
		this.attrsMan = attrsMan;
		this.idTypeSupport = idTypeSupport;
		this.credReqMan = credReqMan;
		this.groupsMan = groupsMan;
		this.profileMan = profileMan;
		this.msgTemplateMan = msgTemplateMan;
		this.registrationMan = registrationMan;
		this.htmlTooltipFactory = htmlTooltipFactory;
		this.dynamicGroupProviders = new ArrayList<>();
	}

	public void init(Supplier<List<DynamicGroupParamWithLabel>> dynamicGroupProvider) throws EngineException
	{
		this.init();
		this.dynamicGroupProviders.add(dynamicGroupProvider);
	}
	
	public void init() throws EngineException
	{
		this.atTypes = new ArrayList<>(attrsMan.getAttributeTypes());
		Collections.sort(atTypes, (a1,a2) -> a1.getName().compareTo(a2.getName()));
		this.groups = new ArrayList<>(groupsMan.getChildGroups("/"));
		Collections.sort(groups);
		Collection<CredentialRequirements> crs = credReqMan.getCredentialRequirements();
		credReqs = new TreeSet<>();
		for (CredentialRequirements cr: crs)
			credReqs.add(cr.getName());
		Collection<IdentityType> idTypesF = idTypeSupport.getIdentityTypes();
		idTypes = new TreeSet<>();
		for (IdentityType it: idTypesF)
			idTypes.add(it.getIdentityTypeProvider());
		inputProfiles = new ArrayList<>(profileMan.listInputProfiles().keySet());
		Collections.sort(inputProfiles);
		outputProfiles = new ArrayList<>(profileMan.listOutputProfiles().keySet());
		Collections.sort(outputProfiles);
		userMessageTemplates = new ArrayList<>(msgTemplateMan.getCompatibleTemplates(
				UserNotificationTemplateDef.NAME).keySet());
		Collections.sort(userMessageTemplates);
		registrationForm = registrationMan.getForms().stream()
				.map(RegistrationForm::getName)
				.collect(Collectors.toList());
	}

	TranslationProfile getInputProfile(String profile) throws EngineException
	{
		return profileMan.getInputProfile(profile);
	}

	TranslationProfile getOutputProfile(String profile) throws EngineException
	{
		return profileMan.getOutputProfile(profile);
	}
	
	
	public ActionParameterComponent getParameterComponent(ActionParameterDefinition param, EditorContext context)
	{
		switch (param.getType())
		{
		case ENUM:
			return new EnumActionParameterComponent(param, msg);
		case UNITY_ATTRIBUTE:
			return new AttributeActionParameterComponent(param, msg, atTypes);
		case UNITY_GROUP:
			return new BaseEnumActionParameterComponent(param, msg, groups);
		case UNITY_DYNAMIC_GROUP:
			return getUnityGroupActionParameterComponent(param);
		case UNITY_CRED_REQ:
			return new BaseEnumActionParameterComponent(param, msg, credReqs);
		case UNITY_ID_TYPE:
			return new BaseEnumActionParameterComponent(param, msg, idTypes);
		case EXPRESSION:
			ExpressionActionParameterComponent component = new ExpressionActionParameterComponent(param, msg, htmlTooltipFactory);
			if (context.equals(EditorContext.WIZARD))
			{
				component.addClassNameToField(CssClassNames.WIDTH_FULL.getName());
			}
			return component;
		case DAYS:
			return new DaysActionParameterComponent(param, msg);
		case LARGE_TEXT:
			return new TextAreaActionParameterComponent(param, msg);
		case I18N_TEXT:
			return new LocalizedTextActionParameterComponent(param, msg);
		case BOOLEAN:
			return new BooleanActionParameterComponent(param, msg);
		case INTEGER:
			return new IntegerActionParameterComponent(param, msg);
		case UNITY_INPUT_TRANSLATION_PROFILE:
			return new BaseEnumActionParameterComponent(param, msg, inputProfiles);
		case UNITY_OUTPUT_TRANSLATION_PROFILE:
			return new BaseEnumActionParameterComponent(param, msg, outputProfiles);
		case USER_MESSAGE_TEMPLATE:
			return new BaseEnumActionParameterComponent(param, msg, userMessageTemplates);
		case REGISTRATION_FORM:
			return new BaseEnumActionParameterComponent(param, msg, registrationForm);
		default: 
			return new DefaultActionParameterComponent(param, msg);
		}
	}
	
	private BaseEnumActionParameterComponent getUnityGroupActionParameterComponent(ActionParameterDefinition param)
	{
		ArrayList<String> groupsWithDynamic = new ArrayList<>(groups);
		Map<String, String> dynamicGroups = new HashMap<>();
		dynamicGroupProviders.forEach(p -> p.get().stream().forEach(dg -> dynamicGroups.put(dg.toSelectionRepresentation(),  dg.getLabel(msg))));
		groupsWithDynamic.addAll(dynamicGroups.keySet());
		BaseEnumActionParameterComponent groupsCombo = new BaseEnumActionParameterComponent(param, msg,
				groupsWithDynamic);
		groupsCombo.setItemLabelGenerator(i -> DynamicGroupParam.isDynamicGroup(i) ? dynamicGroups.get(i) : i);
		return groupsCombo;
	}
}
