/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.services.base;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;
import pl.edu.icm.unity.webui.console.services.ServiceControllerBaseInt;
import pl.edu.icm.unity.webui.console.services.ServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceEditor;
import pl.edu.icm.unity.webui.console.services.ServiceEditorComponent;
import pl.edu.icm.unity.webui.console.services.ServiceTypeInfoHelper;
import pl.edu.icm.unity.webui.console.services.ServiceEditorComponent.ServiceEditorTab;

/**
 * 
 * @author P.Piernik
 *
 */
public class MainServiceEditor extends CustomComponent
{
	private ComboBox<String> serviceTypeCombo;
	private TextField serviceTypeLabel;
	private MessageSource msg;
	private TypesRegistryBase<? extends ServiceControllerBaseInt> editorsRegistry;
	private ServiceDefinition toEdit;
	private ServiceEditor editor;
	private ServiceEditorComponent editorComponent;
	private VerticalLayout mainLayout;
	private ServiceEditorTab initTab;
	private SubViewSwitcher subViewSwitcher;

	public MainServiceEditor(MessageSource msg,
			TypesRegistryBase<? extends ServiceControllerBaseInt> editorsRegistry,
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
		Map<String, String> servicesTypesSorted = getSupportedServicesTypes();

		Set<String> sorted = servicesTypesSorted.entrySet().stream().sorted(Map.Entry.comparingByValue())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1,
						LinkedHashMap::new))
				.keySet();
		serviceTypeCombo = new ComboBox<String>();
		serviceTypeCombo.setCaption(msg.getMessage("MainServiceEditor.typeComboCaption"));
		serviceTypeCombo.addSelectionListener(e -> reloadEditor());
		serviceTypeCombo.setEmptySelectionAllowed(false);
		serviceTypeCombo.setItemCaptionGenerator(i -> servicesTypesSorted.get(i));
		serviceTypeCombo.setWidth(25, Unit.EM);
		serviceTypeCombo.setItems(sorted);

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
			serviceTypeCombo.setValue(sorted.iterator().next());

		}
	}

	private Map<String, String> getSupportedServicesTypes()
	{
		return editorsRegistry.getAll().stream().map(f -> f.getSupportedEndpointType())
				.collect(Collectors.toMap(e -> e, e -> ServiceTypeInfoHelper.getType(msg, e)));
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
			editorComponent.setActiveTab(initTab.toString());

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
