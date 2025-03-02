import subprocess, os
from abc import ABCMeta, abstractmethod


class BuildSystem(metaclass=ABCMeta):
    def __init__(self, project_path: str):
        self.project_path = project_path

    @abstractmethod
    def build(self):
        pass

    @abstractmethod
    def test(self):
        pass

    @abstractmethod
    def detect_test_framework(self):
        pass

    @abstractmethod
    def clean(self):
        pass


class Maven(BuildSystem):
    def __init__(self, project_path):
        super().__init__(project_path)
        self.mvn = os.path.join(project_path, "mvnw") if os.path.exists(os.path.join(project_path, "mvnw")) else "mvn"

    def build(self):
        try:
            subprocess.Popen([self.mvn, "compile"], cwd=self.project_path, stdout=subprocess.PIPE,
                             stderr=subprocess.PIPE).communicate()
        except FileNotFoundError:
            print(f"'{self.mvn}' not found in '{self.project_path}'")

    def test(self):
        try:
            subprocess.Popen([self.mvn, "test"], cwd=self.project_path, stdout=subprocess.PIPE,
                             stderr=subprocess.PIPE).communicate()
        except FileNotFoundError:
            print(f"'{self.mvn}' not found in '{self.project_path}'")

    def clean(self):
        try:
            subprocess.Popen([self.mvn, "clean"], cwd=self.project_path, stdout=subprocess.PIPE,
                             stderr=subprocess.PIPE).communicate()
        except FileNotFoundError:
            print(f"'{self.mvn}' not found in '{self.project_path}'")

    def detect_test_framework(self):
        pass


class Gradle(BuildSystem):
    def __init__(self, project_path):
        super().__init__(project_path)
        self.gradle = os.path.join(self.project_path, "gradlew") if os.path.exists(
            os.path.join(project_path, "gradlew")) else "gradle"

    def build(self):
        try:
            subprocess.Popen([self.gradle, "build"], cwd=self.project_path, stdout=subprocess.PIPE,
                             stderr=subprocess.PIPE).communicate()
        except FileNotFoundError:
            print(f"'{self.gradle}' not found in '{self.project_path}'")

    def test(self):
        try:
            subprocess.Popen([self.gradle, "test"], cwd=self.project_path, stdout=subprocess.PIPE,
                             stderr=subprocess.PIPE).communicate()
        except FileNotFoundError:
            print(f"'{self.gradle}' not found in '{self.project_path}'")

    def clean(self):
        try:
            subprocess.Popen([self.gradle, "clean"], cwd=self.project_path, stdout=subprocess.PIPE,
                             stderr=subprocess.PIPE).communicate()
        except FileNotFoundError:
            print(f"'{self.gradle}' not found in '{self.project_path}'")

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
