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
    LeafExpression = java.type("gr.uom.java.xmi.decomposition.LeafExpression")
    VariableDeclaration = java.type("gr.uom.java.xmi.decomposition.VariableDeclaration")
    TernaryOperatorExpression = java.type("gr.uom.java.xmi.decomposition.TernaryOperatorExpression")
    LambdaExpressionObject = java.type("gr.uom.java.xmi.decomposition.LambdaExpressionObject")
    OperationInvocation = java.type("gr.uom.java.xmi.decomposition.OperationInvocation")
    LeafType = java.type("gr.uom.java.xmi.LeafType")
    CompositeType = java.type("gr.uom.java.xmi.CompositeType")
    Visibility = java.type("gr.uom.java.xmi.Visibility")
except Exception as e:
    logging.error(f"Error loading Java classes: {e}", exc_info=True, stack_info=True)
    exit(1)

def populate_file_contents(base_path):
    logging.info(f"Populating file contents from base path: {base_path}")
    import glob
    python_file_contents = java.type("java.util.LinkedHashMap")()
    repository_directories = java.type("java.util.LinkedHashSet")()
    logging.debug("Initializing python_file_contents and repository_directories")
    for path in glob.iglob(f"{base_path}/**/*.py", recursive=True):
        with open(path, "r") as file:
            python_file_contents.put(path, file.read())
            logging.debug(f"Read file: {path}")
        while 1:
            logging.debug(f"Processing repository directories for file: {path}")
            directory, _ = os.path.split(path)
            if directory in {"", os.sep}:
                break
            else:
                repository_directories.add(directory)
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
        for file_path, content in python_file_contents.entrySet():
            logging.debug(f"Processing file: {file_path}")
            self.process_file(file_path, content)

    def process_file(self, file_path, content):
        try:
            logging.debug(f"Parsed AST for file: {file_path}")
            tree = ast.parse(content)
            source_folder = os.path.dirname(file_path)
            generator = CodeGenerator(source_folder, file_path, content, self.uml_model)
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
        self.imported_types = []
        self.current_operation = None

    def visit_ClassDef(self, node):
        logging.debug(f"Visiting class definition: {node.name}")
        # Calculate package name from file path
        package_name = self.source_folder.replace(os.path.sep, '.')

        # Create location info for the class
        location = self._create_location_info(node, CodeElementType.TYPE_DECLARATION)

        # Create UML Class
        uml_class = UMLClass(
            package_name,
            node.name,
            location,
            True,  # is_package_member topLevel flag; adjust if needed
            self.imported_types
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
        logging.debug(f"Created operation {node.name}")
        # Handle magic methods (e.g., __init__, __str__)
        if node.name.startswith("__") and node.name.endswith("__"):
            operation.setVisibility(Visibility.PUBLIC)
            if node.name == "__init__":
                operation.setConstructor(True)
                logging.debug(f"Function {node.name} is a constructor")
        else:
            # Set visibility based on method name if not a magic method
            operation.setVisibility(self._determine_visibility(node.name))
        _is_classmethod = False
        # Set modifiers (i.e., isAbstract, isStatic) based on decorators
        for d in node.decorator_list:
            if self._is_static_decorator(d):
                # Set the operation as static if any decorator indicates it
                operation.setStatic(True)
                logging.debug(f"Function {node.name} is static")
            elif self._is_classmethod_decorator(d):
                # Set the operation as class method if any decorator indicates it
                operation.setStatic(True)
                _is_classmethod = True
                logging.debug(f"Function {node.name} is class method")
            elif self._is_abstract_decorator(d):
                # Set the operation as abstract if any decorator indicates it
                operation.setAbstract(True)
                logging.debug(f"Function {node.name} is abstract")

        logging.debug(f"Processing parameters and return type for function {node.name}")
        # Process parameters
        for param in node.args.args:
            logging.debug(f"Processing parameter: {param.arg}")
            if param.arg == "self" and not operation.isStatic():
                logging.debug(f'Skipping {param.arg} parameter for non static method as it corresponds to "this" keyword')
                continue
            if param.arg == "cls" and _is_classmethod:
                logging.debug(f'Skipping {param.arg} parameter for class method as it corresponds to "this" keyword in static context')
                continue
            param_name = param.arg
            if param.annotation:
                logging.debug(f"Parameter {param_name} has type hint: {param.annotation}. Parsing type...")
                param_type = self._parse_type(param.annotation)
            else:
                logging.debug(f"Parameter {param_name} has no type hint. Defaulting to Any")
                param_type = LeafType("Any")
            logging.debug(f"Adding parameter {param_name} to operation {node.name}")
            uml_param = UMLParameter(param_name, param_type, "in", False)
            operation.addParameter(uml_param)

        # Process return type
        if node.returns:
            logging.debug(f"Function {node.name} has return type hint: {node.returns}. Parsing return type...")
            return_type = self._parse_type(node.returns)
            return_param = UMLParameter("return", return_type, "return", False)
            operation.addParameter(return_param)

        self.current_class.addOperation(operation)
        self.current_operation = operation

        for body_node in node.body:
            self.visit(body_node)

        self.current_operation = None

    def visit_Import(self, node: ast.Import):
        logging.debug("Visiting import statement")
        for alias in node.names:
            location = self._create_location_info(node, CodeElementType.IMPORT_DECLARATION)
            logging.debug(f"Creating UMLImport: {alias.name}")
            uml_import = UMLImport(alias.name, alias.name == "*", False, location)
            logging.debug(f"Adding UMLImport to model: {uml_import}")
            self.imported_types.append(uml_import)

    def visit_ImportFrom(self, node):
        logging.debug(f"Visiting import from statement: {node.module}")
        module = node.module or ""
        for alias in node.names:
            full_name = f"{module}.{alias.name}" if module else alias.name
            location = self._create_location_info(node, CodeElementType.IMPORT_DECLARATION)
            uml_import = UMLImport(full_name, alias.name == "*", False, location)
            self.imported_types.append(uml_import)

    def visit_AnnAssign(self, node):
        logging.debug(f"Visiting annotated assignment: {node.target.id}")
        # Process annotated assignments as attributes if inside a class
        if isinstance(node.target, ast.Name) and self.current_class:
            attr_name = node.target.id
            logging.debug(f"Variable declaration {attr_name} has type hint: {node.annotation}. Parsing type...")
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
            end,
            end - start,
            node.lineno,
            node.col_offset,
            node.end_lineno,
            node.end_col_offset,
            end - start, # XXX: Not sure if this is correct (what is compilationUnitLength?)
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
            logging.debug(f"Parsed type is LeafType: {node.id}")
            return LeafType(node.id)
        elif isinstance(node, ast.Subscript):
            logging.debug(f"Parsed type is Subscript: {node.value}")
            base = self._parse_type(node.value)
            # Handle subscript slices that might be wrapped in an Index node in older Python versions.
            if hasattr(node, 'slice'):
                subscript = self._parse_type(node.slice)
            else:
                subscript = LeafType("Any")
            return CompositeType(base, subscript) #LeafType(f"{base.getClassType()}[{subscript.getClassType()}]")
        elif isinstance(node, ast.Attribute):
            qualified_name = self._parse_qualified_name(node)
            logging.debug(f"Parsed qualified name: {qualified_name}")
            return LeafType(qualified_name)
        return LeafType("Any")

    def _parse_qualified_name(self, node):
        if isinstance(node, ast.Name):
            return node.id
        elif isinstance(node, ast.Attribute):
            return f"{self._parse_qualified_name(node.value)}.{node.attr}"
        return "Unknown"

    @staticmethod
    def _determine_visibility(name):
        if name.startswith("__"):
            logging.debug(f"Determined visibility for {name}: PRIVATE")
            return Visibility.PRIVATE
        elif name.startswith("_"):
            logging.debug(f"Determined visibility for {name}: PROTECTED")
            return Visibility.PROTECTED
        logging.debug(f"Determined visibility for {name}: PUBLIC")
        return Visibility.PUBLIC

    @staticmethod
    def _is_abstract_decorator(decorator):
        # Only handle simple Name or Attribute nodes.
        if isinstance(decorator, ast.Name):
            logging.debug(f"Decorator {decorator.id} is abstractmethod: {decorator.id == 'abstractmethod'}")
            return decorator.id == "abstractmethod"
        elif isinstance(decorator, ast.Attribute):
            logging.debug(f"Decorator {decorator.attr} is abstractmethod: {decorator.attr == 'abstractmethod'}")
            return decorator.attr == "abstractmethod"
        return False

    @staticmethod
    def _is_static_decorator(decorator):
        # Only handle simple Name or Attribute nodes.
        if isinstance(decorator, ast.Name):
            logging.debug(f"Decorator {decorator.id} is staticmethod: {decorator.id == 'staticmethod'}")
            return decorator.id == 'staticmethod'
        elif isinstance(decorator, ast.Attribute):
            logging.debug(f"Decorator {decorator.attr} is staticmethod: {decorator.attr == 'staticmethod'}")
            return decorator.attr == 'staticmethod'
        return False

    @staticmethod
    def _is_classmethod_decorator(decorator):
        # Only handle simple Name or Attribute nodes.
        if isinstance(decorator, ast.Name):
            logging.debug(f"Decorator {decorator.id} is classmethod: {decorator.id == 'classmethod'}")
            return decorator.id == 'classmethod'
        elif isinstance(decorator, ast.Attribute):
            logging.debug(f"Decorator {decorator.attr} is classmethod: {decorator.attr == 'classmethod'}")
            return decorator.attr == 'classmethod'
        return False

    @staticmethod
    def _is_abc_metaclass(node):
        # Check for keyword arguments in class definition that define the metaclass.
        for kw in node.keywords:
            if kw.arg == 'metaclass' and isinstance(kw.value, ast.Name):
                return kw.value.id == "ABCMeta"
        return False

class SubMethodLevelCodeGenerator(ast.NodeVisitor):
    def __init__(self, source_folder, source_file, source_content):
        self.source_folder = source_folder
        self.source_file = source_file
        self.source_content = source_content
        self._line_offsets = None
        self.variable_declarations = []
        self.method_invocations = []
        self.ternary_expressions = []
        self.lambdas = []
        self.infix_expressions = []
        self.assignments = []
        self.current_operation = None

    def _get_offset(self, line, column):
        if self._line_offsets is None:
            self._line_offsets = []
            offset = 0
            for l in self.source_content.split('\n'):
                self._line_offsets.append(offset)
                offset += len(l) + 1
        if line - 1 >= len(self._line_offsets):
            return 0
        return self._line_offsets[line - 1] + column

    def _create_location_info(self, node, element_type):
        start = self._get_offset(node.lineno, node.col_offset)
        end = self._get_offset(node.end_lineno, node.end_col_offset)
        return LocationInfo(
            self.source_folder,
            self.source_file,
            start,
            end,
            end - start,
            node.lineno,
            node.col_offset,
            node.end_lineno,
            node.end_col_offset,
            end - start,
            element_type
        )

    def _get_node_source(self, node):
        start = self._get_offset(node.lineno, node.col_offset)
        end = self._get_offset(node.end_lineno, node.end_col_offset)
        return self.source_content[start:end]

    def visit_Assign(self, node):
        code_element_type = CodeElementType.ASSIGNMENT
        location_info = self._create_location_info(node, code_element_type)
        code_str = self._get_node_source(node)
        leaf_expr = LeafExpression(code_str, location_info)
        self.assignments.append(leaf_expr)
        self.generic_visit(node)

    def visit_Call(self, node):
        if isinstance(node.func, ast.Name) and node.func.id == 'isinstance':
            code_element_type = CodeElementType.INSTANCEOF_EXPRESSION
        else:
            code_element_type = CodeElementType.METHOD_INVOCATION
        location_info = self._create_location_info(node, code_element_type)
        code_str = self._get_node_source(node)
        leaf_expr = LeafExpression(code_str, location_info)
        self.method_invocations.append(leaf_expr)
        self.generic_visit(node)

    def visit_Lambda(self, node):
        code_element_type = CodeElementType.LAMBDA_EXPRESSION
        location_info = self._create_location_info(node, code_element_type)
        code_str = self._get_node_source(node)
        lambda_expr = LambdaExpressionObject(code_str, location_info)
        self.lambdas.append(lambda_expr)
        self.generic_visit(node)

    def visit_IfExp(self, node):
        code_element_type = CodeElementType.TERNARY_OPERATOR_EXPRESSION
        location_info = self._create_location_info(node, code_element_type)
        code_str = self._get_node_source(node)
        ternary_expr = TernaryOperatorExpression(code_str, location_info)
        self.ternary_expressions.append(ternary_expr)
        self.generic_visit(node)

    def visit_BinOp(self, node):
        code_element_type = CodeElementType.INFIX_EXPRESSION
        location_info = self._create_location_info(node, code_element_type)
        code_str = self._get_node_source(node)
        leaf_expr = LeafExpression(code_str, location_info)
        self.infix_expressions.append(leaf_expr)
        self.generic_visit(node)

    def visit_UnaryOp(self, node):
        code_element_type = CodeElementType.PREFIX_EXPRESSION
        location_info = self._create_location_info(node, code_element_type)
        code_str = self._get_node_source(node)
        leaf_expr = LeafExpression(code_str, location_info)
        self.assignments.append(leaf_expr)
        self.generic_visit(node)

    def visit_Attribute(self, node):
        code_element_type = CodeElementType.FIELD_ACCESS
        location_info = self._create_location_info(node, code_element_type)
        code_str = self._get_node_source(node)
        leaf_expr = LeafExpression(code_str, location_info)
        self.method_invocations.append(leaf_expr)
        self.generic_visit(node)

class CodeGenerator(UMLCodeGenerator, SubMethodLevelCodeGenerator):
    def __init__(self, source_folder, source_file, source_content, uml_model):
        UMLCodeGenerator.__init__(self, source_folder, source_file, source_content, uml_model)
        SubMethodLevelCodeGenerator.__init__(self, source_folder, source_file, source_content)
        self.uml_model = uml_model

    def visit(self, node):
        UMLCodeGenerator.visit(self, node)
        SubMethodLevelCodeGenerator.visit(self, node)