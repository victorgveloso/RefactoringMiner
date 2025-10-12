package extension.base.treesitter.python;

import extension.base.LangASTUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.io.IOException;

class HackyVisitorTest {
    private String code;

    @BeforeEach
    void setUp() {
        code = """
from typing import List, Dict, Generator


class MathContainer:
    def __init__(self, numbers: List[int]):
        self._numbers = numbers

    def __len__(self) -> int:
        return len(self._numbers)

    def __iter__(self):
        for n in self._numbers:
            yield n

    def __getitem__(self, index: int) -> int:
        return self._numbers[index]

    def __str__(self):
        return f"MathContainer({self._numbers})"

    def __repr__(self):
        return f"<MathContainer size={len(self)}>"

    @property
    def numbers(self) -> List[int]:
        return self._numbers

    @numbers.setter
    def numbers(self, new_numbers: List[int]):
        if not all(isinstance(x, int) for x in new_numbers):
            raise ValueError("All elements must be integers")
        self._numbers = new_numbers

    def squared(self) -> List[int]:
        return [n * n for n in self._numbers]

    def as_dict(self) -> Dict[int, int]:
        return {n: n * n for n in self._numbers}

    @staticmethod
    def is_even(n: int) -> bool:
        return n % 2 == 0

    @classmethod
    def from_range(cls, start: int, end: int):
        return cls(list(range(start, end)))


class FileGreeter(MathContainer):
    def __init__(self, numbers: List[int], name: str):
        super().__init__(numbers)
        self.name = name

    def greet(self) -> str:
        return f"Hello, {self.name}. You gave me {len(self)} numbers."

    def save_to_file(self, filename: str):
        try:
            with open(filename, "w") as f:
                for n in self:
                    f.write(f"{n}\\n")
        except OSError as e:
            print(f"File error: {e}")
        finally:
            print(f"Attempted to save numbers to {filename}")

    def generator_sum(self) -> Generator[int, None, None]:
        total = 0
        for n in self:
            total += n
            yield total


if __name__ == "__main__":
    # Create MathContainer from a range
    mc = MathContainer.from_range(1, 6)
    print(mc)
    print("Numbers squared:", mc.squared())
    print("As dict:", mc.as_dict())
    print("Even check on 3:", MathContainer.is_even(3))

    # Override numbers with setter
    mc.numbers = [10, 20, 30]
    print("New numbers:", mc.numbers)

    # Use subclass
    fg = FileGreeter([1, 2, 3, 4], "Victor")
    print(fg.greet())

    # Save to file
    fg.save_to_file("output.txt")

    # Generator in action
    print("Generator sum outputs:", list(fg.generator_sum()))

    # Exception handling demo
    try:
        mc.numbers = [1, "oops", 3]
    except ValueError as e:
        print("Caught error:", e)

    # Set operations
    unique_numbers = set(fg.numbers) | {5, 6, 7.5}
    if (t := isinstance(unique_numbers, set)) == True:
        pass
    elif (t := isinstance(unique_numbers, set)) == False:
        pass
    print("Unique set union:", unique_numbers)
                """;
    }

    @Test
    void build() throws IOException {
        HackyVisitor.visit(LangASTUtil.prepareTSNodeForTreeSitterPythonAST(code), new HackyPrinter());
    }

    @Test
    void buildPrinter() throws IOException {
        try (FileOutputStream fos = new FileOutputStream("ast.txt")) {
            HackyFileWriter hackyFileWriter = new HackyFileWriter(fos, code);
            HackyVisitor.visit(LangASTUtil.prepareTSNodeForTreeSitterPythonAST(code), hackyFileWriter);
        }
    }
}