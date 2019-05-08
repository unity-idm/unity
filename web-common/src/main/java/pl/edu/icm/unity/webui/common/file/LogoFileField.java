/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common.file;

import com.vaadin.ui.Image;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIHelper;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.FileStreamResource;
import pl.edu.icm.unity.webui.common.binding.LocalOrRemoteResource;

/**
 * Allows upload image file or set remote logo file url
 * @author P.Piernik
 *
 */
public class LogoFileField extends FileFieldBase
{
	public static final int PREVIEW_SIZE = 10;
	
	private FileStorageService fileService;
	
	private Image preview;
	private VerticalLayout previewL;
	
	public LogoFileField(UnityMessageSource msg, FileStorageService fileStorageService)
	{
		super(msg, "image/*");
		this.fileService = fileStorageService;
		
		preview = new Image();
		preview.setWidth(PREVIEW_SIZE, Unit.EM);
		preview.setHeight(PREVIEW_SIZE, Unit.EM);

		previewL = new VerticalLayout();
		previewL.setWidth(PREVIEW_SIZE, Unit.EM);
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
						fileService.readURI(URIHelper.parseURI(value.getRemote()), null))
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
