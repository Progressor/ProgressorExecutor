public class CustomClass {

	$CustomCode$

	public static void main(String[] args) {

		int index = Integer.parseInt(args[0]);

		CustomClass inst = new CustomClass();
		$TestCases$
	}

	private static void assertResult(String actual, String expected) { System.out.print(actual.equals(expected) ? "OK" : actual); }
	private static void assertResult(boolean actual, boolean expected) { System.out.print(actual == expected ? "OK" : String.valueOf(actual)); }
	private static void assertResult(char actual, char expected) { System.out.print(actual == expected ? "OK" : String.valueOf(actual)); }
	private static void assertResult(byte actual, byte expected) { System.out.print(actual == expected ? "OK" : String.valueOf(actual)); }
	private static void assertResult(short actual, short expected) { System.out.print(actual == expected ? "OK" : String.valueOf(actual)); }
	private static void assertResult(int actual, int expected) { System.out.print(actual == expected ? "OK" : String.valueOf(actual)); }
	private static void assertResult(long actual, long expected) { System.out.print(actual == expected ? "OK" : String.valueOf(actual)); }
	private static void assertResult(float actual, float expected) { System.out.print(actual == expected ? "OK" : String.valueOf(actual)); }
	private static void assertResult(double actual, double expected) { System.out.print(actual == expected ? "OK" : String.valueOf(actual)); }
}
