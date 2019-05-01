/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import com.vaadin.data.Binder;
import com.vaadin.data.ValidationResult;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.Image;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TabSheet.Tab;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.SucceededEvent;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIHelper;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;

/**
 * 
 * @author P.Piernik
 *
 */
public class LogoFileField extends CustomField<LocalOrUrlResource>
{
	public static final int MAX_LOGO_SIZE = 2000000;
	public static final int PREVIEW_SIZE = 10;

	private UnityMessageSource msg;
	private FileStorageService fileService;

	private LocalOrUrlResource value;
	private Image preview;
	private VerticalLayout previewL;
	private TextField remoteUrl;
	private Tab localTab;
	private Tab remoteTab;
	private TabSheet tab;
	private VerticalLayout main;

	public LogoFileField(UnityMessageSource msg, FileStorageService fileService)
	{
		this.msg = msg;
		this.fileService = fileService;

		preview = new Image();
		preview.setWidth(PREVIEW_SIZE, Unit.EM);
		preview.setHeight(PREVIEW_SIZE, Unit.EM);

		previewL = new VerticalLayout();
		previewL.setWidth(PREVIEW_SIZE, Unit.EM);
		previewL.setMargin(false);
		previewL.setSpacing(false);
		previewL.addComponent(preview);
		previewL.setVisible(false);

		ProgressBar progress = new ProgressBar();
		progress.setVisible(false);
		Upload upload = new Upload();
		upload.setAcceptMimeTypes("image/*");
		ImageUploader uploader = new ImageUploader(upload, progress);
		uploader.register();
		VerticalLayout local = new VerticalLayout();
		local.setMargin(new MarginInfo(true, false));
		local.addComponents(upload);

		VerticalLayout remote = new VerticalLayout();
		remoteUrl = new TextField();
		remoteUrl.addValueChangeListener(e -> {
			if (value == null)
				value = new LocalOrUrlResource();
			value.setRemote(e.getValue());
			setPreview();
			fireEvent(new ValueChangeEvent<LocalOrUrlResource>(LogoFileField.this, value, true));
		});
		remoteUrl.setWidth(FieldSizeConstans.LINK_FIELD_WIDTH, Unit.EM);
		remote.addComponent(remoteUrl);
		remote.setMargin(new MarginInfo(true, false));
		remote.addComponents();

		tab = new TabSheet();
		localTab = tab.addTab(local, msg.getMessage("LogoFileField.local"));
		remoteTab = tab.addTab(remote, msg.getMessage("LogoFileField.remote"));

		main = new VerticalLayout();
		main.setMargin(false);
		main.setSpacing(false);
		main.addComponent(tab);
		main.addComponent(previewL);

	}

	public void configureBinding(Binder<?> binder, String fieldName)
	{
		binder.forField(this).withValidator((v, c) -> {

			if (v != null && v.getRemote() != null && !v.getRemote().isEmpty()
					&& (!URIHelper.isWebReady(v.getRemote())))
			{
				return ValidationResult.error(msg.getMessage("LogoFileField.notWebUri"));
			}
			return ValidationResult.ok();

		}).bind(fieldName);
	}

	@Override
	public LocalOrUrlResource getValue()
	{
		return value;
	}

	@Override
	protected Component initContent()
	{

		return main;
	}

	private void setPreview()
	{
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
						fileService.readURI(URIHelper.parseURI(value.getRemote())))
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

	@Override
	protected void doSetValue(LocalOrUrlResource value)
	{
		this.value = value;
		if (value == null)
			return;

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

	private class ImageUploader extends AbstractUploadReceiver
	{
		private LimitedOuputStream fos;

		public ImageUploader(Upload upload, ProgressBar progress)
		{
			super(upload, progress);
		}

		@Override
		public OutputStream receiveUpload(String filename, String mimeType)
		{
			fos = new LimitedOuputStream(MAX_LOGO_SIZE, new ByteArrayOutputStream(MAX_LOGO_SIZE));
			return fos;
		}

		@Override
		public void uploadSucceeded(SucceededEvent event)
		{
			super.uploadSucceeded(event);

			if (fos.isOverflow())
			{
				NotificationPopup.showError(msg.getMessage("LogoFileField.uploadFailed"),
						msg.getMessage("LogoFileField.imageSizeTooBig"));
				fos = null;
				return;
			}
			try
			{
				remoteUrl.clear();
				if (value == null)
				{
					value = new LocalOrUrlResource();
				}
				value.setLocal(((ByteArrayOutputStream) fos.getWrappedStream()).toByteArray());
				setPreview();
				fireEvent(new ValueChangeEvent<LocalOrUrlResource>(LogoFileField.this, value, true));

			} catch (Exception e)
			{
				NotificationPopup.showError(msg.getMessage("LogoFileField.invalidImage"),
						e.getMessage());
				fos = null;
			}
		}
	}

}
