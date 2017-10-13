import java.math.*; import java.util.*; import java.util.function.*; import java.util.stream.*; import java.util.regex.*; public class Program { $CustomCode$

	public static void main(String[] args) {

		try (java.io.OutputStreamWriter out = new java.io.OutputStreamWriter(System.out, java.nio.charset.Charset.forName("UTF-8").newEncoder())) {
			Program inst = new Program();
$TestCases$

		} catch (java.io.IOException ex) {
			throw new java.io.UncheckedIOException(ex);
		}
	}

	public static boolean hasMinimalDifference(float value1, float value2) {

		if (!Float.isFinite(value1) || !Float.isFinite(value2)) return value1 == value2;
		return value1 == value2 || Math.abs(value1 - value2) <= Math.ulp(value1);
	}

	public static boolean hasMinimalDifference(double value1, double value2) {

		if (!Double.isFinite(value1) || !Double.isFinite(value2)) return value1 == value2;
		return value1 == value2 || Math.abs(value1 - value2) <= Math.ulp(value1);
	}
}
