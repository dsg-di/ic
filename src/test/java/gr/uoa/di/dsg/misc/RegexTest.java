package gr.uoa.di.dsg.misc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;


public class RegexTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void test() {
		String pattern1 = "(d,";
		String pattern2 = ")";
		String text = "(d,qwewqeqwe)";

		Pattern p = Pattern.compile(Pattern.quote(pattern1) + "(.*?)" + Pattern.quote(pattern2));
		Matcher m = p.matcher(text);
		//while (m.find()) {
		m.find();
		System.out.println(m.group(1));
		//}
	}

}
