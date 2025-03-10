package br.ufmg.dcc.labsoft.refactoringanalyzer.dao;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.Query;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.LoggerFactory;

public class CrudProjectDaoGit<T extends AbstractEntity> {

	private static org.slf4j.Logger logger = LoggerFactory.getLogger(CrudProjectDaoGit.class);

	private static EntityManager em;

	public CrudProjectDaoGit() {
		em = getEntityManager();

	}

	private EntityManager getEntityManager() {

		EntityManagerFactory factory = Persistence.createEntityManagerFactory("lambda-study");
		if (em == null) {
		em = factory.createEntityManager();
		}
		return em;
	}

	public T persistObject(T obj) {
		try {

			em.getTransaction().begin();
			em.persist(obj);
			em.getTransaction().commit();
		} catch (Exception ex) {
			logger.error("Error persisting object", ex);
			em.getTransaction().rollback();
		}finally{
			//em.close();
		}
		return obj;
	}

	public void persistRevision(RevisionGit r) {
		try {
			em.getTransaction().begin();
			em.persist(r);
			em.getTransaction().commit();
		} catch (Exception ex) {
			logger.error("Error persisting revision", ex);
			em.getTransaction().rollback();
		}finally{
			//em.close();
		}
	}

	public T mergeObject(T obj) {
		try {

			em.getTransaction().begin();
			em.merge(obj);
			em.flush();
			em.getTransaction().commit();
		}catch (Exception e) {
			    Throwable t = e.getCause();
			    while ((t != null) && !(t instanceof ConstraintViolationException)) {
			        t = t.getCause();
			    }
			    if (t instanceof ConstraintViolationException) {
					logger.error("Operation has already been executed", e);
			    }else{
					logger.error("Error merging object", e);
			    }
			    	
			}finally{
			//em.close();
		}
		return obj;
	}

	public List<T> listProjects(String aClass) {
		return em.createQuery("from " + aClass + " c ").getResultList();
	}

	public void saveList(List<T> objs) {
		em.getTransaction().begin();
		em.merge(objs);
		em.flush();
		em.getTransaction().commit();
	}

	public List<T> findByAll(String query) {
		try {
			return em.createNamedQuery(query).getResultList();
		} catch (Exception ex) {
			Logger.getLogger(CrudProjectDaoGit.class.getName()).log(
					Level.SEVERE, null, ex);
			return null;
		}
	}

	public List<T> getProjects() {
		String sqlQuery = "SELECT * FROM projectgit where analyzed = 0 order by size;";
		Query q = em.createNativeQuery(sqlQuery, ProjectGit.class);
		return q.getResultList();
	}

	@SuppressWarnings("unchecked")
	public List<T> getPreProjects() {
		String sqlQuery = "SELECT * FROM ProjectGit where id in (27, 11, 50);";
		Query q = em.createNativeQuery(sqlQuery, ProjectGit.class);
		return q.getResultList();
	}

	public List<T> getMaxForksProjects() {
		String sqlQuery = "SELECT * FROM ProjectGit where analyzed = 1 order by forks_count desc;";
		Query q = em.createNativeQuery(sqlQuery, ProjectGit.class);
		return q.getResultList();
	}

	public T getSelectedProject(long id) {
		String sqlQuery = "SELECT * FROM ProjectGit where id=" + id + ";";
		Query q = em.createNativeQuery(sqlQuery, ProjectGit.class);
		return (T) q.getSingleResult();
	}

	public List<T> getRevisionsProjects() {
		String sqlQuery = "SELECT * FROM projectGit where analyzed = 1 order by forks;";
		Query q = em.createNativeQuery(sqlQuery, ProjectGit.class);

		return q.getResultList();
	}

	public List<RefactoringGit> findRefactoringDuplicado(String hash) {
		
		return (List<RefactoringGit>) em
				.createNamedQuery("refactoringGit.findRefactoringDuplicates")
				.setParameter("hash", hash).getResultList();
	}
}
