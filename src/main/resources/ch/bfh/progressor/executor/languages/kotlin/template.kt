@file:JvmName("Program") import java.math.BigDecimal; $CustomCode$

fun main(vararg args: String) {

	java.io.OutputStreamWriter(System.out, java.nio.charset.Charset.forName("UTF-8").newEncoder()).use { `out` ->
$TestCases$
	}
}

fun Float.hasMinimalDifference(other: Float): Boolean {

	if (!this.isFinite() || !other.isFinite()) return this == other
	if (this == other) return true
	return Math.abs(this - other) <= Math.ulp(this)
}

fun Double.hasMinimalDifference(other: Double): Boolean {

	if (!this.isFinite() || !other.isFinite()) return this == other
	if (this == other) return true
	return Math.abs(this - other) <= Math.ulp(this)
}
