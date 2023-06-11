/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services.tabs;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.data.Binder;
import com.vaadin.data.ValidationResult;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.endpoint.EndpointPathValidator;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.chips.ChipsWithTextfield;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;
import pl.edu.icm.unity.webui.common.widgets.DescriptionTextField;
import pl.edu.icm.unity.webui.console.services.DefaultServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceTypeInfoHelper;
import pl.edu.icm.unity.webui.console.services.ServiceEditorBase.EditorTab;
import pl.edu.icm.unity.webui.console.services.ServiceEditorComponent.ServiceEditorTab;

/**
 * Service general editor tab
 * 
 * @author P.Piernik
 *
 */
public class GeneralTab extends CustomComponent implements EditorTab
{
	protected MessageSource msg;
	private EndpointTypeDescription type;
	private List<String> usedEndpointsPaths;
	private Set<String> serverContextPaths;
	protected VerticalLayout mainLayout;
	

	public GeneralTab(MessageSource msg, EndpointTypeDescription type, List<String> usedEndpointsPaths, Set<String> serverContextPaths)
	{
		this.msg = msg;
		this.type = type;
		this.usedEndpointsPaths = usedEndpointsPaths;
		this.serverContextPaths = serverContextPaths;
	}

	public void initUI(Binder<DefaultServiceDefinition> binder, boolean editMode)
	{		
		setCaption(msg.getMessage("ServiceEditorBase.general"));
		setIcon(Images.cogs.getResource());
		
		mainLayout = new VerticalLayout();
		mainLayout.setMargin(false);

		FormLayoutWithFixedCaptionWidth mainGeneralLayout = new FormLayoutWithFixedCaptionWidth();

		TextField name = new TextField();
		name.setCaption(msg.getMessage("ServiceEditorBase.name"));
		name.setReadOnly(editMode);
		binder.forField(name).asRequired().bind("name");
		mainGeneralLayout.addComponent(name);

		Label binding = new Label();
		binding.setCaption(msg.getMessage("ServiceEditorBase.binding"));
		binding.setValue(ServiceTypeInfoHelper.getBinding(msg, type.getSupportedBinding()));
		mainGeneralLayout.addComponent(binding);
		if (type.getPaths() != null && !type.getPaths().isEmpty()
				&& !type.getPaths().keySet().iterator().next().isEmpty())

		{
			ChipsWithTextfield paths = new ChipsWithTextfield(msg);
			paths.setCaption(msg.getMessage("ServiceEditorBase.paths"));
			List<String> pathsList = type.getPaths().keySet().stream().collect(Collectors.toList());
			pathsList.set(0, pathsList.get(0).isEmpty() ? ""
					: pathsList.get(0) + " (" + msg.getMessage("default") + ")");
			paths.setValue(pathsList);
			paths.setReadOnly(true);
			mainGeneralLayout.addComponent(paths);
		}

		TextField contextPath = new TextField();
		contextPath.setCaption(msg.getMessage("ServiceEditorBase.contextPath"));
		contextPath.setReadOnly(editMode);
		binder.forField(contextPath).asRequired().withValidator((v, c) -> {

			return editMode ? validatePathForEdit(v) : validatePathForAdd(v);

		}).bind("address");
		mainGeneralLayout.addComponent(contextPath);

		I18nTextField displayedName = new I18nTextField(msg);
		displayedName.setCaption(msg.getMessage("ServiceEditorBase.displayedName"));
		binder.forField(displayedName).bind("displayedName");
		mainGeneralLayout.addComponent(displayedName);

		TextField description = new DescriptionTextField(msg);
		binder.forField(description).bind("description");
		mainGeneralLayout.addComponent(description);

		mainLayout.addComponent(mainGeneralLayout);

		setCompositionRoot(mainLayout);
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
	public CustomComponent getComponent()
	{
		return this;
	}

}
