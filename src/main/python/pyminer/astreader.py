import ast, os, java, logging

logging.basicConfig(level=logging.DEBUG)
# Import Java UML classes
try:
    LocationInfo = java.type("gr.uom.java.xmi.LocationInfo")
    CodeElementType = java.type("gr.uom.java.xmi.LocationInfo$CodeElementType")
    UMLClass = java.type("gr.uom.java.xmi.UMLClass")
    UMLModel = java.type("gr.uom.java.xmi.UMLModel")
    UMLOperation = java.type("gr.uom.java.xmi.UMLOperation")
    UMLParameter = java.type("gr.uom.java.xmi.UMLParameter")
    UMLImport = java.type("gr.uom.java.xmi.UMLImport")
    UMLAttribute = java.type("gr.uom.java.xmi.UMLAttribute")
    UMLGeneralization = java.type("gr.uom.java.xmi.UMLGeneralization")
    UMLType = java.type("gr.uom.java.xmi.UMLType")
    Visibility = java.type("gr.uom.java.xmi.Visibility")
except Exception as e:
    logging.error(f"Error loading Java classes: {e}", exc_info=True, stack_info=True)
    exit(1)

def populate_file_contents(base_path):
    logging.info(f"Populating file contents from base path: {base_path}")
    import glob
    python_file_contents = {}
    repository_directories = []
    logging.debug("Initializing python_file_contents and repository_directories")
    for path in glob.iglob(f"{base_path}/**/*.py", recursive=True):
        with open(path, "r") as file:
            python_file_contents[path] = file.read()
            logging.debug(f"Read file: {path}")
        while 1:
            directory, _ = os.path.split(path)
            if len(directory) == 0:
                break
            else:
                repository_directories.append(directory)
                logging.debug(f"Added directory to repository_directories: {directory}")
                path = directory
    return UMLModelASTReader(python_file_contents, repository_directories).get_uml_model()

class UMLModelASTReader:
    def __init__(self, python_file_contents, repository_directories):
        logging.info("Initializing UMLModelASTReader")
        self.uml_model = UMLModel(repository_directories)
        self.process_files(python_file_contents)
        logging.info("Processed files")

    def process_files(self, python_file_contents):
        for file_path, content in python_file_contents.items():
            logging.debug(f"Processing file: {file_path}")
            self.process_file(file_path, content)

    def process_file(self, file_path, content):
        try:
            logging.debug(f"Parsed AST for file: {file_path}")
            tree = ast.parse(content)
            source_folder = os.path.dirname(file_path)
            generator = UMLCodeGenerator(source_folder, file_path, content, self.uml_model)
            generator.visit(tree)
        except SyntaxError as e:
            logging.error(f"Syntax error in {file_path}: {e}", exc_info=True, stack_info=True)

    def get_uml_model(self):
        logging.info("Returning UML model")
        return self.uml_model

class UMLCodeGenerator(ast.NodeVisitor):
    def __init__(self, source_folder, source_file, source_content, uml_model):
        logging.info(f"Initializing UMLCodeGenerator for file: {source_file}")
        self.source_folder = source_folder
        self.source_file = source_file
        self.source_content = source_content
        self.uml_model = uml_model
        self.current_class = None
        self.class_stack = []
        self._line_offsets = None

    def visit_ClassDef(self, node):
        logging.debug(f"Visiting class definition: {node.name}")
        # Calculate package name from file path
        package_name = self.source_folder.replace(os.path.sep, '.')

        # Create location info for the class
        location = self._create_location_info(node, CodeElementType.CLASS_DECLARATION)

        # Create UML Class
        uml_class = UMLClass(
            package_name,
            node.name,
            location,
            False,  # is_package_member topLevel flag; adjust if needed
            []      # imported_types (empty list for now)
        )

        # If the class explicitly defines ABCMeta as its metaclass, mark as abstract.
        if self._is_abc_metaclass(node):
            uml_class.setAbstract(True)
            logging.debug(f"Class {node.name} is abstract")

        # Push class context
        self.class_stack.append(uml_class)
        self.current_class = uml_class
        logging.debug(f"Set current class to: {uml_class.getName()}")
        self.uml_model.addClass(uml_class)

        # Process body elements (methods, inner classes, etc.)
        self.generic_visit(node)
        logging.debug(f"Visited body of class: {node.name}")

        # Pop class context
        self.class_stack.pop()
        self.current_class = self.class_stack[-1] if self.class_stack else None
        logging.debug(f"Restored previous class context: {self.current_class.getName() if self.current_class else 'None'}")

    def visit_FunctionDef(self, node):
        logging.debug(f"Visiting function definition: {node.name}")
        # Only process methods defined within a class
        if not self.current_class:
            logging.debug(f"Skipping function {node.name} as it is not within a class")
            return

        location = self._create_location_info(node, CodeElementType.METHOD_DECLARATION)
        operation = UMLOperation(node.name, location)

        # Set the operation as abstract if any decorator indicates it
        if any(self._is_abstract_decorator(d) for d in node.decorator_list):
            operation.setAbstract(True)
            logging.debug(f"Function {node.name} is abstract")

        # Process parameters
        for param in node.args.args:
            param_name = param.arg
            if param.annotation:
                param_type = self._parse_type(param.annotation)
            else:
                param_type = UMLType("Any")
            uml_param = UMLParameter(param_name, param_type, "in", False)
            operation.addParameter(uml_param)

        # Process return type
        if node.returns:
            return_type = self._parse_type(node.returns)
            return_param = UMLParameter("return", return_type, "return", False)
            operation.addParameter(return_param)

        self.current_class.addOperation(operation)
        logging.debug(f"Added operation {node.name} to class {self.current_class.getName()}")

    def visit_Import(self, node):
        logging.debug("Visiting import statement")
        for alias in node.names:
            location = self._create_location_info(node, CodeElementType.IMPORT_DECLARATION)
            uml_import = UMLImport(alias.name, alias.name == "*", False, location)
            self.uml_model.addImport(uml_import)

    def visit_ImportFrom(self, node):
        logging.debug(f"Visiting import from statement: {node.module}")
        module = node.module or ""
        for alias in node.names:
            full_name = f"{module}.{alias.name}" if module else alias.name
            location = self._create_location_info(node, CodeElementType.IMPORT_DECLARATION)
            uml_import = UMLImport(full_name, alias.name == "*", False, location)
            self.uml_model.addImport(uml_import)

    def visit_AnnAssign(self, node):
        logging.debug(f"Visiting annotated assignment: {node.target.id}")
        # Process annotated assignments as attributes if inside a class
        if isinstance(node.target, ast.Name) and self.current_class:
            attr_name = node.target.id
            attr_type = self._parse_type(node.annotation)
            location = self._create_location_info(node, CodeElementType.FIELD_DECLARATION)
            attribute = UMLAttribute(attr_name, attr_type, location)
            attribute.setVisibility(self._determine_visibility(attr_name))
            self.current_class.addAttribute(attribute)

    def _create_location_info(self, node, element_type):
        # Calculate start and end offsets based on line and column info.
        start = self._get_offset(node.lineno, node.col_offset)
        end = self._get_offset(node.end_lineno, node.end_col_offset)
        logging.debug(f"Created location info for node: {node}")
        return LocationInfo(
            self.source_folder,
            self.source_file,
            start,
            end - start,
            element_type
        )

    def _get_offset(self, line, column):
        if self._line_offsets is None:
            logging.debug("Calculating line offsets")
            self._line_offsets = []
            offset = 0
            for l in self.source_content.split('\n'):
                self._line_offsets.append(offset)
                offset += len(l) + 1  # +1 for the newline character
        return self._line_offsets[line - 1] + column

    def _parse_type(self, node):
        if isinstance(node, ast.Name):
            logging.debug(f"Parsed type: {node.id}")
            return UMLType(node.id)
        elif isinstance(node, ast.Subscript):
            base = self._parse_type(node.value)
            # Handle subscript slices that might be wrapped in an Index node in older Python versions.
            if hasattr(node, 'slice'):
                subscript = self._parse_type(node.slice)
            else:
                subscript = UMLType("Any")
            return UMLType(f"{base.getType()}[{subscript.getType()}]")
        elif isinstance(node, ast.Attribute):
            logging.debug(f"Parsed qualified name: {self._parse_qualified_name(node)}")
            return UMLType(self._parse_qualified_name(node))
        return UMLType("Any")

    def _parse_qualified_name(self, node):
        if isinstance(node, ast.Name):
            return node.id
        elif isinstance(node, ast.Attribute):
            return f"{self._parse_qualified_name(node.value)}.{node.attr}"
        return "Unknown"

    def _determine_visibility(self, name):
        if name.startswith("__"):
            logging.debug(f"Determined visibility for {name}: PRIVATE")
            return Visibility.PRIVATE
        elif name.startswith("_"):
            logging.debug(f"Determined visibility for {name}: PROTECTED")
            return Visibility.PROTECTED
        logging.debug(f"Determined visibility for {name}: PUBLIC")
        return Visibility.PUBLIC

    def _is_abstract_decorator(self, decorator):
        # Only handle simple Name or Attribute nodes.
        if isinstance(decorator, ast.Name):
            logging.debug(f"Decorator {decorator.id} is abstractmethod: {decorator.id == 'abstractmethod'}")
            return decorator.id == "abstractmethod"
        elif isinstance(decorator, ast.Attribute):
            logging.debug(f"Decorator {decorator.attr} is abstractmethod: {decorator.attr == 'abstractmethod'}")
            return decorator.attr == "abstractmethod"
        return False

    def _is_abc_metaclass(self, node):
        # Check for keyword arguments in class definition that define the metaclass.
        for kw in node.keywords:
            if kw.arg == 'metaclass' and isinstance(kw.value, ast.Name):
                if kw.value.id == "ABCMeta":
                    logging.debug(f"Class has ABCMeta as metaclass: {kw.value.id == 'ABCMeta'}")
                    return True
        return False
