import ast
import os.path


class JavaTranspiler(ast.NodeTransformer):
    def __init__(self, filename):
        self.current_class = None
        self.filename = filename
        self.public_class = None
        self.abstract_classes = set()
        self.function_info = {}  # Stores modifiers/annotations per function

    def visit_ClassDef(self, node):
        self.current_class = node.name
        if self.filename.lower() == node.name.lower():
            self.public_class = node.name
        old_fn = set(self.function_info.keys())
        self.generic_visit(node)  # Process child nodes
        for k, v in self.function_info.items():
            if k not in old_fn and "abstract" in v.get('modifiers', []):
                self.abstract_classes.add(self.current_class)
                break
        self.current_class = None
        return node

    def visit_Name(self, node: ast.Name):
        if node.id.startswith('self'):
            node.id = node.id.replace('self', 'this')
        return node

    def visit_Attribute(self, node):
        match node.value:
            case ast.Call(func=ast.Name(id='super', ctx=ast.Load()), args=[], keywords=[]):
                node.value = ast.Name(id='super', ctx=ast.Load())
        self.generic_visit(node)
        return node

    def visit_Expr(self, node):
        match node.value:
            case ast.Call(func=ast.Name(id='super', ctx=ast.Load()), args=[], keywords=[]):
                node.value = ast.Name(id='super', ctx=ast.Load())
        self.generic_visit(node)
        return node

    def visit_FunctionDef(self, node):
        if not self.current_class:
            return node  # Skip non-class functions

        modifiers = []
        annotations = []
        is_classmethod = False

        # Process PEP 8 Descriptive Naming Styles
        if node.name.startswith('__'):
            if node.name.endswith('__'):
                if node.name == '__init__':
                    node.name = self.current_class
                elif node.name == '__del__':
                    node.name = 'finalize'
                elif node.name == '__str__':
                    node.name = 'toString'
                elif node.name == '__repr__':
                    node.name = 'toString'
                elif node.name == '__eq__':
                    node.name = 'equals'
                elif node.name == '__ne__':
                    node.name = 'equals'
                elif node.name == '__lt__':
                    node.name = 'compareTo'
                elif node.name == '__le__':
                    node.name = 'compareTo'
                elif node.name == '__gt__':
                    node.name = 'compareTo'
                elif node.name == '__ge__':
                    node.name = 'compareTo'
                modifiers.append('public')
            else:
                modifiers.append('private')
                node.name = node.name[2:]
        elif node.name.startswith('_'):
            modifiers.append('private')
            node.name = node.name[1:]
        else:
            modifiers.append('public')

        # Process decorators
        new_decorator_list = []
        for decorator in node.decorator_list:
            if isinstance(decorator, ast.Name):
                if decorator.id == 'abstractmethod':
                    modifiers.append('abstract')
                elif decorator.id == 'staticmethod':
                    modifiers.append('static')
                elif decorator.id == 'classmethod':
                    modifiers.append('static')
                    is_classmethod = True
                else:
                    annotations.append(decorator.id)
            else:
                annotations.append(self._unparse_decorator(decorator))
            new_decorator_list.append(decorator)

        # Remove special decorators from AST
        node.decorator_list = [
            d for d in node.decorator_list
            if not self._is_special_decorator(d)
        ]

        # Handle parameters
        if is_classmethod and node.args.args:
            node.args.args = node.args.args[1:]  # Remove 'cls'
        elif 'static' not in modifiers and node.args.args:
            if node.args.args[0].arg == 'self':
                node.args.args = node.args.args[1:]  # Remove 'self'

        # Store metadata for codegen
        self.function_info[node] = {
            'modifiers': modifiers,
            'annotations': annotations,
            'is_constructor': node.name == self.current_class
        }
        self.generic_visit(node)

        return node

    def _is_special_decorator(self, decorator):
        return (
                isinstance(decorator, ast.Name) and
                decorator.id in {'abstractmethod', 'staticmethod', 'classmethod'}
        )

    def _unparse_decorator(self, node):
        try:
            return ast.unparse(node)
        except:
            return 'UnknownAnnotation'


class JavaCodeGenerator(ast.NodeVisitor):
    def __init__(self, function_info, abstract_classes, public_class):
        self.code = []
        self.public_class = public_class
        self.abstract_classes = abstract_classes
        self.function_info = function_info
        self.current_class = None

    def visit_ClassDef(self, node):
        self.current_class = node.name
        modifiers = []
        if node.name == self.public_class:
            modifiers.append('public')
        if node.name in self.abstract_classes:
            modifiers.append('abstract')
        modifiers = ' '.join(modifiers) + ' ' if modifiers else ''
        self.code.append(f'{modifiers}class {node.name} {{')
        self.generic_visit(node)
        self.code.append('}\n')
        self.current_class = None

    def visit_FunctionDef(self, node):
        if not self.current_class:
            return

        info = self.function_info.get(node, {})
        modifiers = info.get('modifiers', [])
        annotations = info.get('annotations', [])
        is_constructor = info.get('is_constructor', False)

        # Annotations
        for ann in annotations:
            self.code.append(f'@{ann}')

        # Modifiers and signature
        modifier_str = ' '.join(modifiers)
        params = ', '.join([f'{arg.arg}' for arg in node.args.args])

        if is_constructor:
            signature = f'{modifier_str} {self.current_class}({params})'
        else:
            return_type = 'void'  # Simplified
            signature = f'{modifier_str} {return_type} {node.name}({params})'

        if "abstract" in modifiers:
            self.code.append(signature + ';')
        else:
            self.code.append(signature + ' {')
            self._handle_body(node.body)
            self.code.append('}\n')

    def _handle_body(self, body_nodes):
        for stmt in body_nodes:
            if isinstance(stmt, ast.Pass):
                continue
            elif isinstance(stmt, ast.Import):
                continue
            elif isinstance(stmt, ast.Assign):
                for idx, i in enumerate(stmt.targets):
                    match i:
                        case ast.Attribute(value=ast.Name(id='this')):
                            stmt.targets[idx] = ast.Name(id=i.attr, ctx=ast.Store())
                match stmt.targets:
                    case ast.Attribute(value=ast.Call(func=ast.Name(id='super', ctx=ast.Load()))):
                        stmt.value.value = ast.Name(id='super', ctx=ast.Load())
                    case ast.Attribute(value=ast.Name(id='self')):
                        stmt.value = ast.Name(id=stmt.value.attr, ctx=ast.Store())
                self.code.append(f'  var {ast.unparse(stmt)};')
            self.code.append(f'  {ast.unparse(stmt)};')

    def visit_Import(self, node):
        for alias in node.names:
            self.code.append(f'import {alias.name};')

    def visit_ImportFrom(self, node):
        base = node.module if node.module else ''
        for alias in node.names:
            self.code.append(f'import {base}.{alias.name};')


def transpile_python_to_java(input_path, output_path):
    with open(input_path) as f:
        source = f.read()

    tree = ast.parse(source)
    transpiler = JavaTranspiler(os.path.basename(output_path).replace('.java', ''))
    modified_tree = transpiler.visit(tree)
    ast.fix_missing_locations(modified_tree)

    generator = JavaCodeGenerator(transpiler.function_info, transpiler.abstract_classes, transpiler.public_class)
    generator.visit(modified_tree)

    with open(output_path, 'w') as f:
        f.write('\n'.join(generator.code))


if __name__ == '__main__':
    import glob, os
    print(os.getcwd())
    for i in glob.iglob('src/main/resources/python/example/**/*.py', recursive=True):
        transpile_python_to_java(i, i.replace("/python/", "/java/").replace(".py", ".java"))
