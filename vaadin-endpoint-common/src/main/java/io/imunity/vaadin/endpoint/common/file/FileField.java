/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.file;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.TabSheetVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.FileData;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.server.AbstractStreamResource;
import com.vaadin.flow.server.StreamResource;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.files.URIHelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Optional;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;


public class FileField extends CustomField<LocalOrRemoteResource>
{
	private final MessageSource msg;
	private final Tab localTab;
	private final Tab remoteTab;
	private final TabSheet tab;
	private final VerticalLayout main;
	private final Upload upload;
	private Anchor downloader;
	private String fileName;
	private Button clear;
	private LocalOrRemoteResource value;
	private TextField remoteUrl;

	FileField(MessageSource msg, String mimeType, int maxFileSize, boolean remoteOnly)
	{
		this.msg = msg;

		MemoryBuffer memoryBuffer = new MemoryBuffer();
		upload = new Upload(memoryBuffer);
		upload.setMaxFiles(0);
		upload.setAcceptedFileTypes(mimeType);
		upload.setMaxFileSize(maxFileSize);
		upload.addSucceededListener(event ->
		{
			FileData fileData = memoryBuffer.getFileData();
			remoteUrl.clear();
			byte[] byteArray = ((ByteArrayOutputStream) fileData.getOutputBuffer()).toByteArray();
			value = new LocalOrRemoteResource(new StreamResource("logo", () -> new ByteArrayInputStream(byteArray)), "", byteArray);
			setPreview();
			updateValue();
		});

		VerticalLayout local = new VerticalLayout();
		local.setPadding(false);
		local.add(upload);

		VerticalLayout remote = new VerticalLayout();
		remote.setPadding(false);
		remoteUrl = new TextField();
		remoteUrl.setWidth(TEXT_FIELD_BIG.value());
		remoteUrl.addValueChangeListener(e ->
		{
			if (value == null)
			{
				value = new LocalOrRemoteResource();
				updateValue();
			}
			value.setSrc(e.getValue());
			setPreview();
			fireEvent(new ComponentValueChangeEvent<>(this, this, value, true));
		});
		remote.add(remoteUrl);

		tab = new TabSheet();
		tab.addThemeVariants(TabSheetVariant.LUMO_TABS_MINIMAL, TabSheetVariant.LUMO_TABS_HIDE_SCROLL_BUTTONS);
		localTab = new Tab(msg.getMessage("FileField.local"));
		remoteTab = new Tab(msg.getMessage("FileField.remote"));
		tab.add(localTab, local);
		tab.add(remoteTab, remote);

		main = new VerticalLayout();
		main.setMargin(false);
		main.setSpacing(false);
		main.add(remoteOnly ? remoteUrl : tab);
		add(main);
	}

	public FileField(MessageSource msg, String mimeType, String previewFileName, int maxFileSize)
	{
		this(msg, mimeType, maxFileSize, false);
		this.fileName = previewFileName;
		downloader = new Anchor();
		downloader.getElement().setAttribute("download", true);
		Tooltip.forComponent(downloader).setText(msg.getMessage("FileField.download"));
		downloader.add(VaadinIcon.DOWNLOAD.create());
		downloader.setVisible(false);
		clear = new Button(msg.getMessage("FileField.clear"));
		clear.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		clear.addClickListener(e -> {
			setPresentationValue(null);
			fireEvent(new ComponentValueChangeEvent<>(this, this, getValue(), false));
		});
		clear.setVisible(false);

		tab.addSelectedChangeListener(e ->
		{
			if (tab.getSelectedTab().equals(remoteTab))
				downloader.setVisible(false);
			else if (getValue()!= null && getValue().getLocal()!=null)
				downloader.setVisible(true);
			if(tab.getSelectedTab().equals(localTab))
				upload.setMaxFiles(1);
		});

		HorizontalLayout wrapper = new HorizontalLayout();
		wrapper.add(downloader);
		wrapper.add(clear);

		main.add(wrapper);
	}

	@Override
	public void setEnabled(boolean enabled)
	{
		super.setEnabled(enabled);
		upload.setMaxFiles(enabled ? 1 : 0);
		downloader.setEnabled(enabled);
		clear.setEnabled(enabled);
	}

	public void configureBinding(Binder<?> binder, String fieldName,
			Optional<Validator<LocalOrRemoteResource>> additionalValidator)
	{
		binder.forField(this).withValidator((v, c) -> {

			if (v != null)
			{
				if (v.getLocal() == null
						&& (!URIHelper.isWebReady(v.getSrc())))
				{
					return ValidationResult.error(msg.getMessage("FileField.notWebUri"));
				}
				if (v.getSrc() != null && (v.getLocal() == null || v.getLocal().length == 0))
				{
					return ValidationResult.error(msg.getMessage("FileField.invalidFile", v.getSrc()));
				}
			}

			return ValidationResult.ok();

		}).withValidator(additionalValidator.orElse((v, c) -> ValidationResult.ok())).bind(fieldName);
	}

	public void configureBinding(Binder<?> binder, String fieldName)
	{
		configureBinding(binder, fieldName, Optional.empty());
	}


	@Override
	public LocalOrRemoteResource getValue()
	{
		return value;
	}

	@Override
	protected LocalOrRemoteResource generateModelValue()
	{
		return getValue();
	}

	@Override
	protected void setPresentationValue(LocalOrRemoteResource localOrRemoteResource)
	{
		this.value = localOrRemoteResource;

		if (value == null)
		{
			remoteUrl.clear();
			setPreview();
			return;
		}

		if (value.getLocal() != null)
		{
			tab.setSelectedTab(localTab);
			setPreview();
		} else
		{
			tab.setSelectedTab(remoteTab);
			remoteUrl.setValue(value.getSrc());
		}
	}
	
	protected void setPreview()
	{
		downloader.setVisible(false);
		clear.setVisible(false);
		downloader.setHref((AbstractStreamResource) null);
	
		if (value != null && value.getLocal() != null)
		{
			downloader.setHref(new StreamResource(fileName, () -> new ByteArrayInputStream(value.getLocal())));
			downloader.setVisible(true);
			clear.setVisible(true);
		} 
	}	
}