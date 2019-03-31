/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.audit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import pl.edu.icm.unity.store.api.AuditEventDAO;
import pl.edu.icm.unity.store.rdbms.GenericRDBMSCRUD;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionTL;
import pl.edu.icm.unity.types.basic.AuditEvent;
import pl.edu.icm.unity.types.basic.AuditEvent.AuditEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * RDBMS storage of {@link AuditEvent.AuditEntity}. Helper repository to handle actions related to AuditEntity entries.
 *
 * Package private access - public methods are exposed via AuditEventDAO.
 *
 * @author R. Ledzinski
 */
@Repository
class AuditEntityRDBMSStore
{
	Long findOrCreateEntity(AuditEntity auditEntity) {
		if (auditEntity == null) {
			return null;
		}
		Long id = getAuditEntityId(auditEntity);
		if (id == null) {
			id = createAuditEntity(auditEntity);
		}
		return id;
	}

	private Long getAuditEntityId(AuditEntity auditEntity)
	{
		AuditEventMapper mapper = SQLTransactionTL.getSql().getMapper(AuditEventMapper.class);
		return mapper.getAuditEntityId(new AuditEntityBean(auditEntity));
	}

	private long createAuditEntity(AuditEntity auditEntity)
	{
		AuditEventMapper mapper = SQLTransactionTL.getSql().getMapper(AuditEventMapper.class);
		AuditEntityBean bean = new AuditEntityBean(auditEntity);
		mapper.createAuditEntity(bean);
		return bean.getId();
	}
}
