/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.api.services.tabs;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;
import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;
import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;

import io.imunity.vaadin.elements.LocalizedTextFieldDetails;
import io.imunity.vaadin.endpoint.common.api.services.DefaultServiceDefinition;
import io.imunity.vaadin.endpoint.common.api.services.ServiceEditorBase.EditorTab;
import io.imunity.vaadin.endpoint.common.api.services.ServiceEditorComponent.ServiceEditorTab;
import io.imunity.vaadin.endpoint.common.api.services.ServiceTypeInfoHelper;
import pl.edu.icm.unity.base.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.endpoint.EndpointPathValidator;

/**
 * Service general editor tab
 * 
 * @author P.Piernik
 *
 */
public class GeneralTab extends VerticalLayout implements EditorTab
{
	protected final MessageSource msg;
	private final EndpointTypeDescription type;
	private final List<String> usedEndpointsPaths;
	private final Set<String> serverContextPaths;

	public GeneralTab(MessageSource msg, EndpointTypeDescription type, List<String> usedEndpointsPaths,
			Set<String> serverContextPaths)
	{
		this.msg = msg;
		this.type = type;
		this.usedEndpointsPaths = usedEndpointsPaths;
		this.serverContextPaths = serverContextPaths;
	}

	public void initUI(Binder<DefaultServiceDefinition> binder, boolean editMode)
	{
		setMargin(false);

		FormLayout mainGeneralLayout = new FormLayout();
		mainGeneralLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		mainGeneralLayout.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());

		TextField name = new TextField();
		name.setReadOnly(editMode);
		binder.forField(name)
				.asRequired()
				.bind("name");
		mainGeneralLayout.addFormItem(name, msg.getMessage("ServiceEditorBase.name"));

		NativeLabel binding = new NativeLabel();
		binding.setText(ServiceTypeInfoHelper.getBinding(msg, type.getSupportedBinding()));
		mainGeneralLayout.addFormItem(binding, msg.getMessage("ServiceEditorBase.binding"));
		if (type.getPaths() != null && !type.getPaths()
				.isEmpty() && !type.getPaths()
						.keySet()
						.iterator()
						.next()
						.isEmpty())

		{
			TextField paths = new TextField();
			paths.setWidth(TEXT_FIELD_MEDIUM.value());
			List<String> pathsList = type.getPaths()
					.keySet()
					.stream()
					.collect(Collectors.toList());
			pathsList.set(0, pathsList.get(0)
					.isEmpty() ? "" : pathsList.get(0) + " (" + msg.getMessage("default") + ")");
			paths.setValue(String.join(",", pathsList));
			paths.setReadOnly(true);
			mainGeneralLayout.addFormItem(paths, msg.getMessage("ServiceEditorBase.paths"));
		}

		TextField contextPath = new TextField();
		contextPath.setReadOnly(editMode);
		contextPath.setWidth(TEXT_FIELD_MEDIUM.value());
		binder.forField(contextPath)
				.asRequired()
				.withValidator((v, c) ->
				{

					return editMode ? validatePathForEdit(v) : validatePathForAdd(v);

				})
				.bind("address");
		mainGeneralLayout.addFormItem(contextPath, msg.getMessage("ServiceEditorBase.contextPath"));

		LocalizedTextFieldDetails displayedName = new LocalizedTextFieldDetails(msg.getEnabledLocales()
				.values(), msg.getLocale());
		displayedName.setWidth(TEXT_FIELD_BIG.value());
		binder.forField(displayedName)
				.withConverter(I18nString::new, I18nString::getLocalizedMap)
				.bind("displayedName");
		mainGeneralLayout.addFormItem(displayedName, msg.getMessage("ServiceEditorBase.displayedName"));

		TextField description = new TextField();
		binder.forField(description)
				.bind("description");
		description.setWidth(TEXT_FIELD_BIG.value());
		mainGeneralLayout.addFormItem(description, msg.getMessage("ServiceEditorBase.description"));

		add(mainGeneralLayout);
	}

	private ValidationResult validatePathForAdd(String path)
	{
		if (path == null || path.isEmpty())
		{
			return ValidationResult.error(msg.getMessage("fieldRequired"));
		}

		if (usedEndpointsPaths.contains(path))
		{
			return ValidationResult.error(msg.getMessage("ServiceEditorBase.usedContextPath"));
		}

		try
		{
			EndpointPathValidator.validateEndpointPath(path, serverContextPaths);

		} catch (WrongArgumentException e)
		{
			return ValidationResult.error(msg.getMessage("ServiceEditorBase.invalidContextPath"));
		}

		return ValidationResult.ok();
	}

	private ValidationResult validatePathForEdit(String path)
	{
		try
		{
			EndpointPathValidator.validateEndpointPath(path);

		} catch (WrongArgumentException e)
		{
			return ValidationResult.error(msg.getMessage("ServiceEditorBase.invalidContextPath"));
		}

		return ValidationResult.ok();
	}

	@Override
	public String getType()
	{
		return ServiceEditorTab.GENERAL.toString();
	}

	@Override
	public Component getComponent()
	{
		return this;
	}

	@Override
	public VaadinIcon getIcon()
	{
		return VaadinIcon.COGS;
	}

	@Override
	public String getCaption()
	{
		return msg.getMessage("ServiceEditorBase.general");
	}

}
