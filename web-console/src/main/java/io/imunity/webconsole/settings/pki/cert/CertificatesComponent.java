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
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.pki.NamedCertificate;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.GridWithActionColumn;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;
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
	private MessageSource msg;
	private CertificatesController certController;
	private GridWithActionColumn<NamedCertificate> certList;

	public CertificatesComponent(MessageSource msg, CertificatesController controller)
	{
		this.msg = msg;
		this.certController = controller;
		initUI();
	}

	private void initUI()
	{

		certList = new GridWithActionColumn<>(msg, getActionsHandlers(), false);
		certList.addComponentColumn(c -> StandardButtonsHelper.buildLinkButton(c.name, e -> gotoEdit(c)),
				msg.getMessage("CertificatesComponent.certificateNameCaption"), 10).setSortable(true)
				.setComparator((c1, c2) -> {
					return c1.name.compareTo(c2.name);
				}).setId("name");

		certList.setItems(getCertificates());
		certList.sort("name");
		
		VerticalLayout main = new VerticalLayout();
		Label certCaption = new Label(msg.getMessage("CertificatesComponent.caption"));
		certCaption.setStyleName(Styles.sectionTitle.toString());
		main.addComponent(certCaption);
		main.addComponent(StandardButtonsHelper.buildTopButtonsBar(StandardButtonsHelper.build4AddAction(msg,
				e -> NavigationHelper.goToView(NewCertificateView.VIEW_NAME))));
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
			NotificationPopup.showError(msg, e);
		}
		return Collections.emptyList();
	}

	private void remove(NamedCertificate cert)
	{
		try
		{
			certController.removeCertificate(cert);
			certList.removeElement(cert);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
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
