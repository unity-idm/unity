/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.settings.pki.cert;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.settings.pki.PKIView;
import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.helpers.NavigationHelper.CommonViewParam;
import io.imunity.webelements.helpers.StandardButtonsHelper;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.pki.NamedCertificate;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Edit certificate view
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
class EditCertificateView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "EditCertificate";

	private CertificatesController controller;
	private UnityServerConfiguration config;
	private CertificateEditor editor;
	private UnityMessageSource msg;
	private String certName;

	@Autowired
	public EditCertificateView(UnityMessageSource msg, CertificatesController controller,
			UnityServerConfiguration config)
	{
		this.msg = msg;
		this.controller = controller;
		this.config = config;
	}

	private void onConfirm()
	{
		if (editor.hasErrors())
		{
			return;
		}

		try
		{

			controller.updateCertificate(editor.getCertificate());

		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
			return;
		}

		NavigationHelper.goToView(PKIView.VIEW_NAME);

	}

	private void onCancel()
	{
		NavigationHelper.goToView(PKIView.VIEW_NAME);

	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		certName = NavigationHelper.getParam(event, CommonViewParam.name.toString());
		NamedCertificate cert;
		try
		{
			cert = controller.getCertificate(certName);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
			NavigationHelper.goToView(PKIView.VIEW_NAME);
			return;
		}

		editor = new CertificateEditor(msg, config, cert);
		editor.editMode();

		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.addComponent(editor);
		main.addComponent(StandardButtonsHelper.buildConfirmEditButtonsBar(msg, () -> onConfirm(),
				() -> onCancel()));
		setCompositionRoot(main);
	}

	@Override
	public String getDisplayedName()
	{
		return certName;
	}

	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@org.springframework.stereotype.Component
	public static class EditCertificateViewInfoProvider extends WebConsoleNavigationInfoProviderBase
	{

		@Autowired
		public EditCertificateViewInfoProvider(TrustedCertNavigationInfoProvider parent,
				ObjectFactory<EditCertificateView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.ParameterizedView)
					.withParent(parent.getNavigationInfo()).withObjectFactory(factory).build());

		}
	}

}
