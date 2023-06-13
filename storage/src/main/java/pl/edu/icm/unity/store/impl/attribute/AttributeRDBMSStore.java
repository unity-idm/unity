/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attribute;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.store.StorageConfiguration;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.impl.StorageLimits;
import pl.edu.icm.unity.store.rdbms.GenericRDBMSCRUD;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionTL;
import pl.edu.icm.unity.store.types.StoredAttribute;


/**
 * RDBMS storage of {@link Attribute}
 * @author K. Benedyczak
 */
@Repository(AttributeRDBMSStore.BEAN)
public class AttributeRDBMSStore extends GenericRDBMSCRUD<StoredAttribute, AttributeBean> implements AttributeDAO
{
	public static final String BEAN = DAO_ID + "rdbms";
	private final GroupDAO groupDAO;
	private final Integer attributeSizeLimit;

	@Autowired
	AttributeRDBMSStore(AttributeRDBMSSerializer dbSerializer,
			GroupDAO groupDAO,
			StorageConfiguration storageConfiguration)
	{
		super(AttributesMapper.class, dbSerializer, NAME);
		this.groupDAO = groupDAO;
		attributeSizeLimit = storageConfiguration.getIntValue(StorageConfiguration.MAX_ATTRIBUTE_SIZE);
	}

	@Override
	public void updateAttribute(StoredAttribute a)
	{
		AttributesMapper mapper = SQLTransactionTL.getSql().getMapper(AttributesMapper.class);
		AttributeBean toUpdate = jsonSerializer.toDB(a);
		assertContentsLimit(toUpdate.getContents());
		List<AttributeBean> old = getAttributesFiltering(a.getAttribute().getName(), a.getEntityId(), 
				a.getAttribute().getGroupPath());
		if (old.isEmpty())
			throw new IllegalArgumentException(elementName + " [" + a.getAttribute().getName() + 
					"] does not exist");
		AttributeBean oldSingle = old.get(0);
		StoredAttribute oldParsed = jsonSerializer.fromDB(oldSingle);
		preUpdateCheck(oldParsed, a);
		toUpdate.setId(oldSingle.getId());
		mapper.updateByKey(toUpdate);		
	}

	@Override
	protected void assertContentsLimit(byte[] contents)
	{
		StorageLimits.checkAttributeLimit(contents, attributeSizeLimit);
	}
	
	@Override
	public void deleteAttribute(String attribute, long entityId, String group)
	{
		AttributesMapper mapper = SQLTransactionTL.getSql().getMapper(AttributesMapper.class);
		List<AttributeBean> existing = getAttributesFiltering(attribute, entityId, group);
		if (existing.isEmpty())
			throw new IllegalArgumentException(elementName + " [" + attribute + 
					"] does not exist");
		mapper.deleteByKey(existing.get(0).getId());
	}

	@Override
	public void deleteAttributesInGroup(long entityId, String group)
	{
		AttributesMapper mapper = SQLTransactionTL.getSql().getMapper(AttributesMapper.class);
		long groupId = groupDAO.getKeyForName(group);
		AttributeBean param = new AttributeBean();
		param.setEntityId(entityId);
		param.setGroupId(groupId);
		mapper.deleteAttributesInGroup(param);
	}

	@Override
	public List<StoredAttribute> getAttributes(String attribute, Long entityId, String group)
	{
		List<AttributeBean> existing = getAttributesFiltering(attribute, entityId, group);
		return convertList(existing);
	}

	@Override
	public List<AttributeExt> getEntityAttributes(long entityId, String attribute, String group)
	{
		List<AttributeBean> existing = getAttributesFiltering(attribute, entityId, group);
		List<AttributeExt> ret = new ArrayList<>(existing.size());
		for (AttributeBean ab: existing)
			ret.add(jsonSerializer.fromDB(ab).getAttribute());
		return ret;
	}
	
	
	private List<AttributeBean> getAttributesFiltering(String attribute, Long entityId, String group)
	{
		AttributesMapper mapper = SQLTransactionTL.getSql().getMapper(AttributesMapper.class);
		AttributeBean param = new AttributeBean();
		param.setEntityId(entityId);
		param.setName(attribute);
		param.setGroup(group);
		return mapper.getAttributes(param);
	}

	@Override
	public List<StoredAttribute> getAttributesOfGroupMembers(String group)
	{
		AttributesMapper mapper = SQLTransactionTL.getSql().getMapper(AttributesMapper.class);
		List<AttributeBean> groupMembersAttributes = mapper.getGroupMembersAttributes(group);
		return convertList(groupMembersAttributes);
	}

	@Override
	public List<StoredAttribute> getAttributesOfGroupMembers(List<String> attributes, List<String> groups)
	{
		AttributesMapper mapper = SQLTransactionTL.getSql().getMapper(AttributesMapper.class);
		return convertList(mapper.getSelectedGroupsMembersAttributes(groups, attributes));
	}

	@Override
	public List<StoredAttribute> getAttributesOfGroupMembers(List<String> groups)
	{
		AttributesMapper mapper = SQLTransactionTL.getSql().getMapper(AttributesMapper.class);
		return convertList(mapper.getGroupsMembersAttributes(groups));
	}

	@Override
	public long getCountWithoutType(List<String> types)
	{
		AttributesMapper mapper = SQLTransactionTL.getSql().getMapper(AttributesMapper.class);
		return mapper.getCountWithoutType(types);
	}

	@Override
	public void linkKeywordToAttribute(String keyword, long attributeId)
	{
		AttributesLookupMapper lookupMapper = SQLTransactionTL.getSql().getMapper(AttributesLookupMapper.class);
		lookupMapper.create(new AttributeLookupBean(null, keyword, attributeId));
	}

	@Override
	public List<StoredAttribute> getAllWithKeyword(String keyword)
	{
		AttributesLookupMapper lookupMapper = SQLTransactionTL.getSql().getMapper(AttributesLookupMapper.class);
		List<AttributeLookupBean> lookupResult = lookupMapper.getByKeyword(keyword);
		return lookupResult.stream()
				.map(AttributeLookupBean::getAttributeId)
				.map(this::getByKey)
				.collect(toList());
	}

	@Override
	public List<Long> getAllIds()
	{
		AttributesMapper mapper = SQLTransactionTL.getSql().getMapper(AttributesMapper.class);
		return mapper.getAll().stream()
				.map(AttributeBean::getId)
				.collect(toList());
	}

	@Override
	public List<String> getAllKeywordsFor(Long attributeId)
	{
		AttributesLookupMapper lookupMapper = SQLTransactionTL.getSql().getMapper(AttributesLookupMapper.class);
		return lookupMapper.getAllKeywords(attributeId);
	}
}
