/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.file;

import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.TabSheetVariant;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import io.imunity.vaadin.elements.CssClassNames;
import pl.edu.icm.unity.base.message.MessageSource;

import java.util.Optional;

import static java.util.Optional.*;


public class FileField extends CustomField<LocalOrRemoteResource>
{
	private final Tab localTab;
	private final Tab remoteTab;
	private final TabSheet tab;
	private final VerticalLayout main;
	private final UploadComponent uploadComponent;
	private final RemoteUrlComponent remoteUrlComponent;

	FileField(MessageSource msg, String mimeType, int maxFileSize, boolean remoteOnly)
	{
		uploadComponent = new UploadComponent(msg, mimeType, maxFileSize);
		remoteUrlComponent = new RemoteUrlComponent(msg);

		tab = new TabSheet();
		tab.addThemeVariants(TabSheetVariant.LUMO_TABS_MINIMAL, TabSheetVariant.LUMO_TABS_HIDE_SCROLL_BUTTONS);
		tab.addClassName(CssClassNames.TABSHEET_FULL.getName());
		localTab = new Tab(msg.getMessage("FileField.local"));
		remoteTab = new Tab(msg.getMessage("FileField.remote"));
		tab.add(localTab, uploadComponent);
		tab.add(remoteTab, remoteUrlComponent);

		main = new VerticalLayout();
		main.setPadding(false);
		main.setSpacing(false);
		main.add(remoteOnly ? remoteUrlComponent : tab);
		add(main);
	}

	public FileField(MessageSource msg, String mimeType, String previewFileName, int maxFileSize)
	{
		this(msg, mimeType, maxFileSize, false);
		uploadComponent.setFileName(previewFileName);
		tab.addSelectedChangeListener(e ->
		{
			remoteUrlComponent.setEnabled(e.getSelectedTab().equals(remoteTab));
			updateValue();
		});

		HorizontalLayout wrapper = new HorizontalLayout();

		main.add(wrapper);
	}

	public void configureBinding(Binder<?> binder, String fieldName,
			Optional<Validator<LocalOrRemoteResource>> additionalValidator)
	{
		binder.forField(remoteUrlComponent)
				.bindReadOnly(fieldName);
		binder.forField(uploadComponent)
				.bindReadOnly(fieldName);
		binder.forField(this)
			.withValidator(additionalValidator.orElse((v, c) -> ValidationResult.ok()))
			.bind(fieldName);
	}

	public void configureBinding(Binder<?> binder, String fieldName)
	{
		configureBinding(binder, fieldName, empty());
	}

	@Override
	public void setEnabled(boolean enabled)
	{
		uploadComponent.setEnabled(enabled);
		super.setEnabled(enabled);
	}

	@Override
	public LocalOrRemoteResource getValue()
	{
		if(tab.getSelectedTab().equals(localTab))
			return uploadComponent.generateModelValue();
		else
			return remoteUrlComponent.generateModelValue();
	}

	@Override
	protected LocalOrRemoteResource generateModelValue()
	{
		return getValue();
	}

	@Override
	public void setPresentationValue(LocalOrRemoteResource localOrRemoteResource)
	{
		 setValue(localOrRemoteResource);
	}

	@Override
	public void setValue(LocalOrRemoteResource localOrRemoteResource)
	{
		if(localOrRemoteResource == null)
			return;
		if(localOrRemoteResource.getLocal() == null)
		{
			tab.setSelectedTab(remoteTab);
			remoteUrlComponent.setPresentationValue(localOrRemoteResource);
		}
		else
		{
			tab.setSelectedTab(localTab);
			uploadComponent.setPresentationValue(localOrRemoteResource);
		}
		super.setValue(localOrRemoteResource);
	}
}
