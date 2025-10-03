package org.refactoringminer.test.python.refactorings.clazz;

import extension.umladapter.UMLModelAdapter;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import gr.uom.java.xmi.diff.MoveAndRenameClassRefactoring;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Isolated
public class MoveAndRenameClassRefactoringDetectionTest {

    // ... existing code ...
    @Test
    void detectsMoveAndRenameClass_CacheToStorageDataCache() throws Exception {
        String before = """
            class Cache:
                def put(self, k, v): pass
            """;
        String after = """
            class DataCache:
                def put(self, k, v): pass
            """;

        Map<String, String> beforeFiles = Map.of("src/cache/cache.py", before);
        Map<String, String> afterFiles = Map.of("src/storage/data_cache.py", after);

        assertMoveAndRenameClassRefactoringDetected(
                beforeFiles, afterFiles,
                "cache.Cache", "storage.DataCache",
                "src/cache/cache.py", "src/storage/data_cache.py"
        );
    }

    @Test
    void detectsMoveAndRenameClass_ReportToAnalyticsUsageReport() throws Exception {
        String before = """
            class Report:
                def generate(self): return ""
            """;
        String after = """
            class UsageReport:
                def generate(self): return ""
            """;

        Map<String, String> beforeFiles = Map.of("src/reports/report.py", before);
        Map<String, String> afterFiles = Map.of("src/analytics/usage_report.py", after);

        assertMoveAndRenameClassRefactoringDetected(
                beforeFiles, afterFiles,
                "reports.Report", "analytics.UsageReport",
                "src/reports/report.py", "src/analytics/usage_report.py"
        );
    }

    @Test
    void detectsMoveAndRenameClass_OrderToSalesPurchaseOrder() throws Exception {
        String before = """
            class Order:
                def __init__(self, items):
                    self.items = items

                def total(self):
                    return sum(self.items)
            """;
        String after = """
            class PurchaseOrder:
                def __init__(self, items):
                    self.items = items

                def total(self):
                    return sum(self.items)
            """;

        Map<String, String> beforeFiles = Map.of("src/order/order.py", before);
        Map<String, String> afterFiles = Map.of("src/sales/purchase_order.py", after);

        assertMoveAndRenameClassRefactoringDetected(
                beforeFiles, afterFiles,
                "order.Order", "sales.PurchaseOrder",
                "src/order/order.py", "src/sales/purchase_order.py"
        );
    }

    // 2) Με imports και staticmethod
    @Test
    void detectsMoveAndRenameClass_LoggerToMonitoringAppLogger() throws Exception {
        String before = """
            import time

            class Logger:
                @staticmethod
                def ts():
                    return int(time.time())
            """;
        String after = """
            import time

            class AppLogger:
                @staticmethod
                def ts():
                    return int(time.time())
            """;

        Map<String, String> beforeFiles = Map.of("src/log/logger.py", before);
        Map<String, String> afterFiles = Map.of("src/monitoring/app_logger.py", after);

        assertMoveAndRenameClassRefactoringDetected(
                beforeFiles, afterFiles,
                "log.Logger", "monitoring.AppLogger",
                "src/log/logger.py", "src/monitoring/app_logger.py"
        );
    }

    // 3) Με class attributes, property & setter
    @Test
    void detectsMoveAndRenameClass_ConfigToCoreConfiguration() throws Exception {
        String before = """
            class Config:
                DEFAULT_TIMEOUT = 5

                def __init__(self):
                    self._timeout = self.DEFAULT_TIMEOUT

                @property
                def timeout(self):
                    return self._timeout

                @timeout.setter
                def timeout(self, value):
                    if value < 0:
                        value = 0
                    self._timeout = value
            """;
        String after = """
            class Configuration:
                DEFAULT_TIMEOUT = 5

                def __init__(self):
                    self._timeout = self.DEFAULT_TIMEOUT

                @property
                def timeout(self):
                    return self._timeout

                @timeout.setter
                def timeout(self, value):
                    if value < 0:
                        value = 0
                    self._timeout = value
            """;

        Map<String, String> beforeFiles = Map.of("src/config/config.py", before);
        Map<String, String> afterFiles = Map.of("src/core/configuration.py", after);

        assertMoveAndRenameClassRefactoringDetected(
                beforeFiles, afterFiles,
                "config.Config", "core.Configuration",
                "src/config/config.py", "src/core/configuration.py"
        );
    }

    @Test
    void detectsMoveAndRenameClass_ParserToIoJsonParser() throws Exception {
        String before = """
            class Parser:
                def parse(self, path):
                    f = None
                    try:
                        f = open(path, 'r')
                        return f.read()
                    except FileNotFoundError:
                        return ""
                    finally:
                        if f:
                            f.close()
            """;
        String after = """
            class JsonParser:
                def parse(self, path):
                    f = None
                    try:
                        f = open(path, 'r')
                        return f.read()
                    except FileNotFoundError:
                        return ""
                    finally:
                        if f:
                            f.close()
            """;

        Map<String, String> beforeFiles = Map.of("src/parsers/parser.py", before);
        Map<String, String> afterFiles = Map.of("src/io/json_parser.py", after);

        assertMoveAndRenameClassRefactoringDetected(
                beforeFiles, afterFiles,
                "parsers.Parser", "io.JsonParser",
                "src/parsers/parser.py", "src/io/json_parser.py"
        );
    }

    @Test
    void detectsMoveAndRenameClass_MathUtilsToAlgorithmsAdvancedMathUtils() throws Exception {
        String before = """
            class MathUtils:
                @staticmethod
                def add(a, b): return a + b

                @classmethod
                def zero(cls): return 0
            """;
        String after = """
            class AdvancedMathUtils:
                @staticmethod
                def add(a, b): return a + b

                @classmethod
                def zero(cls): return 0
            """;

        Map<String, String> beforeFiles = Map.of("src/math/math_utils.py", before);
        Map<String, String> afterFiles = Map.of("src/algorithms/advanced_math_utils.py", after);

        assertMoveAndRenameClassRefactoringDetected(
                beforeFiles, afterFiles,
                "math.MathUtils", "algorithms.AdvancedMathUtils",
                "src/math/math_utils.py", "src/algorithms/advanced_math_utils.py"
        );
    }

    @Test
    void detectsMoveAndRenameClass_DataSetToAnalyticsDataSetV2() throws Exception {
        String before = """
            class DataSet:
                def normalize(self, nums):
                    cleaned = [n for n in nums if n is not None]
                    return {i: v for i, v in enumerate(cleaned)}
            """;
        String after = """
            class DataSetV2:
                def normalize(self, nums):
                    cleaned = [n for n in nums if n is not None]
                    return {i: v for i, v in enumerate(cleaned)}
            """;

        Map<String, String> beforeFiles = Map.of("src/data/data_set.py", before);
        Map<String, String> afterFiles = Map.of("src/analytics/data_set_v2.py", after);

        assertMoveAndRenameClassRefactoringDetected(
                beforeFiles, afterFiles,
                "data.DataSet", "analytics.DataSetV2",
                "src/data/data_set.py", "src/analytics/data_set_v2.py"
        );
    }

    @Test
    void detectsMoveAndRenameClass_ControllerToApiUserController() throws Exception {
        String before = """
            class Controller:
                def handle(self, users):
                    def is_active(u): return u.get('active', False)
                    result = []
                    for u in users:
                        if is_active(u):
                            result.append(u['name'])
                        else:
                            continue
                    return result
            """;
        String after = """
            class UserController:
                def handle(self, users):
                    def is_active(u): return u.get('active', False)
                    result = []
                    for u in users:
                        if is_active(u):
                            result.append(u['name'])
                        else:
                            continue
                    return result
            """;

        Map<String, String> beforeFiles = Map.of("src/web/controller.py", before);
        Map<String, String> afterFiles = Map.of("src/api/user_controller.py", after);

        assertMoveAndRenameClassRefactoringDetected(
                beforeFiles, afterFiles,
                "web.Controller", "api.UserController",
                "src/web/controller.py", "src/api/user_controller.py"
        );
    }

    @Test
    void detectsMoveAndRenameClass_PaymentToBillingPaymentService() throws Exception {
        String before = """
            class BaseService:
                def ping(self): return "ok"

            class Payment:
                def charge(self, amount): return amount > 0
            """;
        String after = """
            class BaseService:
                def ping(self): return "ok"

            class PaymentService:
                def charge(self, amount): return amount > 0
            """;

        Map<String, String> beforeFiles = Map.of("src/payments/payment.py", before);
        Map<String, String> afterFiles = Map.of("src/billing/payment_service.py", after);

        assertMoveAndRenameClassRefactoringDetected(
                beforeFiles, afterFiles,
                "payments.Payment", "billing.PaymentService",
                "src/payments/payment.py", "src/billing/payment_service.py"
        );
    }

    public static void assertMoveAndRenameClassRefactoringDetected(
            Map<String, String> beforeFiles,
            Map<String, String> afterFiles,
            String originalClassName,
            String renamedClassName,
            String originalFilePath,
            String newFilePath
    ) throws Exception {
        UMLModel beforeUML = new UMLModelAdapter(beforeFiles).getUMLModel();
        UMLModel afterUML = new UMLModelAdapter(afterFiles).getUMLModel();

        UMLModelDiff diff = beforeUML.diff(afterUML);
        List<Refactoring> refactorings = diff.getRefactorings();

        System.out.println("Refactorings size: " + refactorings.size());
        refactorings.forEach(System.out::println);

        boolean found = refactorings.stream().anyMatch(r -> {
            if (r.getRefactoringType() != RefactoringType.MOVE_RENAME_CLASS) {
                return false;
            }
            MoveAndRenameClassRefactoring mr = (MoveAndRenameClassRefactoring) r;

            UMLClass orig = mr.getOriginalClass();
            UMLClass ren = mr.getRenamedClass();

            boolean namesMatch =
                    originalClassName.equals(orig.getName()) &&
                            renamedClassName.equals(ren.getName());

            boolean filesMatch =
                    orig.getLocationInfo() != null &&
                            ren.getLocationInfo() != null &&
                            originalFilePath.equals(orig.getLocationInfo().getFilePath()) &&
                            newFilePath.equals(ren.getLocationInfo().getFilePath());

            return namesMatch && filesMatch;
        });

        assertTrue(
                found,
                "Expected MOVE_RENAME_CLASS from " + originalClassName + " (" + originalFilePath + ") to " +
                        renamedClassName + " (" + newFilePath + "). Actual: " + refactorings
        );
    }
}