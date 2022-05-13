package integration.persistence.service;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import hibernate.cache.model.Bar;
import hibernate.cache.model.Foo;
import hibernate.cache.service.BarService;
import hibernate.cache.service.FooService;
import net.sf.ehcache.CacheManager;
import spring.PersistenceJPAConfigL2Cache;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { PersistenceJPAConfigL2Cache.class }, loader = AnnotationConfigContextLoader.class)
@DirtiesContext
public class SecondLevelCacheIntegrationTest {

    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private FooService fooService;
    @Autowired
    private BarService barService;
    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Before
    public final void before() {
        entityManager.getEntityManagerFactory().getCache().evictAll();
    }

    @Test
    public final void given_entity_is_loaded_then_it_is_cached() {
        final Foo foo = new Foo(randomAlphabetic(6));
        fooService.create(foo);
        fooService.findOne(foo.getId());
        final int size = CacheManager.ALL_CACHE_MANAGERS.get(0).getCache("hibernate.cache.model.Foo").getSize();
        assertThat(size, greaterThan(0));
    }

    @Test
    public final void given_bar_is_updated_in_native_query_then_foos_are_not_evicted() {
        final Foo foo = new Foo(randomAlphabetic(6));
        fooService.create(foo);
        Foo fooFromCache = fooService.findOne(foo.getId());

        new TransactionTemplate(platformTransactionManager).execute(status -> {
            final Bar bar = new Bar(randomAlphabetic(6));
            entityManager.persist(bar);
            final Query nativeQuery = entityManager.createNativeQuery("update BAR set NAME = :updatedName where ID = :id");
            nativeQuery.setParameter("updatedName", "newName");
            nativeQuery.setParameter("id", bar.getId());
            nativeQuery.unwrap(org.hibernate.SQLQuery.class).addSynchronizedEntityClass(Bar.class);
            return nativeQuery.executeUpdate();
        });

        final int size = CacheManager.ALL_CACHE_MANAGERS.get(0).getCache("hibernate.cache.model.Foo").getSize();
        assertThat(size, greaterThan(0));
    }

    @Test
    public final void given_cacheable_query_is_executed_then_it_is_cached() {
        new TransactionTemplate(platformTransactionManager).execute(status -> {
            return entityManager.createQuery("select f from Foo f").setHint("org.hibernate.cacheable", true).getResultList();
        });

        final int size = CacheManager.ALL_CACHE_MANAGERS.get(0).getCache("org.hibernate.cache.internal.StandardQueryCache").getSize();
        assertThat(size, greaterThan(0));
    }
    
    @Test
    public final void given_foo_connected_with_bar_when_updating_foo_should_store_in_cache() {
    	final Foo foo = new Foo(randomAlphabetic(6));
    	System.out.println("Creating Foo");
        fooService.create(foo);
        System.out.println("Getting it should be in cache");
        Foo fooFromCache = fooService.findOne(foo.getId());
        System.out.println("Adding Bar and getting it");
        final Bar bar = new Bar(randomAlphabetic(6));        
        Bar barFromDb = new TransactionTemplate(platformTransactionManager).execute(status -> {
        	entityManager.persist(bar);
        	System.out.println("Bar added");
        	System.out.println("Getting Bar");
            final Query query = entityManager.createQuery("FROM Bar WHERE id = :id", Bar.class);
            query.setParameter("id", bar.getId());
            Bar barDb = (Bar) query.getSingleResult();
            return barDb;
        });
        System.out.println("Got Bar");
        String fooName = "LazyLoadingCheckCache";
        foo.setName(fooName);
        foo.setBar(barFromDb);
        System.out.println("Updating Foo");
        fooService.update(foo);
        System.out.println("Getting Bar connected with Foo (Foo is cached)");
        barFromDb = new TransactionTemplate(platformTransactionManager).execute(status -> {
            final Query query = entityManager.createQuery("FROM Bar WHERE id = :id", Bar.class);
            query.setParameter("id", bar.getId());
            Bar barDb = (Bar) query.getSingleResult();
            return barDb;
        });
        
        new TransactionTemplate(platformTransactionManager).execute(status -> {
            final Query nativeQuery = entityManager.createNativeQuery("update BAR set NAME = :updatedName where ID = :id");
            nativeQuery.setParameter("updatedName", "abc123");
            nativeQuery.setParameter("id", bar.getId());
            nativeQuery.unwrap(org.hibernate.SQLQuery.class).addSynchronizedEntityClass(Bar.class);
            return nativeQuery.executeUpdate();
        });
        
        final long barId = barFromDb.getId();
        System.out.println("Bar getting from service");
        Bar barFromService = barService.findOne(barId);
        System.out.println("Bar got from service");
        
        System.out.println("Check queries");
        fooFromCache = fooService.findOne(foo.getId());
        fooFromCache = fooService.findOne(foo.getId());
        fooFromCache = fooService.findOne(foo.getId());
        fooFromCache = fooService.findOne(foo.getId());
        System.out.println("Getting Bar");
        barFromService = barService.findOne(barId);
        
        final int size = CacheManager.ALL_CACHE_MANAGERS.get(0).getCache("hibernate.cache.model.Foo").getSize();
        assertThat(size, greaterThan(0));
        assertThat(barFromService.getFooList().size()).isGreaterThan(0);
        assertThat(fooFromCache.getName()).isEqualTo(fooName);
    }
    
    @Test
    public final void given_entity_in_db_when_is_loaded_then_should_be_cached() {
        final Foo foo = new Foo(randomAlphabetic(6));
        fooService.create(foo);
        new TransactionTemplate(platformTransactionManager).execute(status -> {
        	Session session = entityManager.unwrap(Session.class);
        	SessionFactory sessionFactory = session.getSessionFactory();
            sessionFactory.getCache().evict(Foo.class);
            return sessionFactory;
        });
        fooService.findOne(foo.getId());
        fooService.findOne(foo.getId());
        fooService.findAll();
        fooService.findAll();
        new TransactionTemplate(platformTransactionManager).execute(status -> {
        	final Query query = entityManager.createQuery("FROM Foo", Foo.class).setHint("org.hibernate.cacheable", true);
            List<Foo> fooDb = (List<Foo>) query.getResultList();
            return fooDb;
        });
        new TransactionTemplate(platformTransactionManager).execute(status -> {
        	System.out.println("Should from cache and dont hit db below");
        	final Query query = entityManager.createQuery("FROM Foo", Foo.class).setHint("org.hibernate.cacheable", true);
            List<Foo> fooDb = (List<Foo>) query.getResultList();
            return fooDb;
        });
        final int size = CacheManager.ALL_CACHE_MANAGERS.get(0).getCache("hibernate.cache.model.Foo").getSize();
    }
}
