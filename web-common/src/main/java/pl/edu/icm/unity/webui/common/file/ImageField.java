/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common.file;

import com.vaadin.ui.Image;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.files.URIHelper;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.FileStreamResource;
import pl.edu.icm.unity.webui.common.binding.LocalOrRemoteResource;

/**
 * Allows upload image file or set remote image file url
 * @author P.Piernik
 *
 */
public class ImageField extends FileFieldBase
{
	private static final int PREVIEW_SIZE_EM = 10;
	
	private URIAccessService uriAccessService;
	
	private Image preview;
	private VerticalLayout previewL;
	
	public ImageField(UnityMessageSource msg, URIAccessService uriAccessService)
	{
		super(msg, "image/*");
		this.uriAccessService = uriAccessService;
		
		preview = new Image();
		preview.setWidth(PREVIEW_SIZE_EM, Unit.EM);
		preview.setHeight(PREVIEW_SIZE_EM, Unit.EM);

		previewL = new VerticalLayout();
		previewL.setWidth(PREVIEW_SIZE_EM, Unit.EM);
		previewL.setMargin(false);
		previewL.setSpacing(false);
		previewL.addComponent(preview);
		previewL.setVisible(false);
		
		main.addComponent(previewL);
	}

	@Override
	protected void setPreview()
	{
		LocalOrRemoteResource value = getValue();
		
		if (value == null)
		{
			previewL.setVisible(false);
			preview.setSource(null);
			return;
		}

		previewL.setVisible(true);
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
		}else
		{
			previewL.setVisible(false);
			preview.setSource(null);
		}
	}
}
