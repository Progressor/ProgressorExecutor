import java.math.BigDecimal; $CustomCode$

java.io.OutputStreamWriter(System.out, java.nio.charset.Charset.forName("UTF-8").newEncoder()).use { `out` ->
$TestCases$
}

fun Float?.hasMinimalDifference(other: Float?): Boolean {

	if (this === null || !this.isFinite() || other === null || !other.isFinite()) return this == other
	else if (this == other) return true
	else return Math.abs(this - other) <= Math.ulp(this)
}

fun Double?.hasMinimalDifference(other: Double?): Boolean {

	if (this === null || !this.isFinite() || other === null || !other.isFinite()) return this == other
	else if (this == other) return true
	else return Math.abs(this - other) <= Math.ulp(this)
}
