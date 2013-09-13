/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import com.vaadin.ui.ProgressIndicator;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.ProgressListener;
import com.vaadin.ui.Upload.Receiver;
import com.vaadin.ui.Upload.StartedEvent;
import com.vaadin.ui.Upload.StartedListener;
import com.vaadin.ui.Upload.SucceededEvent;
import com.vaadin.ui.Upload.SucceededListener;

/**
 * Base class for implementing Receivers needed for Vaadin Upload component.
 * Extensions must implement receiveUpload method and may overwrite some of defined here methods,
 * typically the uploadSucceeded. 
 * @author K. Benedyczak
 */
public abstract class AbstractUploadReceiver implements Receiver, SucceededListener, StartedListener, ProgressListener
{
	private Upload upload;
	private ProgressIndicator progressIndicator;

	public AbstractUploadReceiver(Upload upload, ProgressIndicator progress)
	{
		this.upload = upload;
		this.progressIndicator = progress;
	}

	@Override
	public void uploadSucceeded(SucceededEvent event) 
	{
		progressIndicator.setVisible(false);
		upload.setEnabled(true);
	}

	@Override
	public void uploadStarted(StartedEvent event)
	{
		upload.setEnabled(false);
		long length = event.getContentLength();
		if (length <= 0)
			progressIndicator.setIndeterminate(true);
		else
			progressIndicator.setIndeterminate(false);
		progressIndicator.setVisible(true);
	}

	@Override
	public void updateProgress(long readBytes, long contentLength)
	{
		if (contentLength > 0 && !progressIndicator.isIndeterminate())
		{
			progressIndicator.setValue((float)readBytes/contentLength);
		}
	}
}
