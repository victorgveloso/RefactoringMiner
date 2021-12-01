package gr.uom.java.xmi.diff;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Chunk;
import com.github.difflib.patch.Patch;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.regex.Pattern;

public class StringDistance {
	private static final Pattern COMMENT_LINE = Pattern.compile("^\\s*(//|\\*|import\\s).*");
	
	public static int editDistance(String a, String b, int threshold) {
		return new LevenshteinDistance(threshold).apply(a, b);
	}

	public static int editDistance(String a, String b) {
		return new LevenshteinDistance().apply(a, b);
	}

	public static boolean trivialCommentChange(String fileBefore, String fileAfter) throws IOException {
		if(fileBefore.length() == fileAfter.length()) {
			List<String> original = IOUtils.readLines(new StringReader(fileBefore));
			List<String> revised = IOUtils.readLines(new StringReader(fileAfter));

			Patch<String> patch = DiffUtils.diff(original, revised);
			List<AbstractDelta<String>> deltas = patch.getDeltas();
			for(AbstractDelta<String> delta : deltas) {
				Chunk<String> source = delta.getSource();
				if(source.getLines().size() > 0 && !source.getLines().get(0).isBlank() && !COMMENT_LINE.matcher(source.getLines().get(0)).matches()) {
					return false;
				}
				Chunk<String> target = delta.getTarget();
				if(target.getLines().size() > 0 && !target.getLines().get(0).isBlank() && !COMMENT_LINE.matcher(target.getLines().get(0)).matches()) {
					return false;
				}
			}
			return true;
		}
		else {
			List<String> original = IOUtils.readLines(new StringReader(fileBefore));
			List<String> revised = IOUtils.readLines(new StringReader(fileAfter));

			if(original.size() == revised.size()) {
				Patch<String> patch = DiffUtils.diff(original, revised);
				List<AbstractDelta<String>> deltas = patch.getDeltas();
				for(AbstractDelta<String> delta : deltas) {
					Chunk<String> source = delta.getSource();
					if(source.getLines().size() > 0 && !source.getLines().get(0).isBlank() && !COMMENT_LINE.matcher(source.getLines().get(0)).matches()) {
						return false;
					}
					Chunk<String> target = delta.getTarget();
					if(target.getLines().size() > 0 && !target.getLines().get(0).isBlank() && !COMMENT_LINE.matcher(target.getLines().get(0)).matches()) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}

	public static boolean isNumeric(String str) {
		for(char c : str.toCharArray()) {
			if(!Character.isDigit(c)) return false;
		}
		return true;
	}
}
