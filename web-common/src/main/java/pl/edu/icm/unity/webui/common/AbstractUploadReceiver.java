/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.UI;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.FinishedEvent;
import com.vaadin.ui.Upload.FinishedListener;
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
public abstract class AbstractUploadReceiver implements Receiver, SucceededListener, StartedListener, ProgressListener,
	FinishedListener
{
	private Upload upload;
	private ProgressBar progressIndicator;
	private long lastReadBytes = 0;
	private static final long UPDATE_THRESHOLD = 50000;

	public AbstractUploadReceiver(Upload upload, ProgressBar progress)
	{
		this.upload = upload;
		this.progressIndicator = progress;
	}
	
	/**
	 * Registers this receiver to the upload component.
	 */
	public void register()
	{
		upload.setReceiver(this);
		upload.addSucceededListener(this);
		upload.addFinishedListener(this);
		upload.addStartedListener(this);
		upload.addProgressListener(this);
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
		lastReadBytes = 0;
		long length = event.getContentLength();
		if (length <= 0)
			progressIndicator.setIndeterminate(true);
		else
			progressIndicator.setIndeterminate(false);
		progressIndicator.setVisible(true);
		progressIndicator.setValue(0f);
		UI.getCurrent().setPollInterval(1000);
	}
        
	@Override
	public final void uploadFinished(FinishedEvent event)
        {
		UI.getCurrent().setPollInterval(-1);
        }
        
	@Override
	public void updateProgress(long readBytes, long contentLength)
	{
		if (contentLength > 0 && !progressIndicator.isIndeterminate() && 
				readBytes > lastReadBytes + UPDATE_THRESHOLD)
		{
			progressIndicator.setValue((float)readBytes/contentLength);
			lastReadBytes = readBytes;
		}
	}
}
