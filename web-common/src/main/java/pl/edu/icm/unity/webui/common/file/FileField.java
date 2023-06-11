/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common.file;

import org.vaadin.simplefiledownloader.SimpleFileDownloader;

import com.vaadin.server.StreamResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.FileStreamResource;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.binding.LocalOrRemoteResource;

/**
 * Allows upload file or set remote file url
 * @author P.Piernik
 *
 */
public class FileField extends FileFieldBase
{
	private SimpleFileDownloader downloader;
	private Button downloadButton;
	private String fileName;
	private Button clear;
	
	public FileField(MessageSource msg, String mimeType, String previewFileName, int maxFileSize)
	{
		super(msg, mimeType, maxFileSize, false);
		this.fileName = previewFileName;
		downloader = new SimpleFileDownloader();
		addExtension(downloader);
		downloadButton = new Button();
		downloadButton.setDescription(msg.getMessage("FileField.download"));
		downloadButton.setIcon(Images.download.getResource());
		downloadButton.addClickListener(e -> downloader.download());
		downloadButton.setVisible(false);
		clear = new Button();
		clear.setCaption(msg.getMessage("FileField.clear"));
		clear.addClickListener(e -> {

			doSetValue(null);
			fireEvent(new ValueChangeEvent<LocalOrRemoteResource>(FileField.this, getValue(), true));
		});
		clear.setVisible(false);
		
		tab.addSelectedTabChangeListener(e -> {
			if (tab.getSelectedTab().equals(remoteTab.getComponent()))
			{
				downloadButton.setVisible(false);
			}else
			{
				if (getValue()!= null && getValue().getLocal()!=null)
				{
					downloadButton.setVisible(true);
				}
			}
		});
	
		HorizontalLayout wrapper = new HorizontalLayout();
		wrapper.setMargin(false);
		wrapper.addComponent(downloadButton);
		wrapper.addComponent(clear);
		
		main.addComponent(wrapper);	
	}
	
	@Override
	public void setEnabled(boolean enabled)
	{
		super.setEnabled(enabled);
		downloadButton.setEnabled(enabled);
		clear.setEnabled(enabled);
	}
	
	@Override
	protected void setPreview()
	{
		LocalOrRemoteResource value = getValue();
		downloadButton.setVisible(false);
		clear.setVisible(false);
		downloader.setFileDownloadResource(null);
	
		if (value != null && value.getLocal() != null)
		{
			downloader.setFileDownloadResource(new StreamResource(new FileStreamResource(value.getLocal()), fileName));
			downloadButton.setVisible(true);
			clear.setVisible(true);
		} 
	}	
}
