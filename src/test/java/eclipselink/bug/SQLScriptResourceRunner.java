/*
 * ============================================================================
 *
 *      This program contains proprietary information which is trade secret
 *      of mm-lab GmbH, Kornwestheim, and also is protected as an unpublished 
 *      work under applicable copyright laws. The program is to be retained in
 *      confidence. Any use by third parties (e.g. use as a control program,
 *      reproduction, modification and translation) is governed solely by
 *      written agreements with mm-lab GmbH.
 *
 *      mm-lab GmbH makes no representations or warranties about the suit-
 *      ability of the software, either express or implied, including but
 *      not limited to the implied warranties of merchantability, fitness 
 *      for a particular purpose, or non-infringement. 
 *      mm-lab GmbH shall not be liable for any damages suffered by licensee 
 *      as a result of using, modifying or distributing this software or its 
 *      derivatives.
 *
 * ============================================================================
 */
package eclipselink.bug;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

/**
 * Utility to execute SQL scripts from resource files.
 * 
 * @mmlab.component FRW
 * @mmlab.module frw.jpa.emservice.atp
 * @mmlab.gendate 05.12.2016
 * @author Beat Steiger
 */
public final class SQLScriptResourceRunner {
    private SQLScriptResourceRunner() {
    }
    
    public static void executeSqlScriptFile(EntityManagerFactory emf, String scriptFile) {
        EntityManager em = emf.createEntityManager();
        ClassLoader cl = SQLScriptResourceRunner.class.getClassLoader();
        try (InputStream stream = cl.getResourceAsStream(scriptFile); Scanner scanner = new Scanner(stream)) {
            em.getTransaction().begin();
            int i = 0;
            String sql = "";
            while (scanner.hasNextLine()) {
                i++;
                sql += scanner.nextLine().trim();
                if (sql.endsWith("\\")) {
                    sql = sql.substring(0, sql.length() - 1);
                    continue;
                }
                if (!sql.isEmpty() && !sql.startsWith("--")) {
                    Query q = em.createNativeQuery(sql);
                    try {
                        q.executeUpdate();
                    } catch (Exception e) {
                        throw new RuntimeException("Error during executing sql script at line " + i, e);
                    }
                }
                sql = "";
            }
            em.getTransaction().commit();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            em.close();
        }
    }
    
    public static void executeSql(EntityManagerFactory emf, String sql) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.createNativeQuery(sql).executeUpdate();
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }
}
