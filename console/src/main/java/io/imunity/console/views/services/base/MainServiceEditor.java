/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.services.base;

import static io.imunity.vaadin.elements.CssClassNames.BIG_VAADIN_FORM_ITEM_LABEL;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.FormItem;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import io.imunity.vaadin.auth.services.ServiceControllerBaseInt;
import io.imunity.vaadin.auth.services.ServiceDefinition;
import io.imunity.vaadin.auth.services.ServiceEditor;
import io.imunity.vaadin.auth.services.ServiceEditorComponent;
import io.imunity.vaadin.auth.services.ServiceEditorComponent.ServiceEditorTab;
import io.imunity.vaadin.auth.services.ServiceTypeInfoHelper;
import pl.edu.icm.unity.base.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;

/**
 * 
 * @author P.Piernik
 *
 */
public class MainServiceEditor extends VerticalLayout
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, MainServiceEditor.class);

	
	private final NotificationPresenter notificationPresenter;
	private final MessageSource msg;
	private final TypesRegistryBase<? extends ServiceControllerBaseInt> editorsRegistry;
	private final SubViewSwitcher subViewSwitcher;

	private ServiceDefinition toEdit;
	private ServiceEditor editor;
	private ServiceEditorComponent editorComponent;
	private VerticalLayout mainLayout;
	private ServiceEditorTab initTab;
	private ComboBox<String> serviceTypeCombo;
	private TextField serviceTypeLabel;

	public MainServiceEditor(MessageSource msg,
			TypesRegistryBase<? extends ServiceControllerBaseInt> editorsRegistry,
			Collection<EndpointTypeDescription> autnTypes, ServiceDefinition toEdit,
			ServiceEditorTab initTab, SubViewSwitcher subViewSwitcher, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.toEdit = toEdit;
		this.editorsRegistry = editorsRegistry;
		this.initTab = initTab;
		this.subViewSwitcher = subViewSwitcher;
		this.notificationPresenter = notificationPresenter;
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
		serviceTypeCombo.addValueChangeListener(e -> reloadEditor());
		
		serviceTypeCombo.setItemLabelGenerator(i -> servicesTypesSorted.get(i));
		
		serviceTypeCombo.setWidth(25, Unit.EM);
		serviceTypeCombo.setItems(sorted);

		serviceTypeLabel = new TextField();
		serviceTypeLabel.setWidth(25, Unit.EM);
		serviceTypeLabel.setReadOnly(true);

		mainLayout = new VerticalLayout();
		mainLayout.setMargin(false);
		FormLayout typeWrapper = new FormLayout();
		typeWrapper.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		typeWrapper.addClassName(BIG_VAADIN_FORM_ITEM_LABEL.getName());
		FormItem typeCombo = typeWrapper.addFormItem(serviceTypeCombo, msg.getMessage("MainServiceEditor.typeComboCaption"));
		FormItem typeLabel = typeWrapper.addFormItem(serviceTypeLabel, msg.getMessage("MainServiceEditor.typeLabelCaption"));
		add(typeWrapper);


		if (toEdit != null)
		{
			serviceTypeCombo.setValue(toEdit.getType());
			typeCombo.setVisible(false);
			serviceTypeLabel.setValue(ServiceTypeInfoHelper.getType(msg, toEdit.getType()));
			typeLabel.setVisible(true);
		} else
		{
			typeCombo.setVisible(true);
			typeLabel.setVisible(false);
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
			remove(editorComponent.getComponent());
		}

		try
		{
			editor = editorsRegistry.getByName(type).getEditor(subViewSwitcher);
			editorComponent = editor.getEditor(toEdit);
			editorComponent.setActiveTab(initTab.toString());

			add(editorComponent.getComponent());
		} catch (Exception e)
		{
			log.error("Can not create service editor", e);
			notificationPresenter.showError(msg.getMessage("MainServiceEditor.createSingleAuthenticatorEditorError"), e.getMessage());
		}
	}

	public ServiceDefinition getService() throws FormValidationException
	{
		if (editor == null)
			throw new FormValidationException();
		return editor.getEndpointDefiniton();
	}

}
