/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to3_12;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.base.utils.JsonUtil;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.impl.attributetype.AttributeTypeBean;
import pl.edu.icm.unity.store.impl.attributetype.AttributeTypesMapper;
import pl.edu.icm.unity.store.impl.tokens.TokenRDBMSStore;
import pl.edu.icm.unity.store.migration.InDBContentsUpdater;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionTL;

@Component
public class InDBUpdateFromSchema17 implements InDBContentsUpdater
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, InDBUpdateFromSchema17.class);

	private final TokenRDBMSStore tokensDAO;
	
	@Autowired
	public InDBUpdateFromSchema17(TokenRDBMSStore tokensDAO)
	{
		this.tokensDAO = tokensDAO;
	}

	@Override
	public int getUpdatedVersion()
	{
		return 17;
	}

	@Override
	public void update() throws IOException
	{
		updateOAuthTokens();
		updateRoleAttributeType();
	}

	void updateOAuthTokens() throws IOException
	{
		List<Token> all = tokensDAO.getAll();
		for (Token token : all)
		{
			if (!UpdateHelperTo17.oauthTokenTypes.contains(token.getType()))
				continue;
			ObjectNode objContent = JsonUtil.parse(token.getContents());

			Optional<ObjectNode> fixed = UpdateHelperTo17.fixOauthToken(objContent);
			if (fixed.isPresent())
			{
				token.setContents(JsonUtil.serialize2Bytes(fixed.get()));
				tokensDAO.update(token);
				log.info("Updated OAuth token audience {}", objContent);
			}
		}
	}
	
	private void updateRoleAttributeType()
	{
		AttributeTypesMapper atTypeMapper = SQLTransactionTL.getSql()
				.getMapper(AttributeTypesMapper.class);
		List<AttributeTypeBean> atTypes = atTypeMapper.getAll();
		for (AttributeTypeBean atType : atTypes)
		{
			if ("sys:AuthorizationRole".equals(atType.getName()))
			{
				log.info("Updating attribute type {} adding new value \"Policy documents manager\"", atType.getName());
				AttributeType at = new AttributeType();
				at.setName(atType.getName());
				at.setValueSyntax(atType.getValueSyntaxId());
				at.fromJsonBase(JsonUtil.parse(atType.getContents()));
				at.setValueSyntaxConfiguration(UpdateHelperTo17.getRoleAttributeSyntaxConfig());
				String orgEnRoleDescription = UpdateHelperTo17.getOrgEnRoleDescription();
				if (orgEnRoleDescription.equals(at.getDescription() != null ? at.getDescription()
						.getValue("en") : null))
				{
					I18nString orgDesc = at.getDescription();
					orgDesc.addValue("en", UpdateHelperTo17.getEnRoleDescription());
					at.setDescription(orgDesc);
				}
				atType.setContents(JsonUtil.serialize2Bytes(at.toJsonBase()));
				atTypeMapper.updateByKey(atType);
			}
		}
	}
}
