/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.services.base;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.webui.authn.services.ServiceControllerBaseInt;
import pl.edu.icm.unity.webui.authn.services.ServiceDefinition;
import pl.edu.icm.unity.webui.authn.services.ServiceEditor;
import pl.edu.icm.unity.webui.authn.services.ServiceEditorComponent;
import pl.edu.icm.unity.webui.authn.services.ServiceEditorComponent.ServiceEditorTab;
import pl.edu.icm.unity.webui.authn.services.ServiceTypeInfoHelper;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;

/**
 * 
 * @author P.Piernik
 *
 */
public class MainServiceEditor extends CustomComponent
{
	private ComboBox<String> serviceTypeCombo;
	private TextField serviceTypeLabel;
	private UnityMessageSource msg;
	private TypesRegistryBase<? extends ServiceControllerBaseInt> editorsRegistry;
	private ServiceDefinition toEdit;

	private ServiceEditor editor;
	private ServiceEditorComponent editorComponent;
	private VerticalLayout mainLayout;

	private ServiceEditorTab initTab;
	
	private SubViewSwitcher subViewSwitcher;

	public MainServiceEditor(UnityMessageSource msg, TypesRegistryBase<? extends ServiceControllerBaseInt> editorsRegistry,
			Collection<EndpointTypeDescription> autnTypes, ServiceDefinition toEdit,
			ServiceEditorTab initTab, SubViewSwitcher subViewSwitcher)
	{
		this.msg = msg;
		this.toEdit = toEdit;
		this.editorsRegistry = editorsRegistry;
		this.initTab = initTab;
		this.subViewSwitcher = subViewSwitcher;
		initUI();
	}

	private void initUI()
	{
		List<String> servicesTypesSorted = getSupportedServicesTypes().stream().sorted()
				.collect(Collectors.toList());

		serviceTypeCombo = new ComboBox<String>();
		serviceTypeCombo.setCaption(msg.getMessage("MainServiceEditor.typeComboCaption"));
		serviceTypeCombo.addSelectionListener(e -> reloadEditor());
		serviceTypeCombo.setEmptySelectionAllowed(false);
		serviceTypeCombo.setItemCaptionGenerator(t -> ServiceTypeInfoHelper.getType(msg, t));
		serviceTypeCombo.setWidth(25, Unit.EM);
		serviceTypeCombo.setItems(servicesTypesSorted);

		serviceTypeLabel = new TextField();
		serviceTypeLabel.setWidth(25, Unit.EM);
		serviceTypeLabel.setCaption(msg.getMessage("MainServiceEditor.typeLabelCaption"));
		serviceTypeLabel.setReadOnly(true);

		mainLayout = new VerticalLayout();
		mainLayout.setMargin(false);
		FormLayoutWithFixedCaptionWidth typeWrapper = new FormLayoutWithFixedCaptionWidth();
		typeWrapper.setMargin(false);
		typeWrapper.addComponent(serviceTypeCombo);
		typeWrapper.addComponent(serviceTypeLabel);
		mainLayout.addComponent(typeWrapper);

		setCompositionRoot(mainLayout);

		if (toEdit != null)
		{
			serviceTypeCombo.setValue(toEdit.getType());
			serviceTypeCombo.setVisible(false);
			serviceTypeLabel.setValue(ServiceTypeInfoHelper.getType(msg, toEdit.getType()));
			serviceTypeLabel.setVisible(true);
		} else
		{
			serviceTypeCombo.setVisible(true);
			serviceTypeLabel.setVisible(false);
			serviceTypeCombo.setValue(servicesTypesSorted.iterator().next());

		}
	}

	private List<String> getSupportedServicesTypes()
	{
		return editorsRegistry.getAll().stream().map(f -> f.getSupportedEndpointType())
				.collect(Collectors.toList());
	}

	private void reloadEditor()
	{

		String type = serviceTypeCombo.getValue();
		if (editorComponent != null)
		{
			mainLayout.removeComponent(editorComponent);
		}

		try
		{
			editor = editorsRegistry.getByName(type).getEditor(subViewSwitcher);
			editorComponent = editor.getEditor(toEdit);
			editorComponent.setActiveTab(initTab);

			mainLayout.addComponent(editorComponent);
		} catch (Exception e)
		{
			NotificationPopup.showError(msg,
					msg.getMessage("MainServiceEditor.createSingleAuthenticatorEditorError"), e);
		}
	}

	public ServiceDefinition getService() throws FormValidationException
	{
		if (editor == null)
			throw new FormValidationException();
		return editor.getEndpointDefiniton();
	}

}
