package tests;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import tests.database.DatabaseManagerTest;
import tests.implementation.PUCommsAPIImplTest;
import tests.model.CampaignTest;
import tests.service.AuthServiceTest;
import tests.service.CampaignStoreTest;
import tests.service.CatalogueServiceTest;
import tests.service.CommercialApplicationServiceTest;
import tests.service.MembershipServiceTest;
import tests.service.OrderServiceTest;
import tests.service.PromotionServiceTest;
import tests.service.ReportServiceTest;
import tests.ui.AdminDashboardTest;
import tests.ui.AdminLoginFrameTest;
import tests.ui.CommercialApplicationFrameTest;
import tests.ui.CustomerDashboardTest;
import tests.ui.CustomerLoginFrameTest;
import tests.ui.DatabaseViewerTest;
import tests.ui.IPOS_PU_GUITest;
import tests.ui.NonCommercialRegistrationFrameTest;
import tests.ui.PasswordChangeTest;
import tests.ui.WelcomeFrameTest;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class AllTestsOrchestratorTest {

    private static final Class<?>[] TEST_CLASSES = new Class<?>[]{
            AuthServiceTest.class,
            CampaignStoreTest.class,
            CatalogueServiceTest.class,
            CommercialApplicationServiceTest.class,
            MembershipServiceTest.class,
            OrderServiceTest.class,
            PromotionServiceTest.class,
            ReportServiceTest.class,
            PUCommsAPIImplTest.class,
            CampaignTest.class,
            DatabaseManagerTest.class,
            AdminDashboardTest.class,
            AdminLoginFrameTest.class,
            CommercialApplicationFrameTest.class,
            CustomerDashboardTest.class,
            CustomerLoginFrameTest.class,
            DatabaseViewerTest.class,
            IPOS_PU_GUITest.class,
            NonCommercialRegistrationFrameTest.class,
            PasswordChangeTest.class,
            WelcomeFrameTest.class
    };

    // Expected: exposes all underlying tests individually in the runner.
    @TestFactory
    Collection<DynamicTest> runAllTestsAsDynamicTests() {
        return Arrays.stream(TEST_CLASSES)
                .flatMap(testClass -> {
                    List<Method> testMethods = getAnnotatedMethods(testClass, Test.class);
                    return testMethods.stream().map(testMethod ->
                            DynamicTest.dynamicTest(
                                    testClass.getSimpleName() + " :: " + testMethod.getName(),
                                    () -> runSingleTestMethod(testClass, testMethod)
                            )
                    );
                })
                .toList();
    }

    private List<Method> getAnnotatedMethods(Class<?> type, Class<? extends Annotation> annotationClass) {
        return Arrays.stream(type.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(annotationClass))
                .sorted(Comparator.comparing(Method::getName))
                .toList();
    }

    private void runSingleTestMethod(Class<?> testClass, Method testMethod) throws Exception {
        Object testInstance = testClass.getDeclaredConstructor().newInstance();
        List<Method> beforeEachMethods = getAnnotatedMethods(testClass, BeforeEach.class);
        List<Method> afterEachMethods = getAnnotatedMethods(testClass, AfterEach.class);

        try {
            invokeLifecycleMethods(beforeEachMethods, testInstance);
            invokeMethod(testMethod, testInstance);
        } finally {
            invokeLifecycleMethods(afterEachMethods, testInstance);
        }
    }

    private void invokeLifecycleMethods(List<Method> methods, Object target) throws Exception {
        for (Method method : methods) {
            invokeMethod(method, target);
        }
    }

    private void invokeMethod(Method method, Object target) throws Exception {
        method.setAccessible(true);
        try {
            method.invoke(target);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception ex) {
                throw ex;
            }
            if (cause instanceof Error err) {
                throw err;
            }
            throw new RuntimeException(cause);
        }
    }
}
