package org.refactoringminer.test.python.refactorings.clazz;

import extension.umladapter.UMLModelAdapter;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.ExtractSuperclassRefactoring;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.refactoringminer.api.Refactoring;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Isolated
public class ExtractSuperClassRefactoringDetectionTest {

    @Test
    void detectsExtractSuperclass_AnimalFromDogAndCat() throws Exception {
        // BEFORE: Two separate classes with similar methods
        String beforeDogCode = """
            class Dog:
                def __init__(self, name):
                    self.name = name
                
                def eat(self):
                    print(f"{self.name} is eating")
                
                def sleep(self):
                    print(f"{self.name} is sleeping")
                
                def bark(self):
                    print(f"{self.name} is barking")
            """;

        String beforeCatCode = """
            class Cat:
                def __init__(self, name):
                    self.name = name
                
                def eat(self):
                    print(f"{self.name} is eating")
                
                def sleep(self):
                    print(f"{self.name} is sleeping")
                
                def meow(self):
                    print(f"{self.name} is meowing")
            """;

        // AFTER: Extracted superclass with common methods, subclasses inherit
        String afterAnimalCode = """
            class Animal:
                def __init__(self, name):
                    self.name = name
                
                def eat(self):
                    print(f"{self.name} is eating")
                
                def sleep(self):
                    print(f"{self.name} is sleeping")
            """;

        String afterDogCode = """
            class Dog(Animal):
                def bark(self):
                    print(f"{self.name} is barking")
            """;

        String afterCatCode = """
            class Cat(Animal):
                def meow(self):
                    print(f"{self.name} is meowing")
            """;

        Map<String, String> beforeFiles = Map.of(
                "pets/dog.py", beforeDogCode,
                "pets/cat.py", beforeCatCode
        );

        Map<String, String> afterFiles = Map.of(
                "pets/animal.py", afterAnimalCode,
                "pets/dog.py", afterDogCode,
                "pets/cat.py", afterCatCode
        );

        assertExtractSuperclassRefactoringDetected(beforeFiles, afterFiles, "Animal", "Dog", "Cat");
    }

    @Test
    void detectsExtractSuperclass_VehicleFromCarAndTruck() throws Exception {
        String beforeCarCode = """
            class Car:
                def __init__(self, brand, model):
                    self.brand = brand
                    self.model = model
                
                def start_engine(self):
                    print(f"{self.brand} {self.model} engine started")
                
                def stop_engine(self):
                    print(f"{self.brand} {self.model} engine stopped")
                
                def honk(self):
                    print("Beep beep!")
            """;

        String beforeTruckCode = """
            class Truck:
                def __init__(self, brand, model):
                    self.brand = brand
                    self.model = model
                
                def start_engine(self):
                    print(f"{self.brand} {self.model} engine started")
                
                def stop_engine(self):
                    print(f"{self.brand} {self.model} engine stopped")
                
                def load_cargo(self):
                    print("Loading cargo")
            """;

        String afterVehicleCode = """
            class Vehicle:
                def __init__(self, brand, model):
                    self.brand = brand
                    self.model = model
                
                def start_engine(self):
                    print(f"{self.brand} {self.model} engine started")
                
                def stop_engine(self):
                    print(f"{self.brand} {self.model} engine stopped")
            """;

        String afterCarCode = """
            class Car(Vehicle):
                def honk(self):
                    print("Beep beep!")
            """;

        String afterTruckCode = """
            class Truck(Vehicle):
                def load_cargo(self):
                    print("Loading cargo")
            """;

        Map<String, String> beforeFiles = Map.of(
                "vehicles/car.py", beforeCarCode,
                "vehicles/truck.py", beforeTruckCode
        );

        Map<String, String> afterFiles = Map.of(
                "vehicles/vehicle.py", afterVehicleCode,
                "vehicles/car.py", afterCarCode,
                "vehicles/truck.py", afterTruckCode
        );

        assertExtractSuperclassRefactoringDetected(beforeFiles, afterFiles, "Vehicle", "Car", "Truck");
    }

    @Test
    void detectsExtractSuperclass_ShapeFromCircleAndRectangle() throws Exception {
        String beforeCircleCode = """
        class Circle:
            def __init__(self, x, y, radius):
                self.x = x
                self.y = y
                self.radius = radius
            
            def move(self, dx, dy):
                self.x += dx
                self.y += dy
            
            def get_position(self):
                return (self.x, self.y)
            
            def calculate_area(self):
                return 3.14159 * self.radius * self.radius
        """;

        String beforeRectangleCode = """
        class Rectangle:
            def __init__(self, x, y, width, height):
                self.x = x
                self.y = y
                self.width = width
                self.height = height
            
            def move(self, dx, dy):
                self.x += dx
                self.y += dy
            
            def get_position(self):
                return (self.x, self.y)
            
            def calculate_area(self):
                return self.width * self.height
        """;

        String afterShapeCode = """
        class Shape:
            def __init__(self, x, y):
                self.x = x
                self.y = y
            
            def move(self, dx, dy):
                self.x += dx
                self.y += dy
            
            def get_position(self):
                return (self.x, self.y)
        """;

        String afterCircleCode = """
        class Circle(Shape):
            def __init__(self, x, y, radius):
                super().__init__(x, y)
                self.radius = radius
            
            def calculate_area(self):
                return 3.14159 * self.radius * self.radius
        """;

        String afterRectangleCode = """
        class Rectangle(Shape):
            def __init__(self, x, y, width, height):
                super().__init__(x, y)
                self.width = width
                self.height = height
            
            def calculate_area(self):
                return self.width * self.height
        """;

        Map<String, String> beforeFiles = Map.of(
                "geometry/circle.py", beforeCircleCode,
                "geometry/rectangle.py", beforeRectangleCode
        );

        Map<String, String> afterFiles = Map.of(
                "geometry/shape.py", afterShapeCode,
                "geometry/circle.py", afterCircleCode,
                "geometry/rectangle.py", afterRectangleCode
        );

        assertExtractSuperclassRefactoringDetected(beforeFiles, afterFiles, "Shape", "Circle", "Rectangle");
    }

    @Test
    void detectsExtractSuperclass_DatabaseFromMySQLAndPostgreSQL() throws Exception {
        String beforeMySQLCode = """
        class MySQL:
            def __init__(self, host, port, database):
                self.host = host
                self.port = port
                self.database = database
            
            def connect(self):
                return f"Connected to MySQL at {self.host}:{self.port}"
            
            def disconnect(self):
                return "Disconnected from MySQL"
            
            def execute_query(self, query):
                return f"MySQL executing: {query}"
        """;

        String beforePostgreSQLCode = """
        class PostgreSQL:
            def __init__(self, host, port, database):
                self.host = host
                self.port = port
                self.database = database
            
            def connect(self):
                return f"Connected to PostgreSQL at {self.host}:{self.port}"
            
            def disconnect(self):
                return "Disconnected from PostgreSQL"
            
            def execute_query(self, query):
                return f"PostgreSQL executing: {query}"
        """;

        String afterDatabaseCode = """
        class Database:
            def __init__(self, host, port, database):
                self.host = host
                self.port = port
                self.database = database
            
            def connect(self):
                return f"Connected to database at {self.host}:{self.port}"
            
            def disconnect(self):
                return "Disconnected from database"
        """;

        String afterMySQLCode = """
        class MySQL(Database):
            def execute_query(self, query):
                return f"MySQL executing: {query}"
        """;

        String afterPostgreSQLCode = """
        class PostgreSQL(Database):
            def execute_query(self, query):
                return f"PostgreSQL executing: {query}"
        """;

        Map<String, String> beforeFiles = Map.of(
                "db/mysql.py", beforeMySQLCode,
                "db/postgresql.py", beforePostgreSQLCode
        );

        Map<String, String> afterFiles = Map.of(
                "db/database.py", afterDatabaseCode,
                "db/mysql.py", afterMySQLCode,
                "db/postgresql.py", afterPostgreSQLCode
        );

        assertExtractSuperclassRefactoringDetected(beforeFiles, afterFiles, "Database", "MySQL", "PostgreSQL");
    }

    @Test
    void detectsExtractSuperclass_HandlerFromEmailAndSMSHandler() throws Exception {
        String beforeEmailHandlerCode = """
        class EmailHandler:
            def __init__(self, config):
                self.config = config
                self.active = True
            
            def initialize(self):
                return f"Email handler initialized with {self.config}"
            
            def cleanup(self):
                self.active = False
                return "Email handler cleaned up"
            
            def send_email(self, recipient, message):
                return f"Email sent to {recipient}: {message}"
        """;

        String beforeSMSHandlerCode = """
        class SMSHandler:
            def __init__(self, config):
                self.config = config
                self.active = True
            
            def initialize(self):
                return f"SMS handler initialized with {self.config}"
            
            def cleanup(self):
                self.active = False
                return "SMS handler cleaned up"
            
            def send_sms(self, phone, message):
                return f"SMS sent to {phone}: {message}"
        """;

        String afterHandlerCode = """
        class Handler:
            def __init__(self, config):
                self.config = config
                self.active = True
            
            def initialize(self):
                return f"Handler initialized with {self.config}"
            
            def cleanup(self):
                self.active = False
                return "Handler cleaned up"
        """;

        String afterEmailHandlerCode = """
        class EmailHandler(Handler):
            def send_email(self, recipient, message):
                return f"Email sent to {recipient}: {message}"
        """;

        String afterSMSHandlerCode = """
        class SMSHandler(Handler):
            def send_sms(self, phone, message):
                return f"SMS sent to {phone}: {message}"
        """;

        Map<String, String> beforeFiles = Map.of(
                "messaging/email_handler.py", beforeEmailHandlerCode,
                "messaging/sms_handler.py", beforeSMSHandlerCode
        );

        Map<String, String> afterFiles = Map.of(
                "messaging/handler.py", afterHandlerCode,
                "messaging/email_handler.py", afterEmailHandlerCode,
                "messaging/sms_handler.py", afterSMSHandlerCode
        );

        assertExtractSuperclassRefactoringDetected(beforeFiles, afterFiles, "Handler", "EmailHandler", "SMSHandler");
    }

    @Test
    void detectsExtractSuperclass_ProcessorFromImageAndTextProcessor() throws Exception {
        String beforeImageProcessorCode = """
        class ImageProcessor:
            def __init__(self, name):
                self.name = name
                self.status = "ready"
            
            def start_processing(self):
                self.status = "processing"
                return f"Started processing with {self.name}"
            
            def finish_processing(self):
                self.status = "completed"
                return f"Finished processing with {self.name}"
            
            def resize_image(self, width, height):
                return f"Resizing image to {width}x{height}"
        """;

        String beforeTextProcessorCode = """
        class TextProcessor:
            def __init__(self, name):
                self.name = name
                self.status = "ready"
            
            def start_processing(self):
                self.status = "processing"
                return f"Started processing with {self.name}"
            
            def finish_processing(self):
                self.status = "completed"
                return f"Finished processing with {self.name}"
            
            def format_text(self, text, style):
                return f"Formatting text '{text}' with style {style}"
        """;

        String afterProcessorCode = """
        class Processor:
            def __init__(self, name):
                self.name = name
                self.status = "ready"
            
            def start_processing(self):
                self.status = "processing"
                return f"Started processing with {self.name}"
            
            def finish_processing(self):
                self.status = "completed"
                return f"Finished processing with {self.name}"
        """;

        String afterImageProcessorCode = """
        class ImageProcessor(Processor):
            def resize_image(self, width, height):
                return f"Resizing image to {width}x{height}"
        """;

        String afterTextProcessorCode = """
        class TextProcessor(Processor):
            def format_text(self, text, style):
                return f"Formatting text '{text}' with style {style}"
        """;

        Map<String, String> beforeFiles = Map.of(
                "processing/image_processor.py", beforeImageProcessorCode,
                "processing/text_processor.py", beforeTextProcessorCode
        );

        Map<String, String> afterFiles = Map.of(
                "processing/processor.py", afterProcessorCode,
                "processing/image_processor.py", afterImageProcessorCode,
                "processing/text_processor.py", afterTextProcessorCode
        );

        assertExtractSuperclassRefactoringDetected(beforeFiles, afterFiles, "Processor", "ImageProcessor", "TextProcessor");
    }

    @Test
    void detectsExtractSuperclass_ReaderFromFileAndNetworkReader() throws Exception {
        String beforeFileReaderCode = """
        class FileReader:
            def __init__(self, source):
                self.source = source
                self.buffer_size = 1024
            
            def open_connection(self):
                return f"Opening connection to {self.source}"
            
            def close_connection(self):
                return f"Closing connection to {self.source}"
            
            def read_from_file(self):
                return f"Reading data from file {self.source}"
        """;

        String beforeNetworkReaderCode = """
        class NetworkReader:
            def __init__(self, source):
                self.source = source
                self.buffer_size = 1024
            
            def open_connection(self):
                return f"Opening connection to {self.source}"
            
            def close_connection(self):
                return f"Closing connection to {self.source}"
            
            def read_from_network(self):
                return f"Reading data from network {self.source}"
        """;

        String afterReaderCode = """
        class Reader:
            def __init__(self, source):
                self.source = source
                self.buffer_size = 1024
            
            def open_connection(self):
                return f"Opening connection to {self.source}"
            
            def close_connection(self):
                return f"Closing connection to {self.source}"
        """;

        String afterFileReaderCode = """
        class FileReader(Reader):
            def read_from_file(self):
                return f"Reading data from file {self.source}"
        """;

        String afterNetworkReaderCode = """
        class NetworkReader(Reader):
            def read_from_network(self):
                return f"Reading data from network {self.source}"
        """;

        Map<String, String> beforeFiles = Map.of(
                "io/file_reader.py", beforeFileReaderCode,
                "io/network_reader.py", beforeNetworkReaderCode
        );

        Map<String, String> afterFiles = Map.of(
                "io/reader.py", afterReaderCode,
                "io/file_reader.py", afterFileReaderCode,
                "io/network_reader.py", afterNetworkReaderCode
        );

        assertExtractSuperclassRefactoringDetected(beforeFiles, afterFiles, "Reader", "FileReader", "NetworkReader");
    }

    @Test
    void detectsExtractSuperclass_LoggerFromConsoleAndFileLogger() throws Exception {
        String beforeConsoleLoggerCode = """
        class ConsoleLogger:
            def __init__(self, level):
                self.level = level
                self.enabled = True
            
            def set_level(self, new_level):
                self.level = new_level
            
            def is_enabled(self):
                return self.enabled
            
            def log_to_console(self, message):
                print(f"[{self.level}] {message}")
        """;

        String beforeFileLoggerCode = """
        class FileLogger:
            def __init__(self, level):
                self.level = level
                self.enabled = True
            
            def set_level(self, new_level):
                self.level = new_level
            
            def is_enabled(self):
                return self.enabled
            
            def log_to_file(self, message, filename):
                return f"Logging to {filename}: [{self.level}] {message}"
        """;

        String afterLoggerCode = """
        class Logger:
            def __init__(self, level):
                self.level = level
                self.enabled = True
            
            def set_level(self, new_level):
                self.level = new_level
            
            def is_enabled(self):
                return self.enabled
        """;

        String afterConsoleLoggerCode = """
        class ConsoleLogger(Logger):
            def log_to_console(self, message):
                print(f"[{self.level}] {message}")
        """;

        String afterFileLoggerCode = """
        class FileLogger(Logger):
            def log_to_file(self, message, filename):
                return f"Logging to {filename}: [{self.level}] {message}"
        """;

        Map<String, String> beforeFiles = Map.of(
                "logging/console_logger.py", beforeConsoleLoggerCode,
                "logging/file_logger.py", beforeFileLoggerCode
        );

        Map<String, String> afterFiles = Map.of(
                "logging/logger.py", afterLoggerCode,
                "logging/console_logger.py", afterConsoleLoggerCode,
                "logging/file_logger.py", afterFileLoggerCode
        );

        assertExtractSuperclassRefactoringDetected(beforeFiles, afterFiles, "Logger", "ConsoleLogger", "FileLogger");
    }

    @Test
    void detectsExtractSuperclass_ValidatorFromEmailAndPhoneValidator() throws Exception {
        String beforeEmailValidatorCode = """
        class EmailValidator:
            def __init__(self, strict_mode):
                self.strict_mode = strict_mode
                self.errors = []
            
            def reset_errors(self):
                self.errors.clear()
            
            def has_errors(self):
                return len(self.errors) > 0
            
            def validate_email(self, email):
                if "@" not in email:
                    self.errors.append("Invalid email format")
                    return False
                return True
        """;

        String beforePhoneValidatorCode = """
        class PhoneValidator:
            def __init__(self, strict_mode):
                self.strict_mode = strict_mode
                self.errors = []
            
            def reset_errors(self):
                self.errors.clear()
            
            def has_errors(self):
                return len(self.errors) > 0
            
            def validate_phone(self, phone):
                if len(phone) < 10:
                    self.errors.append("Phone number too short")
                    return False
                return True
        """;

        String afterValidatorCode = """
        class Validator:
            def __init__(self, strict_mode):
                self.strict_mode = strict_mode
                self.errors = []
            
            def reset_errors(self):
                self.errors.clear()
            
            def has_errors(self):
                return len(self.errors) > 0
        """;

        String afterEmailValidatorCode = """
        class EmailValidator(Validator):
            def validate_email(self, email):
                if "@" not in email:
                    self.errors.append("Invalid email format")
                    return False
                return True
        """;

        String afterPhoneValidatorCode = """
        class PhoneValidator(Validator):
            def validate_phone(self, phone):
                if len(phone) < 10:
                    self.errors.append("Phone number too short")
                    return False
                return True
        """;

        Map<String, String> beforeFiles = Map.of(
                "validation/email_validator.py", beforeEmailValidatorCode,
                "validation/phone_validator.py", beforePhoneValidatorCode
        );

        Map<String, String> afterFiles = Map.of(
                "validation/validator.py", afterValidatorCode,
                "validation/email_validator.py", afterEmailValidatorCode,
                "validation/phone_validator.py", afterPhoneValidatorCode
        );

        assertExtractSuperclassRefactoringDetected(beforeFiles, afterFiles, "Validator", "EmailValidator", "PhoneValidator");
    }

    @Test
    void detectsExtractSuperclass_CacheFromMemoryAndDiskCache() throws Exception {
        String beforeMemoryCacheCode = """
        class MemoryCache:
            def __init__(self, max_size):
                self.max_size = max_size
                self.current_size = 0
            
            def get_size(self):
                return self.current_size
            
            def is_full(self):
                return self.current_size >= self.max_size
            
            def store_in_memory(self, key, value):
                self.current_size += 1
                return f"Stored {key} in memory"
        """;

        String beforeDiskCacheCode = """
        class DiskCache:
            def __init__(self, max_size):
                self.max_size = max_size
                self.current_size = 0
            
            def get_size(self):
                return self.current_size
            
            def is_full(self):
                return self.current_size >= self.max_size
            
            def store_on_disk(self, key, value):
                self.current_size += 1
                return f"Stored {key} on disk"
        """;

        String afterCacheCode = """
        class Cache:
            def __init__(self, max_size):
                self.max_size = max_size
                self.current_size = 0
            
            def get_size(self):
                return self.current_size
            
            def is_full(self):
                return self.current_size >= self.max_size
        """;

        String afterMemoryCacheCode = """
        class MemoryCache(Cache):
            def store_in_memory(self, key, value):
                self.current_size += 1
                return f"Stored {key} in memory"
        """;

        String afterDiskCacheCode = """
        class DiskCache(Cache):
            def store_on_disk(self, key, value):
                self.current_size += 1
                return f"Stored {key} on disk"
        """;

        Map<String, String> beforeFiles = Map.of(
                "cache/memory_cache.py", beforeMemoryCacheCode,
                "cache/disk_cache.py", beforeDiskCacheCode
        );

        Map<String, String> afterFiles = Map.of(
                "cache/cache.py", afterCacheCode,
                "cache/memory_cache.py", afterMemoryCacheCode,
                "cache/disk_cache.py", afterDiskCacheCode
        );

        assertExtractSuperclassRefactoringDetected(beforeFiles, afterFiles, "Cache", "MemoryCache", "DiskCache");
    }

    public static void assertExtractSuperclassRefactoringDetected(
            Map<String, String> beforeFiles,
            Map<String, String> afterFiles,
            String extractedSuperclassName,
            String... subclassNames
    ) throws Exception {
        UMLModelAdapter beforeAdapter = new UMLModelAdapter(beforeFiles);
        UMLModelAdapter afterAdapter = new UMLModelAdapter(afterFiles);

        UMLModel beforeUML = beforeAdapter.getUMLModel();
        UMLModel afterUML = afterAdapter.getUMLModel();

        UMLModelDiff diff = beforeUML.diff(afterUML);
        List<Refactoring> refactorings = diff.getRefactorings();

        System.out.println("Refactorings: " + refactorings.size());
        refactorings.forEach(System.out::println);

        // Find ExtractSuperclassRefactoring that matches our criteria
        boolean found = refactorings.stream()
                .filter(r -> r instanceof ExtractSuperclassRefactoring)
                .map(r -> (ExtractSuperclassRefactoring) r)
                .anyMatch(refactoring -> {
                    String actualExtractedName = refactoring.getExtractedClass().getName();
                    boolean nameMatches = actualExtractedName.equals(extractedSuperclassName);

                    Set<String> actualSubclassesAfter = refactoring.getSubclassSetAfter();
                    boolean allSubclassesMatch = Arrays.stream(subclassNames)
                            .allMatch(expectedSubclass -> actualSubclassesAfter.contains(expectedSubclass));

                    boolean countMatches = actualSubclassesAfter.size() == subclassNames.length;


                    return nameMatches && allSubclassesMatch && countMatches;
                });

        if (!found) {
            // Provide detailed error message for debugging
            StringBuilder errorMessage = new StringBuilder();
            errorMessage.append("Extract Superclass refactoring not detected.\n");
            errorMessage.append("Expected: Extract superclass '").append(extractedSuperclassName)
                    .append("' from subclasses ").append(Arrays.toString(subclassNames)).append("\n");

            // Show what refactorings were actually detected
            List<ExtractSuperclassRefactoring> extractRefactorings = refactorings.stream()
                    .filter(r -> r instanceof ExtractSuperclassRefactoring)
                    .map(r -> (ExtractSuperclassRefactoring) r)
                    .collect(Collectors.toList());

            if (extractRefactorings.isEmpty()) {
                errorMessage.append("No Extract Superclass refactorings were detected.");
            } else {
                errorMessage.append("Detected Extract Superclass refactorings:\n");
                for (ExtractSuperclassRefactoring refactoring : extractRefactorings) {
                    errorMessage.append("  - Extracted: '").append(refactoring.getExtractedClass().getName())
                            .append("' from subclasses: ").append(refactoring.getSubclassSetAfter()).append("\n");
                }
            }

            fail(errorMessage.toString());
        }
    }
}