"""
Script to test Java LocationInfo class using GraalPy interoperability
"""
import java


# Import Java classes
try:
    LocationInfo = java.type("gr.uom.java.xmi.LocationInfo")
    CodeElementType = java.type("gr.uom.java.xmi.LocationInfo$CodeElementType")
except Exception as e:
    print(f"Error loading Java classes: {e}")
    print("Make sure GraalPy is properly installed and the Java classes are in the classpath")
    exit(1)

def create_test_location_info(source_folder, file_path, start_offset, end_offset, length,
                              start_line, start_column, end_line, end_column,
                              cu_length, code_element_type):
    """Create a LocationInfo instance with the given parameters."""
    return LocationInfo(
        source_folder, file_path,
        start_offset, end_offset, length,
        start_line, start_column,
        end_line, end_column,
        cu_length, code_element_type
    )

def print_location_info(loc):
    """Print information about a LocationInfo object."""
    print(f"Source folder: {loc.getSourceFolder()}")
    print(f"File path: {loc.getFilePath()}")
    print(f"Position: {loc.getStartOffset()}-{loc.getEndOffset()} (length: {loc.getLength()})")
    print(f"Lines: {loc.getStartLine()}-{loc.getEndLine()}")
    print(f"Columns: {loc.getStartColumn()}-{loc.getEndColumn()}")
    print(f"Code element type: {loc.getCodeElementType()}")
    print(f"String representation: {loc}")

def run_tests():
    """Run test cases for LocationInfo class."""
    print("Creating test cases for LocationInfo...")

    # Test case 1: Method declaration
    method_loc = create_test_location_info(
        "/src/main", "/src/main/Test.java",
        100, 200, 100, 10, 5, 15, 10, 100,
        CodeElementType.METHOD_DECLARATION
    )

    # Test case 2: Field declaration
    field_loc = create_test_location_info(
        "/src/main", "/src/main/Test.java",
        50, 60, 10, 5, 5, 5, 15, 100,
        CodeElementType.FIELD_DECLARATION
    )

    # Test case 3: Class declaration that subsumes the others
    class_loc = create_test_location_info(
        "/src/main", "/src/main/Test.java",
        0, 300, 300, 1, 1, 30, 1, 100,
        CodeElementType.TYPE_DECLARATION
    )

    # Print properties
    print("\n--- Method Declaration Location ---")
    print_location_info(method_loc)

    print("\n--- Field Declaration Location ---")
    print_location_info(field_loc)

    # Test relationship methods
    print("\n--- Testing relationships ---")
    print(f"Is field before method? {field_loc.before(method_loc)}")
    print(f"Does class subsume method? {class_loc.subsumes(method_loc)}")
    print(f"Are field and method on same line? {field_loc.sameLine(method_loc)}")

    # Create adjacent statements to test nextLine
    stmt1 = create_test_location_info(
        "/src/test", "/src/test/File.java",
        100, 120, 20, 10, 1, 10, 20, 100,
        CodeElementType.EXPRESSION_STATEMENT
    )

    stmt2 = create_test_location_info(
        "/src/test", "/src/test/File.java",
        130, 150, 20, 11, 1, 11, 20, 100,
        CodeElementType.EXPRESSION_STATEMENT
    )

    print(f"Is stmt2 on next line after stmt1? {stmt2.nextLine(stmt1)}")

if __name__ == "__main__":
    run_tests()