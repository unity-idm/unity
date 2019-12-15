/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to3_2;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.store.impl.identitytype.IdentityTypesMapper;
import pl.edu.icm.unity.store.migration.InDBSchemaUpdater;
import pl.edu.icm.unity.store.rdbms.BaseBean;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionTL;

/**
 * Update db from 3.1 release version (DB schema version 2.8). Drops extracted attributes from identity types.
 */
@Component
public class InDBUpdateFromSchema2_8 implements InDBSchemaUpdater
{
	@Override
	public void update() throws IOException
	{
		dropExtractedAttributes();
	}

	private void dropExtractedAttributes()
	{
		IdentityTypesMapper mapper = SQLTransactionTL.getSql().getMapper(IdentityTypesMapper.class);
		List<BaseBean> allInDB = mapper.getAll();
		
		for (BaseBean idTypeBean: allInDB)
		{
			ObjectNode idType = JsonUtil.parse(idTypeBean.getContents());
			idType.remove("extractedAttributes");
			idTypeBean.setContents(JsonUtil.serialize2Bytes(idType));
			mapper.updateByKey(idTypeBean);
		}
	}
}
