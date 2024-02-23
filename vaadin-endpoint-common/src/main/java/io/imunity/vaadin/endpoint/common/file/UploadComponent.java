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
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.FileData;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.server.StreamResource;
import pl.edu.icm.unity.base.message.MessageSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static io.imunity.vaadin.elements.CssClassNames.LOGO_IMAGE;
import static io.imunity.vaadin.elements.CssClassNames.POINTER;

class UploadComponent extends CustomField<LocalOrRemoteResource>
{
	private final Upload upload;
	private final Anchor downloader;
	private final LocalOrRemoteResource image;
	private final Button clear;
	private byte[] byteArray;
	private String fileName;

	UploadComponent(MessageSource msg, String mimeType, int maxFileSize)
	{
		image = new LocalOrRemoteResource();
		image.addClassName(LOGO_IMAGE.getName());
		image.getStyle().set("margin-left", "unset");
		MemoryBuffer memoryBuffer = new MemoryBuffer();
		upload = new Upload(memoryBuffer);
		upload.setMaxFiles(1);
		upload.setAcceptedFileTypes(mimeType);
		upload.setMaxFileSize(maxFileSize);
		upload.addFileRejectedListener(e -> {
			setInvalid(true);
			setErrorMessage(e.getErrorMessage());
		});
		upload.getElement().addEventListener("file-remove", e ->
		{
			image.setSrc("");
			image.setVisible(false);
		});
		upload.addSucceededListener(event ->
		{
			FileData fileData = memoryBuffer.getFileData();
			byteArray = ((ByteArrayOutputStream) fileData.getOutputBuffer()).toByteArray();
			image.setSrc(new StreamResource("logo", () -> new ByteArrayInputStream(byteArray)), byteArray);
		});

		downloader = new Anchor();
		downloader.getElement().setAttribute("download", true);
		downloader.addClassName(POINTER.getName());
		Tooltip.forComponent(downloader).setText(msg.getMessage("FileField.download"));
		downloader.add(VaadinIcon.DOWNLOAD.create());
		downloader.setVisible(false);
		clear = new Button(msg.getMessage("FileField.clear"));
		clear.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		clear.addClickListener(e ->
		{
			image.setVisible(false);
			image.setSrc("");
			image.setLocal(null);
			clear.setVisible(false);
			downloader.setVisible(false);
			upload.setVisible(true);
		});
		clear.setVisible(false);

		VerticalLayout layout = new VerticalLayout(upload, new HorizontalLayout(downloader, clear));
		if(mimeType.equals("image/*"))
			layout.add(image);
		layout.setPadding(false);
		layout.setSpacing(false);
		add(layout);
	}

	void setFileName(String fileName)
	{
		this.fileName = fileName;
	}

	@Override
	public void setEnabled(boolean enabled)
	{
		upload.setMaxFiles(enabled ? 1 : 0);
		super.setEnabled(enabled);
	}

	@Override
	protected LocalOrRemoteResource generateModelValue()
	{
		return image;
	}

	@Override
	protected void setPresentationValue(LocalOrRemoteResource resource)
	{
		if(resource != null && resource.getLocal() != null)
		{
			image.setSrc(new StreamResource("logo", () -> new ByteArrayInputStream(resource.getLocal())), resource.getLocal());
			image.setVisible(true);
			upload.setVisible(false);
			downloader.setVisible(true);
			downloader.setHref(new StreamResource(fileName, () -> new ByteArrayInputStream(resource.getLocal())));
			clear.setVisible(true);
		}
	}
}
