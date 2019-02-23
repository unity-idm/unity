/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.settings.pki.cert;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Sets;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.helpers.NavigationHelper.CommonViewParam;
import io.imunity.webelements.helpers.StandardButtonsHelper;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.pki.NamedCertificate;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.ListOfElementsWithActions;
import pl.edu.icm.unity.webui.common.ListOfElementsWithActions.ActionColumn;
import pl.edu.icm.unity.webui.common.ListOfElementsWithActions.ActionColumn.Position;
import pl.edu.icm.unity.webui.common.ListOfElementsWithActions.Column;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Lists all certificates
 * 
 * @author P.Piernik
 *
 */

public class CertificatesComponent extends CustomComponent
{
	private UnityMessageSource msg;
	private CertificatesController certController;
	private ListOfElementsWithActions<NamedCertificate> certList;

	public CertificatesComponent(UnityMessageSource msg, CertificatesController controller)
	{
		this.msg = msg;
		this.certController = controller;
		initUI();
	}

	private void initUI()
	{
		certList = new ListOfElementsWithActions<>(
				Arrays.asList(new Column<>(msg.getMessage("CertificatesComponent.certificateNameCaption"),
						c -> StandardButtonsHelper.buildLinkButton(c.name, e -> gotoEdit(c)),
						2)),
				new ActionColumn<>(msg.getMessage("actions"), getActionsHandlers(), 0, Position.Right));

		certList.setAddSeparatorLine(true);

		for (NamedCertificate cert : getCertificates())
		{
			certList.addEntry(cert);
		}

		VerticalLayout main = new VerticalLayout();
		Label certCaption = new Label(msg.getMessage("CertificatesComponent.caption"));
		certCaption.setStyleName(Styles.bold.toString());
		main.addComponent(certCaption);
		main.addComponent(StandardButtonsHelper.buildTopButtonsBar(StandardButtonsHelper
				.build4AddAction(msg, e -> NavigationHelper.goToView(NewCertificateView.VIEW_NAME))));
		main.addComponent(certList);
		main.setWidth(100, Unit.PERCENTAGE);
		main.setMargin(false);
		setCompositionRoot(main);
	}

	private List<SingleActionHandler<NamedCertificate>> getActionsHandlers()
	{
		SingleActionHandler<NamedCertificate> edit = SingleActionHandler
				.builder4Edit(msg, NamedCertificate.class)
				.withHandler(r -> gotoEdit(r.iterator().next())).build();

		SingleActionHandler<NamedCertificate> remove = SingleActionHandler
				.builder4Delete(msg, NamedCertificate.class)
				.withHandler(r -> tryRemove(r.iterator().next())).build();
		return Arrays.asList(edit, remove);
	}

	private Collection<NamedCertificate> getCertificates()
	{
		try
		{
			return certController.getCertificates();
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
		}
		return Collections.emptyList();
	}

	private void remove(NamedCertificate cert)
	{
		try
		{
			certController.removeCertificate(cert);
			certList.removeEntry(cert);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
		}
	}

	private void tryRemove(NamedCertificate cert)
	{

		String confirmText = MessageUtils.createConfirmFromStrings(msg, Sets.newHashSet(cert.name));
		new ConfirmDialog(msg, msg.getMessage("CertificatesComponent.confirmDeleteCertificate", confirmText),
				() -> remove(cert)).show();
	}

	private void gotoEdit(NamedCertificate cert)
	{
		NavigationHelper.goToView(EditCertificateView.VIEW_NAME + "/" + CommonViewParam.name.toString() + "="
				+ cert.name);
	}
}
