using System; using System.Collections.Generic; using System.Linq; using System.Text.RegularExpressions; class Program { $CustomCode$

	static void Main(params string[] args) {

		Console.OutputEncoding = System.Text.Encoding.GetEncoding("UTF-8");
		Program inst = new Program();
$TestCases$
	}

	public static bool HasMinimalDifference(float value1, float value2, int units) {

		if (float.IsNaN(value1) || float.IsInfinity(value1) || float.IsNaN(value2) || float.IsInfinity(value2)) return value1 == value2;
		if (value1 == value2) return true;

		int iValue1 = BitConverter.ToInt32(BitConverter.GetBytes(value1), 0);
		int iValue2 = BitConverter.ToInt32(BitConverter.GetBytes(value2), 0);

		if ((iValue1 >> 31) != (iValue2 >> 31)) return value1 == value2;
		return Math.Abs(iValue1 - iValue2) <= units;
	}

	public static bool HasMinimalDifference(double value1, double value2, int units) {

		if (double.IsNaN(value1) || double.IsInfinity(value1) || double.IsNaN(value2) || double.IsInfinity(value2)) return value1 == value2;
		if (value1 == value2) return true;

		long lValue1 = BitConverter.DoubleToInt64Bits(value1);
		long lValue2 = BitConverter.DoubleToInt64Bits(value2);

		if ((lValue1 >> 63) != (lValue2 >> 63)) return value1 == value2;
		return Math.Abs(lValue1 - lValue2) <= units;
	}
}
