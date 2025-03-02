import subprocess, os
from abc import ABCMeta, abstractmethod


class BuildSystem(metaclass=ABCMeta):
    def __init__(self, project_path: str):
        self.project_path = project_path

    def compile(self):
        self.execute("compile")

    def test(self):
        self.execute("test")

    def clean(self):
        self.execute("clean")

    @abstractmethod
    def detect_test_framework(self):
        pass

    @abstractmethod
    def execute(self, command):
        pass

    def run_build(self, build_tool, command):
        try:
            subprocess.Popen([build_tool, command], cwd=self.project_path, stdout=subprocess.PIPE,
                             stderr=subprocess.PIPE).communicate()
        except FileNotFoundError:
            print(f"'{build_tool}' not found in '{self.project_path}'")


class Maven(BuildSystem):
    def __init__(self, project_path):
        super().__init__(project_path)
        self.mvn = os.path.join(project_path, "mvnw") if os.path.exists(os.path.join(project_path, "mvnw")) else "mvn"

    def execute(self, command):
        super().run_build(self.mvn, command)

    def detect_test_framework(self):
        pass


class Gradle(BuildSystem):
    def __init__(self, project_path):
        super().__init__(project_path)
        self.gradle = os.path.join(self.project_path, "gradlew") if os.path.exists(
            os.path.join(project_path, "gradlew")) else "gradle"

    def compile(self):
        self.execute("build")

    def execute(self, command):
        super().run_build(self.gradle, command)

    def detect_test_framework(self):
        pass


class Compiler:
    def __init__(self, project_path: str):
        self.project_path = project_path

    def detect_build_system(self) -> list[BuildSystem]:
        import glob
        build_files = []
        for path in glob.iglob(os.path.join(self.project_path, "**", "build.gradle"), recursive=True):
            build_files.append(Gradle(os.path.dirname(path)))
        for path in glob.iglob(os.path.join(self.project_path, "**", "pom.xml"), recursive=True):
            build_files.append(Maven(os.path.dirname(path)))
        return build_files
