/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common.file;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Optional;

import com.vaadin.data.Binder;
import com.vaadin.data.ValidationResult;
import com.vaadin.data.Validator;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TabSheet.Tab;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.SucceededEvent;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.files.URIHelper;
import pl.edu.icm.unity.webui.common.AbstractUploadReceiver;
import pl.edu.icm.unity.webui.common.FieldSizeConstans;
import pl.edu.icm.unity.webui.common.LimitedOuputStream;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.binding.LocalOrRemoteResource;

/**
 * Base for uri related field
 * @author P.Piernik
 *
 */
public abstract class FileFieldBase extends CustomField<LocalOrRemoteResource>
{
	private int maxFileSize;
	
	protected MessageSource msg;

	private LocalOrRemoteResource value;
	protected TextField remoteUrl;
	private Tab localTab;
	protected Tab remoteTab;
	protected TabSheet tab;
	protected VerticalLayout main;
	private Upload upload;
	

	public FileFieldBase(MessageSource msg, String mimeType, int maxFileSize, boolean remoteOnly)
	{
		this.msg = msg;
		this.maxFileSize = maxFileSize;

		ProgressBar progress = new ProgressBar();
		progress.setVisible(false);
		upload = new Upload();
		upload.setAcceptMimeTypes(mimeType);
		FileUploader uploader = new FileUploader(upload, progress);
		uploader.register();
		VerticalLayout local = new VerticalLayout();
		local.setMargin(new MarginInfo(true, false));
		local.addComponents(upload);

		VerticalLayout remote = new VerticalLayout();
		remoteUrl = new TextField();
		remoteUrl.addValueChangeListener(e -> {
			if (value == null)
				value = new LocalOrRemoteResource();
			value.setRemote(e.getValue());
			setPreview();
			fireEvent(new ValueChangeEvent<LocalOrRemoteResource>(this, value, true));
		});
		remoteUrl.setWidth(FieldSizeConstans.LINK_FIELD_WIDTH, Unit.EM);
		remote.addComponent(remoteUrl);
		remote.setMargin(new MarginInfo(true, false));

		tab = new TabSheet();
		tab.addStyleName("u-logoFieldTabsheet");
		localTab = tab.addTab(local, msg.getMessage("FileField.local"));
		remoteTab = tab.addTab(remote, msg.getMessage("FileField.remote"));

		main = new VerticalLayout();
		main.setMargin(false);
		main.setSpacing(false);
		main.addComponent(remoteOnly ? remoteUrl : tab);
		main.setWidth(FieldSizeConstans.LINK_FIELD_WIDTH, Unit.EM);
		setWidth(FieldSizeConstans.LINK_FIELD_WIDTH, Unit.EM);
	}
	
	@Override
	public void setEnabled(boolean enabled)
	{
		super.setEnabled(enabled);
		upload.setEnabled(enabled);
		
	}

	public void configureBinding(Binder<?> binder, String fieldName,
			Optional<Validator<LocalOrRemoteResource>> additionalValidator)
	{
		binder.forField(this).withValidator((v, c) -> {

			if (v != null)
			{
				if (v.getRemote() != null && !v.getRemote().isEmpty()
						&& (!URIHelper.isWebReady(v.getRemote())))
				{
					return ValidationResult.error(msg.getMessage("FileField.notWebUri"));
				}
				if (v.getLocalUri() != null && (v.getLocal() == null || v.getLocal().length == 0))
				{
					return ValidationResult.error(msg.getMessage("FileField.invalidFile", v.getLocalUri()));
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
	protected Component initContent()
	{

		return main;
	}	

	@Override
	protected void doSetValue(LocalOrRemoteResource value)
	{
		this.value = value;
		
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
		} else if (value.getRemote() != null)
		{
			tab.setSelectedTab(remoteTab);
			remoteUrl.setValue(value.getRemote());
		}
	}
	
	protected abstract void setPreview();

	private class FileUploader extends AbstractUploadReceiver
	{
		private LimitedOuputStream fos;

		public FileUploader(Upload upload, ProgressBar progress)
		{
			super(upload, progress);
		}

		@Override
		public OutputStream receiveUpload(String filename, String mimeType)
		{
			fos = new LimitedOuputStream(maxFileSize, new ByteArrayOutputStream(maxFileSize));
			return fos;
		}

		@Override
		public void uploadSucceeded(SucceededEvent event)
		{
			super.uploadSucceeded(event);

			if (fos.isOverflow())
			{
				NotificationPopup.showError(msg.getMessage("FileField.uploadFailed"),
						msg.getMessage("FileField.fileSizeTooBig"));
				fos = null;
				return;
			}
			try
			{
				remoteUrl.clear();
				if (value == null)
				{
					value = new LocalOrRemoteResource();
				}
				value.setLocal(((ByteArrayOutputStream) fos.getWrappedStream()).toByteArray());
				setPreview();
				fireEvent(new ValueChangeEvent<LocalOrRemoteResource>(FileFieldBase.this, value, true));

			} catch (Exception e)
			{
				NotificationPopup.showError(msg.getMessage("FileField.invalidFile"),
						e.getMessage());
				fos = null;
			}
		}
	}

}
