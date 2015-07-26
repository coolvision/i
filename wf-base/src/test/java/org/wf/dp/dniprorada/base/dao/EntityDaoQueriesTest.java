package org.wf.dp.dniprorada.base.dao;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.Assert.fail;

/**
 * This test takes all {@link EntityDao} DAOs and executes all select(get, search, find) methods with RANDOM parameters.
 * The purpose of this test is to make sure all queries were written correctly.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/org/wf/dp/dniprorada/base/dao/testContext.xml")
public class EntityDaoQueriesTest {
    private static final Log LOG = LogFactory.getLog(EntityDaoQueriesTest.class);

    private static final String[] QUERY_METHOD_PREFIXES = {"get", "search", "find"};
    private static final String LOG_SEPARATOR_LINE = StringUtils.repeat("=", 100);
    private static final int REPEAT_NUMBER = 10;

    @Autowired
    private ApplicationContext applicationContext;

    private Set<String> failedMethods = Sets.newLinkedHashSet();

    @Test
    @Transactional(readOnly = true)
    public void shouldFindAllDaoAndExecuteEachQueryMethod() throws Exception {
        LOG.info(LOG_SEPARATOR_LINE);
        LOG.info("Obtaining DAO for test");

        Map<String, EntityDao> entityDaos = applicationContext.getBeansOfType(EntityDao.class);

        LOG.info(LOG_SEPARATOR_LINE);
        LOG.info(String.format("Found %s DAOs : %s", entityDaos.size(), entityDaos));

        for (Map.Entry<String, EntityDao> entityDaoEntry : entityDaos.entrySet()) {
            EntityDao testedDao = entityDaoEntry.getValue();

            LOG.info(LOG_SEPARATOR_LINE);
            LOG.info(String.format("Run test for %s", getType(testedDao).getName()));

            testDaoMethods(testedDao);
        }

        if (CollectionUtils.isNotEmpty(failedMethods)) {
            fail(String.format("Test failed (see stacktrace above for details):\n%s", Joiner.on('\n').join(failedMethods)));
        } else {
            LOG.info(LOG_SEPARATOR_LINE);
            LOG.info(String.format("Testing of %s DAOs completed successfully", entityDaos.size()));
            LOG.info(LOG_SEPARATOR_LINE);
        }
    }

    private void testDaoMethods(final EntityDao testedDao) {
        final Class<? extends EntityDao> testedDaoClass = getType(testedDao);

        ReflectionUtils.doWithMethods(testedDaoClass, new ReflectionUtils.MethodCallback() {
            public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
                LOG.info(LOG_SEPARATOR_LINE);
                LOG.info(String.format("Run test for %s method %s times", method, REPEAT_NUMBER));

                for (int i = 0; i < REPEAT_NUMBER; i++) {
                    LOG.info(String.format("%s execution of %s method", i + 1, method));
                    executeMethodWithRandomParams(testedDao, method);
                }
            }
        }, new ReflectionUtils.MethodFilter() {
            public boolean matches(Method method) {
                return method.getDeclaringClass() == testedDaoClass
                        && StringUtils.startsWithAny(method.getName(), QUERY_METHOD_PREFIXES);
            }
        });
    }

    private Class<? extends EntityDao> getType(EntityDao testedDao) {
        return (Class<? extends EntityDao>) AopUtils.getTargetClass(testedDao);
    }

    private void executeMethodWithRandomParams(EntityDao testedDao, Method testedMethod) {
        try {
            Object[] randomParams = getRandomParams(testedMethod);

            LOG.info(String.format("Generated params for method %s: %s", testedMethod, Arrays.toString(randomParams)));

            ReflectionUtils.invokeMethod(testedMethod, getRealObject(testedDao), randomParams);
        } catch (HibernateException e) {
            handleTestedMethodException(testedMethod, e);
        }
    }

    private Object getRealObject(EntityDao testedDao) {
        if (AopUtils.isAopProxy(testedDao)) {
            Advised advisedTestedDao = (Advised) testedDao;
            try {
                return advisedTestedDao.getTargetSource().getTarget();
            } catch (Exception e) {
                throw new IllegalStateException("Should not get here");
            }
        }

        return testedDao;
    }

    private void handleTestedMethodException(Method testedMethod, HibernateException e) {
        LOG.error("Method invocation failed!", e);
        failedMethods.add(String.format("Failed method: %s", testedMethod));
    }

    private Object[] getRandomParams(Method testedMethod) {
        Class<?>[] parameterTypes = testedMethod.getParameterTypes();

        LinkedList<Object> randomParams = Lists.newLinkedList();

        for (Class<?> parameterType : parameterTypes) {
            randomParams.add(RandomUtils.getRandomValueOf(parameterType));
        }
        return Iterables.toArray(randomParams, Object.class);
    }

}
