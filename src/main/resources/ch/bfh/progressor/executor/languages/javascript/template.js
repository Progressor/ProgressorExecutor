'use strict'; $CustomCode$

$TestCases$

function hasMinimalDifference(value1, value2) { //source: https://gist.github.com/Yaffle/4654250
	function nextUp (x) {
		x = Number(x);
		if (x !== x) return x;
		if (x === -Number.POSITIVE_INFINITY) return -Number.MAX_VALUE;
		if (x === Number.POSITIVE_INFINITY) return Number.POSITIVE_INFINITY;
		if (x === +Number.MAX_VALUE) return Number.POSITIVE_INFINITY;
		let y = x * (x < 0 ? 1 - Number.EPSILON / 2 : 1 + Number.EPSILON);
		if (y === x) {
			let MIN_VALUE = Number.MIN_VALUE;
			if (MIN_VALUE === 0) MIN_VALUE = 2.2250738585072014e-308;
			if (5e-324 !== 0 && 5e-324 < MIN_VALUE) MIN_VALUE = 5e-324;
			y = x + MIN_VALUE;
		}
		if (y === Number.POSITIVE_INFINITY) y = Number.MAX_VALUE;
		const b = x + (y - x) / 2;
		if (x < b && b < y) y = b;
		const c = (y + x) / 2;
		if (x < c && c < y) y = c;
		return y === 0 ? -0 : y;
	}
	function ulp(x) {
		x = Number(x);
		return x < 0 ? nextUp(x) - x : x - (-nextUp(-x));
	}

	if (!Number.isFinite(value1) || !Number.isFinite(value2)) return value1 === value2;
	return value1 === value2 || Math.abs(value1 - value2) <= ulp(value1);
}

function hasSameElements(col1, col2) {
	if (col1 === col2 || !col1 && !col2) return true;
	if (col1 && col1 instanceof Array && col2 && col2 instanceof Array) {
		if (col1.length !== col2.length) return false;
		for (let i = 0, l = col1.length; i < l; i++)
			if (!hasSameElements(col1[i], col2[i])) return false;
		return true;

	} else if (col1 && col1 instanceof Set && col2 && col2 instanceof Set) {
		if (col1.size !== col2.size) return false;
		let elems2 = [...col2];
		for (let elem1 of col1)
			if (elems2.findIndex(elem2 => hasSameElements(elem1, elem2)) < 0) return false;
		return true;

	} else if (col1 && col1 instanceof Map && col2 && col2 instanceof Map) {
		if (col1.size !== col2.size) return false;
		let pairs2 = [...col2];
		for (let [key1, val1] of col1) {
			if (pairs2.findIndex(([key2, val2]) => hasSameElements(key1, key2) && hasSameElements(val1, val2)) < 0) return false;
		}
		return true;
	}
	return false;
}
