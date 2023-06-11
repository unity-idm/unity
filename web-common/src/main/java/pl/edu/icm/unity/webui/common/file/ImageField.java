/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common.file;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Image;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.files.URIHelper;
import pl.edu.icm.unity.webui.common.FileStreamResource;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.binding.LocalOrRemoteResource;

/**
 * Allows upload image file or set remote image file url
 * 
 * @author P.Piernik
 *
 */
public class ImageField extends FileFieldBase
{
	private URIAccessService uriAccessService;

	private Image preview;
	private Button clear;
	private VerticalLayout previewL;

	public ImageField(MessageSource msg, URIAccessService uriAccessService, int maxFileSize)
	{
		this(msg, uriAccessService, maxFileSize, false);
	}
	public ImageField(MessageSource msg, URIAccessService uriAccessService, int maxFileSize, boolean remoteOnly)	
	{
		super(msg, "image/*", maxFileSize, remoteOnly);
		this.uriAccessService = uriAccessService;
		previewL = new VerticalLayout();
		previewL.setWidthUndefined();
		previewL.setMargin(false);
		previewL.setSpacing(false);

		clear = new Button();
		clear.addStyleName(Styles.vButtonLink.toString());
		clear.setIcon(Images.close_small.getResource());
		clear.setDescription(msg.getMessage("FileField.clear"));
		clear.addClickListener(e -> {

			doSetValue(null);
			fireEvent(new ValueChangeEvent<LocalOrRemoteResource>(ImageField.this, getValue(), true));
		});

		previewL.addComponent(clear);
		previewL.setComponentAlignment(clear, Alignment.TOP_RIGHT);

		preview = new Image();
		preview.setStyleName(Styles.imagePreview.toString());
		previewL.addComponent(preview);
		previewL.setVisible(false);

		main.addComponent(previewL);
	}

	@Override
	protected void setPreview()
	{
		LocalOrRemoteResource value = getValue();

		if (value == null || (value.getLocal() == null
				&& (value.getLocalUri() == null || value.getLocalUri().isEmpty())
				&& value.getRemote() == null))
		{
			clear.setVisible(false);
			previewL.setVisible(false);
			preview.setSource(null);
			return;
		}

		previewL.setVisible(true);
		clear.setVisible(true);
		if (value.getLocal() != null)
		{
			preview.setSource(new FileStreamResource(value.getLocal()).getResource());
		} else if (value.getRemote() != null && !value.getRemote().isEmpty())
		{
			try
			{
				preview.setSource(new FileStreamResource(
						uriAccessService.readURI(URIHelper.parseURI(value.getRemote()), null))
								.getResource());

			} catch (Exception e)
			{
				previewL.setVisible(false);
				preview.setSource(null);
			}
		} else
		{
			previewL.setVisible(false);
			preview.setSource(null);
		}
	}
}
