package org.refactoringminer.test.python.refactorings.parameter;

import extension.umladapter.UMLModelAdapter;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Isolated
public class MergeParameterRefactoringDetectionTest {

    @Test
    void detectsMergeParameter_FirstLastNameToFullName() throws Exception {
        String beforePythonCode = """
            class UserService:
                def create_user(self, first_name, last_name, email):
                    full_name = f"{first_name} {last_name}"
                    return User(full_name, email)
                
                def update_user(self, user_id, first_name, last_name):
                    full_name = f"{first_name} {last_name}"
                    return self.repository.update(user_id, full_name)
            """;

        String afterPythonCode = """
            class UserService:
                def create_user(self, full_name, email):
                    return User(full_name, email)
                
                def update_user(self, user_id, full_name):
                    return self.repository.update(user_id, full_name)
            """;

        Map<String, String> beforeFiles = Map.of("user_service.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("user_service.py", afterPythonCode);

        assertMergeParameterRefactoringDetected(beforeFiles, afterFiles,
                Set.of("first_name", "last_name"), "full_name", "create_user", "UserService");
    }

    @Test
    void detectsMergeParameter_CoordinatesToPoint() throws Exception {
        String beforePythonCode = """
            class GeometryService:
                def calculate_distance(self, x1, y1, x2, y2):
                    return ((x2 - x1) ** 2 + (y2 - y1) ** 2) ** 0.5
                
                def create_rectangle(self, x, y, width, height):
                    return Rectangle(x, y, width, height)
            """;

        String afterPythonCode = """
            class GeometryService:
                def calculate_distance(self, point1, point2):
                    return ((point2.x - point1.x) ** 2 + (point2.y - point1.y) ** 2) ** 0.5
                
                def create_rectangle(self, position, dimensions):
                    return Rectangle(position.x, position.y, dimensions.width, dimensions.height)
            """;

        Map<String, String> beforeFiles = Map.of("geometry.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("geometry.py", afterPythonCode);

        assertMergeParameterRefactoringDetected(beforeFiles, afterFiles,
                Set.of("x1", "y1"), "point1", "calculate_distance", "GeometryService");
    }

    @Test
    void detectsMergeParameter_ConfigParametersToObject() throws Exception {
        String beforePythonCode = """
            def setup_database(host, port, username, password, database_name):
                config = {
                    'host': host,
                    'port': port,
                    'username': username,
                    'password': password,
                    'database': database_name
                }
                return DatabaseConnection(config)
            """;

        String afterPythonCode = """
            def setup_database(db_config):
                return DatabaseConnection(db_config)
            """;

        Map<String, String> beforeFiles = Map.of("database.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("database.py", afterPythonCode);

        assertMergeParameterRefactoringDetected(beforeFiles, afterFiles,
                Set.of("host", "port", "username", "password", "database_name"), "db_config", "setup_database", "");
    }

    @Test
    void detectsMergeParameter_DateComponentsToDateObject() throws Exception {
        String beforePythonCode = """
            class EventService:
                def create_event(self, title, year, month, day, hour, minute):
                    from datetime import datetime
                    event_date = datetime(year, month, day, hour, minute)
                    return Event(title, event_date)
                
                def schedule_meeting(self, participants, year, month, day, hour, minute):
                    from datetime import datetime
                    meeting_time = datetime(year, month, day, hour, minute)
                    return Meeting(participants, meeting_time)
            """;

        String afterPythonCode = """
            class EventService:
                def create_event(self, title, event_datetime):
                    return Event(title, event_datetime)
                
                def schedule_meeting(self, participants, meeting_datetime):
                    return Meeting(participants, meeting_datetime)
            """;

        Map<String, String> beforeFiles = Map.of("events.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("events.py", afterPythonCode);

        assertMergeParameterRefactoringDetected(beforeFiles, afterFiles,
                Set.of("year", "month", "day", "hour", "minute"), "event_datetime", "create_event", "EventService");
    }

    @Test
    void detectsMergeParameter_AddressComponentsToAddressObject() throws Exception {
        String beforePythonCode = """
            class CustomerService:
                def register_customer(self, name, email, street, city, state, zip_code):
                    address = f"{street}, {city}, {state} {zip_code}"
                    return Customer(name, email, address)
            """;

        String afterPythonCode = """
            class CustomerService:
                def register_customer(self, name, email, address):
                    return Customer(name, email, address)
            """;

        Map<String, String> beforeFiles = Map.of("customer.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("customer.py", afterPythonCode);

        assertMergeParameterRefactoringDetected(beforeFiles, afterFiles,
                Set.of("street", "city", "state", "zip_code"), "address", "register_customer", "CustomerService");
    }

    public static void assertMergeParameterRefactoringDetected(
            Map<String, String> beforeFiles,
            Map<String, String> afterFiles,
            Set<String> mergedParameterNames,
            String newParameterName,
            String methodName,
            String className
    ) throws Exception {
        UMLModelAdapter beforeAdapter = new UMLModelAdapter(beforeFiles);
        UMLModelAdapter afterAdapter = new UMLModelAdapter(afterFiles);

        UMLModel beforeUML = beforeAdapter.getUMLModel();
        UMLModel afterUML = afterAdapter.getUMLModel();

        UMLModelDiff diff = beforeUML.diff(afterUML);
        List<Refactoring> refactorings = diff.getRefactorings();

        System.out.println("\n=== MERGE PARAMETER TEST: " + mergedParameterNames + " -> " + newParameterName + " ===");
        System.out.println("Merged parameters: " + mergedParameterNames);
        System.out.println("New parameter: " + newParameterName);
        System.out.println("Method: " + methodName + (className.isEmpty() ? " (module level)" : " in class " + className));
        System.out.println("Total refactorings detected: " + refactorings.size());
        refactorings.forEach(r -> System.out.println("  " + r.getRefactoringType() + ": " + r.toString()));

        boolean mergeParameterFound = refactorings.stream()
                .anyMatch(r -> RefactoringType.MERGE_PARAMETER.equals(r.getRefactoringType()) &&
                        r.toString().contains(newParameterName) &&
                        r.toString().contains(methodName));

        if (!mergeParameterFound) {
            System.out.println("Available refactorings:");
            refactorings.forEach(r -> System.out.println("  " + r.getRefactoringType() + ": " + r.toString()));

            fail("Expected merge parameter refactoring from parameters " + mergedParameterNames +
                    " to '" + newParameterName + "' in method '" + methodName + "' was not detected");
        }

        assertTrue(mergeParameterFound, "Expected Merge Parameter refactoring to be detected");
    }
}