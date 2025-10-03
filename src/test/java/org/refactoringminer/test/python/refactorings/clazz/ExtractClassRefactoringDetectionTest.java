package org.refactoringminer.test.python.refactorings.clazz;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import java.util.Map;

import static org.refactoringminer.utils.RefactoringAssertUtils.assertExtractClassRefactoringDetected;

@Isolated
public class ExtractClassRefactoringDetectionTest {

    @Test
    void detectsVerySimpleExtractClass() throws Exception {
        String beforePythonCode = """
            class Person:
                def __init__(self, name, street, city):
                    self.name = name
                    self.street = street
                    self.city = city

                def address(self):
                    return f"{self.street}, {self.city}"
            """;

        String afterPythonCode = """
            class Address:
                def __init__(self, street, city):
                    self.street = street
                    self.city = city

            class Person:
                def __init__(self, name, street, city):
                    self.name = name
                    self.address = Address(street, city)

                def address(self):
                    return f"{self.address.street}, {self.address.city}"
            """;

        Map<String, String> beforeFiles = Map.of("tests/person.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("tests/person.py", afterPythonCode);

        assertExtractClassRefactoringDetected(beforeFiles, afterFiles, "Person", "Address");
    }


    @Test
    void detectsExtractClassRefactoring() throws Exception {
        String beforePythonCode = """
        class Employee:
            def __init__(self, name, street, city, zipcode):
                self.name = name
                self.street = street
                self.city = city
                self.zipcode = zipcode

            def get_address(self):
                return f"{self.street}, {self.city}, {self.zipcode}"

            def display(self):
                print(f"Employee: {self.name}")
                print(f"Address: {self.get_address()}")
        """;
        String afterPythonCode = """
        class Address:
            def __init__(self, street, city, zipcode):
                self.street = street
                self.city = city
                self.zipcode = zipcode

            def get_address(self):
                return f"{self.street}, {self.city}, {self.zipcode}"

        class Employee:
            def __init__(self, name, street, city, zipcode):
                self.name = name
                self.address = Address(street, city, zipcode)

            def display(self):
                print(f"Employee: {self.name}")
                print(f"Address: {self.address.get_address()}")
        """;

        Map<String, String> beforeFiles = Map.of("tests/employee.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("tests/employee.py", afterPythonCode);

        assertExtractClassRefactoringDetected(
                beforeFiles,
                afterFiles,
                "Employee",    // Original class name
                "Address"      // Extracted class name
        );
    }

    @Test
    void detectsExtractClass_BookToAuthor() throws Exception {
        // Test extract class detection with all attributes renamed
        String beforePythonCode = """
        class Book:
            def __init__(self, title, author_name, author_email, author_bio):
                self.title = title
                self.author_name = author_name
                self.author_email = author_email
                self.author_bio = author_bio
            
            def get_author_info(self):
                return f"{self.author_name} - {self.author_email}"
            
            def display_book(self):
                return f"Book: {self.title} by {self.author_name}"
        """;

        String afterPythonCode = """
        class Author:
            def __init__(self, name, email, bio):
                self.name = name
                self.email = email
                self.bio = bio
            
            def get_author_info(self):
                return f"{self.name} - {self.email}"
        
        class Book:
            def __init__(self, title, author_name, author_email, author_bio):
                self.title = title
                self.author = Author(author_name, author_email, author_bio)
            
            def display_book(self):
                return f"Book: {self.title} by {self.author.name}"
        """;

        Map<String, String> beforeFiles = Map.of("library/book.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("library/book.py", afterPythonCode);

        assertExtractClassRefactoringDetected(beforeFiles, afterFiles, "Book", "Author");
    }

    @Test
    void detectsExtractClass_OrderToCustomer() throws Exception {
        String beforePythonCode = """
        class Order:
            def __init__(self, order_id, customer_name, customer_phone, customer_address):
                self.order_id = order_id
                self.customer_name = customer_name
                self.customer_phone = customer_phone
                self.customer_address = customer_address
                self.total = 0
            
            def get_customer_contact(self):
                return f"{self.customer_name}: {self.customer_phone}"
            
            def process_order(self):
                return f"Processing order {self.order_id} for {self.customer_name}"
        """;

        String afterPythonCode = """
        class Customer:
            def __init__(self, customer_name, customer_phone, address):
                self.customer_name = customer_name
                self.customer_phone = customer_phone
                self.address = address
            
            def get_customer_contact(self):
                return f"{self.name}: {self.phone}"
        
        class Order:
            def __init__(self, order_id, customer_name, customer_phone, customer_address):
                self.order_id = order_id
                self.customer = Customer(customer_name, customer_phone, customer_address)
                self.total = 0
            
            def process_order(self):
                return f"Processing order {self.order_id} for {self.customer.name}"
        """;

        Map<String, String> beforeFiles = Map.of("commerce/order.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("commerce/order.py", afterPythonCode);

        assertExtractClassRefactoringDetected(beforeFiles, afterFiles, "Order", "Customer");
    }

    @Test
    void detectsExtractClass_StudentToGrade() throws Exception {
        String beforePythonCode = """
        class Student:
            def __init__(self, name, student_id, math_score, science_score, english_score):
                self.name = name
                self.student_id = student_id
                self.math_score = math_score
                self.science_score = science_score
                self.english_score = english_score
            
            def calculate_average(self):
                return (self.math_score + self.science_score + self.english_score) / 3
            
            def get_student_info(self):
                return f"Student: {self.name} (ID: {self.student_id})"
        """;

        String afterPythonCode = """
        class Grade:
            def __init__(self, math_score, science_score, english_score):
                self.math_score = math_score
                self.science_score = science_score
                self.english_score = english_score
            
            def calculate_average(self):
                return (self.math_score + self.science_score + self.english_score) / 3
        
        class Student:
            def __init__(self, name, student_id, math_score, science_score, english_score):
                self.name = name
                self.student_id = student_id
                self.grade = Grade(math_score, science_score, english_score)
            
            def get_student_info(self):
                return f"Student: {self.name} (ID: {self.student_id})"
        """;

        Map<String, String> beforeFiles = Map.of("school/student.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("school/student.py", afterPythonCode);

        assertExtractClassRefactoringDetected(beforeFiles, afterFiles, "Student", "Grade");
    }

    @Test
    void detectsExtractClass_CarToEngine() throws Exception {
        String beforePythonCode = """
        class Car:
            def __init__(self, brand, model, engine_type, horsepower, fuel_type):
                self.brand = brand
                self.model = model
                self.engine_type = engine_type
                self.horsepower = horsepower
                self.fuel_type = fuel_type
            
            def start_engine(self):
                return f"Starting {self.engine_type} engine with {self.horsepower} HP"
            
            def get_car_info(self):
                return f"{self.brand} {self.model}"
        """;

        String afterPythonCode = """
        class Engine:
            def __init__(self, engine_type, horsepower, fuel_type):
                self.engine_type = engine_type
                self.horsepower = horsepower
                self.fuel_type = fuel_type
            
            def start_engine(self):
                return f"Starting {self.engine_type} engine with {self.horsepower} HP"
        
        class Car:
            def __init__(self, brand, model, engine_type, horsepower, fuel_type):
                self.brand = brand
                self.model = model
                self.engine = Engine(engine_type, horsepower, fuel_type)
            
            def get_car_info(self):
                return f"{self.brand} {self.model}"
        """;

        Map<String, String> beforeFiles = Map.of("vehicles/car.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("vehicles/car.py", afterPythonCode);

        assertExtractClassRefactoringDetected(beforeFiles, afterFiles, "Car", "Engine");
    }

    @Test
    void detectsExtractClass_ProductToPrice() throws Exception {
        String beforePythonCode = """
        class Product:
            def __init__(self, name, description, base_price, tax_rate, discount):
                self.name = name
                self.description = description
                self.base_price = base_price
                self.tax_rate = tax_rate
                self.discount = discount
            
            def calculate_final_price(self):
                discounted = self.base_price * (1 - self.discount)
                return discounted * (1 + self.tax_rate)
            
            def get_product_name(self):
                return self.name
        """;

        String afterPythonCode = """
        class Price:
            def __init__(self, base_price, tax_rate, discount):
                self.base_price = base_price
                self.tax_rate = tax_rate
                self.discount = discount
            
            def calculate_final_price(self):
                discounted = self.base_price * (1 - self.discount)
                return discounted * (1 + self.tax_rate)
        
        class Product:
            def __init__(self, name, description, base_price, tax_rate, discount):
                self.name = name
                self.description = description
                self.price = Price(base_price, tax_rate, discount)
            
            def get_product_name(self):
                return self.name
        """;

        Map<String, String> beforeFiles = Map.of("catalog/product.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("catalog/product.py", afterPythonCode);

        assertExtractClassRefactoringDetected(beforeFiles, afterFiles, "Product", "Price");
    }

    @Test
    void detectsExtractClass_BankAccountToTransaction() throws Exception {
        String beforePythonCode = """
        class BankAccount:
            def __init__(self, account_number, balance, last_transaction_amount, transaction_type, transaction_date):
                self.account_number = account_number
                self.balance = balance
                self.last_transaction_amount = last_transaction_amount
                self.transaction_type = transaction_type
                self.transaction_date = transaction_date
            
            def get_transaction_details(self):
                return f"{self.transaction_type}: ${self.last_transaction_amount} on {self.transaction_date}"
            
            def get_balance(self):
                return f"Account {self.account_number}: ${self.balance}"
        """;

        String afterPythonCode = """
        class Transaction:
            def __init__(self, amount, transaction_type, transaction_date):
                self.amount = amount
                self.transaction_type = transaction_type
                self.transaction_date = transaction_date
            
            def get_transaction_details(self):
                return f"{self.transaction_type}: ${self.amount} on {self.date}"
        
        class BankAccount:
            def __init__(self, account_number, balance, last_transaction_amount, transaction_type, transaction_date):
                self.account_number = account_number
                self.balance = balance
                self.last_transaction = Transaction(last_transaction_amount, transaction_type, transaction_date)
            
            def get_balance(self):
                return f"Account {self.account_number}: ${self.balance}"
        """;

        Map<String, String> beforeFiles = Map.of("banking/account.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("banking/account.py", afterPythonCode);

        assertExtractClassRefactoringDetected(beforeFiles, afterFiles, "BankAccount", "Transaction");
    }

    @Test
    void detectsExtractClass_EventToLocation() throws Exception {
        String beforePythonCode = """
        class Event:
            def __init__(self, name, date, venue_name, venue_address, venue_capacity):
                self.name = name
                self.date = date
                self.venue_name = venue_name
                self.venue_address = venue_address
                self.venue_capacity = venue_capacity
            
            def get_venue_info(self):
                return f"{self.venue_name} at {self.venue_address} (Capacity: {self.venue_capacity})"
            
            def get_event_summary(self):
                return f"Event: {self.name} on {self.date}"
        """;

        String afterPythonCode = """
        class Location:
            def __init__(self, venue_name, venue_address, venue_capacity):
                self.venue_name = venue_name
                self.venue_address = venue_address
                self.venue_capacity = venue_capacity
            
            def get_venue_info(self):
                return f"{self.venue_name} at {self.venue_address} (Capacity: {self.venue_capacity})"
        
        class Event:
            def __init__(self, name, date, venue_name, venue_address, venue_capacity):
                self.name = name
                self.date = date
                self.location = Location(venue_name, venue_address, venue_capacity)
            
            def get_event_summary(self):
                return f"Event: {self.name} on {self.date}"
        """;

        Map<String, String> beforeFiles = Map.of("events/event.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("events/event.py", afterPythonCode);

        assertExtractClassRefactoringDetected(beforeFiles, afterFiles, "Event", "Location");
    }

    @Test
    void detectsExtractClass_ComputerToProcessor() throws Exception {
        String beforePythonCode = """
        class Computer:
            def __init__(self, brand, model, cpu_brand, cpu_speed, cpu_cores):
                self.brand = brand
                self.model = model
                self.cpu_brand = cpu_brand
                self.cpu_speed = cpu_speed
                self.cpu_cores = cpu_cores
            
            def get_cpu_specs(self):
                return f"{self.cpu_brand} {self.cpu_speed}GHz {self.cpu_cores} cores"
            
            def get_computer_info(self):
                return f"{self.brand} {self.model}"
        """;

        String afterPythonCode = """
        class Processor:
            def __init__(self, cpu_brand, cpu_speed, cpu_cores):
                self.cpu_brand = cpu_brand
                self.cpu_speed = cpu_speed
                self.cpu_cores = cpu_cores
            
            def get_cpu_specs(self):
                return f"{self.cpu_brand} {self.cpu_speed}GHz {self.cpu_cores} cores"
        
        class Computer:
            def __init__(self, brand, model, cpu_brand, cpu_speed, cpu_cores):
                self.brand = brand
                self.model = model
                self.processor = Processor(cpu_brand, cpu_speed, cpu_cores)
            
            def get_computer_info(self):
                return f"{self.brand} {self.model}"
        """;

        Map<String, String> beforeFiles = Map.of("hardware/computer.py", beforePythonCode);
        Map<String, String> afterFiles = Map.of("hardware/computer.py", afterPythonCode);

        assertExtractClassRefactoringDetected(beforeFiles, afterFiles, "Computer", "Processor");
    }
}
