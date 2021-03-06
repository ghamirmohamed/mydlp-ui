package com.mydlp.ui.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.Query;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.mydlp.ui.domain.ADDomainItem;
import com.mydlp.ui.domain.ADDomainOU;
import com.mydlp.ui.domain.ADDomainUser;
import com.mydlp.ui.domain.AuthUser;
import com.mydlp.ui.domain.IncidentLog;
import com.mydlp.ui.domain.IncidentLogFile;
import com.mydlp.ui.domain.IncidentLogFileContent;

@Repository("incidentLogDAO")
@Transactional
public class IncidentLogDAOImpl extends AbstractLogDAO implements IncidentLogDAO {
	
	protected static final String MAPKEY_LABEL = "labelKey";
	protected static final String MAPKEY_VALUE = "value";
	
	@Autowired
	protected ADDomainDAO adDomainDAO;

	@Override
	public Long getIncidentCount(AuthUser user, List<List<Object>> criteriaList) {
		DetachedCriteria criteria = 
				DetachedCriteria.forClass(IncidentLog.class)
					.setProjection(Projections.rowCount());
		criteria = applyUserCriteria(criteria, user);
		criteria = applyCriteriaList(criteria, criteriaList);
		@SuppressWarnings("unchecked")
		List<Long> returnList = getHibernateTemplate().findByCriteria(criteria);
		return DAOUtil.getSingleResult(returnList);
	}
	
	protected DetachedCriteria applyUserCriteria(DetachedCriteria detachedCriteria, AuthUser user)
	{
		DetachedCriteria criteria = detachedCriteria;
		
		if (!user.getHasAuthorityScope())
			return criteria;
		
		Disjunction disjunction = Restrictions.disjunction();
		disjunction.add(Restrictions.sqlRestriction("(1=0)")); //  defaults to false
		
		for (ADDomainItem item : user.getAuthorityScopeADItems()) {
			if (item instanceof ADDomainUser) {
				ADDomainUser adUser = (ADDomainUser) item;
				disjunction.add(Property.forName("sourceUser").eq(adUser.getUserPrincipalName()));
			} else if (item instanceof ADDomainOU) {
				ADDomainOU adOU = (ADDomainOU) item;
				List<String> names = adDomainDAO.getUserPrincipalNames(adOU);
				disjunction.add(Property.forName("sourceUser").in(names));
			}
		}
		
		criteria = criteria.add(disjunction);
		
		return criteria;
	}
	
	protected DetachedCriteria applyCriteriaList(DetachedCriteria detachedCriteria, List<List<Object>> criteriaList)
	{
		DetachedCriteria criteria = detachedCriteria;
		for (List<Object> list : criteriaList) {
			String field = (String) list.get(0);
			String operation = (String) list.get(1);
			if (field.equals("date") && operation.equals("bw"))
			{
				Date startDate = (Date) list.get(2);
				Date endDate = (Date) list.get(3);
				criteria = criteria.add(Restrictions.between(field, startDate, endDate));
			}
			if (field.equals("contentId") && operation.equals("eq"))
			{
				criteria.createAlias("files", "incidentFile", CriteriaSpecification.LEFT_JOIN);
				Integer contentId = (Integer) list.get(2);
				criteria.add(Restrictions.eq("incidentFile.content.id", contentId));
			}
		}
		return criteria;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<IncidentLog> getIncidents(AuthUser user, List<List<Object>> criteriaList, Integer offset, Integer limit) {
		DetachedCriteria criteria = 
				DetachedCriteria.forClass(IncidentLog.class)
					.addOrder(Order.desc("id"));
		criteria = applyUserCriteria(criteria, user);
		criteria = applyCriteriaList(criteria, criteriaList);
		return criteria.getExecutableCriteria(getSession())
			.setFirstResult(offset)
			.setMaxResults(limit)
			.list();
	}

	@Override
	public IncidentLogFile geIncidentLogFile(Integer id) {
		return getHibernateTemplate().load(IncidentLogFile.class, id);
	}

	@Override
	public IncidentLogFileContent getIncidentContent(Integer id) {
		DetachedCriteria criteria = 
				DetachedCriteria.forClass(IncidentLogFileContent.class)
					.add(Restrictions.eq("id", id));
		@SuppressWarnings("unchecked")
		List<IncidentLogFileContent> l = getHibernateTemplate().findByCriteria(criteria);
		return DAOUtil.getSingleResult(l);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<String> getFilenamesForContent(Integer id) {
		return getHibernateTemplate().findByNamedParam(
				"select distinct f.filename from IncidentLogFile f " +
				"where f.content.id=:contentId", "contentId", id);
	}
	

	@Override
	public List<Map<String, Object>> getProtocolIncidentCount(Integer hours) {
		Date now = new Date();
		Date startDate = new Date(now.getTime() - hours * 60L * 60L * 1000L);
		List<Map<String, Object>> returnList = new ArrayList<Map<String, Object>>();
		
		Query query = getSession().createQuery(
				"select count(l), l.channel from IncidentLog l " +
				"where l.date between :startDate and :endDate " +
				"group by l.channel order by 1 desc");
		query.setTimestamp("startDate", startDate);
		query.setTimestamp("endDate", now);
		
		for (@SuppressWarnings("unchecked")
		Iterator<Object[]> iterator = query.list().iterator(); iterator.hasNext();) {
			Object[] row = (Object[]) iterator.next();
			Map<String, Object> returnMap = new HashMap<String, Object>();
			returnMap.put(MAPKEY_LABEL, row[1]);
			returnMap.put(MAPKEY_VALUE, row[0]);
			returnList.add(returnMap);
		}
		return returnList;
	}
	
	public static String intToIpStr(long i) {
        return ((i >> 24 ) & 0xFF) + "." +
               ((i >> 16 ) & 0xFF) + "." +
               ((i >>  8 ) & 0xFF) + "." +
               ( i        & 0xFF);
    }

	@Override
	public List<Map<String, Object>> topSourceAddress(Integer hours, Integer count) {
		Date now = new Date();
		Date startDate = new Date(now.getTime() - hours * 60L * 60L * 1000L);
		List<Map<String, Object>> returnList = new ArrayList<Map<String, Object>>();
		
		Query query = getSession().createQuery(
				"select count(l), l.sourceIp from IncidentLog l " +
				"where l.date between :startDate and :endDate " +
				"group by l.sourceIp order by 1 desc");
		query.setTimestamp("startDate", startDate);
		query.setTimestamp("endDate", now);
		query.setMaxResults(count);
		
		for (@SuppressWarnings("unchecked")
		Iterator<Object[]> iterator = query.list().iterator(); iterator.hasNext();) {
			Object[] row = (Object[]) iterator.next();
			Map<String, Object> returnMap = new HashMap<String, Object>();
			returnMap.put(MAPKEY_LABEL, intToIpStr(((Long) row[1]).longValue()));
			returnMap.put(MAPKEY_VALUE, row[0]);
			returnList.add(returnMap);
		}	
		return returnList;
	}

	@Override
	public List<Map<String, Object>> topSourceUser(Integer hours, Integer count) {
		Date now = new Date();
		Date startDate = new Date(now.getTime() - hours * 60L * 60L * 1000L);
		List<Map<String, Object>> returnList = new ArrayList<Map<String, Object>>();
		
		Query query = getSession().createQuery(
				"select count(l), l.sourceUser from IncidentLog l " +
				"where l.date between :startDate and :endDate " +
				"group by l.sourceUser order by 1 desc");
		query.setTimestamp("startDate", startDate);
		query.setTimestamp("endDate", now);
		query.setMaxResults(count);
		
		for (@SuppressWarnings("unchecked")
		Iterator<Object[]> iterator = query.list().iterator(); iterator.hasNext();) {
			Object[] row = (Object[]) iterator.next();
			Map<String, Object> returnMap = new HashMap<String, Object>();
			returnMap.put(MAPKEY_LABEL, row[1]);
			returnMap.put(MAPKEY_VALUE, row[0]);
			returnList.add(returnMap);
		}	
		return returnList;
	}

	@Override
	public List<Map<String, Object>> getActionIncidentCount(Integer hours) {
		Date now = new Date();
		Date startDate = new Date(now.getTime() - hours * 60L * 60L * 1000L);
		List<Map<String, Object>> returnList = new ArrayList<Map<String, Object>>();
		
		Query query = getSession().createQuery(
				"select count(l), l.action from IncidentLog l " +
				"where l.date between :startDate and :endDate " +
				"group by l.action order by 1 desc");
		query.setTimestamp("startDate", startDate);
		query.setTimestamp("endDate", now);
		
		for (@SuppressWarnings("unchecked")
		Iterator<Object[]> iterator = query.list().iterator(); iterator.hasNext();) {
			Object[] row = (Object[]) iterator.next();
			Map<String, Object> returnMap = new HashMap<String, Object>();
			returnMap.put(MAPKEY_LABEL, row[1]);
			returnMap.put(MAPKEY_VALUE, row[0]);
			returnList.add(returnMap);
		}
		return returnList;
	}

	@Override
	public List<Map<String, Object>> topRuleId(Integer hours, Integer count) {
		Date now = new Date();
		Date startDate = new Date(now.getTime() - hours * 60L * 60L * 1000L);
		List<Map<String, Object>> returnList = new ArrayList<Map<String, Object>>();
		
		Query query = getSession().createQuery(
				"select count(l), l.ruleId from IncidentLog l " +
				"where l.date between :startDate and :endDate " +
				"group by l.ruleId order by 1 desc");
		query.setTimestamp("startDate", startDate);
		query.setTimestamp("endDate", now);
		query.setMaxResults(count);
		
		for (@SuppressWarnings("unchecked")
		Iterator<Object[]> iterator = query.list().iterator(); iterator.hasNext();) {
			Object[] row = (Object[]) iterator.next();
			Map<String, Object> returnMap = new HashMap<String, Object>();
			returnMap.put(MAPKEY_LABEL, row[1]);
			returnMap.put(MAPKEY_VALUE, row[0]);
			returnList.add(returnMap);
		}	
		return returnList;
	}

	@Override
	public List<Map<String, Object>> topInformationTypeId(Integer hours, Integer count) {
		Date now = new Date();
		Date startDate = new Date(now.getTime() - hours * 60L * 60L * 1000L);
		List<Map<String, Object>> returnList = new ArrayList<Map<String, Object>>();
		
		Query query = getSession().createQuery(
				"select count(l), l.informationTypeId from IncidentLog l " +
				"where l.date between :startDate and :endDate " +
				"group by l.informationTypeId order by 1 desc");
		query.setTimestamp("startDate", startDate);
		query.setTimestamp("endDate", now);
		query.setMaxResults(count);
		
		for (@SuppressWarnings("unchecked")
		Iterator<Object[]> iterator = query.list().iterator(); iterator.hasNext();) {
			Object[] row = (Object[]) iterator.next();
			Map<String, Object> returnMap = new HashMap<String, Object>();
			returnMap.put(MAPKEY_LABEL, row[1]);
			returnMap.put(MAPKEY_VALUE, row[0]);
			returnList.add(returnMap);
		}	
		return returnList;
	}
		
}
