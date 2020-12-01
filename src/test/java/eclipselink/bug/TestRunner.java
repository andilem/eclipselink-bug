package eclipselink.bug;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.RollbackException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("boxing")
public class TestRunner {
    private EntityManagerFactory emf;
    
    @Before
    public void before() {
        emf = Persistence.createEntityManagerFactory("h2-eclipselink-bug");
    }
    
    @After
    public void after() {
        emf.close();
    }
    
    @Test
    public void invalidatedObjectReturned() {
        // to show that the bug is in fact present
        execute(null);
    }
    
    @Test
    public void invalidatedObjectReturned_workaroundEmFind() {
        // if we query the invalidated object directly, it will be refreshed in the cache and fetched correctly afterwards
        execute(f -> runWithoutTransaction(em -> em.find(Tree.class, f.trees.get(0).id)));
    }
    
    @Test
    public void invalidatedObjectReturned_workaroundCacheEvictTree() {
        // if we invalidate the tree, it still doesn't work (because it is already invalidated)
        execute(f -> emf.getCache().evict(Tree.class, f.trees.get(0).id));
    }
    
    @Test
    public void invalidatedObjectReturned_workaroundCacheEvictForest() {
        // if we invalidate the forest (NOT the tree!!!), the trees will be refreshed
        execute(f -> emf.getCache().evict(Forest.class, f.id));
    }
    
    private void execute(Consumer<Forest> workaround) {
        
        // persist one forest with one trees (height 1)
        Forest forest = new Forest();
        forest.trees.add(new Tree());
        runInTransaction(em -> em.persist(forest));
        
        emf.getCache().evictAll();
        
        // load the forest again from the DB, just to verify that everything works
        Forest dbForest = getForest(forest.id);
        assertEquals(1, dbForest.trees.size());
        assertTrue(dbForest.trees.stream().allMatch(t -> t.height == 1));
        
        // modify one tree from outside of EL
        runInTransaction(em -> em.createNativeQuery("UPDATE TREE SET HEIGHT=10, VERSION=CURRENT_TIMESTAMP WHERE ID=" + forest.trees.get(0).id).executeUpdate());
        
        // EL cannot know about the update, so the height should be the initial one
        dbForest = getForest(forest.id);
        assertEquals(1, dbForest.trees.size());
        assertTrue(dbForest.trees.stream().allMatch(t -> t.height == 1));
        
        // try to modify the tree, this should throw a RollbackException with an OptimisticLockException as cause
        Tree modifyTree = dbForest.trees.get(0);
        modifyTree.height = 2;
        Forest finalForest = dbForest;
        assertThrows(RollbackException.class, () -> runInTransaction(em -> {
            Forest myForest = em.find(Forest.class, finalForest.id);
            myForest.trees.removeIf(t -> t.id == modifyTree.id);
            myForest.trees.add(em.merge(modifyTree));
        }));
        // the cache entry for the tree is invalidated now, so a further query should return the updated tree
        
        // apply workaround if given
        if (workaround != null) {
            workaround.accept(dbForest);
        }
        
        // check if it works
        dbForest = getForest(forest.id);
        System.out.println(dbForest.trees);
        System.out.println(Arrays.toString((Object[]) runWithoutTransaction(em -> em.createNativeQuery("SELECT * FROM TREE").getSingleResult())));
        assertEquals(1, dbForest.trees.size());
        assertEquals(10, dbForest.trees.get(0).height);
    }
    
    private Forest getForest(long id) {
        return runWithoutTransaction(em -> em.find(Forest.class, id));
    }
    
    private void runInTransaction(Consumer<EntityManager> action) {
        EntityManager entityManager = emf.createEntityManager();
        try {
            entityManager.getTransaction().begin();
            action.accept(entityManager);
            entityManager.getTransaction().commit();
        } catch (Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw e;
        } finally {
            entityManager.close();
        }
    }
    
    private <T> T runWithoutTransaction(Function<EntityManager, T> action) {
        EntityManager entityManager = emf.createEntityManager();
        try {
            T result = action.apply(entityManager);
            return result;
        } finally {
            entityManager.close();
        }
    }
}
